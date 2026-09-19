package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Language
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.data.TransliterationLanguageCatalog
import com.sumit.varnamala.data.ocr.OcrLanguageCatalog
import com.sumit.varnamala.data.ocr.OcrLanguageModel
import com.sumit.varnamala.data.preferences.OCRSettingsRepository
import com.sumit.varnamala.models.TesseractModelStore
import com.sumit.varnamala.models.modeldownload.ModelDownloadManager
import com.sumit.varnamala.models.modeldownload.ModelDownloadWorker
import com.sumit.varnamala.ui.components.GlassButton

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.launch

@Composable
fun OcrLanguageScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { OCRSettingsRepository(context) }
    val selectedCode by settingsRepository.selectedOcrModelCode.collectAsState(
        initial = OcrLanguageCatalog.DEFAULT_SOURCE_CODE
    )
    val workManager = remember { WorkManager.getInstance(context) }
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    var query by remember { mutableStateOf("") }
    val languages = remember(query) {
        OcrLanguageCatalog.selectableSources.filter { language ->
            query.isBlank() ||
                language.displayName.contains(query, ignoreCase = true) ||
                language.nativeName.contains(query, ignoreCase = true) ||
                language.code.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
    ) {
        PickerHeader(
            title = stringResource(R.string.select_ocr_language),
            topPadding = topInset + 10.dp,
            onBack = { navController.popBackStack() }
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_languages)) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(languages, key = { it.code }) { language ->
                val tag = ModelDownloadManager.tagForLanguage(language.code)
                val workInfos by remember(tag) {
                    workManager.getWorkInfosByTagFlow(tag)
                }.collectAsState(initial = emptyList())
                val activeWork = workInfos.firstOrNull {
                    it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                }
                val progress = activeWork?.progress?.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                val downloaded = TesseractModelStore.isModelDownloaded(context, language)

                OcrLanguageRow(
                    language = language,
                    selected = selectedCode == language.code,
                    downloaded = downloaded,
                    progress = progress,
                    onClick = {
                        if (downloaded) {
                            scope.launch {
                                settingsRepository.setSelectedOcrModelCode(language.code)
                                navController.popBackStack()
                            }
                        } else if (activeWork == null) {
                            val started = runCatching {
                                ModelDownloadManager.enqueueDownload(context, language.code)
                            }.getOrDefault(false)
                            if (!started) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.no_internet_connection),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TransliterationLanguageScreen(navController: NavController, role: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { OCRSettingsRepository(context) }
    val isSource = role == Screen.TransliterationLanguage.ROLE_SOURCE
    val selectedCode by if (isSource) {
        settingsRepository.selectedSourceTransliterationCode.collectAsState(initial = "auto")
    } else {
        settingsRepository.selectedTargetTransliterationCode.collectAsState(initial = "hi")
    }
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    var query by remember { mutableStateOf("") }
    val languages = remember(query, isSource) {
        val base = if (isSource) {
            listOf(Language("auto", "Auto-detect", "Auto-detect", "", localeCode = "auto")) +
                TransliterationLanguageCatalog.supportedLanguages
        } else {
            TransliterationLanguageCatalog.supportedLanguages
        }
        base.filter { language ->
            query.isBlank() ||
                language.name.contains(query, ignoreCase = true) ||
                language.nativeName.contains(query, ignoreCase = true) ||
                language.localeCode.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
    ) {
        PickerHeader(
            title = if (isSource) {
                stringResource(R.string.select_source_language)
            } else {
                stringResource(R.string.select_transliteration_language)
            },
            topPadding = topInset + 10.dp,
            onBack = { navController.popBackStack() }
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_languages)) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(languages, key = { it.localeCode }) { language ->
                TransliterationLanguageRow(
                    label = language.name,
                    nativeName = language.nativeName,
                    selected = selectedCode == language.localeCode,
                    onClick = {
                        scope.launch {
                            if (isSource) {
                                settingsRepository.setSelectedSourceTransliterationCode(language.localeCode)
                            } else {
                                settingsRepository.setSelectedTargetTransliterationCode(language.localeCode)
                            }
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PickerHeader(
    title: String,
    topPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassButton(
            icon = painterResource(R.drawable.ic_chevron_left),
            onClick = onBack
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OcrLanguageRow(
    language: OcrLanguageModel,
    selected: Boolean,
    downloaded: Boolean,
    progress: Int?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (selected) 24.dp else 12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.background
            )
            .clickable { onClick() }
            .padding(horizontal = if (selected) 18.dp else 0.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (language.nativeName != language.displayName) {
                    "${language.nativeName} • ${language.code}"
                } else {
                    language.code
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            progress != null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { (progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("$progress%", style = MaterialTheme.typography.bodySmall)
                }
            }

            downloaded -> {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.model_downloaded),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            else -> {
                IconButton(onClick = onClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = stringResource(R.string.model_download)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransliterationLanguageRow(
    label: String,
    nativeName: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (selected) 24.dp else 12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.background
            )
            .clickable { onClick() }
            .padding(horizontal = if (selected) 18.dp else 0.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (nativeName != label) {
                Text(
                    text = nativeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
