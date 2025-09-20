package com.sumit.varnamala.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
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
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextElement
import com.sumit.varnamala.data.ocr.TextLine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun OCRCameraPreview(
    modifier: Modifier = Modifier,
    isDetectionRunning: Boolean = false,
    flashEnabled: Boolean = false,
    preferredLanguage: String? = null,
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

    val latinRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    val devanagariRecognizer = remember {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    var frameSkipCounter by remember { mutableIntStateOf(0) }
    val skipFrames = 5

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }.also {
                    previewView = it
                }
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
                updateAnalysisUseCase(
                    cameraProvider = provider,
                    lifecycleOwner = lifecycleOwner,
                    preview = preview,
                    isDetectionRunning = isDetectionRunning,
                    cameraExecutor = cameraExecutor,
                    skipFrames = skipFrames,
                    latinRecognizer = latinRecognizer,
                    devanagariRecognizer = devanagariRecognizer,
                    onOCRResult = onOCRResult,
                    onError = onError,
                    preferredLanguage = preferredLanguage,
                    onManualLanguageRequired = onManualLanguageRequired,
                    onFrameSkip = { frameSkipCounter = it },
                    onAnalyzerReady = { analyzer -> imageAnalyzer = analyzer }
                )
            }
        }

        LaunchedEffect(flashEnabled, camera) {
            camera?.let { cam ->
                if (cam.cameraInfo.hasFlashUnit()) {
                    cam.cameraControl.enableTorch(flashEnabled)
                }
            }
        }

        LaunchedEffect(isDetectionRunning) {
            if (!isDetectionRunning) {
                camera?.cameraControl?.enableTorch(false)
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

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()

            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            )

            onCameraProviderReady(cameraProvider, camera, preview)

        } catch (exc: Exception) {
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
    onOCRResult: (OCRResult) -> Unit,
    onError: (String) -> Unit,
    preferredLanguage: String?,
    onManualLanguageRequired: () -> Unit,
    onFrameSkip: (Int) -> Unit,
    onAnalyzerReady: (ImageAnalysis?) -> Unit
) {
    try {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        if (isDetectionRunning) {
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    val analyzerCounter = java.util.concurrent.atomic.AtomicInteger(0)
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            val current = analyzerCounter.incrementAndGet() % (skipFrames + 1)
                            onFrameSkip(current)
                            if (current != 0) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            processImageProxy(
                                imageProxy = imageProxy,
                                latinRecognizer = latinRecognizer,
                                devanagariRecognizer = devanagariRecognizer,
                                preferredLanguage = preferredLanguage,
                                onOCRResult = onOCRResult,
                                onManualLanguageRequired = onManualLanguageRequired,
                                onError = onError
                            )
                        } catch (t: Throwable) {
                            try {
                                imageProxy.close()
                            } catch (_: Throwable) {
                            }
                        }
                    }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview!!,
                imageAnalyzer
            )

            onAnalyzerReady(imageAnalyzer)
        } else {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview!!
            )

            onAnalyzerReady(null)
        }
    } catch (exc: Exception) {
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
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // Processing order: preferredLanguage -> devanagari -> latin
        fun handleNoText() {
            onManualLanguageRequired()
            val emptyResult = OCRResult(
                text = "",
                confidence = 0f,
                language = "Unknown",
                boundingBoxes = emptyList(),
                processingTimeMs = System.currentTimeMillis() - startTime
            )
            onOCRResult(emptyResult)
            imageProxy.close()
        }

        val tryLatin: () -> Unit = {
            latinRecognizer.process(image)
                .addOnSuccessListener { latinResult ->
                    val finalProcessingTime = System.currentTimeMillis() - startTime
                    if (latinResult.text.isNotEmpty()) {
                        val ocrResult =
                            convertToOCRResult(latinResult, "English", finalProcessingTime)
                        onOCRResult(ocrResult)
                    } else {
                        handleNoText()
                    }
                }
                .addOnFailureListener { exception ->
                    handleNoText()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

        val tryDevanagari: () -> Unit = {
            devanagariRecognizer.process(image)
                .addOnSuccessListener { devanagariResult ->
                    val processingTime = System.currentTimeMillis() - startTime
                    if (devanagariResult.text.isNotEmpty()) {
                        val ocrResult =
                            convertToOCRResult(devanagariResult, "Devanagari", processingTime)
                        onOCRResult(ocrResult)
                        imageProxy.close()
                    } else {
                        tryLatin()
                    }
                }
                .addOnFailureListener { exception ->
                    tryLatin()
                }
        }

        when (preferredLanguage?.lowercase()) {
            null -> {
                // Default order: devanagari then latin
                tryDevanagari()
            }

            "english", "en", "latin" -> {
                tryLatin()
            }

            "devanagari", "hi", "hindi" -> {
                tryDevanagari()
            }

            else -> {
                tryDevanagari()
            }
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
    processingTime: Long
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
        processingTimeMs = processingTime
    )
}