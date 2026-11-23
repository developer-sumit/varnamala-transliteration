package com.sumit.varnamala.data

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.sumit.varnamala.data.ocr.OCRResult

/**
 * Session state holder for TextInputScreen to preserve user typed text and
 * transliterated results across tab switching, screen navigation, and language changes.
 */
object TextInputSession {
    var inputText by mutableStateOf("")
    var transliteratedText by mutableStateOf("")
    var lastSavedHistoryKey: String? = null

    fun clear() {
        inputText = ""
        transliteratedText = ""
        lastSavedHistoryKey = null
    }
}

/**
 * Session state holder for CameraScreen to preserve captured photos, OCR text,
 * bounding boxes, and zoom/pan state across screen navigation and language changes.
 */
object CameraSession {
    var capturedBitmap by mutableStateOf<Bitmap?>(null)
    var capturedDisplaySize by mutableStateOf(IntSize.Zero)
    var capturedImageProcessing by mutableStateOf(false)
    var capturedPhotoScale by mutableFloatStateOf(1f)
    var capturedPhotoOffset by mutableStateOf(Offset.Zero)
    var rawOcrText by mutableStateOf("")
    var detectedLanguage by mutableStateOf("unknown")
    var lastOCRResult by mutableStateOf<OCRResult?>(null)
    var lastSavedHistoryKey: String? = null

    fun clearCaptured() {
        capturedBitmap = null
        capturedDisplaySize = IntSize.Zero
        capturedImageProcessing = false
        capturedPhotoScale = 1f
        capturedPhotoOffset = Offset.Zero
        rawOcrText = ""
        detectedLanguage = "unknown"
        lastOCRResult = null
        lastSavedHistoryKey = null
    }
}
