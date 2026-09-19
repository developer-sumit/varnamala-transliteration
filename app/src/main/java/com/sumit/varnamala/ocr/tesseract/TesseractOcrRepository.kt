package com.sumit.varnamala.ocr.tesseract

import com.googlecode.tesseract.android.TessBaseAPI
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.OcrLanguageModel
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextElement
import com.sumit.varnamala.data.ocr.TextLine
import com.sumit.varnamala.models.TesseractModelStore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TesseractOcrRepository(private val context: Context) {
    suspend fun processUri(
        uri: Uri,
        model: OcrLanguageModel,
        pageSegMode: Int = TessBaseAPI.PageSegMode.PSM_AUTO
    ): OCRResult = withContext(Dispatchers.IO) {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalArgumentException("Unable to load image")
        processBitmap(bitmap, model, pageSegMode)
    }

    suspend fun processBitmap(
        bitmap: Bitmap,
        model: OcrLanguageModel,
        pageSegMode: Int = TessBaseAPI.PageSegMode.PSM_AUTO
    ): OCRResult = withContext(Dispatchers.IO) {
        require(TesseractModelStore.isModelDownloaded(context, model)) {
            "Download ${model.displayName} OCR model before scanning."
        }

        val api = TessBaseAPI()
        try {
            val initialized = api.init(
                TesseractTrainedDataManager.dataPathDir(context).absolutePath,
                model.code,
                TessBaseAPI.OEM_LSTM_ONLY
            )
            if (!initialized) {
                throw IllegalStateException("Unable to initialize Tesseract for ${model.displayName}.")
            }
            api.setPageSegMode(pageSegMode)
            api.setImage(bitmap)
            val text = api.getUTF8Text().orEmpty().trim()
            val block = buildTextBlock(api, text)

            OCRResult(
                text = text,
                language = model.code,
                boundingBoxes = block?.let { listOf(it) } ?: emptyList(),
                imageWidth = bitmap.width,
                imageHeight = bitmap.height
            )
        } finally {
            api.clear()
            api.recycle()
        }
    }

    private fun buildTextBlock(api: TessBaseAPI, fallbackText: String): TextBlock? {
        val iterator = api.resultIterator ?: return null
        val elements = mutableListOf<TextElement>()
        try {
            iterator.begin()
            do {
                val word = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                    ?.trim()
                    .orEmpty()
                val bounds = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                if (word.isNotBlank() && bounds != null && !bounds.isEmpty) {
                    elements.add(TextElement(word, bounds))
                }
            } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
        } finally {
            iterator.delete()
        }

        if (elements.isEmpty()) return null

        val lines = groupIntoLines(elements)
        val blockBounds = union(lines.map { it.boundingBox })
        return TextBlock(
            text = fallbackText.ifBlank { lines.joinToString("\n") { it.text } },
            boundingBox = blockBounds,
            lines = lines
        )
    }

    private fun groupIntoLines(elements: List<TextElement>): List<TextLine> {
        val sorted = elements.sortedWith(compareBy({ it.boundingBox.top }, { it.boundingBox.left }))
        val rows = mutableListOf<MutableList<TextElement>>()

        sorted.forEach { element ->
            val centerY = element.boundingBox.centerY()
            val row = rows.firstOrNull { existing ->
                val rowBounds = union(existing.map { it.boundingBox })
                val tolerance = maxOf(8, rowBounds.height() / 2)
                kotlin.math.abs(rowBounds.centerY() - centerY) <= tolerance
            }
            if (row == null) {
                rows.add(mutableListOf(element))
            } else {
                row.add(element)
            }
        }

        return rows.map { row ->
            val ordered = row.sortedBy { it.boundingBox.left }
            TextLine(
                text = ordered.joinToString(" ") { it.text },
                boundingBox = union(ordered.map { it.boundingBox }),
                elements = ordered
            )
        }
    }

    private fun union(rects: List<Rect>): Rect {
        val first = rects.firstOrNull() ?: return Rect()
        return rects.drop(1).fold(Rect(first)) { acc, rect ->
            acc.union(rect)
            acc
        }
    }
}

fun Bitmap.rotatedIfNeeded(rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return this
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
