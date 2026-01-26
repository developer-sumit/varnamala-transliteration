package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.ui.components.OCRInfoCard
import com.sumit.varnamala.ui.components.CameraPermissionHandler
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.OCRCameraPreview
import com.sumit.varnamala.ui.components.CameraCaptureButton
import com.sumit.varnamala.ui.components.LanguageSelectionDialog
import com.sumit.varnamala.ui.components.LanguageSelectionRow
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.utils.LanguageDetector
import com.sumit.varnamala.utils.Transliterator
import com.sumit.varnamala.data.Language
import com.sumit.varnamala.data.AppLanguageRepository
import com.sumit.varnamala.data.HistoryEntry
import com.sumit.varnamala.data.TransliterationHistoryRepository
import com.sumit.varnamala.data.preferences.OCRSettingsRepository
import com.sumit.varnamala.data.preferences.OnboardingRepository
import com.sumit.varnamala.data.preferences.UserPreferencesRepository
import com.sumit.varnamala.ui.tutorial.CoachMarkStep
import com.sumit.varnamala.ui.tutorial.CoachMarksOverlay
import com.sumit.varnamala.ui.tutorial.captureBounds

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    val onboardingRepository = remember { OnboardingRepository(context) }
    val hasSeenTutorial by onboardingRepository.hasSeenCameraTutorialLoaded.collectAsState(initial = null)
    val tutorialScope = rememberCoroutineScope()
    val tutorialTargets = remember { mutableStateMapOf<String, Rect>() }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var showCameraInfoDialog by remember { mutableStateOf(false) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasBackCamera by remember { mutableStateOf<Boolean?>(null) }
    var hasFrontCamera by remember { mutableStateOf<Boolean?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    LaunchedEffect(cameraProviderFuture) {
        cameraProviderFuture.addListener(
            {
                val provider = cameraProviderFuture.get()
                hasBackCamera = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                hasFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    LaunchedEffect(hasBackCamera, hasFrontCamera) {
        if (hasBackCamera == false && hasFrontCamera == true) {
            lensFacing = CameraSelector.LENS_FACING_FRONT
        }
    }

    val ocrSettingsRepository = remember { OCRSettingsRepository(context) }
    val userPrefsRepository = remember { UserPreferencesRepository(context) }
    val historyRepository = remember { TransliterationHistoryRepository(context) }

    val detectionIntervalMs by ocrSettingsRepository.cameraDetectionIntervalMs.collectAsState(
        initial = 1000L
    )
    val readingMsPerWord by ocrSettingsRepository.cameraReadingMsPerWord.collectAsState(initial = 300)
    val maxExtraDelayMs by ocrSettingsRepository.cameraMaxExtraDelayMs.collectAsState(initial = 5000)
    val skipFrames by ocrSettingsRepository.cameraSkipFrames.collectAsState(initial = 5)

    // User preferences
    val autoCopyEnabled by userPrefsRepository.autoCopyEnabled.collectAsState(initial = false)
    val ttsEnabled by userPrefsRepository.ttsEnabled.collectAsState(initial = false)
    val ttsAutoSpeak by userPrefsRepository.ttsAutoSpeak.collectAsState(initial = false)

    // TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    LaunchedEffect(ttsEnabled) {
        if (ttsEnabled) {
            tts = TextToSpeech(context) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
            }
        } else {
            tts?.shutdown()
            tts = null
            ttsReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
        }
    }

    fun speakText(text: String, targetLangCode: String) {
        if (ttsReady && tts != null && text.isNotBlank()) {
            val locale = when (targetLangCode) {
                "hi" -> Locale("hi", "IN")
                "bn" -> Locale("bn", "IN")
                "ta" -> Locale("ta", "IN")
                "te" -> Locale("te", "IN")
                "gu" -> Locale("gu", "IN")
                "kn" -> Locale("kn", "IN")
                "ml" -> Locale("ml", "IN")
                "pa" -> Locale("pa", "IN")
                "mr" -> Locale("mr", "IN")
                else -> Locale.getDefault()
            }
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // State variables
    var flashEnabled by remember { mutableStateOf(false) }
    var detectionRunning by remember { mutableStateOf(false) }
    var lastOCRResult by remember { mutableStateOf<OCRResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Single capture functionality
    var captureImage by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedImageProcessing by remember { mutableStateOf(false) }

    // OCR result processing variables
    var rawOcrText by remember { mutableStateOf("") }
    var detectedLanguage by remember { mutableStateOf("unknown") }

    // Language selection states
    var showSourceLanguageDialog by remember { mutableStateOf(false) }
    var showTargetLanguageDialog by remember { mutableStateOf(false) }
    val availableLanguages = remember { AppLanguageRepository.getSupportedLanguages() }
    var selectedSourceLanguage by remember { mutableStateOf<Language?>(null) }
    var selectedTargetLanguage by remember { mutableStateOf(availableLanguages.firstOrNull { it.code == "hi" }) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CameraPermissionHandler(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            onGranted = {
                SideEffect {
                    cameraPermissionGranted = true
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    OCRCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        isDetectionRunning = detectionRunning,
                        flashEnabled = flashEnabled,
                        lensFacing = lensFacing,
                        preferredLanguage = null,
                        detectionIntervalMs = detectionIntervalMs,
                        readingMsPerWord = readingMsPerWord,
                        maxExtraDelayMs = maxExtraDelayMs,
                        skipFrames = skipFrames,
                        showBoundingBoxes = false,
                        showTextOverlay = false,
                        showWordBoundingBoxes = false,
                        showLineBoundingBoxes = false,
                        showBlockBoundingBoxes = false,
                        textOverride = selectedTargetLanguage?.let { targetLang ->
                            lastOCRResult?.boundingBoxes
                                ?.flatMap { it.lines }
                                ?.flatMap { it.elements }
                                ?.associate { element ->
                                    element.boundingBox to Transliterator.transliterate(
                                        text = element.text,
                                        sourceLanguage = detectedLanguage,
                                        targetLanguage = targetLang.localeCode
                                    )
                                }
                        },
                        // Single capture functionality
                        captureImage = captureImage,
                        onCaptureComplete = {
                            captureImage = false
                            capturedImageProcessing = false
                        },
                        onImageCaptured = { bitmap ->
                            capturedBitmap = bitmap
                            capturedImageProcessing = true
                            isCapturing = false
                        },
                        onOCRResult = { result ->
                            if (capturedBitmap != null) {
                                // This is a result from captured image
                                rawOcrText = result.text
                                lastOCRResult = result
                                capturedImageProcessing = false
                            } else {
                                // This is a real-time OCR result
                                lastOCRResult = result
                                rawOcrText = result.text
                                errorMessage = null

                                // Detect language for OCR results
                                if (result.text.isNotBlank()) {
                                    detectedLanguage = LanguageDetector.detectLanguage(result.text)
                                }
                            }
                        },
                        onManualLanguageRequired = { },
                        onError = { error ->
                            errorMessage = error
                            if (isCapturing) {
                                isCapturing = false
                                capturedImageProcessing = false
                            }
                        }
                    )

                    // Status indicator
                    if (detectionRunning || isCapturing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(12.dp, 8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = if (isCapturing) Color.Blue else Color.Green
                            ) {}
                            Text(
                                text = if (isCapturing) "Capturing" else "Scanning",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            })

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, topInset + 10.dp, 16.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Info button
            GlassButton(
                icon = painterResource(R.drawable.ic_info),
                background = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                onClick = { showCameraInfoDialog = true }
            )

            // Right side - History and Settings
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    icon = painterResource(R.drawable.ic_history),
                    background = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    onClick = { navController.navigate(Screen.History.route) }
                )
                GlassButton(
                    modifier = Modifier.captureBounds("settings") { key, rect ->
                        tutorialTargets[key] = rect
                    },
                    icon = painterResource(R.drawable.ic_settings_outline),
                    background = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    onClick = { navController.navigate(Screen.Settings.route) }
                )
            }
        }

        // Bottom section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = topInset + 74.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // OCR Result Card
            if (rawOcrText.isNotBlank() || errorMessage != null) {
                val targetLang = selectedTargetLanguage
                val transliteratedText = if (targetLang != null) {
                    Transliterator.transliterate(
                        text = rawOcrText,
                        sourceLanguage = detectedLanguage,
                        targetLanguage = targetLang.localeCode
                    )
                } else rawOcrText

                LaunchedEffect(rawOcrText, transliteratedText) {
                    if (rawOcrText.isNotBlank() && transliteratedText.isNotBlank() && transliteratedText != rawOcrText) {
                        // Save to history
                        historyRepository.addEntry(
                            HistoryEntry(
                                originalText = rawOcrText,
                                transliteratedText = transliteratedText,
                                sourceLanguage = detectedLanguage,
                                targetLanguage = targetLang?.localeCode ?: "en",
                                source = "camera"
                            )
                        )

                        // Auto-copy if enabled
                        if (autoCopyEnabled) {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Transliterated Text",
                                    transliteratedText
                                )
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Auto-speak if enabled
                        if (ttsEnabled && ttsAutoSpeak && targetLang != null) {
                            speakText(transliteratedText, targetLang.localeCode)
                        }
                    }
                }

                OCRInfoCard(
                    ocrLoading = (detectionRunning || isCapturing) && rawOcrText.isBlank(),
                    ocrError = errorMessage,
                    rawOcrText = rawOcrText,
                    transliteratedText = transliteratedText,
                    onCopy = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(
                            context.getString(R.string.ocr_text),
                            transliteratedText
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            context.getString(R.string.copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }

            LanguageSelectionRow(
                sourceLanguage = selectedSourceLanguage,
                targetLanguage = selectedTargetLanguage,
                onSourceLanguageClick = { showSourceLanguageDialog = true },
                onTargetLanguageClick = { showTargetLanguageDialog = true },
                modifier = Modifier.captureBounds("languages") { key, rect ->
                    tutorialTargets[key] = rect
                }
            )

            // Bottom controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash toggle
                GlassButton(
                    modifier = Modifier.captureBounds("flash") { key, rect ->
                        tutorialTargets[key] = rect
                    },
                    icon = if (flashEnabled) painterResource(R.drawable.ic_flash_on_outline) else painterResource(
                        R.drawable.ic_flash_off_outline
                    ),
                    size = 60.dp,
                    onClick = { flashEnabled = !flashEnabled }
                )

                // Capture button
                CameraCaptureButton(
                    text = when {
                        isCapturing -> "Capturing..."
                        detectionRunning -> "Scanning..."
                        else -> "Tap/Hold"
                    },
                    size = 90.dp,
                    textColor = Color.White,
                    backgroundColor = Color(0xFF2196F3),
                    longPressBackgroundColor = Color(0xFF4CAF50),
                    progressColor = Color.White,
                    isRealTimeMode = detectionRunning,
                    modifier = Modifier.captureBounds("capture") { key, rect ->
                        tutorialTargets[key] = rect
                    },
                    onSingleCapture = {
                        // Tap = capture single image
                        if (!isCapturing && !detectionRunning) {
                            // Clear any previous capture
                            capturedBitmap = null
                            rawOcrText = ""
                            lastOCRResult = null
                            errorMessage = null

                            isCapturing = true
                            captureImage = true
                        }
                    },
                    onLongPressStart = {
                        // Hold = start real-time scanning
                        if (!isCapturing) {
                            // Clear any captured image
                            capturedBitmap = null
                            rawOcrText = ""
                            lastOCRResult = null
                            errorMessage = null

                            detectionRunning = true
                        }
                    },
                    onLongPressEnd = {
                        // Release = stop real-time scanning
                        detectionRunning = false
                    }
                )

                // Camera Switch button
                GlassButton(
                    modifier = Modifier.captureBounds("switch_camera") { key, rect ->
                        tutorialTargets[key] = rect
                    },
                    icon = painterResource(R.drawable.ic_switch_camera),
                    size = 60.dp,
                    enabled = hasBackCamera == true && hasFrontCamera == true,
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                )
            }
        }

        val shouldShowTutorial =
            cameraPermissionGranted &&
                    capturedBitmap == null &&
                    hasSeenTutorial != null &&
                    (hasSeenTutorial == false)
        if (shouldShowTutorial) {
            val steps = listOf(
                CoachMarkStep(
                    targetKey = "settings",
                    title = stringResource(R.string.tutorial_settings_title),
                    message = stringResource(R.string.tutorial_settings_message)
                ),
                CoachMarkStep(
                    targetKey = "languages",
                    title = stringResource(R.string.tutorial_languages_title),
                    message = stringResource(R.string.tutorial_languages_message)
                ),
                CoachMarkStep(
                    targetKey = "flash",
                    title = stringResource(R.string.tutorial_flash_title),
                    message = stringResource(R.string.tutorial_flash_message)
                ),
                CoachMarkStep(
                    targetKey = "capture",
                    title = stringResource(R.string.tutorial_capture_title),
                    message = stringResource(R.string.tutorial_capture_message)
                ),
                CoachMarkStep(
                    targetKey = "switch_camera",
                    title = stringResource(R.string.tutorial_switch_camera_title),
                    message = stringResource(R.string.tutorial_switch_camera_message)
                ),
            )

            CoachMarksOverlay(
                steps = steps,
                targetRects = tutorialTargets,
                nextLabel = stringResource(R.string.tutorial_next),
                skipLabel = stringResource(R.string.tutorial_skip),
                doneLabel = stringResource(R.string.done),
                onFinished = {
                    tutorialScope.launch {
                        onboardingRepository.setHasSeenCameraTutorial(true)
                    }
                }
            )
        }

        // Captured Image Overlay - shows as full screen over camera
        if (capturedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Show captured image - properly oriented
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )

                // Close button at top right
                IconButton(
                    onClick = {
                        capturedBitmap = null
                        rawOcrText = ""
                        lastOCRResult = null
                        errorMessage = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_outline),
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Bottom section with same controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    // OCR Result Card
                    if (rawOcrText.isNotBlank() || capturedImageProcessing || errorMessage != null) {
                        val transliteratedText = selectedTargetLanguage?.let { targetLang ->
                            if (rawOcrText.isNotBlank()) {
                                Transliterator.transliterate(
                                    text = rawOcrText,
                                    sourceLanguage = detectedLanguage,
                                    targetLanguage = targetLang.localeCode
                                )
                            } else rawOcrText
                        } ?: rawOcrText

                        OCRInfoCard(
                            ocrLoading = capturedImageProcessing,
                            ocrError = errorMessage,
                            rawOcrText = rawOcrText,
                            transliteratedText = transliteratedText,
                            onCopy = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(
                                    context.getString(R.string.ocr_text),
                                    transliteratedText
                                )
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.copied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        )
                    }

                    // Bottom controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flash toggle
                        GlassButton(
                            icon = if (flashEnabled) painterResource(R.drawable.ic_flash_on_outline) else painterResource(
                                R.drawable.ic_flash_off_outline
                            ), onClick = { flashEnabled = !flashEnabled }
                        )

                        // Capture button - can capture another or start scanning
                        CameraCaptureButton(
                            text = "Tap/Hold",
                            size = 90.dp,
                            textColor = Color.White,
                            backgroundColor = Color(0xFF2196F3),
                            longPressBackgroundColor = Color(0xFF4CAF50),
                            progressColor = Color.White,
                            isRealTimeMode = false,
                            onSingleCapture = {
                                // Tap = capture another image
                                capturedBitmap = null
                                rawOcrText = ""
                                lastOCRResult = null
                                errorMessage = null
                                isCapturing = true
                                captureImage = true
                            },
                            onLongPressStart = {
                                // Hold = start real-time scanning (closes captured image)
                                capturedBitmap = null
                                rawOcrText = ""
                                lastOCRResult = null
                                errorMessage = null
                                detectionRunning = true
                            },
                            onLongPressEnd = {
                                detectionRunning = false
                            }
                        )

                        // Switch camera button
                        GlassButton(
                            icon = painterResource(R.drawable.ic_switch_camera),
                            enabled = hasBackCamera == true && hasFrontCamera == true,
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showCameraInfoDialog) {
            AlertDialog(
                onDismissRequest = { showCameraInfoDialog = false },
                confirmButton = {
                    TextButton(onClick = { showCameraInfoDialog = false }) {
                        Text(text = stringResource(R.string.close))
                    }
                },
                title = { Text(text = stringResource(R.string.camera_info_title)) },
                text = { Text(text = stringResource(R.string.camera_info_message)) }
            )
        }

        // Language Selection Dialogs
        if (showSourceLanguageDialog) {
            LanguageSelectionDialog(
                title = stringResource(R.string.select_ocr_language),
                currentLanguage = selectedSourceLanguage,
                onLanguageSelected = { language ->
                    selectedSourceLanguage = language
                    showSourceLanguageDialog = false
                    // Trigger OCR re-processing if needed
                },
                onDismiss = { showSourceLanguageDialog = false },
                includeDefaultOption = true,
                defaultOptionLabel = stringResource(R.string.default_mlkit),
                onDefaultSelected = {
                    selectedSourceLanguage = null
                    showSourceLanguageDialog = false
                }
            )
        }

        if (showTargetLanguageDialog) {
            LanguageSelectionDialog(
                title = stringResource(R.string.select_transliteration_language),
                currentLanguage = selectedTargetLanguage,
                onLanguageSelected = { language ->
                    selectedTargetLanguage = language
                    showTargetLanguageDialog = false
                    // Trigger transliteration update if needed
                },
                onDismiss = { showTargetLanguageDialog = false },
                showDownloadOption = false  // No download needed for transliteration
            )
        }
    }
}

