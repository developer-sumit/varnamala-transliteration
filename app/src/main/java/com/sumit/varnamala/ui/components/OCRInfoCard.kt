package com.sumit.varnamala.ui.components

import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.utils.LanguageDetector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OCRInfoCard(
    ocrLoading: Boolean,
    ocrError: String?,
    rawOcrText: String,
    detectedLanguage: String,
    languageConfidence: Float,
    lastResult: OCRResult?,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 300.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        when {
            ocrLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("OCR Processing...", fontSize = 12.sp)
            }

            ocrError != null -> Text(
                "Error: $ocrError",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )

            rawOcrText.isNotBlank() -> {
                val confMap = LanguageDetector.getDetectionConfidenceMap(rawOcrText)
                val top = confMap.entries.sortedByDescending { it.value }.take(2)
                val langLabel = LanguageDetector.languageCodeToLabel(detectedLanguage)
                Text(
                    "$langLabel  ${String.format("%.0f", languageConfidence * 100)}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    top.forEach { e ->
                        val label = LanguageDetector.languageCodeToLabel(e.key)
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${label}: ${String.format("%.0f", e.value * 100)}%",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    rawOcrText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                lastResult?.let { r ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${r.processingTimeMs}ms  ${r.boundingBoxes.size} blocks",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRefresh, enabled = !ocrLoading) {
                        Text(
                            "Refresh",
                            fontSize = 11.sp
                        )
                    }
                    OutlinedButton(onClick = onClear, enabled = !ocrLoading) {
                        Text(
                            "Clear",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}