package com.sumit.varnamala.ocr.tesseract

import android.content.Context
import java.io.File

object TesseractTrainedDataManager {
    private const val TESSDATA_DIR_NAME = "tessdata"

    /**
     * Returns the directory that should contain the `tessdata/` folder.
     * Tesseract expects `<dataPath>/tessdata/<lang>.traineddata`.
     */
    fun dataPathDir(context: Context): File {
        return context.noBackupFilesDir
    }

    fun tessdataDir(context: Context): File {
        return File(dataPathDir(context), TESSDATA_DIR_NAME)
    }
}
