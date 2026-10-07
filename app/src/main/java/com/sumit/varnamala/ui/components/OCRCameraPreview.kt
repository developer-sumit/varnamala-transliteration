package com.sumit.varnamala.ui.components

import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.OcrLanguageModel
import com.sumit.varnamala.ocr.paddle.PaddleOcrRepository

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.withContext
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
    paddleLanguage: OcrLanguageModel? = null,
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
    onWordTap: ((String) -> Unit)? = null,
    highContrast: Boolean = false,
    onCaptureComplete: () -> Unit = {},
    onImageCaptured: (android.graphics.Bitmap) -> Unit = {},
    onOCRResult: (OCRResult) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val paddleRepository = remember { PaddleOcrRepository(context) }
    // Recognition runs here, not on the camera analyzer thread, so the analyzer only copies a
    // frame and returns. Frames that arrive while a run is active are dropped.
    val ocrScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    var camera: Camera? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var preview: Preview? by remember { mutableStateOf(null) }
    var imageAnalyzer: ImageAnalysis? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    var lastOCRResult by remember { mutableStateOf<OCRResult?>(null) }
    var lastNonEmptyAt by remember { mutableStateOf(0L) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(paddleRepository) {
        withContext(Dispatchers.IO) {
            runCatching { paddleRepository.warmUp() }
                .onFailure { Log.w("OCRCameraPreview", "OCR warm-up failed", it) }
        }
    }

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
            cropToFill = true,
            showBoundingBoxes = showBoundingBoxes,
            showTextOverlay = showTextOverlay,
            showWordBoundingBoxes = showWordBoundingBoxes,
            showLineBoundingBoxes = showLineBoundingBoxes,
            showBlockBoundingBoxes = showBlockBoundingBoxes,
            textBackgroundOpacity = textBackgroundOpacity,
            boundingBoxStrokeWidth = boundingBoxStrokeWidth,
            enableTextCopy = true,
            onWordTap = onWordTap,
            highContrast = highContrast,
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(isDetectionRunning, lensFacing, cameraProvider) {
            cameraProvider?.let { provider ->
                val wrappedOnOCR: (OCRResult) -> Unit = { result ->
                    // Keep showing the previous lines briefly when a frame finds nothing (motion
                    // blur, a blink), so the overlay does not flicker between runs.
                    val now = System.currentTimeMillis()
                    if (result.boundingBoxes.isNotEmpty()) lastNonEmptyAt = now
                    if (result.boundingBoxes.isNotEmpty() || now - lastNonEmptyAt > KEEP_LAST_RESULT_MS) {
                        lastOCRResult = result
                    }
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
                    timeBasedIntervalMs = detectionIntervalMs,
                    readingMsPerWord = readingMsPerWord,
                    maxExtraDelayMs = maxExtraDelayMs,
                    suppressDuplicateDurationMs = 2000L,
                    paddleLanguage = paddleLanguage,
                    paddleRepository = paddleRepository,
                    ocrScope = ocrScope,
                    onOCRResult = wrappedOnOCR,
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
                                        .rotated(imageProxy.imageInfo.rotationDegrees)

                                    // Pass bitmap to parent for overlay display
                                    onImageCaptured(bitmap)

                                    // Process the captured image for OCR off the main thread.
                                    val mainExecutor = ContextCompat.getMainExecutor(context)
                                    cameraExecutor.execute {
                                        processCapturedBitmap(
                                            bitmap = bitmap,
                                            paddleLanguage = paddleLanguage,
                                            paddleRepository = paddleRepository,
                                            onOCRResult = { result ->
                                                mainExecutor.execute {
                                                    lastOCRResult = result
                                                    onOCRResult(result)
                                                    onCaptureComplete()
                                                }
                                            },
                                            onError = { error ->
                                                mainExecutor.execute {
                                                    onError(error)
                                                    onCaptureComplete()
                                                }
                                            }
                                        )
                                    }
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
            ocrScope.cancel()
            // Release off the main thread: it waits for any run still in flight.
            CoroutineScope(Dispatchers.IO).launch { paddleRepository.release() }
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
    timeBasedIntervalMs: Long,
    readingMsPerWord: Int,
    maxExtraDelayMs: Int,
    paddleLanguage: OcrLanguageModel?,
    paddleRepository: PaddleOcrRepository,
    ocrScope: CoroutineScope,
    onOCRResult: (OCRResult) -> Unit,
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
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    val analyzerCounter = AtomicInteger(0)

                    val lastAllowedRun = java.util.concurrent.atomic.AtomicLong(0L)
                    var lastSentText = ""
                    var lastSentTime = 0L

                    val busy = AtomicBoolean(false)
                    val mainHandler = Handler(Looper.getMainLooper())

                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val current = analyzerCounter.incrementAndGet() % (skipFrames + 1)
                        // Drop the frame when a run is active or the pause between runs has not passed.
                        if (current != 0 || busy.get() || System.currentTimeMillis() < lastAllowedRun.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val bitmap = try {
                            imageProxyToBitmap(imageProxy).rotated(imageProxy.imageInfo.rotationDegrees)
                        } catch (t: Throwable) {
                            Log.w("OCRCameraPreview", "Frame conversion failed", t)
                            null
                        } finally {
                            runCatching { imageProxy.close() }
                        }
                        if (bitmap == null || !busy.compareAndSet(false, true)) return@setAnalyzer

                        ocrScope.launch {
                            try {
                                val result = paddleRepository.processBitmap(bitmap, paddleLanguage?.code)
                                val ts = System.currentTimeMillis()
                                val text = result.text
                                val words = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
                                val extraDelay = min(maxExtraDelayMs.toLong(), words.toLong() * readingMsPerWord)
                                // The pause counts from when this run finished, not when it started.
                                lastAllowedRun.set(ts + timeBasedIntervalMs + extraDelay)
                                val duplicate = text.isNotEmpty() && text == lastSentText && ts - lastSentTime < suppressDuplicateDurationMs
                                if (!duplicate) {
                                    lastSentText = text
                                    lastSentTime = ts
                                    mainHandler.post { onOCRResult(result) }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (t: Throwable) {
                                Log.e("OCRCameraPreview", "Live OCR failed", t)
                                mainHandler.post { onError(t.message ?: "OCR failed") }
                            } finally {
                                busy.set(false)
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

/**
 * Decodes any CameraX frame (RGBA analysis frames, YUV frames with row padding, or JPEG
 * captures) into a bitmap; rotation is applied separately. The previous manual NV21 copy ignored row and
 * pixel strides and crashed on single-plane JPEG captures, so OCR never received a valid image.
 */
private fun imageProxyToBitmap(imageProxy: ImageProxy): android.graphics.Bitmap = imageProxy.toBitmap()

private fun android.graphics.Bitmap.rotated(degrees: Int): android.graphics.Bitmap {
    if (degrees == 0) return this
    return android.graphics.Bitmap.createBitmap(
        this,
        0,
        0,
        width,
        height,
        Matrix().apply { postRotate(degrees.toFloat()) },
        true,
    )
}


private fun processCapturedBitmap(
    bitmap: android.graphics.Bitmap,
    paddleLanguage: OcrLanguageModel?,
    paddleRepository: PaddleOcrRepository,
    onOCRResult: (OCRResult) -> Unit,
    onError: (String) -> Unit
) {
    runCatching {
        runBlocking {
            paddleRepository.processBitmap(
                bitmap,
                paddleLanguage?.code,
                com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner.Tier.MEDIUM,
                enhance = true,
            )
        }
    }
        .onSuccess(onOCRResult)
        .onFailure {
            Log.e("OCRCameraPreview", "Captured image OCR failed", it)
            onError(it.message ?: "PaddleOCR failed")
        }
}

private const val KEEP_LAST_RESULT_MS = 1500L
