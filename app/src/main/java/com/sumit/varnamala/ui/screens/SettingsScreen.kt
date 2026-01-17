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
import com.sumit.varnamala.data.preferences.UserPreferencesRepository

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Slider
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
import android.content.Intent
import android.app.Activity
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.runtime.mutableFloatStateOf
import androidx.core.net.toUri

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
    val userPrefsRepository = remember { UserPreferencesRepository(context) }

    val isDarkTheme by themeRepository.isDarkTheme.collectAsState(initial = true)
    val enableCacheStorage by ocrSettingsRepository.enableCacheStorage.collectAsState(initial = true)
    val cameraDetectionIntervalMs by ocrSettingsRepository.cameraDetectionIntervalMs.collectAsState(
        initial = 1000L
    )
    val cameraReadingMsPerWord by ocrSettingsRepository.cameraReadingMsPerWord.collectAsState(
        initial = 300
    )
    val cameraMaxExtraDelayMs by ocrSettingsRepository.cameraMaxExtraDelayMs.collectAsState(initial = 5000)
    val cameraSkipFrames by ocrSettingsRepository.cameraSkipFrames.collectAsState(initial = 5)
    val currentAppLanguage by languageRepository.appLanguage.collectAsState(initial = "en")
    val currentLanguageDisplayName =
        AppLanguageRepository.getLanguageDisplayName(currentAppLanguage)
    var showClearCacheDialog by remember { mutableStateOf(false) }

    var showDetectionIntervalDialog by remember { mutableStateOf(false) }
    var showReadingMsPerWordDialog by remember { mutableStateOf(false) }
    var showMaxExtraDelayDialog by remember { mutableStateOf(false) }
    var showSkipFramesDialog by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }

    // User preferences
    val hapticEnabled by userPrefsRepository.hapticFeedbackEnabled.collectAsState(initial = true)
    val autoCopyEnabled by userPrefsRepository.autoCopyEnabled.collectAsState(initial = false)
    val ttsEnabled by userPrefsRepository.ttsEnabled.collectAsState(initial = false)
    val textDisplaySize by userPrefsRepository.textDisplaySize.collectAsState(initial = 1.0f)

    val appName = stringResource(R.string.app_name)
    val appVersion = packageInfo?.versionName ?: "Unknown"

    val privacyPolicyUrl = "https://sumitrathore.web.app/privacy/varnamala"

    fun openExternalUrl(url: String) {
        val initialUri = url.toUri()
        val normalizedUrl = if (initialUri.scheme.isNullOrBlank()) {
            "https://$url"
        } else {
            url
        }

        val uri = normalizedUrl.toUri()
        val viewIntent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        val isActivityContext = context is Activity

        if (!isActivityContext) {
            viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooserIntent = Intent.createChooser(viewIntent, null).apply {
            if (!isActivityContext) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        try {
            context.startActivity(chooserIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_to_open_link),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openPlayStoreListing() {
        val packageId = context.packageName
        val marketUri = "market://details?id=$packageId".toUri()
        val viewIntent =
            Intent(Intent.ACTION_VIEW, marketUri).addCategory(Intent.CATEGORY_BROWSABLE)
        val isActivityContext = context is Activity

        if (!isActivityContext) {
            viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(viewIntent)
        } catch (_: ActivityNotFoundException) {
            openExternalUrl("https://play.google.com/store/apps/details?id=$packageId")
        }
    }

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
                .clip(RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp))
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

            SettingsCategory(title = stringResource(R.string.camera_ocr_settings)) {
                SettingItem(
                    title = stringResource(R.string.camera_ocr_detection_interval),
                    subtitle = stringResource(R.string.ms_value, cameraDetectionIntervalMs.toInt()),
                    icon = painterResource(id = R.drawable.ic_time),
                    isLastItem = false,
                    showDivider = true,
                    onClick = { showDetectionIntervalDialog = true },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                SettingItem(
                    title = stringResource(R.string.camera_ocr_reading_ms_per_word),
                    subtitle = stringResource(R.string.ms_value, cameraReadingMsPerWord),
                    icon = painterResource(id = R.drawable.ic_time),
                    isLastItem = false,
                    showDivider = true,
                    onClick = { showReadingMsPerWordDialog = true },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                SettingItem(
                    title = stringResource(R.string.camera_ocr_max_extra_delay_ms),
                    subtitle = stringResource(R.string.ms_value, cameraMaxExtraDelayMs),
                    icon = painterResource(id = R.drawable.ic_time),
                    isLastItem = false,
                    showDivider = true,
                    onClick = { showMaxExtraDelayDialog = true },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                SettingItem(
                    title = stringResource(R.string.camera_ocr_skip_frames),
                    subtitle = stringResource(R.string.frames_value, cameraSkipFrames),
                    icon = painterResource(id = R.drawable.ic_settings_outline),
                    isLastItem = true,
                    showDivider = false,
                    onClick = { showSkipFramesDialog = true },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }

            SettingsCategory(title = stringResource(R.string.ocr_features)) {
                // Overlay Toggle
                SettingItem(
                    title = stringResource(R.string.enable_overlay),
                    subtitle = if (overlayServiceRunning) stringResource(R.string.overlay_enabled) else stringResource(
                        R.string.overlay_disabled
                    ),
                    icon = painterResource(id = R.drawable.ic_image_outline),
                    isLastItem = true,
                    showDivider = false,
                    onClick = { },
                    trailingContent = {
                        Switch(
                            checked = overlayFeatureEnabled,
                            onCheckedChange = { _ ->
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
                                    val activity = context as? Activity
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
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        if (overlayServiceRunning) MaterialTheme.colorScheme.error else Color(
                                            0xFF00BCD4
                                        ),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (overlayServiceRunning) stringResource(R.string.stop_overlay)
                                else stringResource(R.string.start_overlay),
                                color = if (overlayServiceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Data & Storage Section
            SettingsCategory(title = stringResource(R.string.data_storage)) {
                // Language models
                SettingItem(
                    title = stringResource(R.string.models),
                    subtitle = stringResource(R.string.manage_models),
                    icon = painterResource(id = R.drawable.ic_database),
                    isLastItem = false,
                    showDivider = true,
                    onClick = { navController.navigate(Screen.Models.route) },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                // Cache Storage
                SettingItem(
                    title = stringResource(R.string.enable_cache_storage),
                    subtitle = stringResource(R.string.enable_cache_storage_hint),
                    icon = painterResource(id = R.drawable.ic_database),
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
                    isLastItem = true,
                    showDivider = false,
                    onClick = { showClearCacheDialog = true })
            }

            // User Experience Section
            SettingsCategory(title = stringResource(R.string.user_experience)) {
                // History
                SettingItem(
                    title = stringResource(R.string.history),
                    subtitle = stringResource(R.string.no_history_hint),
                    icon = painterResource(id = R.drawable.ic_history),
                    isLastItem = false,
                    showDivider = true,
                    onClick = { navController.navigate(Screen.History.route) },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                // Text Display Size
                SettingItem(
                    title = stringResource(R.string.text_display_size),
                    subtitle = when {
                        textDisplaySize <= 0.85f -> stringResource(R.string.text_size_small)
                        textDisplaySize <= 1.1f -> stringResource(R.string.text_size_normal)
                        textDisplaySize <= 1.35f -> stringResource(R.string.text_size_large)
                        else -> stringResource(R.string.text_size_extra_large)
                    },
                    icon = painterResource(id = R.drawable.ic_settings_outline),
                    isLastItem = false,
                    showDivider = true,
                    onClick = { showTextSizeDialog = true },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                // Haptic Feedback
                SettingItem(
                    title = stringResource(R.string.haptic_feedback),
                    subtitle = stringResource(R.string.haptic_feedback_hint),
                    icon = painterResource(id = R.drawable.ic_settings_outline),
                    isLastItem = false,
                    showDivider = true,
                    trailingContent = {
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = { newValue ->
                                scope.launch { userPrefsRepository.setHapticFeedbackEnabled(newValue) }
                            }
                        )
                    }
                )

                // Auto-copy
                SettingItem(
                    title = stringResource(R.string.auto_copy),
                    subtitle = stringResource(R.string.auto_copy_hint),
                    icon = painterResource(id = R.drawable.ic_copy),
                    isLastItem = false,
                    showDivider = true,
                    trailingContent = {
                        Switch(
                            checked = autoCopyEnabled,
                            onCheckedChange = { newValue ->
                                scope.launch { userPrefsRepository.setAutoCopyEnabled(newValue) }
                            }
                        )
                    }
                )

                // Text-to-Speech
                SettingItem(
                    title = stringResource(R.string.enable_tts),
                    subtitle = stringResource(R.string.enable_tts_hint),
                    icon = painterResource(id = R.drawable.ic_volume_up),
                    isLastItem = true,
                    showDivider = false,
                    trailingContent = {
                        Switch(
                            checked = ttsEnabled,
                            onCheckedChange = { newValue ->
                                scope.launch { userPrefsRepository.setTtsEnabled(newValue) }
                            }
                        )
                    }
                )
            }

            // Legal Section
            SettingsCategory(title = stringResource(R.string.legal)) {
                // Privacy Policy
                SettingItem(
                    title = stringResource(R.string.privacy_policy),
                    icon = painterResource(id = R.drawable.ic_shield_outline),
                    isLastItem = false,
                    showDivider = true,
                    onClick = { openExternalUrl(privacyPolicyUrl) },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    })

                // Rate App
                SettingItem(
                    title = stringResource(R.string.rate_app),
                    icon = painterResource(id = R.drawable.ic_info),
                    isLastItem = true,
                    showDivider = false,
                    onClick = { openPlayStoreListing() },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
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

    if (showDetectionIntervalDialog) {
        var pending by remember { mutableFloatStateOf(cameraDetectionIntervalMs.toFloat()) }
        AlertDialog(
            onDismissRequest = { showDetectionIntervalDialog = false },
            title = { Text(stringResource(R.string.camera_ocr_detection_interval)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.ms_value, pending.toInt()))
                    Slider(
                        value = pending,
                        onValueChange = { pending = it },
                        valueRange = 250f..3000f,
                        steps = 11
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            ocrSettingsRepository.setCameraDetectionIntervalMs(pending.toLong())
                        }
                        showDetectionIntervalDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetectionIntervalDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showReadingMsPerWordDialog) {
        var pending by remember { mutableFloatStateOf(cameraReadingMsPerWord.toFloat()) }
        AlertDialog(
            onDismissRequest = { showReadingMsPerWordDialog = false },
            title = { Text(stringResource(R.string.camera_ocr_reading_ms_per_word)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.ms_value, pending.toInt()))
                    Slider(
                        value = pending,
                        onValueChange = { pending = it },
                        valueRange = 100f..800f,
                        steps = 13
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            ocrSettingsRepository.setCameraReadingMsPerWord(pending.toInt())
                        }
                        showReadingMsPerWordDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReadingMsPerWordDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showMaxExtraDelayDialog) {
        var pending by remember { mutableFloatStateOf(cameraMaxExtraDelayMs.toFloat()) }
        AlertDialog(
            onDismissRequest = { showMaxExtraDelayDialog = false },
            title = { Text(stringResource(R.string.camera_ocr_max_extra_delay_ms)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.ms_value, pending.toInt()))
                    Slider(
                        value = pending,
                        onValueChange = { pending = it },
                        valueRange = 1000f..15000f,
                        steps = 14
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            ocrSettingsRepository.setCameraMaxExtraDelayMs(pending.toInt())
                        }
                        showMaxExtraDelayDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMaxExtraDelayDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSkipFramesDialog) {
        var pending by remember { mutableFloatStateOf(cameraSkipFrames.toFloat()) }
        AlertDialog(
            onDismissRequest = { showSkipFramesDialog = false },
            title = { Text(stringResource(R.string.camera_ocr_skip_frames)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.frames_value, pending.toInt()))
                    Slider(
                        value = pending,
                        onValueChange = { pending = it },
                        valueRange = 5f..15f,
                        steps = 9
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            ocrSettingsRepository.setCameraSkipFrames(pending.toInt())
                        }
                        showSkipFramesDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipFramesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Text Size Dialog
    if (showTextSizeDialog) {
        var pending by remember { mutableFloatStateOf(textDisplaySize) }
        AlertDialog(
            onDismissRequest = { showTextSizeDialog = false },
            title = { Text(stringResource(R.string.text_display_size)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        when {
                            pending <= 0.85f -> stringResource(R.string.text_size_small)
                            pending <= 1.1f -> stringResource(R.string.text_size_normal)
                            pending <= 1.35f -> stringResource(R.string.text_size_large)
                            else -> stringResource(R.string.text_size_extra_large)
                        }
                    )
                    Slider(
                        value = pending,
                        onValueChange = { pending = it },
                        valueRange = 0.8f..1.5f,
                        steps = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            userPrefsRepository.setTextDisplaySize(pending)
                        }
                        showTextSizeDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextSizeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
    textColor: Color? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    subtitle: String? = null,
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
