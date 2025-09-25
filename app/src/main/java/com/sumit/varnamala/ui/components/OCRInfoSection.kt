package com.sumit.varnamala.ui.components

import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.utils.LanguageDetector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OCRInfoSection(
    ocrLoading: Boolean,
    ocrError: String?,
    rawOcrText: String,
    detectedLanguage: String,
    languageConfidence: Float,
    lastResult: OCRResult?,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    when {
        ocrLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Recognizing text...", fontSize = 14.sp)
        }

        ocrError != null -> Text(
            "Error: $ocrError",
            color = MaterialTheme.colorScheme.error,
            fontSize = 14.sp
        )

        rawOcrText.isBlank() -> Text("No text recognized.", fontSize = 14.sp)
        else -> {
            val confMap = LanguageDetector.getDetectionConfidenceMap(rawOcrText)
            val top = confMap.entries.sortedByDescending { it.value }.take(3)
            val langLabel = LanguageDetector.languageCodeToLabel(detectedLanguage)
            Text(
                "Detected Language: $langLabel (${
                    String.format(
                        "%.0f",
                        languageConfidence * 100
                    )
                }%)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                top.forEach { e ->
                    val label = LanguageDetector.languageCodeToLabel(e.key)
                    OutlinedButton(
                        onClick = {},
                        enabled = false
                    ) { Text("${label} ${String.format("%.0f", e.value * 100)}%") }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCopy, enabled = rawOcrText.isNotBlank()) { Text("Copy") }
                OutlinedButton(
                    onClick = onShare,
                    enabled = rawOcrText.isNotBlank()
                ) { Text("Share") }
            }
            Spacer(Modifier.height(6.dp))
            Text(rawOcrText, fontSize = 14.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(8.dp))
            lastResult?.let { r ->
                val words = rawOcrText.split("\\s+".toRegex()).filter { it.isNotBlank() }
                val chars = rawOcrText.length
                val blocks = r.boundingBoxes.size
                val lines = r.boundingBoxes.sumOf { it.lines.size }
                val elements = r.boundingBoxes.sumOf { it.lines.sumOf { ln -> ln.elements.size } }
                Text(
                    "Blocks: $blocks  Lines: $lines  Elements: $elements  Words: ${words.size}  Chars: $chars  Time: ${r.processingTimeMs}ms",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
