package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.ui.components.CameraPermissionHandler
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.InfoBottomSheet
import com.sumit.varnamala.ui.components.OCRCameraPreview
import com.sumit.varnamala.ui.components.OCROverlayCanvas
import com.sumit.varnamala.ui.components.CameraCaptureButton
import com.sumit.varnamala.ui.components.LanguageSelectionRow
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.OcrLanguageCatalog
import com.sumit.varnamala.transliteration.LanguageDetector
import com.sumit.varnamala.utils.Transliterator
import com.sumit.varnamala.data.Language
import com.sumit.varnamala.data.HistoryEntry
import com.sumit.varnamala.data.TransliterationLanguageCatalog
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
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sumit.varnamala.data.ScanThumbnails
import com.sumit.varnamala.tts.SpeechPart
import com.sumit.varnamala.ui.components.ScanResultCard
import com.sumit.varnamala.ui.components.rememberSpeechController
import com.sumit.varnamala.ui.components.speechLanguage
import kotlinx.coroutines.launch

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
    val selectedOcrModelCode by ocrSettingsRepository.selectedOcrModelCode.collectAsState(
        initial = OcrLanguageCatalog.DEFAULT_SOURCE_CODE
    )
    val selectedTargetCode by ocrSettingsRepository.selectedTargetTransliterationCode.collectAsState(
        initial = "hi"
    )

    // User preferences
    val autoCopyEnabled by userPrefsRepository.autoCopyEnabled.collectAsState(initial = false)
    val ttsAutoSpeak by userPrefsRepository.ttsAutoSpeak.collectAsState(initial = false)

    // Read-aloud: Rasa voice for Indic languages once downloaded, system voice otherwise.
    val speech = rememberSpeechController()
    val highContrast by userPrefsRepository.highContrast.collectAsState(initial = false)
    val textDisplaySize by userPrefsRepository.textDisplaySize.collectAsState(initial = 1f)
    val readingMode by userPrefsRepository.readingMode.collectAsState(initial = false)
    val hapticEnabled by userPrefsRepository.hapticFeedbackEnabled.collectAsState(initial = true)
    var lastSpokenText by remember { mutableStateOf("") }

    // State variables
    var flashEnabled by remember { mutableStateOf(false) }
    var detectionRunning by remember { mutableStateOf(false) }
    var lastOCRResult by remember { mutableStateOf<OCRResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Single capture functionality
    var captureImage by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedDisplaySize by remember { mutableStateOf(IntSize.Zero) }
    var capturedImageProcessing by remember { mutableStateOf(false) }

    // OCR result processing variables
    var rawOcrText by remember { mutableStateOf("") }
    var detectedLanguage by remember { mutableStateOf("unknown") }

    val selectedOcrModel = remember(selectedOcrModelCode) {
        OcrLanguageCatalog.sourceOrDefault(selectedOcrModelCode)
    }
    val selectedTargetLanguage = remember(selectedTargetCode) {
        TransliterationLanguageCatalog.find(selectedTargetCode)
            ?: TransliterationLanguageCatalog.defaultTarget()
    }
    val selectedSourceLanguage = remember(selectedOcrModel) {
        Language(
            code = selectedOcrModel.code,
            name = selectedOcrModel.displayName,
            nativeName = selectedOcrModel.nativeName,
            flagEmoji = "",
            localeCode = selectedOcrModel.code
        )
    }

    // PP-OCRv6 models are bundled with the app and loaded by its SDK.
    fun hasRequiredOcrModel() = true

    fun promptModelDownload() {
        Toast.makeText(
            context,
            context.getString(R.string.model_required_for_ocr, selectedOcrModel.displayName),
            Toast.LENGTH_LONG
        ).show()
        navController.navigate(Screen.OcrLanguage.route)
    }

    // The whole recognized text, converted once per result rather than on every recomposition.
    val convertedText = remember(rawOcrText, detectedLanguage, selectedTargetLanguage) {
        if (rawOcrText.isBlank()) "" else Transliterator.transliterate(
            text = rawOcrText,
            sourceLanguage = detectedLanguage,
            targetLanguage = selectedTargetLanguage.localeCode
        )
    }

    fun vibrateCue(pulses: Int) {
        if (!hapticEnabled) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        // Short pulses with gaps: one = text on the left, two = in the middle, three = on the right.
        val pattern = LongArray(pulses * 2 + 1) { i -> if (i == 0) 0L else if (i % 2 == 1) 120L else 60L }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        else @Suppress("DEPRECATION") vibrator.vibrate(pattern, -1)
    }

    // Keep a record of what was scanned, with a small picture, once a photo has been read.
    LaunchedEffect(rawOcrText, convertedText, capturedBitmap) {
        val photo = capturedBitmap ?: return@LaunchedEffect
        if (rawOcrText.isNotBlank() && convertedText.isNotBlank() && convertedText != rawOcrText) {
            historyRepository.addEntry(
                HistoryEntry(
                    originalText = rawOcrText,
                    transliteratedText = convertedText,
                    sourceLanguage = detectedLanguage,
                    targetLanguage = selectedTargetLanguage.localeCode,
                    source = "camera",
                    imageRef = ScanThumbnails.save(context, photo),
                )
            )
            if (autoCopyEnabled) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.clipboard_label_transliterated_text), convertedText))
            }
            // Auto-speak: read a photographed sign as soon as it has been recognized.
            if (ttsAutoSpeak) {
                speech.toggleConverted(
                    "scan",
                    SpeechPart(rawOcrText, speechLanguage(detectedLanguage)),
                    SpeechPart(convertedText, selectedTargetLanguage.localeCode),
                )
            }
        }
    }

    // Read-aloud mode: scan continuously, speak each new sign once it has finished reading the last
    // one, and buzz to say where the text is in the frame.
    LaunchedEffect(readingMode) {
        detectionRunning = readingMode
        if (!readingMode) speech.stop()
    }
    LaunchedEffect(lastOCRResult) {
        val result = lastOCRResult ?: return@LaunchedEffect
        if (!readingMode || capturedBitmap != null || result.text.isBlank()) return@LaunchedEffect
        if (result.text == lastSpokenText || speech.activeKey != null) return@LaunchedEffect
        lastSpokenText = result.text
        val centre = result.allLines.filter { !it.isLowConfidence }.map { it.boundingBox.centerX() }.average() / result.imageWidth.coerceAtLeast(1)
        vibrateCue(if (centre < 0.38) 1 else if (centre > 0.62) 3 else 2)
        val converted = Transliterator.transliterate(
            text = result.text,
            sourceLanguage = LanguageDetector.detectLanguage(result.text),
            targetLanguage = selectedTargetLanguage.localeCode,
        )
        speech.toggleConverted(
            "reading",
            SpeechPart(result.text, speechLanguage(LanguageDetector.detectLanguage(result.text))),
            SpeechPart(converted, selectedTargetLanguage.localeCode),
        )
    }

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
                        paddleLanguage = selectedOcrModel,
                        onWordTap = { word -> speech.start("word", listOf(SpeechPart(word, speechLanguage(detectedLanguage)))) },
                        highContrast = highContrast,
                        detectionIntervalMs = detectionIntervalMs,
                        readingMsPerWord = readingMsPerWord,
                        maxExtraDelayMs = maxExtraDelayMs,
                        skipFrames = skipFrames,
                        showBoundingBoxes = true,
                        showTextOverlay = true,
                        showWordBoundingBoxes = false,
                        showLineBoundingBoxes = true,
                        showBlockBoundingBoxes = true,
                        textOverride = selectedTargetLanguage?.let { targetLang ->
                            lastOCRResult?.boundingBoxes
                                ?.flatMap { it.lines }
                                ?.flatMap { it.elements }
                                ?.filter { !it.isLowConfidence }
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
            // Left side - Info button and read-aloud mode
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    description = stringResource(R.string.a11y_info),
                    icon = painterResource(R.drawable.ic_info),
                    background = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    onClick = { showCameraInfoDialog = true }
                )
                GlassButton(
                    modifier = Modifier.semantics {
                        contentDescription = context.getString(if (readingMode) R.string.reading_mode_on else R.string.reading_mode_off)
                    },
                    icon = painterResource(R.drawable.ic_volume_up),
                    background = if (readingMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    onClick = { tutorialScope.launch { userPrefsRepository.setReadingMode(!readingMode) } }
                )
            }

            // Right side - History and Settings
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    description = stringResource(R.string.history),
                    icon = painterResource(R.drawable.ic_history),
                    background = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    onClick = { navController.navigate(Screen.History.route) }
                )
                GlassButton(
                    modifier = Modifier.captureBounds("settings") { key, rect ->
                        tutorialTargets[key] = rect
                    },
                    description = stringResource(R.string.a11y_settings),
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
            LanguageSelectionRow(
                sourceLanguage = selectedSourceLanguage,
                targetLanguage = selectedTargetLanguage,
                onSourceLanguageClick = { navController.navigate(Screen.OcrLanguage.route) },
                onTargetLanguageClick = {
                    navController.navigate(
                        Screen.TransliterationLanguage.create(
                            Screen.TransliterationLanguage.ROLE_TARGET
                        )
                    )
                },
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
                    description = stringResource(if (flashEnabled) R.string.a11y_flash_on else R.string.a11y_flash_off),
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
                            if (!hasRequiredOcrModel()) {
                                promptModelDownload()
                                return@CameraCaptureButton
                            }
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
                            if (!hasRequiredOcrModel()) {
                                promptModelDownload()
                                return@CameraCaptureButton
                            }
                            // Clear any captured image
                            capturedBitmap = null
                            rawOcrText = ""
                            lastOCRResult = null
                            errorMessage = null

                            detectionRunning = true
                        }
                    },
                    onLongPressEnd = {
                        // Release = stop real-time scanning (read-aloud mode keeps scanning)
                        detectionRunning = readingMode
                    }
                )

                // Camera Switch button
                GlassButton(
                    modifier = Modifier.captureBounds("switch_camera") { key, rect ->
                        tutorialTargets[key] = rect
                    },
                    description = stringResource(R.string.a11y_switch_camera),
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
                    .onGloballyPositioned { capturedDisplaySize = it.size }
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

                lastOCRResult?.let { result ->
                    val bitmap = capturedBitmap!!
                    OCROverlayCanvas(
                        result = result,
                        textOverride = selectedTargetLanguage?.let { targetLang ->
                            result.boundingBoxes
                                .flatMap { it.lines }
                                .flatMap { it.elements }
                                .filter { !it.isLowConfidence }
                                .associate { element ->
                                    element.boundingBox to Transliterator.transliterate(
                                        text = element.text,
                                        sourceLanguage = detectedLanguage,
                                        targetLanguage = targetLang.localeCode
                                    )
                                }
                        },
                        containerSize = capturedDisplaySize,
                        sourceBitmap = bitmap,
                        showBoundingBoxes = true,
                        showTextOverlay = true,
                        enableTextCopy = true,
                        onWordTap = { word -> speech.start("word", listOf(SpeechPart(word, speechLanguage(detectedLanguage)))) },
                        highContrast = highContrast,
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                    )
                }

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
                    if (rawOcrText.isNotBlank()) {
                        ScanResultCard(
                            original = rawOcrText,
                            converted = convertedText,
                            originalLanguage = speechLanguage(detectedLanguage),
                            convertedLanguage = selectedTargetLanguage.localeCode,
                            speech = speech,
                            textScale = textDisplaySize,
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
                            description = stringResource(if (flashEnabled) R.string.a11y_flash_on else R.string.a11y_flash_off),
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
                                if (!hasRequiredOcrModel()) {
                                    promptModelDownload()
                                    return@CameraCaptureButton
                                }
                                capturedBitmap = null
                                rawOcrText = ""
                                lastOCRResult = null
                                errorMessage = null
                                isCapturing = true
                                captureImage = true
                            },
                            onLongPressStart = {
                                // Hold = start real-time scanning (closes captured image)
                                if (!hasRequiredOcrModel()) {
                                    promptModelDownload()
                                    return@CameraCaptureButton
                                }
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
                            description = stringResource(R.string.a11y_switch_camera),
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
            InfoBottomSheet(
                title = stringResource(R.string.camera_info_title),
                message = stringResource(R.string.camera_info_message),
                onDismiss = { showCameraInfoDialog = false }
            )
        }
    }
}
