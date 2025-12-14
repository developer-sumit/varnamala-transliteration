package com.sumit.varnamala.ocr.mlkit

import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextLine
import com.sumit.varnamala.data.ocr.TextElement
import com.sumit.varnamala.utils.await

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MLKitOCRRepository(private val context: Context) {
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val devanagariRecognizer =
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    private val recognizers: Map<String, TextRecognizer> = mapOf(
        "latin" to TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
        "devanagari" to TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    )

    companion object {
        // Devanagari script
        private val DEVANAGARI_LANGUAGES = setOf(
            "hin", // Hindi
            "mar", // Marathi
            "nep", // Nepali
            "san"  // Sanskrit
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun processImageFromUri(uri: Uri, preferredScript: String? = null): OCRResult {
        return withContext(Dispatchers.IO) {
            val bitmap = loadBitmapFromUri(uri)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            val script = when {
                preferredScript in DEVANAGARI_LANGUAGES -> "devanagari"
                preferredScript != null -> preferredScript
                else -> "latin"
            }

            val recognizer = recognizers[script] ?: recognizers["latin"]!!

            val mlKitResult = recognizeText(inputImage, recognizer)
            convertToOCRResult(mlKitResult, bitmap.width, bitmap.height)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun processImageWithAutoDetection(uri: Uri): OCRResult {
        return withContext(Dispatchers.IO) {
            val bitmap = loadBitmapFromUri(uri)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val results = mutableListOf<Pair<OCRResult, String>>()

            val latinResult = recognizeText(inputImage, latinRecognizer)
            if (latinResult.text.isNotBlank()) {
                val ocrResult =
                    convertToOCRResult(latinResult, bitmap.width, bitmap.height, "latin")
                results.add(ocrResult to "latin")
            }

            val devanagariResult = recognizeText(inputImage, devanagariRecognizer)
            if (devanagariResult.text.isNotBlank()) {
                val ocrResult = convertToOCRResult(
                    devanagariResult,
                    bitmap.width,
                    bitmap.height,
                    "devanagari"
                )
                results.add(ocrResult to "devanagari")
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
                bestResult.first
            } else {
                // Return empty result if no recognizer produced text
                OCRResult(
                    text = "",
                    language = "unknown",
                    boundingBoxes = emptyList(),
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height
                )
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
                            boundingBox = elementBounds
                        )
                    }
                }

                // If no elements available, create a single element from the line
                val finalElements = if (elements.isEmpty()) {
                    listOf(
                        TextElement(
                            text = lineText,
                            boundingBox = lineBounds
                        )
                    )
                } else {
                    elements
                }

                lines.add(
                    TextLine(
                        text = lineText,
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

    @RequiresApi(Build.VERSION_CODES.P)
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
        return listOf("auto") + recognizers.keys.toList()
    }
}