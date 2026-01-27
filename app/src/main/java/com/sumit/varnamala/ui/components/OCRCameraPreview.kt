package com.sumit.varnamala.ui.components

import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextElement
import com.sumit.varnamala.data.ocr.TextLine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
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
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    preferredLanguage: String? = null,
    // time-based sampling (milliseconds)
    detectionIntervalMs: Long = 1000L,
    // reading time estimate per word (ms)
    readingMsPerWord: Int = 300,
    // maximum extra delay (ms) based on reading estimate
    maxExtraDelayMs: Int = 5000,
    skipFrames: Int = 5,
    // Display settings
    showBoundingBoxes: Boolean = true,
    showTextOverlay: Boolean = true,
    showWordBoundingBoxes: Boolean = false,
    showLineBoundingBoxes: Boolean = true,
    showBlockBoundingBoxes: Boolean = true,
    textBackgroundOpacity: Float = 0.8f,
    boundingBoxStrokeWidth: Float = 2f,
    // Single capture functionality
    textOverride: Map<Rect, String>? = null,
    captureImage: Boolean = false,
    onCaptureComplete: () -> Unit = {},
    onImageCaptured: (android.graphics.Bitmap) -> Unit = {},
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
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    val latinRecognizer =
        remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val devanagariRecognizer =
        remember { TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()) }

    var lastOCRResult by remember { mutableStateOf<OCRResult?>(null) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    previewSize = coordinates.size
                },
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
                        lensFacing = lensFacing,
                        onCameraProviderReady = { provider, cam, prev, imgCapture ->
                            cameraProvider = provider
                            camera = cam
                            preview = prev
                            imageCapture = imgCapture
                        },
                        onError = onError
                    )
                }
            }
        )

        OCROverlayCanvas(
            result = lastOCRResult,
            textOverride = textOverride,
            containerSize = previewSize,
            showBoundingBoxes = showBoundingBoxes,
            showTextOverlay = showTextOverlay,
            showWordBoundingBoxes = showWordBoundingBoxes,
            showLineBoundingBoxes = showLineBoundingBoxes,
            showBlockBoundingBoxes = showBlockBoundingBoxes,
            textBackgroundOpacity = textBackgroundOpacity,
            boundingBoxStrokeWidth = boundingBoxStrokeWidth,
            enableTextCopy = true,
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(isDetectionRunning, lensFacing) {
            cameraProvider?.let { provider ->
                val wrappedOnOCR: (OCRResult) -> Unit = { result ->
                    lastOCRResult = result
                    onOCRResult(result)
                }

                updateAnalysisUseCase(
                    cameraProvider = provider,
                    lifecycleOwner = lifecycleOwner,
                    preview = preview,
                    imageCapture = imageCapture,
                    lensFacing = lensFacing,
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

        // Handle single image capture
        LaunchedEffect(captureImage) {
            if (captureImage && imageCapture != null) {
                try {
                    imageCapture!!.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                try {
                                    // Convert ImageProxy to Bitmap
                                    val bitmap = imageProxyToBitmap(imageProxy)

                                    // Pass bitmap to parent for overlay display
                                    onImageCaptured(bitmap)

                                    // Process the captured image for OCR
                                    processCapturedImageProxy(
                                        imageProxy = imageProxy,
                                        preferredLanguage = preferredLanguage,
                                        latinRecognizer = latinRecognizer,
                                        devanagariRecognizer = devanagariRecognizer,
                                        onOCRResult = { result ->
                                            lastOCRResult = result
                                            onOCRResult(result)
                                            onCaptureComplete()
                                        },
                                        onError = { error ->
                                            onError(error)
                                            onCaptureComplete()
                                        }
                                    )
                                } catch (e: Exception) {
                                    onError("Failed to process captured image: ${e.message}")
                                    onCaptureComplete()
                                } finally {
                                    imageProxy.close()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                onError("Image capture failed: ${exception.message}")
                                onCaptureComplete()
                            }
                        }
                    )
                } catch (e: Exception) {
                    onError("Failed to capture image: ${e.message}")
                    onCaptureComplete()
                }
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
    lensFacing: Int,
    onCameraProviderReady: (ProcessCameraProvider, Camera, Preview, ImageCapture) -> Unit,
    onError: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = selectCameraSelector(cameraProvider, lensFacing)
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            onCameraProviderReady(cameraProvider, camera, preview, imageCapture)
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
    imageCapture: ImageCapture?,
    lensFacing: Int,
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
        val cameraSelector = selectCameraSelector(cameraProvider, lensFacing)

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
            val useCases = mutableListOf<UseCase>()
            preview?.let { useCases.add(it) }
            imageAnalyzer.let { useCases.add(it) }
            imageCapture?.let { useCases.add(it) }
            val cam = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )

            onAnalyzerReady(imageAnalyzer)
            onCameraReady(cam)
        } else {
            cameraProvider.unbindAll()
            val useCases = mutableListOf<UseCase>()
            preview?.let { useCases.add(it) }
            imageCapture?.let { useCases.add(it) }
            if (useCases.isNotEmpty()) {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )
            }
            onAnalyzerReady(null)
        }
    } catch (exc: Exception) {
        Log.e("OCRCameraPreview", "Analysis update failed", exc)
        onError("Camera analysis update failed: ${exc.message}")
    }
}

private fun selectCameraSelector(
    cameraProvider: ProcessCameraProvider,
    lensFacing: Int
): CameraSelector {
    val desired = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()
    return when {
        cameraProvider.hasCamera(desired) -> desired
        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
        else -> desired
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
                language = "Unknown",
                boundingBoxes = emptyList(),
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
            boundingBox = block.boundingBox ?: Rect(),
            lines = block.lines.map { line ->
                TextLine(
                    text = line.text,
                    boundingBox = line.boundingBox ?: Rect(),
                    elements = line.elements.map { element ->
                        TextElement(
                            text = element.text,
                            boundingBox = element.boundingBox ?: Rect()
                        )
                    }
                )
            }
        )
    }

    return OCRResult(
        text = mlkitResult.text,
        language = language,
        boundingBoxes = textBlocks,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        imageRotation = imageRotation
    )
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): android.graphics.Bitmap {
    val buffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}


private fun processCapturedImageProxy(
    imageProxy: ImageProxy,
    preferredLanguage: String?,
    latinRecognizer: TextRecognizer,
    devanagariRecognizer: TextRecognizer,
    onOCRResult: (OCRResult) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        val startTime = System.currentTimeMillis()

        fun tryLatin() {
            latinRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val processingTime = System.currentTimeMillis() - startTime
                    val result = convertToOCRResult(visionText, "latin", processingTime)
                    onOCRResult(result)
                }
                .addOnFailureListener { exc ->
                    onError("Latin OCR failed: ${exc.message}")
                }
        }

        fun tryDevanagari() {
            devanagariRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val processingTime = System.currentTimeMillis() - startTime
                    val result = convertToOCRResult(visionText, "devanagari", processingTime)

                    if (result.text.isNotBlank()) {
                        onOCRResult(result)
                    } else {
                        tryLatin()
                    }
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
    }
}

