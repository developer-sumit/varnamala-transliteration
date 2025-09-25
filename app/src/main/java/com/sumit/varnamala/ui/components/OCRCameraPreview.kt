package com.sumit.varnamala.ui.components

import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextElement
import com.sumit.varnamala.data.ocr.TextLine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

@Composable
fun OCRCameraPreview(
    modifier: Modifier = Modifier,
    isDetectionRunning: Boolean = false,
    flashEnabled: Boolean = false,
    preferredLanguage: String? = null,
    // time-based sampling (milliseconds)
    detectionIntervalMs: Long = 1000L,
    // reading time estimate per word (ms)
    readingMsPerWord: Int = 300,
    // maximum extra delay (ms) based on reading estimate
    maxExtraDelayMs: Int = 5000,
    skipFrames: Int = 5,
    onOCRResult: (OCRResult) -> Unit = {},
    onManualLanguageRequired: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var camera: Camera? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var preview: Preview? by remember { mutableStateOf(null) }
    var imageAnalyzer: ImageAnalysis? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    val latinRecognizer =
        remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val devanagariRecognizer =
        remember { TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()) }

    var lastOCRResult by remember { mutableStateOf<OCRResult?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }.also { previewView = it }
            },
            update = { currentPreviewView ->
                if (cameraProvider == null) {
                    setupCameraOnce(
                        context = context,
                        lifecycleOwner = lifecycleOwner,
                        previewView = currentPreviewView,
                        onCameraProviderReady = { provider, cam, prev ->
                            cameraProvider = provider
                            camera = cam
                            preview = prev
                        },
                        onError = onError
                    )
                }
            }
        )

        LaunchedEffect(isDetectionRunning) {
            cameraProvider?.let { provider ->
                val wrappedOnOCR: (OCRResult) -> Unit = { result ->
                    lastOCRResult = result
                    onOCRResult(result)
                }

                updateAnalysisUseCase(
                    cameraProvider = provider,
                    lifecycleOwner = lifecycleOwner,
                    preview = preview,
                    isDetectionRunning = isDetectionRunning,
                    cameraExecutor = cameraExecutor,
                    skipFrames = skipFrames,
                    latinRecognizer = latinRecognizer,
                    devanagariRecognizer = devanagariRecognizer,
                    timeBasedIntervalMs = detectionIntervalMs,
                    readingMsPerWord = readingMsPerWord,
                    maxExtraDelayMs = maxExtraDelayMs,
                    suppressDuplicateDurationMs = 2000L,
                    preferredLanguage = preferredLanguage,
                    onOCRResult = wrappedOnOCR,
                    onManualLanguageRequired = onManualLanguageRequired,
                    onError = onError,
                    onAnalyzerReady = { analyzer -> imageAnalyzer = analyzer },
                    onCameraReady = { cam ->
                        camera = cam
                        try {
                            if (flashEnabled) cam.cameraControl.enableTorch(true)
                        } catch (_: Throwable) {
                        }
                    }
                )
            }
        }

        LaunchedEffect(flashEnabled, camera) {
            camera?.let { cam ->
                if (cam.cameraInfo.hasFlashUnit()) cam.cameraControl.enableTorch(
                    flashEnabled
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            latinRecognizer.close()
            devanagariRecognizer.close()
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun setupCameraOnce(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onCameraProviderReady: (ProcessCameraProvider, Camera, Preview) -> Unit,
    onError: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            onCameraProviderReady(cameraProvider, camera, preview)
        } catch (exc: Exception) {
            Log.e("OCRCameraPreview", "Camera setup failed", exc)
            onError("Camera initialization failed: ${exc.message}")
        }
    }, ContextCompat.getMainExecutor(context))
}

