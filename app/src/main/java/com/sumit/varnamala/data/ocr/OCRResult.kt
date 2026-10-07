package com.sumit.varnamala.data.ocr

import android.graphics.Rect
import androidx.compose.runtime.Immutable

/** Lines the recognizer scores below [LOW] are shown greyed out and left out of [OCRResult.text]. */
object OcrConfidence {
    const val LOW = 0.5f
}

@Immutable
data class OCRResult(
    val text: String,
    val language: String?,
    val boundingBoxes: List<TextBlock> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val imageRotation: Int = 0
) {
    /** Lines in reading order, including the unsure ones. */
    val allLines: List<TextLine> get() = boundingBoxes.flatMap { it.lines }
}

@Immutable
data class TextBlock(
    val text: String,
    val boundingBox: Rect,
    val lines: List<TextLine> = emptyList(),
    val confidence: Float = 1f,
) {
    val isLowConfidence: Boolean get() = confidence < OcrConfidence.LOW
}

@Immutable
data class TextLine(
    val text: String,
    val boundingBox: Rect,
    val elements: List<TextElement> = emptyList(),
    val confidence: Float = 1f,
) {
    val isLowConfidence: Boolean get() = confidence < OcrConfidence.LOW
}

@Immutable
data class TextElement(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float = 1f,
) {
    val isLowConfidence: Boolean get() = confidence < OcrConfidence.LOW
}