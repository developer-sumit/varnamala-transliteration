package com.sumit.varnamala.ocr.paddle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import com.paddle.ocr.EngineConfig
import com.paddle.ocr.PaddleOCR
import com.paddle.ocr.PaddleOCRConfig
import com.paddle.ocr.util.ImageEnhancer
import com.paddle.ocr.util.OpenCVUtils
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.OcrConfidence
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextElement
import com.sumit.varnamala.data.ocr.TextLine
import com.sumit.varnamala.ocr.ReadingOrder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * PP-OCRv6 adapter. Every detected line is kept with its confidence; lines the recognizer is
 * unsure about are flagged (shown greyed out by the UI) rather than dropped. Photos can be
 * cleaned up first: contrast boost, then rotation and de-skew when the first pass reads poorly.
 */
class PaddleOcrRepository(private val context: Context) {
    private var engine: PaddleOCR? = null
    private var loadedTier: PaddleModelProvisioner.Tier? = null
    // Live frames and still captures share this instance from different threads; switching
    // tiers releases the native engine, so runs must never overlap.
    private val engineLock = Mutex()

    private class Found(val quad: List<PointF>, val text: String, val confidence: Float) {
        val rect: Rect
            get() = Rect(
                quad.minOf { it.x }.toInt(), quad.minOf { it.y }.toInt(),
                quad.maxOf { it.x }.toInt(), quad.maxOf { it.y }.toInt(),
            )
        val score: Float get() = if (confidence >= OcrConfidence.LOW) confidence * text.length else 0f
    }

