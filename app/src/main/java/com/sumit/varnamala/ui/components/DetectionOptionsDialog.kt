package com.sumit.varnamala.ui.components

import com.sumit.varnamala.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun DetectionOptionsDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    currentLanguage: String = "auto",
    onDetectAgain: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onClearAll: () -> Unit = {},
    ocrLoading: Boolean = false
) {
    val availableLanguages = listOf(
        "auto" to "Automatic Detection",
        "latin" to "Latin Script",
        "devanagari" to "Devanagari Script",
        "eng" to "English",
        "hin" to "Hindi",
        "san" to "Sanskrit"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.detection_options),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                // Language Selection Section
                Text(
                    stringResource(R.string.language_detection),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableLanguages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (currentLanguage == code)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        Color.Transparent
                                )
                                .clickable {
                                    onLanguageSelected(code)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == code,
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                                if (code == "auto") {
                                    Text(
                                        "Detects multiple languages automatically",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // OCR Actions Section
                Text(
                    stringResource(R.string.ocr_actions),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDetectAgain,
                        enabled = !ocrLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.detect_again))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onClearCache,
                            enabled = !ocrLoading,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.clear_cache)) }

                        OutlinedButton(
                            onClick = onClearAll,
                            enabled = !ocrLoading,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.clear_all)) }
                    }
                }

                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}