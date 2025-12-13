package com.sumit.varnamala.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
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
import com.sumit.varnamala.ui.viewmodels.OCRViewModel
import com.sumit.varnamala.ui.viewmodels.OCRViewModelFactory
import com.sumit.varnamala.utils.LanguageDetector
import com.sumit.varnamala.utils.Transliterator
import com.sumit.varnamala.data.Language
import com.sumit.varnamala.data.AppLanguageRepository

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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    val ocrViewModel: OCRViewModel = viewModel(factory = OCRViewModelFactory(context))

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

    // Initialize ViewModel
    LaunchedEffect(Unit) {
        try {
            ocrViewModel.initialize()
        } catch (e: Exception) {
            // Failure will be handled during OCR if needed
        }
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    OCRCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        isDetectionRunning = detectionRunning,
                        flashEnabled = flashEnabled,
                        preferredLanguage = null,
                        detectionIntervalMs = 1000L,
                        readingMsPerWord = 300,
                        maxExtraDelayMs = 5000,
                        skipFrames = 5,
                        showBoundingBoxes = false,
                        showTextOverlay = false,
                        showWordBoundingBoxes = false,
                        showLineBoundingBoxes = false,
                        showBlockBoundingBoxes = false,
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

        // Bottom section - always visible
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // OCR Result Card - only show when there's content
            if (rawOcrText.isNotBlank() || errorMessage != null) {
                val targetLang = selectedTargetLanguage
                val transliteratedText = if (targetLang != null) {
                    Transliterator.transliterate(
                        text = rawOcrText,
                        sourceLanguage = detectedLanguage,
                        targetLanguage = targetLang.code
                    )
                } else rawOcrText

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
                            rawOcrText
                        )
                        clipboard.setPrimaryClip(clip)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }

            // Language Selection Row
            LanguageSelectionRow(
                sourceLanguage = selectedSourceLanguage,
                targetLanguage = selectedTargetLanguage,
                onSourceLanguageClick = { showSourceLanguageDialog = true },
                onTargetLanguageClick = { showTargetLanguageDialog = true },
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
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
                    icon = if (flashEnabled) painterResource(R.drawable.ic_flash_on_outline) else painterResource(
                        R.drawable.ic_flash_off_outline
                    ), onClick = { flashEnabled = !flashEnabled }
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

                // Gallery button
                GlassButton(
                    icon = painterResource(R.drawable.ic_image_outline),
                    onClick = { navController.navigate(Screen.Gallery.route) }
                )
            }
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
                                    targetLanguage = targetLang.code
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
                                    rawOcrText
                                )
                                clipboard.setPrimaryClip(clip)
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

                        // Gallery button
                        GlassButton(
                            icon = painterResource(R.drawable.ic_image_outline),
                            onClick = { navController.navigate(Screen.Gallery.route) }
                        )
                    }
                }
            }
        }

        // Language Selection Dialogs
        if (showSourceLanguageDialog) {
            LanguageSelectionDialog(
                title = "Select OCR Language",
                currentLanguage = selectedSourceLanguage,
                onLanguageSelected = { language ->
                    selectedSourceLanguage = language
                    showSourceLanguageDialog = false
                    // Trigger OCR re-processing if needed
                },
                onDismiss = { showSourceLanguageDialog = false }
            )
        }

        if (showTargetLanguageDialog) {
            LanguageSelectionDialog(
                title = "Select Translation Language",
                currentLanguage = selectedTargetLanguage,
                onLanguageSelected = { language ->
                    selectedTargetLanguage = language
                    showTargetLanguageDialog = false
                    // Trigger transliteration update if needed
                },
                onDismiss = { showTargetLanguageDialog = false }
            )
        }
    }
}

