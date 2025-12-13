package com.sumit.varnamala.ocr.tesseract

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextLine
import com.sumit.varnamala.data.ocr.TextElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Repository for Tesseract OCR operations
 * Handles offline text recognition using Tesseract engine
 */
class TesseractOCRRepository(private val context: Context) {

    companion object {
        private const val TESSDATA_FOLDER = "tessdata"
        private const val TESSERACT_DATA_PATH_KEY = "tesseract_data_path"
        
        // Language code mappings for Tesseract
        val TESSERACT_LANGUAGE_MAP = mapOf(
            "en" to "eng",
            "asm" to "asm",
            "ben" to "ben",
            "guj" to "guj",
            "hi" to "hin",
            "kan" to "kan",
            "mal" to "mal",
            "mar" to "mar",
            "ori" to "ori",
            "pan" to "pan",
            "tam" to "tam",
            "tel" to "tel"
        )
    }

    private var tessBaseAPI: TessBaseAPI? = null
    private var currentLanguage: String? = null
    private val dataPath: String = context.filesDir.absolutePath + "/"

    /**
     * Initialize Tesseract with specified language
     * @param languageCode ISO language code (e.g., "en", "hi", "te")
     * @return true if initialization successful
     */
    suspend fun initialize(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val tessLanguageCode = TESSERACT_LANGUAGE_MAP[languageCode] ?: "eng"
            
            // Check if language data exists
            if (!checkLanguageDataExists(tessLanguageCode)) {
                return@withContext false
            }

            // Initialize if language changed or first time
            if (currentLanguage != tessLanguageCode || tessBaseAPI == null) {
                tessBaseAPI?.recycle()
                
                tessBaseAPI = TessBaseAPI().apply {
                    init(dataPath, tessLanguageCode)
                    setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                }
                currentLanguage = tessLanguageCode
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Process image from URI and extract text
     */
    suspend fun processImageFromUri(uri: Uri, languageCode: String): OCRResult =
        withContext(Dispatchers.IO) {
            try {
                if (!initialize(languageCode)) {
                    return@withContext OCRResult(
                        text = "",
                        confidence = 0f,
                        language = languageCode,
                        boundingBoxes = emptyList(),
                        imageWidth = 0,
                        imageHeight = 0
                    )
                }

                val bitmap = loadBitmapFromUri(uri)
                    ?: return@withContext OCRResult(
                        text = "",
                        confidence = 0f,
                        language = languageCode,
                        boundingBoxes = emptyList(),
                        imageWidth = 0,
                        imageHeight = 0
                    )

                processImageWithTesseract(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                OCRResult(
                    text = "",
                    confidence = 0f,
                    language = null,
                    boundingBoxes = emptyList(),
                    imageWidth = 0,
                    imageHeight = 0
                )
            }
        }

    /**
     * Process bitmap with Tesseract OCR
     */
    private fun processImageWithTesseract(bitmap: Bitmap): OCRResult {
        return try {
            tessBaseAPI?.setImage(bitmap)
            val recognizedText = tessBaseAPI?.utF8Text ?: ""
            val meanConfidence = tessBaseAPI?.meanConfidence()?.toFloat() ?: 0f
            
            // Get detailed results with bounding boxes
            val hocrText = tessBaseAPI?.getHOCRText(0) ?: ""
            val blocks = parseHOCRText(hocrText, meanConfidence)

            OCRResult(
                text = recognizedText,
                confidence = meanConfidence / 100f,
                language = currentLanguage,
                boundingBoxes = blocks,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height
            )
        } catch (e: Exception) {
            e.printStackTrace()
            OCRResult(
                text = "",
                confidence = 0f,
                language = currentLanguage,
                boundingBoxes = emptyList(),
                imageWidth = bitmap.width,
                imageHeight = bitmap.height
            )
        }
    }

    /**
     * Parse HOCR format to extract bounding boxes
     * This is a simplified parser - for production, consider using an XML parser
     */
    private fun parseHOCRText(hocr: String, confidence: Float): List<TextBlock> {
        // Simplified parsing - returns blocks with basic structure
        // In production, you'd want to parse the full HOCR XML format
        return listOf(
            TextBlock(
                text = tessBaseAPI?.utF8Text ?: "",
                confidence = confidence / 100f,
                boundingBox = android.graphics.Rect(0, 0, 100, 100),
                lines = emptyList()
            )
        )
    }

    /**
     * Load bitmap from URI
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Check if language data file exists
     */
    private fun checkLanguageDataExists(tessLanguageCode: String): Boolean {
        val tessDataDir = File(dataPath, TESSDATA_FOLDER)
        val trainedDataFile = File(tessDataDir, "$tessLanguageCode.traineddata")
        return trainedDataFile.exists()
    }

    /**
     * Copy language data from assets to app storage
     * Call this method to install a language pack
     * @param languageCode ISO code ("en") or special model ("osd", "equ")
     */
    suspend fun installLanguageData(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val tessLanguageCode = TESSERACT_LANGUAGE_MAP[languageCode] ?: return@withContext false
            
            val tessDataDir = File(dataPath, TESSDATA_FOLDER)
            
            if (!tessDataDir.exists()) {
                tessDataDir.mkdirs()
            }

            val trainedDataFile = File(tessDataDir, "$tessLanguageCode.traineddata")
            
            // Check if already exists
            if (trainedDataFile.exists()) {
                return@withContext true
            }

            // Copy from assets if available
            try {
                context.assets.open("$TESSDATA_FOLDER/$tessLanguageCode.traineddata").use { input ->
                    FileOutputStream(trainedDataFile).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                // File doesn't exist in assets
                e.printStackTrace()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get list of installed language packs
     */
    fun getInstalledLanguages(): List<String> {
        val tessDataDir = File(dataPath, TESSDATA_FOLDER)
        if (!tessDataDir.exists()) return emptyList()

        return tessDataDir.listFiles()
            ?.filter { it.name.endsWith(".traineddata") }
            ?.mapNotNull { file ->
                val tessCode = file.nameWithoutExtension
                TESSERACT_LANGUAGE_MAP.entries.find { it.value == tessCode }?.key
            } ?: emptyList()
    }

    /**
     * Install all available language data from assets
     */
    suspend fun installAllLanguages(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Boolean>()
        
        // Install primary languages
        TESSERACT_LANGUAGE_MAP.keys.forEach { langCode ->
            results[langCode] = installLanguageData(langCode)
        }
        
        results
    }

    /**
     * Clean up resources
     */
    fun release() {
        tessBaseAPI?.recycle()
        tessBaseAPI = null
        currentLanguage = null
    }
}
