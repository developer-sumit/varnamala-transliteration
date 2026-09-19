package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.ocr.OcrLanguageCatalog
import com.sumit.varnamala.models.ModelRegistry
import com.sumit.varnamala.models.ModelSizeFetcher
import com.sumit.varnamala.models.TesseractModelStore
import com.sumit.varnamala.ui.components.ConfirmBottomSheet
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.models.modeldownload.ModelDownloadManager
import com.sumit.varnamala.models.modeldownload.ModelDownloadWorker

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import android.widget.Toast

@Composable
fun ModelsScreen(navController: NavController) {
    val context = LocalContext.current
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val languages = remember { OcrLanguageCatalog.all }
    val workManager = remember { WorkManager.getInstance(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp, topInset + 5.dp, 16.dp, 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassButton(
                    icon = painterResource(R.drawable.ic_chevron_left),
                    onClick = { navController.popBackStack() }
                )

                Text(
                    text = stringResource(R.string.models),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.width(45.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(20.dp)),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                val tesseractLanguages = languages.map { lang ->
                    val model = ModelRegistry.forOcrLanguage(lang)
                    ModelRowData(
                        name = lang.displayName,
                        nativeName = lang.nativeName,
                        flagEmoji = "",
                        tessCode = model.tesseractLanguageCode,
                        url = model.downloadUrl
                    )
                }

                items(
                    items = tesseractLanguages,
                    key = { it.tessCode }
                ) { row ->
                    val tag = ModelDownloadManager.tagForLanguage(row.tessCode)
                    val workInfosFlow = remember(tag) { workManager.getWorkInfosByTagFlow(tag) }
                    val workInfos by workInfosFlow.collectAsState(initial = emptyList())

                    val activeWork =
                        workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    val percent = activeWork?.progress?.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                    val remoteSizeBytes by produceState<Long?>(
                        initialValue = null,
                        key1 = row.url
                    ) {
                        value = ModelSizeFetcher.getRemoteSizeBytes(row.url)
                    }

                    val downloaded = TesseractModelStore.isModelDownloaded(context, row.tessCode)
                    val localSizeBytes =
                        if (downloaded) TesseractModelStore.modelFile(context, row.tessCode)
                            .length() else null

                    ModelRow(
                        name = row.name,
                        nativeName = row.nativeName,
                        flagEmoji = row.flagEmoji,
                        tessCode = row.tessCode,
                        downloaded = downloaded,
                        percent = percent,
                        sizeBytes = localSizeBytes ?: remoteSizeBytes,
                        onDownload = {
                            val started = runCatching {
                                ModelDownloadManager.enqueueDownload(context, row.tessCode)
                            }.getOrDefault(false)

                            if (!started) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.no_internet_connection),
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                        },
                        modifier = Modifier.animateItem(
                            fadeInSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    )
                }
            }
        }
    }
}

private data class ModelRowData(
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val tessCode: String,
    val url: String
)

@Composable
private fun ModelRow(
    name: String,
    nativeName: String,
    flagEmoji: String,
    tessCode: String,
    downloaded: Boolean,
    percent: Int?,
    sizeBytes: Long?,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = !downloaded && percent == null) { onDownload() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = flagEmoji,
                fontSize = 32.sp,
                modifier = Modifier.size(40.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (nativeName != name) {
                    Text(
                        text = nativeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                val sizeText = sizeBytes?.let { formatBytes(it) }
                if (sizeText != null) {
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        when {
            downloaded -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.model_downloaded),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_outline),
                            contentDescription = "Delete model",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            percent != null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { (percent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            else -> {
                IconButton(onClick = onDownload) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = stringResource(R.string.model_download)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmBottomSheet(
            title = stringResource(R.string.delete_model),
            message = stringResource(R.string.delete_model_confirmation, name),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                val deleted = TesseractModelStore.deleteModel(context, tessCode)
                Toast.makeText(
                    context,
                    context.getString(
                        if (deleted) R.string.model_deleted_successfully
                        else R.string.failed_to_delete_model
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                showDeleteDialog = false
            }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format("%.1f MB", mb)
}
