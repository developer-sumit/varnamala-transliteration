package com.sumit.varnamala.ui.components

import com.sumit.varnamala.R
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextElement

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private data class OverlayTransform(val scale: Float, val offsetX: Float, val offsetY: Float)

private data class QuadScreenGeometry(
    val path: Path,
    val screenPoints: List<Offset>,
    val center: Offset,
    val width: Float,
    val height: Float,
    val angle: Float,
)

private fun computeQuadScreenGeometry(
    points: List<PointF>,
    fallbackRect: Rect,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): QuadScreenGeometry {
    if (points.size == 4) {
        val sp = points.map { p ->
            Offset(p.x * scale + offsetX, p.y * scale + offsetY)
        }
        val cx = (sp[0].x + sp[1].x + sp[2].x + sp[3].x) / 4f
        val cy = (sp[0].y + sp[1].y + sp[2].y + sp[3].y) / 4f
        val center = Offset(cx, cy)

        val cyclic = sp.sortedBy { p -> atan2((p.y - cy).toDouble(), (p.x - cx).toDouble()) }

        val path = Path().apply {
            moveTo(cyclic[0].x, cyclic[0].y)
            for (i in 1..3) {
                lineTo(cyclic[i].x, cyclic[i].y)
            }
            close()
        }

        val e0 = Offset(cyclic[1].x - cyclic[0].x, cyclic[1].y - cyclic[0].y)
        val l0 = hypot(e0.x, e0.y)

        val e1 = Offset(cyclic[2].x - cyclic[1].x, cyclic[2].y - cyclic[1].y)
        val l1 = hypot(e1.x, e1.y)

        val e2 = Offset(cyclic[3].x - cyclic[2].x, cyclic[3].y - cyclic[2].y)
        val l2 = hypot(e2.x, e2.y)

        val e3 = Offset(cyclic[0].x - cyclic[3].x, cyclic[0].y - cyclic[3].y)
        val l3 = hypot(e3.x, e3.y)

        val lenA = (l0 + l2) / 2f
        val lenB = (l1 + l3) / 2f

        val readingVec: Offset
        val boxWidth: Float
        val boxHeight: Float

        if (lenA >= lenB) {
            readingVec = Offset((e0.x - e2.x) / 2f, (e0.y - e2.y) / 2f)
            boxWidth = max(lenA, 1f)
            boxHeight = max(lenB, 1f)
        } else {
            readingVec = Offset((e1.x - e3.x) / 2f, (e1.y - e3.y) / 2f)
            boxWidth = max(lenB, 1f)
            boxHeight = max(lenA, 1f)
        }

        val forwardVec = if (abs(readingVec.x) >= abs(readingVec.y)) {
            if (readingVec.x < 0) Offset(-readingVec.x, -readingVec.y) else readingVec
        } else {
            if (readingVec.y < 0) Offset(-readingVec.x, -readingVec.y) else readingVec
        }

        val angle = Math.toDegrees(atan2(forwardVec.y.toDouble(), forwardVec.x.toDouble())).toFloat()

        return QuadScreenGeometry(
            path = path,
            screenPoints = cyclic,
            center = center,
            width = boxWidth,
            height = boxHeight,
            angle = angle
        )
    } else {
        val left = fallbackRect.left * scale + offsetX
        val top = fallbackRect.top * scale + offsetY
        val right = fallbackRect.right * scale + offsetX
        val bottom = fallbackRect.bottom * scale + offsetY

        val w = max(abs(right - left), 1f)
        val h = max(abs(bottom - top), 1f)
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val center = Offset(cx, cy)

        val sp = listOf(
            Offset(left, top),
            Offset(right, top),
            Offset(right, bottom),
            Offset(left, bottom)
        )

        val path = Path().apply {
            moveTo(left, top)
            lineTo(right, top)
            lineTo(right, bottom)
            lineTo(left, bottom)
            close()
        }

        return QuadScreenGeometry(
            path = path,
            screenPoints = sp,
            center = center,
            width = w,
            height = h,
            angle = 0f
        )
    }
}

private fun overlayTransform(
    result: OCRResult,
    containerSize: IntSize,
    cropToFill: Boolean,
): OverlayTransform {
    val xScale = containerSize.width.toFloat() / result.imageWidth
    val yScale = containerSize.height.toFloat() / result.imageHeight
    val scale = if (cropToFill) max(xScale, yScale) else min(xScale, yScale)
    return OverlayTransform(
        scale = scale,
        offsetX = (containerSize.width - result.imageWidth * scale) / 2f,
        offsetY = (containerSize.height - result.imageHeight * scale) / 2f,
    )
}

