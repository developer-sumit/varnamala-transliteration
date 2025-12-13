package com.sumit.varnamala.data.ocr


enum class OcrEngine {
    /**
     * Google's ML Kit OCR engine - the primary engine used for OCR.
     */
    ML_KIT,

    /**
     * Tesseract OCR engine - offline open-source OCR
     */
    TESSERACT,

    CUSTOM
}