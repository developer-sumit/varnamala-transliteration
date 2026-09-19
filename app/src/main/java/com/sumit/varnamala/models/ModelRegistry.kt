package com.sumit.varnamala.models

import com.sumit.varnamala.data.Language
import com.sumit.varnamala.data.ocr.OcrLanguageCatalog
import com.sumit.varnamala.data.ocr.OcrLanguageModel

/**
 * Central mapping of app languages to downloadable Tesseract traineddata.
 */
object ModelRegistry {
    data class Model(
        val tesseractLanguageCode: String,
        val fileName: String,
        val downloadUrl: String,
        val trainedDataPath: String = fileName
    )

    /**
     * Maps app/MLKit language codes to Tesseract traineddata codes.
     */
    fun toTesseractLanguageCode(appLanguageCode: String): String? {
        return OcrLanguageCatalog.find(appLanguageCode)?.code
    }

    fun forLanguage(language: Language): Model? = forAppLanguageCode(language.code)

    fun forOcrLanguage(language: OcrLanguageModel): Model {
        return Model(
            tesseractLanguageCode = language.code,
            fileName = language.fileName,
            downloadUrl = language.downloadUrl,
            trainedDataPath = language.trainedDataPath
        )
    }

    fun forAppLanguageCode(appLanguageCode: String): Model? {
        val language = OcrLanguageCatalog.find(appLanguageCode) ?: return null
        return forOcrLanguage(language)
    }
}