private fun isPointInPolygon(p: Offset, poly: List<Offset>): Boolean {
    if (poly.size < 3) return false
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val pi = poly[i]
        val pj = poly[j]
        if ((pi.y > p.y) != (pj.y > p.y) &&
            p.x < (pj.x - pi.x) * (p.y - pi.y) / (pj.y - pi.y) + pi.x
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

private fun fractionAlongQuad(tap: Offset, center: Offset, width: Float, angle: Float): Float {
    if (width <= 0f) return 0.5f
    val rad = Math.toRadians(-angle.toDouble())
    val cosA = kotlin.math.cos(rad).toFloat()
    val sinA = kotlin.math.sin(rad).toFloat()
    val dx = tap.x - center.x
    val dy = tap.y - center.y
    val localX = dx * cosA - dy * sinA
    return ((localX + width / 2f) / width).coerceIn(0f, 1f)
}

private data class ElementHit(
    val element: TextElement,
    val displayText: String,
    val geom: QuadScreenGeometry
)

private fun findElementAt(
    tap: Offset,
    result: OCRResult,
    textOverride: Map<Rect, String>?,
    transform: OverlayTransform
): ElementHit? {
    result.boundingBoxes.forEach { block ->
        block.lines.forEach { line ->
            line.elements.forEach { element ->
                val geom = computeQuadScreenGeometry(
                    points = element.points,
                    fallbackRect = element.boundingBox,
                    scale = transform.scale,
                    offsetX = transform.offsetX,
                    offsetY = transform.offsetY
                )
                if (isPointInPolygon(tap, geom.screenPoints)) {
                    val converted = if (element.isLowConfidence) null else textOverride?.get(element.boundingBox)
                    val displayText = if (!converted.isNullOrBlank() && converted != element.text) converted else element.text
                    return ElementHit(element, displayText, geom)
                }
            }
        }
    }
    return null
}

@Composable
fun OCROverlayCanvas(
    result: OCRResult?,
    textOverride: Map<Rect, String>? = null,
    containerSize: IntSize,
    cropToFill: Boolean = false,
    sourceBitmap: Bitmap? = null,
    modifier: Modifier = Modifier,
    // Display settings
    showBoundingBoxes: Boolean = true,
    showTextOverlay: Boolean = true,
    showWordBoundingBoxes: Boolean = false,
    showLineBoundingBoxes: Boolean = true,
    showBlockBoundingBoxes: Boolean = true,
    textBackgroundOpacity: Float = 0.8f,
    boundingBoxStrokeWidth: Float = 2f,
    // Colors
    blockBoundingBoxColor: Color = Color.Blue,
    lineBoundingBoxColor: Color = Color.Green,
    wordBoundingBoxColor: Color = Color.Red,
    textColor: Color = Color.White,
    textBackgroundColor: Color = Color.Black.copy(alpha = 0.85f),
    // Interaction
    enableTextCopy: Boolean = true,
    /** Called with the tapped word (or whole line when it is a single word). Long-press still copies. */
    onWordTap: ((String) -> Unit)? = null,
    /** Black-and-yellow rendering with solid backgrounds, for low vision and bright sunlight. */
    highContrast: Boolean = false,
) {
    if (result == null || containerSize == IntSize.Zero || result.imageWidth <= 0 || result.imageHeight <= 0) return

    val context = LocalContext.current
    var copiedConfirmationText by remember { mutableStateOf<String?>(null) }

    val transform = remember(result, containerSize, cropToFill) {
        overlayTransform(result, containerSize, cropToFill)
    }

    val accessibilityText = remember(result, textOverride) {
        result.boundingBoxes.flatMap { it.lines }.flatMap { it.elements }.joinToString(" ") { element ->
            val converted = if (element.isLowConfidence) null else textOverride?.get(element.boundingBox)
            if (!converted.isNullOrBlank() && converted != element.text) converted else element.text
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = accessibilityText }
                .pointerInput(result, onWordTap, enableTextCopy, textOverride) {
                    if (enableTextCopy || onWordTap != null) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val hit = findElementAt(tapOffset, result, textOverride, transform)
                                if (hit != null) {
                                    if (onWordTap != null) {
                                        val frac = fractionAlongQuad(tapOffset, hit.geom.center, hit.geom.width, hit.geom.angle)
                                        onWordTap(wordAt(hit.element.text, frac))
                                    } else if (enableTextCopy) {
                                        copyTextToClipboard(context, hit.displayText)
                                        copiedConfirmationText = hit.displayText
                                    }
                                }
                            },
                            onLongPress = { tapOffset ->
                                if (enableTextCopy) {
                                    val hit = findElementAt(tapOffset, result, textOverride, transform)
                                    if (hit != null) {
                                        copyTextToClipboard(context, hit.displayText)
                                        copiedConfirmationText = hit.displayText
                                    }
                                }
                            }
                        )
                    }
                }
        ) {
            val strokePx = max(2f, (if (highContrast) boundingBoxStrokeWidth * 2f else boundingBoxStrokeWidth) * density)

            // Step 1: Background replacement patches (drawn at bottom so camera pixels under text are covered)
            if (showTextOverlay) {
                result.boundingBoxes.forEach { block ->
                    block.lines.forEach { line ->
                        line.elements.forEach { element ->
                            val unsure = element.isLowConfidence
                            val replacementColor = when {
                                highContrast -> Color.Black
                                unsure -> Color.DarkGray.copy(alpha = 0.7f)
                                else -> sourceBitmap?.sampleBackgroundColor(element.boundingBox)
                                    ?: textBackgroundColor.copy(alpha = textBackgroundOpacity)
                            }

                            val geom = computeQuadScreenGeometry(
                                points = element.points,
                                fallbackRect = element.boundingBox,
                                scale = transform.scale,
                                offsetX = transform.offsetX,
                                offsetY = transform.offsetY
                            )

                            drawPath(
                                path = geom.path,
                                color = replacementColor
                            )
                        }
                    }
                }
            }

            // Step 2: Bounding box outlines
            if (showBoundingBoxes) {
                val blockColorToUse = if (highContrast) Color.Yellow else blockBoundingBoxColor
                val lineColorToUse = if (highContrast) Color.Yellow else lineBoundingBoxColor
                val wordColorToUse = if (highContrast) Color.Yellow else wordBoundingBoxColor

                result.boundingBoxes.forEach { block ->
                    val unsure = block.isLowConfidence
                    val bColor = if (unsure) Color.Gray else blockColorToUse
                    val lColor = if (unsure) Color.Gray else lineColorToUse

                    if (showBlockBoundingBoxes) {
                        val geom = computeQuadScreenGeometry(
                            points = block.points,
                            fallbackRect = block.boundingBox,
                            scale = transform.scale,
                            offsetX = transform.offsetX,
                            offsetY = transform.offsetY
                        )
                        if (textBackgroundOpacity > 0f) {
                            drawPath(
                                path = geom.path,
                                color = bColor.copy(alpha = textBackgroundOpacity * 0.2f)
                            )
                        }
                        drawPath(
                            path = geom.path,
                            color = bColor,
                            style = Stroke(width = strokePx)
                        )
                    }

                    if (showLineBoundingBoxes) {
                        block.lines.forEach { line ->
                            val geom = computeQuadScreenGeometry(
                                points = line.points,
                                fallbackRect = line.boundingBox,
                                scale = transform.scale,
                                offsetX = transform.offsetX,
                                offsetY = transform.offsetY
                            )
                            if (textBackgroundOpacity > 0f) {
                                drawPath(
                                    path = geom.path,
                                    color = lColor.copy(alpha = textBackgroundOpacity * 0.15f)
                                )
                            }
                            drawPath(
                                path = geom.path,
                                color = lColor,
                                style = Stroke(width = strokePx)
                            )
                        }
                    }

                    if (showWordBoundingBoxes) {
                        block.lines.forEach { line ->
                            line.elements.forEach { element ->
                                val geom = computeQuadScreenGeometry(
                                    points = element.points,
                                    fallbackRect = element.boundingBox,
                                    scale = transform.scale,
                                    offsetX = transform.offsetX,
                                    offsetY = transform.offsetY
                                )
                                drawPath(
                                    path = geom.path,
                                    color = wordColorToUse,
                                    style = Stroke(width = strokePx)
                                )
                            }
                        }
                    }
                }
            }

            // Step 3: Text overlay drawn directly on nativeCanvas with matching quad transform
            if (showTextOverlay) {
                val textPaint = TextPaint().apply {
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }

                result.boundingBoxes.forEach { block ->
                    block.lines.forEach { line ->
                        line.elements.forEach { element ->
                            val unsure = element.isLowConfidence
                            val converted = if (unsure) null else textOverride?.get(element.boundingBox)
                            val displayText = if (!converted.isNullOrBlank() && converted != element.text) converted else element.text

                            if (displayText.isNotBlank()) {
                                val geom = computeQuadScreenGeometry(
                                    points = element.points,
                                    fallbackRect = element.boundingBox,
                                    scale = transform.scale,
                                    offsetX = transform.offsetX,
                                    offsetY = transform.offsetY
                                )

                                val targetH = max(8f, geom.height * 0.82f)
                                val targetW = max(5f, geom.width * 0.96f)

                                textPaint.textSize = targetH
                                textPaint.color = when {
                                    highContrast -> android.graphics.Color.YELLOW
                                    unsure -> android.graphics.Color.LTGRAY
                                    else -> textColor.toArgb()
                                }
                                textPaint.setShadowLayer(
                                    3f,
                                    1f,
                                    1f,
                                    android.graphics.Color.BLACK
                                )

                                val measuredW = textPaint.measureText(displayText)
                                if (measuredW > targetW && measuredW > 0f) {
                                    val scaleFactor = targetW / measuredW
                                    textPaint.textSize = max(3.5f, targetH * scaleFactor)
                                }

                                val fontMetrics = textPaint.fontMetrics
                                val baselineY = -(fontMetrics.ascent + fontMetrics.descent) / 2f

                                drawContext.canvas.nativeCanvas.apply {
                                    save()
                                    translate(geom.center.x, geom.center.y)
                                    rotate(geom.angle)
                                    drawText(displayText, 0f, baselineY, textPaint)
                                    restore()
                                }
                            }
                        }
                    }
                }
            }
        }

        // Show copy confirmation toast when an element is tapped/copied
        copiedConfirmationText?.let { text ->
            LaunchedEffect(text) {
                kotlinx.coroutines.delay(1000)
                copiedConfirmationText = null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = stringResource(R.string.copied),
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "\"$text\" copied!",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/** The word under a tap at [fraction] (0..1) across a line, estimating word widths by letter count. */
internal fun wordAt(line: String, fraction: Float): String {
    val words = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.size <= 1) return line.trim()
    val total = words.sumOf { it.length + 1 }.toFloat()
    var edge = 0f
    for (word in words) {
        edge += (word.length + 1) / total
        if (fraction <= edge) return word
    }
    return words.last()
}

/**
 * Copy text to clipboard with feedback
 */
private fun copyTextToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clipData = ClipData.newPlainText(context.getString(R.string.ocr_text), text)
    clipboardManager?.setPrimaryClip(clipData)
}

