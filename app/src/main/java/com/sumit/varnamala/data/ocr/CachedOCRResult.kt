package com.sumit.varnamala.data.ocr

import android.graphics.Rect
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CachedOCRResult(
    val imageId: Long,
    val text: String,
    val confidence: Float,
    val detectedLanguage: String,
    val languageGroups: List<CachedLanguageGroup>,
    val processingTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class CachedLanguageGroup(
    val language: String,
    val confidence: Float,
    val elements: List<CachedTextElement>
) : Parcelable

@Parcelize
data class CachedTextElement(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect
) : Parcelable