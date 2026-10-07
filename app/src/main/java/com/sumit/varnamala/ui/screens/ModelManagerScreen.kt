package com.sumit.varnamala.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sumit.varnamala.R
import com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner
import com.sumit.varnamala.tts.RasaModelProvisioner
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.ModelCard
import com.sumit.varnamala.utils.rememberDownloadGate

/** Every downloadable model in one place: size, status, download progress, and delete. */
@Composable
fun ModelManagerScreen(navController: NavController) {
    val context = LocalContext.current
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val ocr = remember { PaddleModelProvisioner.get(context) }
    val ocrState by ocr.state.collectAsState()
    val voice = remember { RasaModelProvisioner.get(context) }
    val voiceState by voice.state.collectAsState()
    val gate = rememberDownloadGate()
    var requestedTier by remember { mutableStateOf<PaddleModelProvisioner.Tier?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp, topInset + 5.dp, 16.dp, 0.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            GlassButton(description = stringResource(R.string.back), icon = painterResource(R.drawable.ic_chevron_left), onClick = { navController.popBackStack() })
            Text(stringResource(R.string.model_manager), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(45.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.model_manager_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            PaddleModelProvisioner.Tier.entries.forEach { tier ->
                val ready = remember(ocrState) { ocr.isReady(tier) }
                val small = tier == PaddleModelProvisioner.Tier.TINY
                ModelCard(
                    title = stringResource(if (small) R.string.model_ocr_small else R.string.model_ocr_large),
                    description = stringResource(if (small) R.string.model_ocr_small_hint else R.string.model_ocr_large_hint),
                    sizeBytes = PaddleModelProvisioner.totalBytes(tier),
                    installed = ready,
                    downloading = ocrState.isDownloading && requestedTier == tier && !ready,
                    progress = ocrState.progress(tier),
                    error = ocrState.error.takeIf { requestedTier == tier },
                    onDownload = {
                        gate.request(PaddleModelProvisioner.totalBytes(tier)) {
                            requestedTier = tier
                            ocr.start(setOf(tier))
                        }
                    },
                    // The small model is what live scanning needs, so it stays.
                    onDelete = if (small) null else ({ ocr.delete(tier) }),
                )
            }

            ModelCard(
                title = stringResource(R.string.offline_voice),
                description = stringResource(R.string.offline_voice_description),
                sizeBytes = RasaModelProvisioner.MODEL_BYTES,
                installed = voiceState.isInstalled,
                downloading = voiceState.isDownloading,
                progress = voiceState.progress,
                error = voiceState.error,
                onDownload = { gate.request(RasaModelProvisioner.MODEL_BYTES) { voice.start() } },
                onDelete = { voice.delete() },
            )

            Text(
                stringResource(R.string.offline_voice_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    gate.Dialogs()
}
