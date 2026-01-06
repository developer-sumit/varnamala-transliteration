package com.sumit.varnamala.ocr.tesseract

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object TesseractTrainedDataManager {
    private const val TESSDATA_DIR_NAME = "tessdata"
    private const val BASE_URL = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/main/"

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

    fun trainedDataFile(context: Context, languageCode: String): File {
        return File(tessdataDir(context), "$languageCode.traineddata")
    }

    suspend fun ensureTrainedData(
        context: Context,
        languageCode: String
    ): File = withContext(Dispatchers.IO) {
        val outFile = trainedDataFile(context, languageCode)
        if (outFile.exists() && outFile.length() > 0) {
            return@withContext outFile
        }

        val tessdataDir = tessdataDir(context)
        if (!tessdataDir.exists()) {
            tessdataDir.mkdirs()
        }

        val tmpFile = File(tessdataDir, "$languageCode.traineddata.download")
        if (tmpFile.exists()) {
            tmpFile.delete()
        }

        val url = URL("$BASE_URL$languageCode.traineddata")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Varnamala")
        }

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }

            connection.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (tmpFile.length() <= 0) {
                throw IllegalStateException("Downloaded file is empty")
            }

            if (outFile.exists()) {
                outFile.delete()
            }

            if (!tmpFile.renameTo(outFile)) {
                // Fallback if rename fails (e.g. cross-device)
                tmpFile.copyTo(outFile, overwrite = true)
                tmpFile.delete()
            }

            return@withContext outFile
        } finally {
            connection.disconnect()
        }
    }
}
