package com.sumit.varnamala.models

import com.sumit.varnamala.ocr.tesseract.TesseractTrainedDataManager

import java.io.File
import android.content.Context

object TesseractModelStore {
    fun tessdataDir(context: Context): File {
        val dir = TesseractTrainedDataManager.tessdataDir(context)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun legacyTessdataDir(context: Context): File {
        return File(context.filesDir, "tessdata")
    }

    fun modelFile(context: Context, tesseractLanguageCode: String): File {
        return File(tessdataDir(context), "$tesseractLanguageCode.traineddata")
    }

    private fun legacyModelFile(context: Context, tesseractLanguageCode: String): File {
        return File(legacyTessdataDir(context), "$tesseractLanguageCode.traineddata")
    }

    fun isModelDownloaded(context: Context, tesseractLanguageCode: String): Boolean {
        // One-time migration: if a model exists in the legacy location, move it.
        val legacy = legacyModelFile(context, tesseractLanguageCode)
        val current = modelFile(context, tesseractLanguageCode)
        if (!current.exists() && legacy.exists() && legacy.length() > 0L) {
            legacy.parentFile?.mkdirs()
            current.parentFile?.mkdirs()
            runCatching {
                legacy.copyTo(current, overwrite = true)
                legacy.delete()
            }
        }

        val file = modelFile(context, tesseractLanguageCode)
        return file.exists() && file.length() > 0L
    }

    fun deleteModel(context: Context, tesseractLanguageCode: String): Boolean {
        return modelFile(context, tesseractLanguageCode).delete()
    }
}
