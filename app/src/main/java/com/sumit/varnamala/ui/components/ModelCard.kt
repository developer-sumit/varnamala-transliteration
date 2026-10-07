package com.sumit.varnamala.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumit.varnamala.R

/**
 * One downloadable model: its name, size, whether it is installed, live download progress, and
 * Download / Delete buttons. Deleting always asks for confirmation first.
 */
@Composable
fun ModelCard(
    title: String,
    description: String,
    sizeBytes: Long,
    installed: Boolean,
    downloading: Boolean,
    progress: Float,
    error: String?,
    onDownload: () -> Unit,
    /** Null when the model cannot be removed. */
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val size = Formatter.formatShortFileSize(context, sizeBytes)
    val status = when {
        downloading -> stringResource(R.string.model_status_downloading, (progress * 100).toInt())
        installed -> stringResource(R.string.model_status_installed)
        else -> stringResource(R.string.model_status_not_installed)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $size, $status" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(size, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (installed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (downloading) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        if (error != null && !downloading) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!installed && !downloading) {
                Button(onClick = onDownload) {
                    Text(stringResource(if (error != null) R.string.retry else R.string.download))
                }
            }
            if (installed && onDelete != null) {
                OutlinedButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete)) }
            }
        }
    }

    if (confirmDelete) {
        ConfirmBottomSheet(
            title = stringResource(R.string.model_delete_title, title),
            message = stringResource(R.string.model_delete_message, size),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete?.invoke()
            },
        )
    }
}
