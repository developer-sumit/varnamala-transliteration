package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.ui.components.OCRInfoCard
import com.sumit.varnamala.ui.components.CameraPermissionHandler
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.OCRCameraPreview
import com.sumit.varnamala.ui.components.DisplayOptionsDialog
import com.sumit.varnamala.ui.components.DetectionOptionsDialog
import com.sumit.varnamala.ui.components.CameraCaptureButton
import com.sumit.varnamala.ui.components.CapturedImageOverlay
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.preferences.OCRSettingsRepository
import com.sumit.varnamala.utils.LanguageDetector

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    // Initialize repositories
    val settingsRepository = remember { OCRSettingsRepository(context) }

    // Create the ViewModel
    val viewModel: com.sumit.varnamala.ui.viewmodels.OCRViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(
            factory = com.sumit.varnamala.ui.viewmodels.OCRViewModelFactory(context)
        )

    // State variables
    var flashEnabled by remember { mutableStateOf(false) }
    var detectionRunning by remember { mutableStateOf(false) }
    var lastOCRResult by remember { mutableStateOf<OCRResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detectionCount by remember { mutableIntStateOf(0) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }
    var preferredLanguage by remember { mutableStateOf<String?>(null) }
    var manualLanguageRequired by remember { mutableStateOf(false) }
    var detectionIntervalMs by remember { mutableLongStateOf(1000L) }
    var readingMsPerWord by remember { mutableIntStateOf(300) }
    var maxExtraDelayMs by remember { mutableIntStateOf(5000) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Single capture functionality
    var captureImage by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var capturedImageOCRResult by remember { mutableStateOf<OCRResult?>(null) }
    var capturedImageProcessing by remember { mutableStateOf(false) }
    var capturedImageError by remember { mutableStateOf<String?>(null) }

    // OCR result processing variables
    var rawOcrText by remember { mutableStateOf("") }
    var detectedLanguage by remember { mutableStateOf("unknown") }
    var languageConfidence by remember { mutableStateOf(0f) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDisplayOptionsDialog by remember { mutableStateOf(false) }
    var showDetectionOptionsDialog by remember { mutableStateOf(false) }

    // Settings from repository
    // Global settings from repository
    val repoShowBoundingBoxes by settingsRepository.showBoundingBoxes.collectAsState(initial = true)
    val repoShowTextOverlay by settingsRepository.showTextOverlay.collectAsState(initial = true)
    val repoShowWordBoundingBoxes by settingsRepository.showWordBoundingBoxes.collectAsState(initial = true)
    val repoShowLineBoundingBoxes by settingsRepository.showLineBoundingBoxes.collectAsState(initial = true)
    val repoShowBlockBoundingBoxes by settingsRepository.showBlockBoundingBoxes.collectAsState(initial = true)
    val textBackgroundOpacity by settingsRepository.textBackgroundOpacity.collectAsState(initial = 0.8f)
    val boundingBoxStrokeWidth by settingsRepository.boundingBoxStrokeWidth.collectAsState(initial = 2f)
    val savedMlKitLanguage by settingsRepository.mlKitLanguage.collectAsState(initial = "auto")

    var mlKitLanguage by remember { mutableStateOf("auto") }

    // Camera-screen specific display settings (start disabled for cleaner view)
    var camShowBoundingBoxes by remember { mutableStateOf(false) }
    var camShowTextOverlay by remember { mutableStateOf(false) }
    var camShowWordBoundingBoxes by remember { mutableStateOf(false) }
    var camShowLineBoundingBoxes by remember { mutableStateOf(false) }
    var camShowBlockBoundingBoxes by remember { mutableStateOf(false) }

    // When user opens display options first time, initialize with repo values to allow enabling
    LaunchedEffect(showDisplayOptionsDialog) {
        if (showDisplayOptionsDialog && !camShowTextOverlay && !camShowBoundingBoxes) {
            camShowBoundingBoxes = repoShowBoundingBoxes
            camShowTextOverlay = repoShowTextOverlay
            camShowWordBoundingBoxes = repoShowWordBoundingBoxes
            camShowLineBoundingBoxes = repoShowLineBoundingBoxes
            camShowBlockBoundingBoxes = repoShowBlockBoundingBoxes
        }
    }

    // Update mlKitLanguage when settings change
    LaunchedEffect(savedMlKitLanguage) {
        mlKitLanguage = savedMlKitLanguage
        preferredLanguage = if (savedMlKitLanguage == "auto") null else savedMlKitLanguage
    }

    // Initialize ViewModel
    LaunchedEffect(Unit) {
        try {
            viewModel.initialize()
        } catch (e: Exception) {
            // Failure will be handled during OCR if needed
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(stringResource(R.string.scanner_settings)) },
            text = {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Detection interval controls
                    Text(
                        stringResource(R.string.detection_interval),
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${detectionIntervalMs}ms", fontSize = 14.sp)
                        Row {
                            OutlinedIconButton(
                                onClick = {
                                    detectionIntervalMs = max(250L, detectionIntervalMs - 250L)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-")
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedIconButton(
                                onClick = {
                                    detectionIntervalMs = min(5000L, detectionIntervalMs + 250L)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+")
                            }
                        }
                    }
                    Slider(
                        value = detectionIntervalMs.toFloat(),
                        onValueChange = { detectionIntervalMs = it.toLong() },
                        valueRange = 250f..5000f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // Reading speed controls
                    Text(
                        stringResource(R.string.reading_time_per_word),
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${readingMsPerWord}ms", fontSize = 14.sp)
                        Row {
                            OutlinedIconButton(
                                onClick = { readingMsPerWord = max(100, readingMsPerWord - 50) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-")
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedIconButton(
                                onClick = { readingMsPerWord = min(1000, readingMsPerWord + 50) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+")
                            }
                        }
                    }
                    Slider(
                        value = readingMsPerWord.toFloat(),
                        onValueChange = { readingMsPerWord = it.toInt() },
                        valueRange = 100f..1000f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, topInset + 10.dp, 16.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display Options button
            GlassButton(
                icon = painterResource(R.drawable.ic_image_outline),
                onClick = { showDisplayOptionsDialog = true }
            )

            // Detection Options button
            GlassButton(
                icon = painterResource(R.drawable.ic_language),
                onClick = { showDetectionOptionsDialog = true }
            )

            // Camera Settings button
            GlassButton(
                icon = painterResource(R.drawable.ic_time),
                onClick = { showSettingsDialog = true }
            )
        }

        CameraPermissionHandler(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface),
            onGranted = {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    OCRCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        isDetectionRunning = detectionRunning,
                        flashEnabled = flashEnabled,
                        preferredLanguage = preferredLanguage,
                        detectionIntervalMs = detectionIntervalMs,
                        readingMsPerWord = readingMsPerWord,
                        maxExtraDelayMs = maxExtraDelayMs,
                        skipFrames = 5,
                        // Display settings
                        showBoundingBoxes = camShowBoundingBoxes,
                        showTextOverlay = camShowTextOverlay,
                        showWordBoundingBoxes = camShowWordBoundingBoxes,
                        showLineBoundingBoxes = camShowLineBoundingBoxes,
                        showBlockBoundingBoxes = camShowBlockBoundingBoxes,
                        textBackgroundOpacity = textBackgroundOpacity,
                        boundingBoxStrokeWidth = boundingBoxStrokeWidth,
                        // Single capture functionality
                        captureImage = captureImage,
                        onCaptureComplete = {
                            captureImage = false
                            capturedImageProcessing = false
                        },
                        onImageCaptured = { bitmap ->
                            capturedBitmap = bitmap
                            capturedImageProcessing = true
                            capturedImageError = null
                            capturedImageOCRResult = null
                            isCapturing = false
                        },
                        onOCRResult = { result ->
                            if (capturedBitmap != null) {
                                // This is a result from captured image
                                capturedImageOCRResult = result
                                capturedImageProcessing = false
                                capturedImageError = null
                            } else {
                                // This is a real-time OCR result
                                lastOCRResult = result
                                rawOcrText = result.text
                                errorMessage = null
                                detectionCount += 1
                                lastUpdateTime = System.currentTimeMillis()

                                // Detect language for OCR results
                                if (result.text.isNotBlank()) {
                                    val lang = LanguageDetector.detectLanguage(result.text)
                                    detectedLanguage = lang
                                    languageConfidence =
                                        LanguageDetector.getDetectionConfidenceMap(result.text)[lang]
                                            ?: 0f
                                }
                            }
                        },
                        onManualLanguageRequired = { manualLanguageRequired = true },
                        onError = { error ->
                            if (capturedBitmap != null) {
                                // This is an error from captured image processing
                                capturedImageError = error
                                capturedImageProcessing = false
                            } else {
                                // This is a real-time OCR error
                                errorMessage = error
                            }

                            if (isCapturing) {
                                isCapturing = false
                            }
                        }
                    )

                    // Status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (detectionRunning) Color.Green.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(
                                    alpha = 0.2f
                                )
                            )
                            .padding(14.dp, 10.dp)
                    ) {
                        if (detectionRunning || isCapturing) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    isCapturing -> Color.Blue
                                    detectionRunning -> Color.Green
                                    else -> Color.Gray
                                }
                            ) {}
                        }
                        Text(
                            text = when {
                                isCapturing -> "Capturing"
                                detectionRunning -> "Scanning"
                                else -> "Ready"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                isCapturing -> Color.Blue
                                detectionRunning -> Color.Green
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                        if (detectionRunning && detectionCount > 0) {
                            Text(
                                text = "($detectionCount)",
                                fontSize = 10.sp,
                                color = Color.Green.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // OCR Info Card at bottom - show if there's content or error
                    if (rawOcrText.isNotBlank() || errorMessage != null || lastOCRResult != null) {
                        OCRInfoCard(
                            ocrLoading = false,
                            ocrError = errorMessage,
                            rawOcrText = rawOcrText,
                            detectedLanguage = detectedLanguage,
                            languageConfidence = languageConfidence,
                            lastResult = lastOCRResult,
                            onCopy = {
                                val clipboard =
                                    context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText(
                                    context.getString(R.string.ocr_text),
                                    rawOcrText
                                )
                                clipboard.setPrimaryClip(clip)
                            },
                            onShare = {
                                if (rawOcrText.isNotBlank()) {
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, rawOcrText)
                                        type = "text/plain"
                                    }
                                    val chooser = android.content.Intent.createChooser(
                                        sendIntent,
                                        "Share text"
                                    )
                                    context.startActivity(chooser)
                                }
                            },
                            onRefresh = {
                                // For camera, restart detection
                                if (!detectionRunning) {
                                    detectionRunning = true
                                    detectionCount = 0
                                    lastUpdateTime = System.currentTimeMillis()
                                }
                            },
                            onClear = {
                                lastOCRResult = null
                                rawOcrText = ""
                                detectedLanguage = "unknown"
                                languageConfidence = 0f
                                errorMessage = null
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        )
                    }
                }
            })

        // Bottom bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Error message display
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 8.dp),
                    color = Color(0xFFf44336).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFf44336),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 16.dp, 20.dp, topInset),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash toggle
                GlassButton(
                    icon = if (flashEnabled) painterResource(R.drawable.ic_flash_on_outline) else painterResource(
                        R.drawable.ic_flash_off_outline
                    ), onClick = { flashEnabled = !flashEnabled }
                )

                // Enhanced Capture button - Snapchat-like behavior
                CameraCaptureButton(
                    text = when {
                        isCapturing -> "Capturing..."
                        detectionRunning -> "Scanning..."
                        capturedBitmap != null -> "Captured"
                        else -> "Hold/Tap"
                    },
                    size = 80.dp,
                    textColor = Color.White,
                    backgroundColor = if (capturedBitmap != null) Color(0xFF4CAF50) else Color(
                        0xFF2196F3
                    ),
                    longPressBackgroundColor = Color(0xFFf44336),
                    progressColor = Color.White,
                    isRealTimeMode = detectionRunning,
                    onSingleCapture = {
                        // Tap = capture single image (only if not in real-time mode and no captured image)
                        if (!isCapturing && !detectionRunning && capturedBitmap == null) {
                            isCapturing = true
                            captureImage = true
                            errorMessage = null
                            // Clear any previous results
                            capturedImageOCRResult = null
                            capturedImageError = null
                        }
                    },
                    onLongPressStart = {
                        // Hold = start real-time scanning (only if no captured image)
                        if (!isCapturing && capturedBitmap == null) {
                            detectionRunning = true
                            detectionCount = 0
                            lastUpdateTime = System.currentTimeMillis()
                            errorMessage = null
                            // Clear any captured image data when starting real-time
                            capturedBitmap = null
                            capturedImageOCRResult = null
                            capturedImageError = null
                        }
                    },
                    onLongPressEnd = {
                        // Release = stop real-time scanning
                        detectionRunning = false
                        detectionCount = 0
                    }
                )

                // Gallery button
                GlassButton(
                    icon = painterResource(R.drawable.ic_image_outline),
                    onClick = { navController.navigate(Screen.Gallery.route) }
                )
            }
        }
    }

    // Display Options Dialog
    if (showDisplayOptionsDialog) {
        DisplayOptionsDialog(
            onDismiss = { showDisplayOptionsDialog = false },
            showBoundingBoxes = camShowBoundingBoxes,
            onShowBoundingBoxesChange = {
                camShowBoundingBoxes = it; coroutineScope.launch { settingsRepository.setShowBoundingBoxes(it) }
            },
            showTextOverlay = camShowTextOverlay,
            onShowTextOverlayChange = {
                camShowTextOverlay = it; coroutineScope.launch { settingsRepository.setShowTextOverlay(it) }
            },
            showWordBoundingBoxes = camShowWordBoundingBoxes,
            onShowWordBoundingBoxesChange = {
                camShowWordBoundingBoxes = it; coroutineScope.launch { settingsRepository.setShowWordBoundingBoxes(it) }
            },
            showLineBoundingBoxes = camShowLineBoundingBoxes,
            onShowLineBoundingBoxesChange = {
                camShowLineBoundingBoxes = it; coroutineScope.launch { settingsRepository.setShowLineBoundingBoxes(it) }
            },
            showBlockBoundingBoxes = camShowBlockBoundingBoxes,
            onShowBlockBoundingBoxesChange = {
                camShowBlockBoundingBoxes = it; coroutineScope.launch { settingsRepository.setShowBlockBoundingBoxes(it) }
            },
            textBackgroundOpacity = textBackgroundOpacity,
            onTextBackgroundOpacityChange = { coroutineScope.launch { settingsRepository.setTextBackgroundOpacity(it) } },
            boundingBoxStrokeWidth = boundingBoxStrokeWidth,
            onBoundingBoxStrokeWidthChange = { coroutineScope.launch { settingsRepository.setBoundingBoxStrokeWidth(it) } }
        )
    }

    // Detection Options Dialog
    if (showDetectionOptionsDialog) {
        DetectionOptionsDialog(
            onDismiss = { showDetectionOptionsDialog = false },
            onLanguageSelected = { code ->
                mlKitLanguage = code
                coroutineScope.launch { settingsRepository.setMlKitLanguage(code) }
                preferredLanguage = if (code == "auto") null else code
            },
            currentLanguage = mlKitLanguage,
            onDetectAgain = {
                // For camera, we don't need to detect again - it's real-time
                // But we can restart detection if it's stopped
                if (!detectionRunning) {
                    detectionRunning = true
                    detectionCount = 0
                    lastUpdateTime = System.currentTimeMillis()
                }
            },
            onClearCache = {
                // For camera, we can clear the current result
                lastOCRResult = null
                rawOcrText = ""
                detectedLanguage = "unknown"
                languageConfidence = 0f
            },
            onClearAll = {
                // For camera, same as clear cache
                lastOCRResult = null
                rawOcrText = ""
                detectedLanguage = "unknown"
                languageConfidence = 0f
            },
            ocrLoading = false // Camera OCR doesn't have a loading state like static images
        )
    }

    // Captured Image Overlay (Snapchat-like functionality)
    if (capturedBitmap != null) {
        CapturedImageOverlay(
            capturedBitmap = capturedBitmap,
            ocrResult = capturedImageOCRResult,
            isProcessing = capturedImageProcessing,
            errorMessage = capturedImageError,
            onClose = {
                // Clear captured image and return to camera
                capturedBitmap = null
                capturedImageOCRResult = null
                capturedImageProcessing = false
                capturedImageError = null
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
