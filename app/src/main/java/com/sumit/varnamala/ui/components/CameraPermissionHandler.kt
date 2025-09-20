package com.sumit.varnamala.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
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
fun CameraPermissionHandler(
    onGranted: @Composable () -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraPermissionState: PermissionState =
        rememberPermissionState(permission = Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        onGranted()
    } else {
        LaunchedEffect(Unit) {
            if (!cameraPermissionState.status.isGranted &&
                cameraPermissionState.status.shouldShowRationale.not()
            ) {
                cameraPermissionState.launchPermissionRequest()
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
                    text = stringResource(R.string.camera_permission_required),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (cameraPermissionState.status.shouldShowRationale) {
                    Text(
                        text = stringResource(R.string.camera_permission_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text(text = stringResource(R.string.grant_permission))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.camera_permission_denied_description) + "\n" +
                                stringResource(R.string.camera_permission_allow_hint),
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