private fun Bitmap.sampleBackgroundColor(rect: Rect): Color {
    val left = rect.left.coerceIn(0, width - 1)
    val top = rect.top.coerceIn(0, height - 1)
    val right = rect.right.coerceIn(left + 1, width)
    val bottom = rect.bottom.coerceIn(top + 1, height)
    val insetX = max(1, (right - left) / 8)
    val insetY = max(1, (bottom - top) / 8)
    val samples = mutableListOf<Int>()

    fun addSample(x: Int, y: Int) {
        samples.add(getPixel(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1)))
    }

    val sampleXs = listOf(left, left + insetX, right - insetX, right - 1)
    val sampleYs = listOf(top, top + insetY, bottom - insetY, bottom - 1)
    sampleXs.forEach { x ->
        addSample(x, top)
        addSample(x, bottom - 1)
    }
    sampleYs.forEach { y ->
        addSample(left, y)
        addSample(right - 1, y)
    }

    if (samples.isEmpty()) return Color.Black.copy(alpha = 0.72f)

    var red = 0
    var green = 0
    var blue = 0
    samples.forEach { color ->
        red += android.graphics.Color.red(color)
        green += android.graphics.Color.green(color)
        blue += android.graphics.Color.blue(color)
    }
    val count = samples.size
    return Color(red / count, green / count, blue / count).copy(alpha = 0.94f)
}