@SuppressLint("UnsafeOptInUsageError")
private fun updateAnalysisUseCase(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    preview: Preview?,
    isDetectionRunning: Boolean,
    cameraExecutor: ExecutorService,
    skipFrames: Int,
    latinRecognizer: TextRecognizer,
    devanagariRecognizer: TextRecognizer,
    timeBasedIntervalMs: Long,
    readingMsPerWord: Int,
    maxExtraDelayMs: Int,
    preferredLanguage: String?,
    onOCRResult: (OCRResult) -> Unit,
    onManualLanguageRequired: () -> Unit,
    onError: (String) -> Unit,
    onAnalyzerReady: (ImageAnalysis?) -> Unit,
    onCameraReady: (Camera) -> Unit,
    suppressDuplicateDurationMs: Long = 2000L
) {
    try {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        if (isDetectionRunning) {
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    val analyzerCounter = AtomicInteger(0)

                    var lastAllowedRun = 0L
                    var lastSentText = ""
                    var lastSentTime = 0L

                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            val current = analyzerCounter.incrementAndGet() % (skipFrames + 1)
                            if (current != 0) {
                                imageProxy.close(); return@setAnalyzer
                            }

                            val now = System.currentTimeMillis()
                            if (now < lastAllowedRun) {
                                imageProxy.close(); return@setAnalyzer
                            }

                            val sendWrapper: (OCRResult) -> Unit = { result ->
                                val ts = System.currentTimeMillis()
                                val text = result.text
                                val words =
                                    if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
                                val extraDelay =
                                    min(maxExtraDelayMs.toLong(), words.toLong() * readingMsPerWord)
                                // schedule next allowed run after base interval + reading extra
                                lastAllowedRun = ts + timeBasedIntervalMs + extraDelay

                                if (text.isNotEmpty() && text == lastSentText && ts - lastSentTime < suppressDuplicateDurationMs) {
                                    // suppressed duplicate
                                } else {
                                    lastSentText = text
                                    lastSentTime = ts
                                    onOCRResult(result)
                                }
                            }

                            processImageProxy(
                                imageProxy = imageProxy,
                                latinRecognizer = latinRecognizer,
                                devanagariRecognizer = devanagariRecognizer,
                                preferredLanguage = preferredLanguage,
                                onOCRResult = sendWrapper,
                                onManualLanguageRequired = onManualLanguageRequired,
                                onError = onError
                            )
                            return@setAnalyzer
                        } catch (t: Throwable) {
                            try {
                                imageProxy.close()
                            } catch (_: Throwable) {
                            }
                        }
                    }
                }

            cameraProvider.unbindAll()
            val cam = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview!!,
                imageAnalyzer
            )

            onAnalyzerReady(imageAnalyzer)
            onCameraReady(cam)
        } else {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview!!)
            onAnalyzerReady(null)
        }
    } catch (exc: Exception) {
        Log.e("OCRCameraPreview", "Analysis update failed", exc)
        onError("Camera analysis update failed: ${exc.message}")
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    imageProxy: ImageProxy,
    latinRecognizer: TextRecognizer,
    devanagariRecognizer: TextRecognizer,
    preferredLanguage: String?,
    onOCRResult: (OCRResult) -> Unit,
    onManualLanguageRequired: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val startTime = System.currentTimeMillis()
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close(); return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        fun handleNoText() {
            onManualLanguageRequired()
            val emptyResult = OCRResult(
                text = "",
                confidence = 0f,
                language = "Unknown",
                boundingBoxes = emptyList(),
                processingTimeMs = System.currentTimeMillis() - startTime,
                imageWidth = image.width,
                imageHeight = image.height,
                imageRotation = imageProxy.imageInfo.rotationDegrees
            )
            onOCRResult(emptyResult)
            imageProxy.close()
        }

        val tryLatin: () -> Unit = {
            latinRecognizer.process(image)
                .addOnSuccessListener { latinResult ->
                    val finalProcessingTime = System.currentTimeMillis() - startTime
                    if (latinResult.text.isNotEmpty()) onOCRResult(
                        convertToOCRResult(
                            latinResult,
                            "English",
                            finalProcessingTime,
                            image.width,
                            image.height,
                            imageProxy.imageInfo.rotationDegrees
                        )
                    ) else handleNoText()
                }
                .addOnFailureListener { _ -> handleNoText() }
                .addOnCompleteListener { imageProxy.close() }
        }

        val tryDevanagari: () -> Unit = {
            devanagariRecognizer.process(image)
                .addOnSuccessListener { devanagariResult ->
                    val processingTime = System.currentTimeMillis() - startTime
                    if (devanagariResult.text.isNotEmpty()) {
                        onOCRResult(
                            convertToOCRResult(
                                devanagariResult,
                                "Devanagari",
                                processingTime,
                                image.width,
                                image.height,
                                imageProxy.imageInfo.rotationDegrees
                            )
                        ); imageProxy.close()
                    } else tryLatin()
                }
                .addOnFailureListener { _ -> tryLatin() }
        }

        when (preferredLanguage?.lowercase()) {
            null -> tryDevanagari()
            "english", "en", "latin" -> tryLatin()
            "devanagari", "hi", "hindi" -> tryDevanagari()
            else -> tryDevanagari()
        }

    } catch (exc: Exception) {
        onError("Image processing failed: ${exc.message}")
        try {
            imageProxy.close()
        } catch (_: Throwable) {
        }
    }
}

private fun convertToOCRResult(
    mlkitResult: Text,
    language: String,
    processingTime: Long,
    imageWidth: Int = 0,
    imageHeight: Int = 0,
    imageRotation: Int = 0
): OCRResult {
    val textBlocks = mlkitResult.textBlocks.map { block ->
        TextBlock(
            text = block.text,
            confidence = 0f,
            boundingBox = block.boundingBox ?: Rect(),
            lines = block.lines.map { line ->
                TextLine(
                    text = line.text,
                    confidence = line.confidence,
                    boundingBox = line.boundingBox ?: Rect(),
                    elements = line.elements.map { element ->
                        TextElement(
                            text = element.text,
                            confidence = element.confidence,
                            boundingBox = element.boundingBox ?: Rect()
                        )
                    }
                )
            }
        )
    }

    return OCRResult(
        text = mlkitResult.text,
        confidence = textBlocks.flatMap { it.lines }.flatMap { it.elements }
            .maxOfOrNull { it.confidence } ?: 0f,
        language = language,
        boundingBoxes = textBlocks,
        processingTimeMs = processingTime,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        imageRotation = imageRotation
    )
}