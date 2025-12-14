package com.sumit.varnamala.data.ocr

import android.graphics.Rect
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CachedOCRResult(
    val imageId: Long,
    val text: String,
    val detectedLanguage: String,
    val languageGroups: List<CachedLanguageGroup>,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class CachedLanguageGroup(
    val language: String,
    val elements: List<CachedTextElement>
) : Parcelable

@Parcelize
data class CachedTextElement(
    val text: String,
    val boundingBox: Rect
) : Parcelable