    /**
     * @param enhance photo clean-up for stills: contrast boost, and rotated / de-skewed retries
     *   when the upright pass reads poorly. Live frames leave it off to stay fast.
     */
    suspend fun processBitmap(
        bitmap: Bitmap,
        language: String? = null,
        quality: PaddleModelProvisioner.Tier = PaddleModelProvisioner.Tier.TINY,
        enhance: Boolean = false,
    ): OCRResult = engineLock.withLock {
        val ocr = engineFor(quality)
        var working = bitmap.downscaledForOcr()
        if (enhance) working = ImageEnhancer.boostContrast(working)
        val scaleX = bitmap.width.toFloat() / working.width
        val scaleY = bitmap.height.toFloat() / working.height

        var best = recognize(ocr, working, 0f)
        if (enhance) {
            for (degrees in retryAngles(best, working)) {
                val candidate = recognize(ocr, working, degrees)
                if (candidate.sumOf { it.score.toDouble() } > best.sumOf { it.score.toDouble() }) best = candidate
                // Stop at the first angle that reads well; each extra pass costs a full recognition run.
                if (reads(best)) break
            }
        }

        val rtl = language in RTL_LANGUAGES
        val lines = ReadingOrder.sort(best, rtl) { found -> found.rect.let { ReadingOrder.Box(it.left, it.top, it.right, it.bottom) } }.map { found ->
            val scaledQuad = found.quad.map { PointF(it.x * scaleX, it.y * scaleY) }
            val (orderedQuad, angle) = if (scaledQuad.size == 4) {
                val p0 = scaledQuad[0]
                val p1 = scaledQuad[1]
                val p2 = scaledQuad[2]
                val p3 = scaledQuad[3]
                val midLeft = PointF((p0.x + p3.x) / 2f, (p0.y + p3.y) / 2f)
                val midRight = PointF((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                val readingVec = PointF(midRight.x - midLeft.x, midRight.y - midLeft.y)
                val angleDeg = Math.toDegrees(atan2(readingVec.y.toDouble(), readingVec.x.toDouble())).toFloat()
                scaledQuad to angleDeg
            } else scaledQuad to 0f

            val rect = found.rect.let {
                Rect((it.left * scaleX).toInt(), (it.top * scaleY).toInt(), (it.right * scaleX).toInt(), (it.bottom * scaleY).toInt())
            }
            val element = TextElement(found.text, rect, found.confidence, orderedQuad, angle)
            TextLine(found.text, rect, listOf(element), found.confidence, orderedQuad, angle)
        }
        val blocks = lines.map { TextBlock(it.text, it.boundingBox, listOf(it), it.confidence, it.points, it.angle) }
        OCRResult(
            text = lines.filter { !it.isLowConfidence }.joinToString("\n") { it.text },
            language = language ?: "pp-ocrv6",
            boundingBoxes = blocks,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )
    }

    /** Loads the models ahead of time so the first hold-to-scan frame is not spent on setup. */
    suspend fun warmUp(quality: PaddleModelProvisioner.Tier = PaddleModelProvisioner.Tier.TINY) {
        engineLock.withLock { engineFor(quality) }
    }

    /** Runs the engine on [source] turned by [degrees]; results are mapped back to [source] coordinates. */
    private suspend fun recognize(ocr: PaddleOCR, source: Bitmap, degrees: Float): List<Found> {
        val (image, toSource) = rotated(source, degrees)
        val run = ocr.recognize(image)
        return run.results.mapNotNull { line ->
            val text = line.text.trim()
            if (text.isEmpty()) return@mapNotNull null
            val points = FloatArray(8)
            line.box.points.forEachIndexed { i, p -> points[i * 2] = p.x; points[i * 2 + 1] = p.y }
            toSource.mapPoints(points)
            Found(List(4) { PointF(points[it * 2], points[it * 2 + 1]) }, text, line.confidence)
        }
    }

    /** Extra angles worth trying after the upright pass: quarter turns for sideways text, or the measured skew. */
    private fun retryAngles(first: List<Found>, image: Bitmap): List<Float> {
        if (reads(first)) return emptyList()

        val tall = first.count { height(it.quad) > width(it.quad) * 1.5f }
        if (first.isEmpty() || tall * 2 >= first.size) return listOf(90f, 270f, 180f)

        val angles = first.map { skew(it.quad) }.sorted()
        val median = angles[angles.size / 2]
        return if (abs(median) in MIN_SKEW..MAX_SKEW) listOf(-median) else emptyList()
    }

    /** True when a pass found at least two confident lines and is confident on average. */
    private fun reads(found: List<Found>): Boolean {
        val readable = found.filter { it.confidence >= OcrConfidence.LOW }
        val mean = if (readable.isEmpty()) 0f else readable.map { it.confidence }.average().toFloat()
        return readable.size >= 2 && mean >= GOOD_CONFIDENCE
    }

    private fun skew(quad: List<PointF>): Float =
        Math.toDegrees(atan2((quad[1].y - quad[0].y).toDouble(), (quad[1].x - quad[0].x).toDouble())).toFloat()

    private fun width(q: List<PointF>) = hypot(q[1].x - q[0].x, q[1].y - q[0].y)
    private fun height(q: List<PointF>) = hypot(q[2].x - q[1].x, q[2].y - q[1].y)

    /** The rotated copy and the matrix that maps its points back onto [src]. */
    private fun rotated(src: Bitmap, degrees: Float): Pair<Bitmap, Matrix> {
        if (degrees == 0f) return src to Matrix()
        val forward = Matrix().apply { postRotate(degrees, src.width / 2f, src.height / 2f) }
        val corners = floatArrayOf(0f, 0f, src.width.toFloat(), 0f, src.width.toFloat(), src.height.toFloat(), 0f, src.height.toFloat())
        forward.mapPoints(corners)
        val minX = corners.filterIndexed { i, _ -> i % 2 == 0 }.min()
        val minY = corners.filterIndexed { i, _ -> i % 2 == 1 }.min()
        val maxX = corners.filterIndexed { i, _ -> i % 2 == 0 }.max()
        val maxY = corners.filterIndexed { i, _ -> i % 2 == 1 }.max()
        forward.postTranslate(-minX, -minY)
        val out = Bitmap.createBitmap((maxX - minX).toInt().coerceAtLeast(1), (maxY - minY).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(Color.WHITE)
            drawBitmap(src, forward, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val inverse = Matrix().also { forward.invert(it) }
        return out to inverse
    }

    private suspend fun engineFor(requested: PaddleModelProvisioner.Tier): PaddleOCR {
        val provisioner = PaddleModelProvisioner(context)
        // The large tier can be deleted from the model manager; photos then use the small one.
        val quality = if (provisioner.isReady(requested)) requested else PaddleModelProvisioner.Tier.TINY
        val paths = provisioner.paths(quality)
        require(listOf(paths.det, paths.rec, paths.config).all { it.exists() }) {
            "Required OCR resources are not available."
        }
        if (loadedTier != null && loadedTier != quality) {
            engine?.release()
            engine = null
        }
        check(OpenCVUtils.init(context)) { "Unable to initialize the OCR image runtime." }
        return engine ?: PaddleOCR.create(
            context = context,
            config = PaddleOCRConfig(
                // Use the detector's normal max-side resize policy. The previous 64px
                // minimum-side setup could feed very large camera frames unchanged.
                detLimitSideLen = if (quality == PaddleModelProvisioner.Tier.TINY) 640 else 960,
                detLimitType = "max",
                detThresh = 0.2f,
                detBoxThresh = 0.3f,
                // Keep unsure lines so the UI can grey them out; only discard near-certain noise.
                recScoreThresh = NOISE_FLOOR,
            ),
            engineConfig = EngineConfig(numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)),
            detModelAssetPath = "file://${paths.det.absolutePath}",
            recModelAssetPath = "file://${paths.rec.absolutePath}",
            recConfigAssetPath = "file://${paths.config.absolutePath}"
        ).also {
            engine = it
            loadedTier = quality
        }
    }

    suspend fun release() = engineLock.withLock { engine?.release(); engine = null; loadedTier = null }

    private fun Bitmap.downscaledForOcr(maxSide: Int = 2400): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= maxSide) return this
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
    }

    private companion object {
        const val NOISE_FLOOR = 0.15f
        const val GOOD_CONFIDENCE = 0.75f
        const val MIN_SKEW = 5f
        const val MAX_SKEW = 40f
        val RTL_LANGUAGES = setOf("urd", "ara", "fas", "pus", "snd", "uig", "heb", "yid", "div", "syr")
    }
}
