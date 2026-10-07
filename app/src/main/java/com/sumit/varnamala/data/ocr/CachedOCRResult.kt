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
    val timestamp: Long = System.currentTimeMillis(),
    /** Size of the picture the boxes were measured on (after rotation is applied). 0 for older entries. */
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
) : Parcelable

@Parcelize
data class CachedLanguageGroup(
    val language: String,
    val elements: List<CachedTextElement>
) : Parcelable

@Parcelize
data class CachedTextElement(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float = 1f,
    /** Position in reading order, so a cached result comes back in the order it was read. */
    val order: Int = 0,
) : Parcelable
