package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.data.preferences.OnboardingRepository
import com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

private enum class PackStatus { WAITING, DOWNLOADING, READY }

@Composable
fun ResourceSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val provisioner = remember { PaddleModelProvisioner.get(context) }
    val gate = com.sumit.varnamala.utils.rememberDownloadGate()
    val onboarding = remember { OnboardingRepository(context) }
    val state by provisioner.state.collectAsState()
    val scope = rememberCoroutineScope()
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val ready = state.isComplete

    fun continueToApp() {
        scope.launch {
            onboarding.setHasRequiredOcrResources(true)
            navController.navigate(Screen.OnboardingLanguage.route) {
                popUpTo(Screen.ResourceSetup.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = insets.calculateTopPadding() + 24.dp,
                bottom = insets.calculateBottomPadding() + 20.dp,
            )
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderBadge(ready = ready)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.resource_setup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (ready) R.string.resource_setup_subtitle_ready
                    else R.string.resource_setup_subtitle
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
            OverallProgressCard(state)

            Spacer(Modifier.height(16.dp))
            PackRow(
                title = stringResource(R.string.resource_pack_live_title),
                description = stringResource(R.string.resource_pack_live_description),
                iconRes = R.drawable.ic_camera,
                tier = PaddleModelProvisioner.Tier.TINY,
                state = state,
            )
            Spacer(Modifier.height(12.dp))
            PackRow(
                title = stringResource(R.string.resource_pack_photo_title),
                description = stringResource(R.string.resource_pack_photo_description),
                iconRes = R.drawable.ic_info,
                tier = PaddleModelProvisioner.Tier.MEDIUM,
                state = state,
            )

            AnimatedVisibility(visible = state.error != null) {
                ErrorBanner(message = state.error.orEmpty())
            }
        }

        Spacer(Modifier.height(16.dp))
        if (!ready && !state.isDownloading) {
            Text(
                text = stringResource(R.string.resource_setup_wifi_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
        }
        Button(
            onClick = {
                if (provisioner.isReady()) continueToApp()
                else gate.request((state.totalBytes - state.downloadedBytes).coerceAtLeast(0L)) { provisioner.start() }
            },
            enabled = !state.isDownloading,
            shape = RoundedCornerShape(28.dp),
            contentPadding = ButtonDefaults.ContentPadding,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            if (state.isDownloading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.resource_setup_downloading))
            } else {
                Icon(
                    painter = painterResource(
                        when {
                            ready -> R.drawable.ic_check
                            state.error != null -> R.drawable.ic_refresh
                            else -> R.drawable.ic_download
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when {
                        ready -> stringResource(R.string.resource_setup_continue)
                        state.error != null -> stringResource(R.string.resource_setup_retry)
                        else -> stringResource(
                            R.string.resource_setup_download,
                            formatMegabytes(state.totalBytes - state.downloadedBytes),
                        )
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    gate.Dialogs()
}

@Composable
private fun HeaderBadge(ready: Boolean) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (ready) R.drawable.ic_check else R.drawable.ic_download),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(40.dp),
        )
    }

}

@Composable
private fun OverallProgressCard(state: PaddleModelProvisioner.State) {
    val progress by animateFloatAsState(targetValue = state.progress, label = "overallProgress")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.resource_setup_bytes,
                    formatMegabytes(state.downloadedBytes),
                    formatMegabytes(state.totalBytes),
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.stage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun PackRow(
    title: String,
    description: String,
    iconRes: Int,
    tier: PaddleModelProvisioner.Tier,
    state: PaddleModelProvisioner.State,
) {
    val total = PaddleModelProvisioner.totalBytes(tier)
    val downloaded = state.downloaded(tier)
    val status = when {
        downloaded >= total -> PackStatus.READY
        state.isDownloading && downloaded > 0 -> PackStatus.DOWNLOADING
        else -> PackStatus.WAITING
    }
    val progress by animateFloatAsState(targetValue = state.progress(tier), label = "packProgress")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(8.dp))
            if (status == PackStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { progress },
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = when (status) {
                    PackStatus.READY -> stringResource(R.string.resource_pack_ready, formatMegabytes(total))
                    PackStatus.DOWNLOADING -> stringResource(
                        R.string.resource_setup_bytes,
                        formatMegabytes(downloaded),
                        formatMegabytes(total),
                    )
                    PackStatus.WAITING -> formatMegabytes(total)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (status == PackStatus.READY) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        if (status == PackStatus.READY) {
            Spacer(Modifier.width(12.dp))
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.resource_pack_ready_description),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.resource_setup_error_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

private fun formatMegabytes(bytes: Long): String = "%.1f MB".format(bytes / 1_048_576.0)
