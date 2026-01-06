package com.sumit.varnamala.ui.components

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Language
import com.sumit.varnamala.data.AppLanguageRepository
import com.sumit.varnamala.models.ModelRegistry
import com.sumit.varnamala.models.TesseractModelStore
import com.sumit.varnamala.models.modeldownload.ModelDownloadManager
import com.sumit.varnamala.models.modeldownload.ModelDownloadWorker

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.work.WorkInfo
import androidx.work.WorkManager

@Composable
fun LanguageSelectionRow(
    sourceLanguage: Language?,
    targetLanguage: Language?,
    onSourceLanguageClick: () -> Unit,
    onTargetLanguageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Source Language Button
        LanguageButton(
            language = sourceLanguage,
            label = stringResource(R.string.detect),
            onClick = onSourceLanguageClick,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(100.dp, 0.dp, 0.dp, 100.dp))
        )

        // Arrow Icon
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = stringResource(R.string.to),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(30.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Target Language Button
        LanguageButton(
            language = targetLanguage,
            label = stringResource(R.string.transliterate),
            onClick = onTargetLanguageClick,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(0.dp, 100.dp, 100.dp, 0.dp))
        )
    }
}


@Composable
private fun LanguageButton(
    language: Language?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (language != null) {
            Text(
                text = language.flagEmoji,
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = language.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Language selection dialog
 */
@Composable
fun LanguageSelectionDialog(
    title: String,
    currentLanguage: Language?,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit,
    includeDefaultOption: Boolean = false,
    defaultOptionLabel: String = "",
    onDefaultSelected: (() -> Unit)? = null
) {
    val languages = remember { AppLanguageRepository.getSupportedLanguages() }
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(20.dp)
                )

                HorizontalDivider()

                // Language List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (includeDefaultOption && onDefaultSelected != null) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDefaultSelected()
                                        onDismiss()
                                    },
                                color = if (currentLanguage == null) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    Color.Transparent
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = defaultOptionLabel,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (currentLanguage == null) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    if (currentLanguage == null) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        item { HorizontalDivider() }
                    }

                    items(languages) { language ->
                        val model = remember(language.code) { ModelRegistry.forLanguage(language) }
                        val isModelDownloaded = model?.let {
                            TesseractModelStore.isModelDownloaded(context, it.tesseractLanguageCode)
                        } ?: true

                        val tessCode = model?.tesseractLanguageCode
                        val tag = tessCode?.let { ModelDownloadManager.tagForLanguage(it) }
                        val workInfos by remember(tag) {
                            if (tag == null) {
                                null
                            } else {
                                workManager.getWorkInfosByTagFlow(tag)
                            }
                        }?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

                        val activeWork = workInfos.firstOrNull {
                            it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                        }
                        val progressPercent = activeWork?.progress?.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                        val isDownloading = activeWork != null

                        LanguageItem(
                            language = language,
                            isSelected = language.code == currentLanguage?.code,
                            showDownload = (model != null && !isModelDownloaded && !isDownloading),
                            downloadPercent = if (isDownloading) progressPercent else null,
                            onDownloadClick = {
                                val code = model?.tesseractLanguageCode ?: return@LanguageItem
                                val started = runCatching { ModelDownloadManager.enqueueDownload(context, code) }
                                    .getOrDefault(false)
                                if (!started) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.no_internet_connection),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onClick = {
                                if (model != null && !isModelDownloaded && !isDownloading) {
                                    val code = model.tesseractLanguageCode
                                    val started = runCatching { ModelDownloadManager.enqueueDownload(context, code) }
                                        .getOrDefault(false)
                                    if (!started) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.no_internet_connection),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                onLanguageSelected(language)
                                onDismiss()
                            }
                        )
                    }
                }

                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    showDownload: Boolean,
    downloadPercent: Int?,
    onDownloadClick: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = language.flagEmoji,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = language.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = language.nativeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (downloadPercent != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { (downloadPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${downloadPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else if (showDownload) {
                IconButton(onClick = onDownloadClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = stringResource(R.string.model_download),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * OCR Engine selection dialog
 */
@Composable
fun OCREngineSelectionDialog(
    currentEngine: com.sumit.varnamala.data.ocr.OcrEngine,
    onEngineSelected: (com.sumit.varnamala.data.ocr.OcrEngine) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_ocr_engine),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                // ML Kit Option
                OCREngineOption(
                    name = stringResource(R.string.default_mlkit),
                    description = stringResource(R.string.ocr_engine_mlkit_description),
                    isSelected = currentEngine == com.sumit.varnamala.data.ocr.OcrEngine.ML_KIT,
                    onClick = {
                        onEngineSelected(com.sumit.varnamala.data.ocr.OcrEngine.ML_KIT)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tesseract Option
                OCREngineOption(
                    name = stringResource(R.string.ocr_engine_tesseract_name),
                    description = stringResource(R.string.ocr_engine_tesseract_description),
                    isSelected = currentEngine == com.sumit.varnamala.data.ocr.OcrEngine.TESSERACT,
                    onClick = {
                        onEngineSelected(com.sumit.varnamala.data.ocr.OcrEngine.TESSERACT)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun OCREngineOption(
    name: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
