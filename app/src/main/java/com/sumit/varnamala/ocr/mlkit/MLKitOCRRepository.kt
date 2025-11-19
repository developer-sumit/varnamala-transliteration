package com.sumit.varnamala.ocr.mlkit

import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextLine
import com.sumit.varnamala.data.ocr.TextElement

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.sumit.varnamala.utils.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MLKitOCRRepository(private val context: Context) {
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val devanagariRecognizer =
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    companion object {
        private const val TAG = "MLKitOCR"

        // Devanagari script
        private val DEVANAGARI_LANGUAGES = setOf(
            "hin", // Hindi
            "mar", // Marathi
            "nep", // Nepali
            "san"  // Sanskrit
        )
    }

    suspend fun processImageFromUri(uri: Uri, preferredScript: String? = null): OCRResult {
        return withContext(Dispatchers.IO) {
            try {
                // Load bitmap from URI
                val bitmap = loadBitmapFromUri(uri)

                // Process image
                val inputImage = InputImage.fromBitmap(bitmap, 0)

                // Choose recognizer based on preferred script
                val recognizer = when {
                    preferredScript == "devanagari" ||
                            DEVANAGARI_LANGUAGES.contains(preferredScript) -> devanagariRecognizer

                    else -> latinRecognizer
                }

                // Process with ML Kit
                val mlKitResult = recognizeText(inputImage, recognizer)

                // Convert ML Kit result to our common OCRResult format
                convertToOCRResult(mlKitResult, bitmap.width, bitmap.height)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
                throw e
            }
        }
    }

    suspend fun processImageWithAutoDetection(uri: Uri): OCRResult {
        return withContext(Dispatchers.IO) {
            try {
                // Load bitmap from URI
                val bitmap = loadBitmapFromUri(uri)
                val inputImage = InputImage.fromBitmap(bitmap, 0)

                Log.d(TAG, "Running comprehensive language detection...")

                // Try both recognizers and collect results
                val results = mutableListOf<Pair<OCRResult, String>>()

                try {
                    Log.d(TAG, "Testing Latin script recognizer...")
                    val latinResult = recognizeText(inputImage, latinRecognizer)
                    if (latinResult.text.isNotBlank()) {
                        val ocrResult =
                            convertToOCRResult(latinResult, bitmap.width, bitmap.height, "latin")
                        results.add(ocrResult to "latin")
                        Log.d(
                            TAG,
                            "Latin result: ${latinResult.text.length} characters, ${latinResult.textBlocks.size} blocks"
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Latin recognizer failed", e)
                }

                try {
                    Log.d(TAG, "Testing Devanagari script recognizer...")
                    val devanagariResult = recognizeText(inputImage, devanagariRecognizer)
                    if (devanagariResult.text.isNotBlank()) {
                        val ocrResult = convertToOCRResult(
                            devanagariResult,
                            bitmap.width,
                            bitmap.height,
                            "devanagari"
                        )
                        results.add(ocrResult to "devanagari")
                        Log.d(
                            TAG,
                            "Devanagari result: ${devanagariResult.text.length} characters, ${devanagariResult.textBlocks.size} blocks"
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Devanagari recognizer failed", e)
                }

                // Select the best result based on multiple criteria
                val bestResult = results.maxByOrNull { (result, _) ->
                    // Score based on text length, number of blocks, and character variety
                    val textScore = result.text.length
                    val blockScore = result.boundingBoxes.size * 10
                    val varietyScore = result.text.toSet().size
                    textScore + blockScore + varietyScore
                }

                return@withContext if (bestResult != null) {
                    Log.d(
                        TAG,
                        "Selected ${bestResult.second} as best result with ${bestResult.first.text.length} characters"
                    )
                    bestResult.first
                } else {
                    Log.w(TAG, "No valid OCR results found, returning empty result")
                    // Return empty result if no recognizer produced text
                    OCRResult(
                        text = "",
                        confidence = 0f,
                        language = "unknown",
                        processingTimeMs = 0L,
                        boundingBoxes = emptyList(),
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image with auto-detection", e)
                throw e
            }
        }
    }

    private fun convertToOCRResult(
        mlKitResult: com.google.mlkit.vision.text.Text,
        imageWidth: Int,
        imageHeight: Int,
        script: String? = null
    ): OCRResult {
        val blocks = mutableListOf<TextBlock>()

        // Calculate overall confidence (this is estimated as ML Kit doesn't provide confidence)
        var totalElements = 0
        var estimatedConfidence = 0.85f // Default high confidence for ML Kit

        val fullText = StringBuilder()

        // Process each text block
        mlKitResult.textBlocks.forEach { mlkitBlock ->
            val blockBounds = mlkitBlock.boundingBox ?: Rect(0, 0, 10, 10)
            val blockText = mlkitBlock.text
            fullText.append(blockText).append("\n")

            val lines = mutableListOf<TextLine>()

            // Process each line in the block
            mlkitBlock.lines.forEach { mlkitLine ->
                val lineBounds = mlkitLine.boundingBox ?: Rect(0, 0, 10, 10)
                val lineText = mlkitLine.text

                // Create TextElements from ML Kit elements if available
                val elements = mlkitLine.elements.mapNotNull { mlkitElement ->
                    mlkitElement.boundingBox?.let { elementBounds ->
                        TextElement(
                            text = mlkitElement.text,
                            confidence = estimatedConfidence,
                            boundingBox = elementBounds
                        )
                    }
                }

                // If no elements available, create a single element from the line
                val finalElements = if (elements.isEmpty()) {
                    listOf(
                        TextElement(
                            text = lineText,
                            confidence = estimatedConfidence,
                            boundingBox = lineBounds
                        )
                    )
                } else {
                    elements
                }

                lines.add(
                    TextLine(
                        text = lineText,
                        confidence = estimatedConfidence,
                        boundingBox = lineBounds,
                        elements = finalElements
                    )
                )

                totalElements += finalElements.size
            }

            // Create TextBlock
            blocks.add(
                TextBlock(
                    text = blockText,
                    confidence = estimatedConfidence,
                    boundingBox = blockBounds,
                    lines = lines
                )
            )
        }

        // Detect script from content if not provided
        val detectedScript = script ?: detectScriptFromContent(fullText.toString())
        val language = when (detectedScript) {
            "devanagari" -> "hin" // Default to Hindi for Devanagari
            else -> "eng" // Default to English for Latin
        }

        return OCRResult(
            text = mlKitResult.text,
            confidence = estimatedConfidence,
            language = language,
            boundingBoxes = blocks,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
    }

    private suspend fun recognizeText(
        image: InputImage,
        recognizer: TextRecognizer
    ): com.google.mlkit.vision.text.Text {
        return recognizer.process(image).await()
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val contentResolver = context.contentResolver
        return android.graphics.ImageDecoder.decodeBitmap(
            android.graphics.ImageDecoder.createSource(contentResolver, uri)
        ) { decoder, _, _ ->
            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
    }

    private fun detectScriptFromContent(text: String): String {
        // Simple check for Devanagari characters
        val devanagariRange = '\u0900'..'\u097F'
        val devanagariChars = text.count { it in devanagariRange }

        return if (devanagariChars > text.length * 0.2) {
            "devanagari"
        } else {
            "latin"
        }
    }

    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "eng" -> "English"
            "hin" -> "Hindi"
            "san" -> "Sanskrit"
            "mar" -> "Marathi"
            "ben" -> "Bengali"
            "guj" -> "Gujarati"
            "kan" -> "Kannada"
            "mal" -> "Malayalam"
            "ori" -> "Oriya"
            "pan" -> "Punjabi"
            "tam" -> "Tamil"
            "tel" -> "Telugu"
            "latin" -> "Latin script"
            "devanagari" -> "Devanagari script"
            else -> languageCode
        }
    }

    fun getAvailableLanguages(): List<String> {
        return listOf("auto", "latin", "devanagari")
    }
}