package com.sumit.varnamala.models

import com.sumit.varnamala.data.Language

/**
 * Central mapping of app languages to downloadable Tesseract traineddata.
 */
object ModelRegistry {
    private const val BASE_URL = "https://github.com/tesseract-ocr/tessdata_best/raw/main/"

    data class Model(
        val tesseractLanguageCode: String,
        val fileName: String,
        val downloadUrl: String
    )

    /**
     * Maps app/MLKit language codes to Tesseract traineddata codes.
     */
    fun toTesseractLanguageCode(appLanguageCode: String): String? {
        return when (appLanguageCode) {
            // App uses 2-letter codes for these.
            "en" -> "eng"
            "hi" -> "hin"
            "asm", "ben", "guj", "kan", "mal", "mar", "ori", "pan", "tam", "tel" -> appLanguageCode
            "eng", "hin" -> appLanguageCode
            else -> null
        }
    }

    fun forLanguage(language: Language): Model? = forAppLanguageCode(language.code)

    fun forAppLanguageCode(appLanguageCode: String): Model? {
        val tessCode = toTesseractLanguageCode(appLanguageCode) ?: return null
        val fileName = "$tessCode.traineddata"
        return Model(
            tesseractLanguageCode = tessCode,
            fileName = fileName,
            downloadUrl = BASE_URL + fileName
        )
    }
}
