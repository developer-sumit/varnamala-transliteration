package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.data.ocr.OCRCacheRepository

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController,
    isDarkTheme: Boolean
) {
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName
    val packageInfo = packageManager.getPackageInfo(packageName, 0)

    val scope = rememberCoroutineScope()
    val cacheRepository = remember { OCRCacheRepository(context) }

    val appName = stringResource(R.string.app_name)
    val appVersion = packageInfo?.versionName ?: "Unknown"

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp, topInset + 5.dp, 16.dp, 0.dp)
    ) {
        // Back button
        GlassButton(
            icon = painterResource(R.drawable.ic_chevron_left),
            onClick = { navController.popBackStack() })

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .verticalScroll(scrollState)
        ) {
            // Dark Mode Toggle
            SettingItem(
                title = stringResource(R.string.dark_mode),
                icon = if (isDarkTheme) painterResource(id = R.drawable.ic_moon_outline) else painterResource(
                    id = R.drawable.ic_sun_outline
                ),
                trailingContent = {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { })
                })
            // Language Selection
            SettingItem(
                title = stringResource(R.string.language),
                icon = painterResource(id = R.drawable.ic_language),
                onClick = { },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                })
            // Terms of Service
            SettingItem(
                title = stringResource(R.string.terms_of_service),
                icon = painterResource(id = R.drawable.ic_shield_outline),
                onClick = { },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                })
            // Privacy Policy
            SettingItem(
                title = stringResource(R.string.privacy_policy),
                icon = painterResource(id = R.drawable.ic_shield_outline),
                onClick = { },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                })
            // Clear Cached Data (Detected Language, Text Blocks, etc)
            SettingItem(
                title = stringResource(R.string.clear_cache),
                icon = painterResource(id = R.drawable.ic_delete_outline),
                textColor = MaterialTheme.colorScheme.errorContainer,
                bgColor = MaterialTheme.colorScheme.error,
                onClick = {
                    scope.launch {
                        cacheRepository.clearCache()
                    }
                })

            // App Version
            Text(
                text = "$appName v$appVersion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    icon: Painter,
    bgColor: Color? = null,
    textColor: Color? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor ?: MaterialTheme.colorScheme.surface)
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(16.dp, 10.dp)) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = textColor ?: MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                color = textColor ?: MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        if (trailingContent != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .defaultMinSize(minHeight = 52.dp)
                    .align(Alignment.CenterVertically)
            ) {
                trailingContent()
            }
        }
    }
}

