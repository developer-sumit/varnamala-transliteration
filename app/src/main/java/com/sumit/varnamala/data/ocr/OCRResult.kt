package com.sumit.varnamala.data.ocr

import android.graphics.Rect
import androidx.compose.runtime.Immutable

@Immutable
data class OCRResult(
    val text: String,
    val confidence: Float,
    val language: String?,
    val boundingBoxes: List<TextBlock> = emptyList(),
    val processingTimeMs: Long = 0L
)

@Immutable
data class TextBlock(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect,
    val lines: List<TextLine> = emptyList()
)

@Immutable
data class TextLine(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect,
    val elements: List<TextElement> = emptyList()
)

@Immutable
data class TextElement(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect
)