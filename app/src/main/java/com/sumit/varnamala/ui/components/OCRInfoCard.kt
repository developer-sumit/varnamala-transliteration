package com.sumit.varnamala.ui.components

import com.sumit.varnamala.R
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.utils.LanguageDetector
import com.sumit.varnamala.utils.Transliterator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
    var targetLanguage by remember { mutableStateOf(Transliterator.Languages.ENGLISH) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(0.95f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                ocrLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text(
                                "OCR Processing...", style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                ocrError != null -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Error: $ocrError",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onRefresh, modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.retry),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.retry))
                        }
                    }
                }

                rawOcrText.isNotBlank() -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ocr_result),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isExpanded) {
                                // Language selector (only show when expanded)
                                Box(
                                    modifier = Modifier.wrapContentSize(Alignment.TopStart)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { showLanguageDropdown = true }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = Transliterator.Languages.getDisplayName(
                                                targetLanguage
                                            ),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = stringResource(R.string.select_language),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(16.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showLanguageDropdown,
                                        onDismissRequest = { showLanguageDropdown = false }) {
                                        Transliterator.Languages.ALL.forEach { lang ->
                                            DropdownMenuItem(text = {
                                                Text(
                                                    text = Transliterator.Languages.getDisplayName(
                                                        lang
                                                    ),
                                                    fontWeight = if (lang == targetLanguage) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }, onClick = {
                                                targetLanguage = lang
                                                showLanguageDropdown = false
                                            })
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Confidence badge
                            val confidence = languageConfidence
                            Surface(
                                color = when {
                                    confidence > 0.8f -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    confidence > 0.5f -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                    else -> Color(0xFFf44336).copy(alpha = 0.15f)
                                }, shape = RoundedCornerShape(20.dp), modifier = Modifier.border(
                                    1.dp, when {
                                        confidence > 0.8f -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                                        confidence > 0.5f -> Color(0xFFFF9800).copy(alpha = 0.3f)
                                        else -> Color(0xFFf44336).copy(alpha = 0.3f)
                                    }, RoundedCornerShape(20.dp)
                                )
                            ) {
                                Text(
                                    text = "${(confidence * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        confidence > 0.8f -> Color(0xFF2E7D32)
                                        confidence > 0.5f -> Color(0xFFE65100)
                                        else -> Color(0xFFD32F2F)
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // Expand/Collapse button
                            IconButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = if (isExpanded) painterResource(R.drawable.ic_chevron_up) else painterResource(
                                        R.drawable.ic_chevron_down
                                    ),
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Content section (only show when expanded)
                    if (isExpanded) {
                        val transliteratedText = Transliterator.transliterate(
                            text = rawOcrText,
                            sourceLanguage = detectedLanguage,
                            targetLanguage = targetLanguage
                        )

                        val showTransliteration =
                            transliteratedText != rawOcrText && transliteratedText.isNotBlank()

                        if (showTransliteration) {
                            // Side by side layout without arrow
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Detected Text Column
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(max = 120.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.3f
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.original),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 100.dp, max = 180.dp)
                                        ) {
                                            Text(
                                                text = rawOcrText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 24.sp,
                                                fontSize = 16.sp,
                                                modifier = Modifier.verticalScroll(
                                                    rememberScrollState()
                                                )
                                            )
                                        }
                                    }
                                }

                                // Transliterated Text Column
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(max = 120.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                            alpha = 0.3f
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.transliterated),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 100.dp, max = 180.dp)
                                        ) {
                                            Text(
                                                text = transliteratedText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 24.sp,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.verticalScroll(
                                                    rememberScrollState()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Single text display when no transliteration
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 200.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.3f
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = rawOcrText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 24.sp,
                                        fontSize = 16.sp,
                                        modifier = Modifier.verticalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Copy button
                            Button(
                                onClick = onCopy,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_copy_outline),
                                    contentDescription = stringResource(R.string.copy),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.copy), fontSize = 11.sp)
                            }

                            // Share button
                            Button(
                                onClick = onShare,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.share),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.share), fontSize = 11.sp)
                            }

                            // Refresh button
                            OutlinedButton(
                                onClick = onRefresh, modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.refresh),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.refresh), fontSize = 11.sp)
                            }
                        }

                        // Bottom statistics
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Source language
                            OCRStatItem(
                                label = "Detected",
                                value = LanguageDetector.languageCodeToLabel(detectedLanguage),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )

                            // Target language
                            OCRStatItem(
                                label = "Target",
                                value = Transliterator.Languages.getDisplayName(targetLanguage),
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Spacer(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )

                            // Detection time
                            OCRStatItem(
                                label = "Time",
                                value = if (lastResult != null) "${lastResult.processingTimeMs}ms" else "---",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OCRStatItem(
    label: String, value: String, color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color
        )
    }
}