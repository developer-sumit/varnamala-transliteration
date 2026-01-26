package com.sumit.varnamala.service

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Language
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextElement
import com.sumit.varnamala.data.ocr.TextLine
import com.sumit.varnamala.ui.theme.VarnamalaTheme
import com.sumit.varnamala.utils.ScreenCaptureHelper
import com.sumit.varnamala.utils.LanguageDetector
import com.sumit.varnamala.utils.Transliterator
import com.sumit.varnamala.ui.components.OCRInfoCard
import com.sumit.varnamala.data.AppLanguageRepository
import com.sumit.varnamala.ui.components.CameraCaptureButton
import com.sumit.varnamala.ui.components.LanguageSelectionRow

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var floatingButtonView: ComposeView? = null
    private var ocrOverlayView: ComposeView? = null
    private var closeButtonView: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var screenCaptureHelper: ScreenCaptureHelper? = null
    private var capturedBitmap: Bitmap? = null
    private var ocrResult: OCRResult? = null
    private var isProcessing = mutableStateOf(false)
    private var rawOcrText = mutableStateOf("")
    private var detectedLanguage = mutableStateOf("unknown")
    private var transliteratedText = mutableStateOf("")

    private var overlayX = mutableIntStateOf(0)
    private var overlayY = mutableIntStateOf(0)
    private var selectedLanguage = mutableStateOf<Language?>(null)
    private var targetLanguage = mutableStateOf<Language?>(null)
    private var overlayParams: WindowManager.LayoutParams? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "overlay_service_channel"
        const val ACTION_INIT_SCREEN_CAPTURE = "com.sumit.varnamala.INIT_SCREEN_CAPTURE"
        const val ACTION_STOP_OVERLAY = "com.sumit.varnamala.STOP_OVERLAY"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        screenCaptureHelper = ScreenCaptureHelper(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        createOverlayView()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Varnamala Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen overlay for OCR"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Create PendingIntent for stop action
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.drawable.ic_image_outline)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_delete_outline,
                getString(R.string.overlay_notification_stop_action),
                stopPendingIntent
            )
            .build()
    }

    private fun createOverlayView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        overlayY.intValue = (screenHeight * 0.1f).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.START
        params.x = overlayX.intValue
        params.y = overlayY.intValue

        overlayParams = params

        floatingButtonView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                VarnamalaTheme {
                    OverlayContent(
                        overlayX = overlayX,
                        overlayY = overlayY,
                        isProcessing = isProcessing,
                        selectedLanguage = selectedLanguage,
                        targetLanguage = targetLanguage,
                        onDragUpdate = { x, y ->
                            overlayParams?.let { p ->
                                p.x = x
                                p.y = y
                                try {
                                    windowManager?.updateViewLayout(this, p)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                        },
                        onDragStart = {},
                        onDragEnd = { x, y, isInDeleteZone ->
                            if (isInDeleteZone) {
                                stopSelf()
                            } else {
                                overlayX.intValue = x
                                overlayY.intValue = y
                                overlayParams?.let { p ->
                                    p.x = x
                                    p.y = y
                                    try {
                                        windowManager?.updateViewLayout(this, p)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        onCaptureClick = {
                            captureAndProcessScreen()
                        },
                        onLanguageSelect = { language ->
                            selectedLanguage.value = language
                        },
                        onTargetLanguageSelect = { language ->
                            targetLanguage.value = language
                        }
                    )
                }
            }
        }

        try {
            windowManager?.addView(floatingButtonView, params)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    private fun OverlayContent(
        overlayX: MutableState<Int>,
        overlayY: MutableState<Int>,
        isProcessing: MutableState<Boolean>,
        selectedLanguage: MutableState<Language?>,
        targetLanguage: MutableState<Language?>,
        onDragUpdate: (Int, Int) -> Unit,
        onDragStart: () -> Unit,
        onDragEnd: (Int, Int, Boolean) -> Unit,
        onCaptureClick: () -> Unit,
        onLanguageSelect: (Language?) -> Unit,
        onTargetLanguageSelect: (Language?) -> Unit
    ) {
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var localIsDragging by remember { mutableStateOf(false) }
        val availableLanguages = remember { AppLanguageRepository.getSupportedLanguages() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // Captured image as background (when available)
            if (capturedBitmap != null) {
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Captured Screen",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    localIsDragging = true
                                    onDragStart()
                                },
                                onDragEnd = {
                                    val finalX = overlayX.value
                                    val finalY = (overlayY.value + offsetY).roundToInt()

                                    onDragEnd(finalX, finalY, false)

                                    // Small delay to prevent click trigger after drag
                                    serviceScope.launch {
                                        kotlinx.coroutines.delay(150)
                                        localIsDragging = false
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                },
                                onDragCancel = {
                                    localIsDragging = false
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetY -= dragAmount.y
                                    val newY = (overlayY.value + offsetY).roundToInt()
                                    onDragUpdate(overlayX.value, newY)
                                }
                            )
                        },
                    contentScale = ContentScale.Fit
                )

                // Close button
                if (!isProcessing.value) {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            // Clear captured image and results
                            capturedBitmap?.recycle()
                            capturedBitmap = null
                            ocrResult = null
                            rawOcrText.value = ""
                            detectedLanguage.value = "unknown"
                            transliteratedText.value = ""
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_outline),
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom section
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // OCR Results
                    if (rawOcrText.value.isNotBlank() && !isProcessing.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            OCRInfoCard(
                                ocrLoading = false,
                                ocrError = null,
                                rawOcrText = rawOcrText.value,
                                transliteratedText = transliteratedText.value,
                                onCopy = {
                                    val clipboard =
                                        getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText(
                                        getString(R.string.clipboard_label_ocr_text),
                                        transliteratedText.value
                                    )
                                    clipboard.setPrimaryClip(clip)
                                    serviceScope.launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@OverlayService,
                                            getString(R.string.overlay_copied_to_clipboard),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Loading indicator
                    if (isProcessing.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Processing OCR...",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Language Selector
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LanguageSelectionRow(
                            sourceLanguage = selectedLanguage.value,
                            targetLanguage = targetLanguage.value,
                            onSourceLanguageClick = {
                                if (!localIsDragging) {
                                    // Cycle through languages
                                    val currentIndex =
                                        availableLanguages.indexOf(selectedLanguage.value)
                                    val nextLanguage =
                                        if (currentIndex >= 0 && currentIndex < availableLanguages.size - 1) {
                                            availableLanguages[currentIndex + 1]
                                        } else {
                                            availableLanguages.firstOrNull()
                                        }
                                    onLanguageSelect(nextLanguage)
                                }
                            },
                            onTargetLanguageClick = {
                                if (!localIsDragging) {
                                    // Cycle through languages for target
                                    val currentIndex =
                                        availableLanguages.indexOf(targetLanguage.value)
                                    val nextLanguage =
                                        if (currentIndex >= 0 && currentIndex < availableLanguages.size - 1) {
                                            availableLanguages[currentIndex + 1]
                                        } else {
                                            availableLanguages.firstOrNull()
                                        }
                                    onTargetLanguageSelect(nextLanguage)
                                }
                            },
                            modifier = Modifier
                        )
                    }

                    // Control buttons
                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Delete Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                                .clickable {
                                    if (!localIsDragging) {
                                        stopSelf()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_outline),
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Capture Button
                        CameraCaptureButton(
                            text = if (isProcessing.value) "Capturing..." else "Tap",
                            size = 80.dp,
                            textColor = Color.White,
                            backgroundColor = Color(0xFFFF9800),
                            longPressBackgroundColor = Color(0xFFFF9800),
                            progressColor = Color.White,
                            isRealTimeMode = false,
                            onSingleCapture = {
                                if (!localIsDragging) {
                                    onCaptureClick()
                                }
                            },
                            onLongPressStart = {},
                            onLongPressEnd = {}
                        )

                        // Drag Handle Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            localIsDragging = true
                                            onDragStart()
                                        },
                                        onDragEnd = {
                                            val finalX = overlayX.value
                                            val finalY = (overlayY.value + offsetY).roundToInt()

                                            onDragEnd(finalX, finalY, false)

                                            // Small delay to prevent click trigger after drag
                                            serviceScope.launch {
                                                kotlinx.coroutines.delay(150)
                                                localIsDragging = false
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                        },
                                        onDragCancel = {
                                            localIsDragging = false
                                            offsetX = 0f
                                            offsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetY -= dragAmount.y
                                            val newY = (overlayY.value + offsetY).roundToInt()
                                            onDragUpdate(overlayX.value, newY)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessing.value) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                // Drag icon (three horizontal lines)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    repeat(3) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(3.dp)
                                                .background(Color.White, RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // When no image captured, show just the controls
            if (capturedBitmap == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Language Selector
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LanguageSelectionRow(
                            sourceLanguage = selectedLanguage.value,
                            targetLanguage = targetLanguage.value,
                            onSourceLanguageClick = {
                                if (!localIsDragging) {
                                    // Cycle through languages
                                    val currentIndex =
                                        availableLanguages.indexOf(selectedLanguage.value)
                                    val nextLanguage =
                                        if (currentIndex >= 0 && currentIndex < availableLanguages.size - 1) {
                                            availableLanguages[currentIndex + 1]
                                        } else {
                                            availableLanguages.firstOrNull()
                                        }
                                    onLanguageSelect(nextLanguage)
                                }
                            },
                            onTargetLanguageClick = {
                                if (!localIsDragging) {
                                    // Cycle through languages for target
                                    val currentIndex =
                                        availableLanguages.indexOf(targetLanguage.value)
                                    val nextLanguage =
                                        if (currentIndex >= 0 && currentIndex < availableLanguages.size - 1) {
                                            availableLanguages[currentIndex + 1]
                                        } else {
                                            availableLanguages.firstOrNull()
                                        }
                                    onTargetLanguageSelect(nextLanguage)
                                }
                            },
                            modifier = Modifier
                        )
                    }

                    Row(
                        modifier = Modifier.wrapContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Delete Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                                .clickable {
                                    if (!localIsDragging) {
                                        stopSelf()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_outline),
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Capture Button
                        CameraCaptureButton(
                            text = if (isProcessing.value) "Capturing..." else "Tap",
                            size = 80.dp,
                            textColor = Color.White,
                            backgroundColor = Color(0xFFFF9800),
                            longPressBackgroundColor = Color(0xFFFF9800),
                            progressColor = Color.White,
                            isRealTimeMode = false,
                            onSingleCapture = {
                                if (!localIsDragging) {
                                    onCaptureClick()
                                }
                            },
                            onLongPressStart = {},
                            onLongPressEnd = {}
                        )

                        // Drag Handle Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            localIsDragging = true
                                            onDragStart()
                                        },
                                        onDragEnd = {
                                            val finalX = overlayX.value
                                            val finalY = (overlayY.value + offsetY).roundToInt()

                                            onDragEnd(finalX, finalY, false)

                                            // Small delay to prevent click trigger after drag
                                            serviceScope.launch {
                                                kotlinx.coroutines.delay(150)
                                                localIsDragging = false
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                        },
                                        onDragCancel = {
                                            localIsDragging = false
                                            offsetX = 0f
                                            offsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetY -= dragAmount.y
                                            val newY = (overlayY.value + offsetY).roundToInt()
                                            onDragUpdate(overlayX.value, newY)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessing.value) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                // Drag icon (three horizontal lines)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    repeat(3) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(3.dp)
                                                .background(Color.White, RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun captureAndProcessScreen() {
        if (isProcessing.value) {
            serviceScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    this@OverlayService,
                    "Processing previous capture...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        if (screenCaptureHelper == null) {
            serviceScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    this@OverlayService,
                    "Screen capture not initialized",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        if (!screenCaptureHelper!!.isActive()) {
            serviceScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    this@OverlayService,
                    "Screen capture permission needed. Please stop and restart overlay from settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        isProcessing.value = true

        // Hide overlay temporarily to avoid capturing it
        serviceScope.launch(Dispatchers.Main) {
            floatingButtonView?.visibility = android.view.View.INVISIBLE

            // Wait for overlay to disappear from screen
            kotlinx.coroutines.delay(100)

            screenCaptureHelper?.captureScreen { bitmap ->
                // Restore overlay visibility
                floatingButtonView?.visibility = android.view.View.VISIBLE

                if (bitmap != null) {
                    capturedBitmap = bitmap
                    // Process OCR - results will show in same overlay
                    processOCR(bitmap)
                } else {
                    isProcessing.value = false
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@OverlayService,
                            "Failed to capture screen",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun processOCR(bitmap: Bitmap) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)

                // Use selected language
                val langCode = selectedLanguage.value?.code ?: "en"
                when {
                    langCode == "hi" || langCode.startsWith("mr") || langCode.startsWith("ne") || langCode.startsWith(
                        "sa"
                    ) -> {
                        // Devanagari script languages
                        val devanagariRecognizer = TextRecognition.getClient(
                            DevanagariTextRecognizerOptions.Builder().build()
                        )
                        devanagariRecognizer.process(inputImage)
                            .addOnSuccessListener { result ->
                                val ocrResult = convertToOCRResult(
                                    result,
                                    "devanagari",
                                    bitmap.width,
                                    bitmap.height
                                )
                                this@OverlayService.ocrResult = ocrResult

                                // Store raw OCR text and detect language
                                rawOcrText.value = result.text
                                detectedLanguage.value = if (result.text.isNotBlank()) {
                                    LanguageDetector.detectLanguage(result.text)
                                } else {
                                    "unknown"
                                }

                                transliteratedText.value = targetLanguage.value?.let { targetLang ->
                                    if (rawOcrText.value.isNotBlank()) {
                                        Transliterator.transliterate(
                                            text = rawOcrText.value,
                                            sourceLanguage = detectedLanguage.value,
                                            targetLanguage = targetLang.localeCode
                                        )
                                    } else rawOcrText.value
                                } ?: rawOcrText.value

                                isProcessing.value = false
                            }
                            .addOnFailureListener { e ->
                                isProcessing.value = false
                                serviceScope.launch(Dispatchers.Main) {
                                    floatingButtonView?.alpha = 1f
                                }
                            }
                    }

                    else -> {
                        // Latin script languages (default)
                        val latinRecognizer =
                            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        latinRecognizer.process(inputImage)
                            .addOnSuccessListener { result ->
                                val ocrResult =
                                    convertToOCRResult(result, "latin", bitmap.width, bitmap.height)
                                this@OverlayService.ocrResult = ocrResult

                                // Store raw OCR text and detect language
                                rawOcrText.value = result.text
                                detectedLanguage.value = if (result.text.isNotBlank()) {
                                    LanguageDetector.detectLanguage(result.text)
                                } else {
                                    "unknown"
                                }

                                transliteratedText.value = targetLanguage.value?.let { targetLang ->
                                    if (rawOcrText.value.isNotBlank()) {
                                        Transliterator.transliterate(
                                            text = rawOcrText.value,
                                            sourceLanguage = detectedLanguage.value,
                                            targetLanguage = targetLang.localeCode
                                        )
                                    } else rawOcrText.value
                                } ?: rawOcrText.value

                                isProcessing.value = false
                            }
                            .addOnFailureListener { e ->
                                isProcessing.value = false
                                serviceScope.launch(Dispatchers.Main) {
                                    floatingButtonView?.alpha = 1f
                                }
                            }
                    }
                }
            } catch (e: Exception) {
                isProcessing.value = false
                serviceScope.launch(Dispatchers.Main) {
                    floatingButtonView?.alpha = 1f
                }
            }
        }
    }

    private fun convertToOCRResult(
        mlkitResult: Text,
        language: String,
        imageWidth: Int,
        imageHeight: Int
    ): OCRResult {
        val textBlocks = mlkitResult.textBlocks.map { block ->
            TextBlock(
                text = block.text,
                boundingBox = block.boundingBox ?: android.graphics.Rect(),
                lines = block.lines.map { line ->
                    TextLine(
                        text = line.text,
                        boundingBox = line.boundingBox ?: android.graphics.Rect(),
                        elements = line.elements.map { element ->
                            TextElement(
                                text = element.text,
                                boundingBox = element.boundingBox ?: android.graphics.Rect()
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
            imageRotation = 0
        )
    }

    private fun closeOCROverlay() {
        closeButtonView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        closeButtonView = null

        ocrOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        ocrOverlayView = null
        ocrResult = null

        // Clear text state
        rawOcrText.value = ""
        detectedLanguage.value = "unknown"
        transliteratedText.value = ""

        capturedBitmap?.recycle()
        capturedBitmap = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INIT_SCREEN_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                if (resultCode == Activity.RESULT_OK && data != null) {
                    screenCaptureHelper?.initMediaProjection(resultCode, data)
                    serviceScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@OverlayService,
                            "Screen capture ready!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            ACTION_STOP_OVERLAY -> {
                stopSelf()
            }

            else -> {
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@OverlayService,
                        "Please stop and restart overlay from settings to enable screen capture",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceScope.cancel()

        closeOCROverlay()

        floatingButtonView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        floatingButtonView = null

        screenCaptureHelper?.release()
        screenCaptureHelper = null

        capturedBitmap?.recycle()
        capturedBitmap = null

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        windowManager = null

        // Notify manager that service is stopped
        OverlayManager.markServiceStopped()
    }
}
