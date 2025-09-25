package com.sumit.varnamala.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.sumit.varnamala.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaPermissionHandler(
    modifier: Modifier = Modifier,
    onGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val mediaPermissionState: PermissionState = rememberPermissionState(permission = permission)

    if (mediaPermissionState.status.isGranted) {
        onGranted()
    } else {
        LaunchedEffect(Unit) {
            if (!mediaPermissionState.status.isGranted && mediaPermissionState.status.shouldShowRationale.not()) {
                mediaPermissionState.launchPermissionRequest()
            }
        }

        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.media_permission_required),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (mediaPermissionState.status.shouldShowRationale) {
                    Text(
                        text = stringResource(R.string.media_permission_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Button(onClick = { mediaPermissionState.launchPermissionRequest() }) {
                        Text(text = stringResource(R.string.grant_permission))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.media_permission_denied_description) + "\n" +
                                stringResource(R.string.media_permission_allow_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Button(onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }) {
                        Text(text = stringResource(R.string.open_settings))
                    }
                }
            }
        }
    }
}
