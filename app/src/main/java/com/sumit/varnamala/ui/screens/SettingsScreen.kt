package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.data.ocr.OCRCacheRepository
import com.sumit.varnamala.service.OverlayManager
import com.sumit.varnamala.utils.OverlayPermissionHelper
import com.sumit.varnamala.utils.ScreenCaptureHelper
import com.sumit.varnamala.data.preferences.OCRSettingsRepository
import com.sumit.varnamala.data.preferences.ThemeSettingsRepository
import com.sumit.varnamala.data.preferences.AppLanguageRepository

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController, themeRepository: ThemeSettingsRepository
) {
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName
    val packageInfo = packageManager.getPackageInfo(packageName, 0)

    val scope = rememberCoroutineScope()
    val cacheRepository = remember { OCRCacheRepository(context) }
    val ocrSettingsRepository = remember { OCRSettingsRepository(context) }
    val languageRepository = remember { AppLanguageRepository(context) }

    val isDarkTheme by themeRepository.isDarkTheme.collectAsState(initial = true)
    val enableCacheStorage by ocrSettingsRepository.enableCacheStorage.collectAsState(initial = true)
    val currentAppLanguage by languageRepository.appLanguage.collectAsState(initial = "en")
    val currentLanguageDisplayName =
        AppLanguageRepository.getLanguageDisplayName(currentAppLanguage)
    var showClearCacheDialog by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            // Appearance Section
            SettingsCategory(title = stringResource(R.string.appearance)) {
                // Dark Mode Toggle
                SettingItem(
                    title = stringResource(R.string.dark_mode),
                    icon = if (isDarkTheme) painterResource(id = R.drawable.ic_moon_outline) else painterResource(
                        id = R.drawable.ic_sun_outline
                    ),
                    isFirstItem = true,
                    isLastItem = false,
                    showDivider = true,
                    trailingContent = {
                        Switch(
                            checked = isDarkTheme, onCheckedChange = { newValue ->
                                scope.launch { themeRepository.setIsDarkTheme(newValue) }
                            })
                    })

                // Language Selection
                SettingItem(
                    title = stringResource(R.string.language),
                    subtitle = currentLanguageDisplayName,
                    icon = painterResource(id = R.drawable.ic_language),
                    isFirstItem = false,
                    isLastItem = true,
                    showDivider = false,
                    onClick = { navController.navigate(Screen.Language.route) },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    })
            }

            // OCR Features Section
            var overlayServiceRunning by remember { mutableStateOf(OverlayManager.isOverlayRunning()) }
            // Initialize overlayFeatureEnabled based on current permission status
            var overlayFeatureEnabled by remember {
                mutableStateOf(OverlayManager.canDrawOverlays(context))
            }
            var hasOverlayPermission by remember {
                mutableStateOf(OverlayManager.canDrawOverlays(context))
            }
            var permissionCheckTrigger by remember { mutableIntStateOf(0) }

            // Check permission state on composition and when returning from settings
            LaunchedEffect(permissionCheckTrigger) {
                val previousPermission = hasOverlayPermission
                hasOverlayPermission = OverlayManager.canDrawOverlays(context)

                // Check if overlay service is running
                overlayServiceRunning = OverlayManager.isOverlayRunning()

                // Update feature enabled state based on current permission
                // If permission was just granted, enable the feature
                if (!previousPermission && hasOverlayPermission) {
                    overlayFeatureEnabled = true
                }
                // If permission was revoked, disable the feature
                if (previousPermission && !hasOverlayPermission) {
                    overlayFeatureEnabled = false
                    // Stop service if running
                    if (overlayServiceRunning) {
                        OverlayManager.stopOverlay(context)
                        overlayServiceRunning = false
                    }
                }
            }

            // Recheck permission when window gains focus (user returns from settings)
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionCheckTrigger++
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            SettingsCategory(title = stringResource(R.string.ocr_features)) {
                // Overlay Toggle
                SettingItem(
                    title = stringResource(R.string.enable_overlay),
                    subtitle = if (overlayServiceRunning) stringResource(R.string.overlay_enabled) else stringResource(
                        R.string.overlay_disabled
                    ),
                    icon = painterResource(id = R.drawable.ic_image_outline),
                    isFirstItem = true,
                    isLastItem = true,
                    showDivider = false,
                    onClick = { },
                    trailingContent = {
                        Switch(
                            checked = overlayFeatureEnabled,
                            onCheckedChange = { newValue ->
                                OverlayPermissionHelper.requestOverlayPermission(
                                    context
                                )
                            }
                        )
                    }
                )

                // Show "Start Overlay" button when feature is enabled
                if (overlayFeatureEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                if (!overlayServiceRunning) {
                                    val activity = context as? android.app.Activity
                                    if (activity != null) {
                                        OverlayManager.requestScreenCapturePermission(
                                            activity,
                                            ScreenCaptureHelper.REQUEST_MEDIA_PROJECTION
                                        )
                                    }
                                } else {
                                    OverlayManager.stopOverlay(context)
                                    overlayServiceRunning = false
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_image_outline),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (overlayServiceRunning) stringResource(R.string.stop_overlay)
                                else stringResource(R.string.start_overlay)
                            )
                        }
                    }
                }
            }

            // Data & Storage Section
            SettingsCategory(title = stringResource(R.string.data_storage)) {
                // Cache Storage
                SettingItem(
                    title = stringResource(R.string.enable_cache_storage),
                    subtitle = stringResource(R.string.enable_cache_storage_hint),
                    icon = painterResource(id = R.drawable.ic_database),
                    isFirstItem = true,
                    isLastItem = false,
                    showDivider = true,
                    trailingContent = {
                        Switch(
                            checked = enableCacheStorage, onCheckedChange = { newValue ->
                                scope.launch { ocrSettingsRepository.setEnableCacheStorage(newValue) }
                            })
                    })

                // Clear Cache
                SettingItem(
                    title = stringResource(R.string.clear_cache),
                    icon = painterResource(id = R.drawable.ic_delete_outline),
                    textColor = MaterialTheme.colorScheme.error,
                    isFirstItem = false,
                    isLastItem = true,
                    showDivider = false,
                    onClick = { showClearCacheDialog = true })
            }

            // Legal Section
            SettingsCategory(title = stringResource(R.string.legal)) {
                // Terms of Service
                SettingItem(
                    title = stringResource(R.string.terms_of_service),
                    icon = painterResource(id = R.drawable.ic_shield_outline),
                    isFirstItem = true,
                    isLastItem = false,
                    showDivider = true,
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
                    isFirstItem = false,
                    isLastItem = true,
                    showDivider = false,
                    onClick = { },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    })
            }

            // App Version
            Text(
                text = "$appName v$appVersion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache)) },
            text = {
                Text(stringResource(R.string.clear_cache_confirmation_settings))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            cacheRepository.clearCache()
                        }
                        showClearCacheDialog = false
                    }) {
                    Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            })
    }
}

@Composable
fun SettingsCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Category header
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Card containing grouped settings
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            content()
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
    trailingContent: @Composable (() -> Unit)? = null,
    subtitle: String? = null,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    showDivider: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null) {
                    onClick?.invoke()
                }
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = textColor ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        color = textColor ?: MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = (textColor
                                ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (trailingContent != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    trailingContent()
                }
            }
        }

        // Divider between items in the same group
        if (showDivider && !isLastItem) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
    trailingContent: @Composable (() -> Unit)? = null,
    subtitle: String? = null
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
            .heightIn(min = 80.dp)
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
            Column {
                Text(
                    text = title,
                    color = textColor ?: MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = (textColor
                            ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
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

