package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.ui.components.OCRInfoCard
import com.sumit.varnamala.ui.components.AppModalBottomSheet
import com.sumit.varnamala.ui.components.ConfirmBottomSheet
import com.sumit.varnamala.ui.components.DisplayOptionsDialog
import com.sumit.varnamala.ui.components.DetectionOptionsDialog
import com.sumit.varnamala.ui.components.LanguageSelectionRow
import com.sumit.varnamala.ui.components.OCREngineSelectionDialog
import com.sumit.varnamala.data.Language
import com.sumit.varnamala.data.HistoryEntry
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.data.TransliterationLanguageCatalog
import com.sumit.varnamala.data.TransliterationHistoryRepository
import com.sumit.varnamala.data.ocr.OCRCacheRepository
import com.sumit.varnamala.data.ocr.CachedOCRResult
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.OcrLanguageCatalog
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextLine
import com.sumit.varnamala.data.ocr.TextElement
import com.sumit.varnamala.utils.LanguageDetector
import com.sumit.varnamala.utils.Transliterator
import com.sumit.varnamala.data.ocr.CachedLanguageGroup
import com.sumit.varnamala.data.ocr.CachedTextElement
import com.sumit.varnamala.data.preferences.OCRSettingsRepository
import com.sumit.varnamala.data.preferences.UserPreferencesRepository
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.OCROverlayCanvas
import com.sumit.varnamala.data.ocr.OcrEngine
import com.sumit.varnamala.ocr.mlkit.MLKitOCRRepository
import com.sumit.varnamala.ocr.tesseract.TesseractOcrRepository
import com.sumit.varnamala.models.TesseractModelStore

import android.content.ClipData
import android.content.ClipboardManager
import kotlinx.coroutines.flow.first
import android.content.ContentUris
import android.provider.MediaStore
import android.content.ContentResolver
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageDetailScreen(navController: NavController, imageId: Long) {
    val context = LocalContext.current
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    var meta by remember { mutableStateOf<ImageMeta?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val cacheRepository = remember { OCRCacheRepository(context) }
    val settingsRepository = remember { OCRSettingsRepository(context) }
    val userPrefsRepository = remember { UserPreferencesRepository(context) }
    val historyRepository = remember { TransliterationHistoryRepository(context) }

    // User preferences
    val autoCopyEnabled by userPrefsRepository.autoCopyEnabled.collectAsState(initial = false)

    // OCR state variables
    var ocrLoading by remember { mutableStateOf(false) }
    var ocrError by remember { mutableStateOf<String?>(null) }
    var rawOcrText by remember { mutableStateOf("") }
    var detectedLanguage by remember { mutableStateOf("unknown") }
    var languageConfidence by remember { mutableFloatStateOf(0f) }
    var lastResult by remember { mutableStateOf<OCRResult?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showDisplayOptionsDialog by remember { mutableStateOf(false) }
    var showDetectionOptionsDialog by remember { mutableStateOf(false) }
    var showImageDetailsDialog by remember { mutableStateOf(false) }

    // Language selection state
    var showEngineSelectionDialog by remember { mutableStateOf(false) }
    val selectedOcrModelCode by settingsRepository.selectedOcrModelCode.collectAsState(
        initial = OcrLanguageCatalog.DEFAULT_SOURCE_CODE
    )
    val selectedTargetCode by settingsRepository.selectedTargetTransliterationCode.collectAsState(
        initial = "hi"
    )
    val selectedOcrModel = remember(selectedOcrModelCode) {
        OcrLanguageCatalog.sourceOrDefault(selectedOcrModelCode)
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
    val selectedTargetLanguage = remember(selectedTargetCode) {
        TransliterationLanguageCatalog.find(selectedTargetCode)
            ?: TransliterationLanguageCatalog.defaultTarget()
    }

    // Settings from repository
    val showBoundingBoxes by settingsRepository.showBoundingBoxes.collectAsState(initial = true)
    val showTextOverlay by settingsRepository.showTextOverlay.collectAsState(initial = true)
    val showWordBoundingBoxes by settingsRepository.showWordBoundingBoxes.collectAsState(initial = true)
    val showLineBoundingBoxes by settingsRepository.showLineBoundingBoxes.collectAsState(initial = true)
    val showBlockBoundingBoxes by settingsRepository.showBlockBoundingBoxes.collectAsState(initial = true)
    val textBackgroundOpacity by settingsRepository.textBackgroundOpacity.collectAsState(initial = 0.8f)
    val boundingBoxStrokeWidth by settingsRepository.boundingBoxStrokeWidth.collectAsState(initial = 2f)
    val visualReplacementMode by settingsRepository.visualReplacementMode.collectAsState(
        initial = com.sumit.varnamala.data.ocr.OcrVisualReplacementMode.REPLACE_LITE
    )

    // Image display state
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val selectedEngine by settingsRepository.selectedOcrEngine.collectAsState(initial = OcrEngine.TESSERACT)
    var mlKitLanguage by remember { mutableStateOf("auto") }

    LaunchedEffect(imageId) {
        meta = loadImageMeta(context.contentResolver, imageId)
        runOCRWithCache(
            repository = cacheRepository,
            imageId = imageId,
            context = context,
            settingsRepository = settingsRepository,
            setLoading = { ocrLoading = it },
            setError = { ocrError = it },
            setText = { rawOcrText = it },
            setLang = { newLang -> detectedLanguage = newLang },
            setResult = { lastResult = it })
    }

    LaunchedEffect(selectedOcrModelCode) {
        if (meta != null) {
            runOCRWithCache(
                repository = cacheRepository,
                imageId = imageId,
                context = context,
                forceRefresh = true,
                preferredLanguage = selectedOcrModel.code,
                settingsRepository = settingsRepository,
                setLoading = { ocrLoading = it },
                setError = { ocrError = it },
                setText = { rawOcrText = it },
                setLang = { newLang -> detectedLanguage = newLang },
                setResult = { lastResult = it }
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        meta?.let { m ->
            val targetUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId
            )
            val aspectRatio =
                if (m.width != null && m.height != null && m.height != 0) m.width.toFloat() / m.height.toFloat() else 1f
            val transformableState =
                rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                    offset += offsetChange
                }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(aspectRatio)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(targetUri).crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(aspectRatio)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        )
                        .transformable(state = transformableState)
                        .onGloballyPositioned { coordinates ->
                            imageSize = coordinates.size
                        },
                    contentScale = ContentScale.Fit
                )

                val overlayTextOverrides =
                    remember(lastResult, selectedTargetLanguage, detectedLanguage) {
                        val targetLang = selectedTargetLanguage
                        if (targetLang == null || lastResult == null) {
                            null
                        } else {
                            lastResult!!.boundingBoxes
                                .flatMap { it.lines }
                                .flatMap { it.elements }
                                .associate { element ->
                                    element.boundingBox to Transliterator.transliterate(
                                        text = element.text,
                                        sourceLanguage = detectedLanguage,
                                        targetLanguage = targetLang.localeCode
                                    )
                                }
                        }
                    }

                OCROverlayCanvas(
                    result = lastResult,
                    textOverride = overlayTextOverrides,
                    containerSize = imageSize,
                    visualReplacementMode = visualReplacementMode,
                    showBoundingBoxes = showBoundingBoxes,
                    showTextOverlay = showTextOverlay,
                    showWordBoundingBoxes = showWordBoundingBoxes,
                    showLineBoundingBoxes = showLineBoundingBoxes,
                    showBlockBoundingBoxes = showBlockBoundingBoxes,
                    textBackgroundOpacity = textBackgroundOpacity,
                    boundingBoxStrokeWidth = boundingBoxStrokeWidth,
                    enableTextCopy = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(aspectRatio)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, topInset + 10.dp, 16.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(
                icon = painterResource(R.drawable.ic_chevron_left),
                size = 45.dp,
                onClick = { navController.popBackStack() })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Image details button
                GlassButton(
                    icon = painterResource(R.drawable.ic_info),
                    size = 45.dp,
                    onClick = { showImageDetailsDialog = true })

                // Display options button
                GlassButton(
                    icon = painterResource(R.drawable.ic_image_outline),
                    size = 45.dp,
                    onClick = { showDisplayOptionsDialog = true })
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = topInset * 3),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ocrLoading || ocrError != null || rawOcrText.isNotBlank() || lastResult != null) {
                val transliteratedText = selectedTargetLanguage?.let { targetLang ->
                    if (rawOcrText.isNotBlank()) {
                        Transliterator.transliterate(
                            text = rawOcrText,
                            sourceLanguage = detectedLanguage,
                            targetLanguage = targetLang.localeCode
                        )
                    } else rawOcrText
                } ?: rawOcrText

                LaunchedEffect(rawOcrText, transliteratedText) {
                    if (rawOcrText.isNotBlank() && transliteratedText.isNotBlank() && transliteratedText != rawOcrText) {
                        // Save to history
                        historyRepository.addEntry(
                            HistoryEntry(
                                originalText = rawOcrText,
                                transliteratedText = transliteratedText,
                                sourceLanguage = detectedLanguage,
                                targetLanguage = selectedTargetLanguage?.localeCode ?: "en",
                                source = "gallery"
                            )
                        )

                        // Auto-copy if enabled
                        if (autoCopyEnabled) {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    context.getString(R.string.clipboard_label_transliterated_text),
                                    transliteratedText
                                )
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                OCRInfoCard(
                    ocrLoading = ocrLoading,
                    ocrError = ocrError,
                    rawOcrText = rawOcrText,
                    transliteratedText = transliteratedText,
                    onCopy = {
                        val clip = ClipData.newPlainText(
                            context.getString(R.string.ocr_text),
                            transliteratedText
                        )
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            context.getString(R.string.copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                )
            }

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
            )

//            GlassButton(
//                icon = painterResource(R.drawable.ic_settings_outline),
//                background = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
//                onClick = { rotation = (rotation + 90f) % 360f })
        }
    }

    // Display Options Dialog
    if (showDisplayOptionsDialog) {
        DisplayOptionsDialog(
            onDismiss = { showDisplayOptionsDialog = false },
            showBoundingBoxes = showBoundingBoxes,
            onShowBoundingBoxesChange = {
                coroutineScope.launch {
                    settingsRepository.setShowBoundingBoxes(
                        it
                    )
                }
            },
            showTextOverlay = showTextOverlay,
            onShowTextOverlayChange = {
                coroutineScope.launch {
                    settingsRepository.setShowTextOverlay(
                        it
                    )
                }
            },
            showWordBoundingBoxes = showWordBoundingBoxes,
            onShowWordBoundingBoxesChange = {
                coroutineScope.launch {
                    settingsRepository.setShowWordBoundingBoxes(
                        it
                    )
                }
            },
            showLineBoundingBoxes = showLineBoundingBoxes,
            onShowLineBoundingBoxesChange = {
                coroutineScope.launch {
                    settingsRepository.setShowLineBoundingBoxes(
                        it
                    )
                }
            },
            showBlockBoundingBoxes = showBlockBoundingBoxes,
            onShowBlockBoundingBoxesChange = {
                coroutineScope.launch {
                    settingsRepository.setShowBlockBoundingBoxes(
                        it
                    )
                }
            },
            textBackgroundOpacity = textBackgroundOpacity,
            onTextBackgroundOpacityChange = {
                coroutineScope.launch {
                    settingsRepository.setTextBackgroundOpacity(
                        it
                    )
                }
            },
            boundingBoxStrokeWidth = boundingBoxStrokeWidth,
            onBoundingBoxStrokeWidthChange = {
                coroutineScope.launch {
                    settingsRepository.setBoundingBoxStrokeWidth(
                        it
                    )
                }
            }
        )
    }

    // Detection Options Dialog
    if (showDetectionOptionsDialog) {
        DetectionOptionsDialog(
            onDismiss = { showDetectionOptionsDialog = false },
            onLanguageSelected = { code ->
                mlKitLanguage = code
                coroutineScope.launch {
                    runOCRWithCache(
                        repository = cacheRepository,
                        imageId = imageId,
                        context = context,
                        forceRefresh = true,
                        preferredLanguage = if (code == "auto") null else code,
                        settingsRepository = settingsRepository,
                        setLoading = { ocrLoading = it },
                        setError = { ocrError = it },
                        setText = { rawOcrText = it },
                        setLang = { newLang -> detectedLanguage = newLang },
                        setResult = { lastResult = it }
                    )
                }
            },
            currentLanguage = mlKitLanguage,
            onDetectAgain = {
                coroutineScope.launch {
                    runOCRWithCache(
                        repository = cacheRepository,
                        imageId = imageId,
                        context = context,
                        forceRefresh = true,
                        preferredLanguage = mlKitLanguage,
                        settingsRepository = settingsRepository,
                        setLoading = { ocrLoading = it },
                        setError = { ocrError = it },
                        setText = { rawOcrText = it },
                        setLang = { newLang -> detectedLanguage = newLang },
                        setResult = { lastResult = it }
                    )
                }
            },
            onClearCache = {
                coroutineScope.launch {
                    cacheRepository.clearCache(imageId)
                    rawOcrText = ""
                    detectedLanguage = "unknown"
                    languageConfidence = 0f
                    lastResult = null
                }
            },
            onClearAll = { showClearAllDialog = true },
            ocrLoading = ocrLoading
        )
    }

    // Image Details Dialog
    if (showImageDetailsDialog) {
        ImageDetailsDialog(
            onDismiss = { showImageDetailsDialog = false },
            imageId = imageId,
            imageMeta = meta
        )
    }

    // Clear All confirmation dialog
    if (showClearAllDialog) {
        ConfirmBottomSheet(
            title = stringResource(R.string.clear_all_ocr_cache),
            message = stringResource(R.string.clear_cache_confirmation),
            destructive = true,
            onDismiss = { showClearAllDialog = false },
            onConfirm = {
                showClearAllDialog = false
                coroutineScope.launch {
                    cacheRepository.clearCache(null)
                    if (!ocrLoading) {
                        rawOcrText = ""
                        detectedLanguage = "unknown"
                        languageConfidence = 0f
                        lastResult = null
                    }
                }
            }
        )
    }

    // OCR Engine Selection Dialog
    if (showEngineSelectionDialog) {
        OCREngineSelectionDialog(
            currentEngine = selectedEngine,
            onEngineSelected = { engine ->
                // Trigger OCR with new engine
                coroutineScope.launch {
                    settingsRepository.setSelectedOcrEngine(engine)
                    runOCRWithCache(
                        repository = cacheRepository,
                        imageId = imageId,
                        context = context,
                        forceRefresh = true,
                        preferredLanguage = selectedSourceLanguage.code,
                        settingsRepository = settingsRepository,
                        setLoading = { ocrLoading = it },
                        setError = { ocrError = it },
                        setText = { rawOcrText = it },
                        setLang = { newLang -> detectedLanguage = newLang },
                        setResult = { lastResult = it }
                    )
                }
            },
            onDismiss = { showEngineSelectionDialog = false }
        )
    }
}


data class ImageMeta(
    val id: Long,
    val displayName: String?,
    val mimeType: String?,
    val width: Int?,
    val height: Int?,
    val size: Long?,
    val dateAdded: Long?,
    val orientation: Int?
)


private suspend fun loadImageMeta(resolver: ContentResolver, id: Long): ImageMeta? =
    withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.ORIENTATION
            )
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Images.Media._ID + "=?",
                arrayOf(id.toString()),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val displayName =
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                    val mime =
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                    val width = c.getInt(c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                    val height = c.getInt(c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                    val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                    val dateAdded =
                        c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                    val orientationIdx = c.getColumnIndex(MediaStore.Images.Media.ORIENTATION)
                    val orientation = if (orientationIdx != -1) c.getInt(orientationIdx) else null
                    return@withContext ImageMeta(
                        id, displayName, mime, width, height, size, dateAdded, orientation
                    )
                }
            }
        } catch (_: Exception) {
        }
        null
    }

private suspend fun runOCRWithCache(
    repository: OCRCacheRepository,
    imageId: Long,
    context: Context,
    forceRefresh: Boolean = false,
    preferredLanguage: String? = null,
    settingsRepository: OCRSettingsRepository? = null,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    setText: (String) -> Unit,
    setLang: (String) -> Unit,
    setResult: (OCRResult) -> Unit
) {
    val enableCacheStorage = settingsRepository?.enableCacheStorage?.first() ?: true

    if (!forceRefresh && enableCacheStorage) {
        repository.getCachedResult(imageId)?.let { cached ->
            setText(cached.text)
            setLang(cached.detectedLanguage)

            // Convert cached language groups back to OCRResult with bounding boxes
            val textBlocks = mutableListOf<TextBlock>()

            // Group elements by their position to reconstruct text blocks and lines
            val sortedGroups = cached.languageGroups.sortedBy { group ->
                group.elements.minOfOrNull { it.boundingBox.top } ?: 0
            }

            sortedGroups.forEach { group ->
                // Create text elements from cached elements
                val elements = group.elements.map { cachedElement ->
                    TextElement(
                        text = cachedElement.text,
                        boundingBox = cachedElement.boundingBox
                    )
                }

                // Group elements into lines by Y position (approximate)
                val lineGroups = elements.groupBy { element ->
                    // Group by approximate line (within 10 pixels)
                    element.boundingBox.top / 10
                }.values.sortedBy { lineElements ->
                    lineElements.minOf { it.boundingBox.top }
                }

                val lines = lineGroups.map { lineElements ->
                    val sortedElements = lineElements.sortedBy { it.boundingBox.left }
                    val lineText = sortedElements.joinToString(" ") { it.text }
                    val lineBounds =
                        sortedElements.fold(sortedElements.first().boundingBox) { acc, element ->
                            Rect(
                                minOf(acc.left, element.boundingBox.left),
                                minOf(acc.top, element.boundingBox.top),
                                maxOf(acc.right, element.boundingBox.right),
                                maxOf(acc.bottom, element.boundingBox.bottom)
                            )
                        }

                    TextLine(
                        text = lineText,
                        boundingBox = lineBounds,
                        elements = sortedElements
                    )
                }

                // Create text block from lines
                if (lines.isNotEmpty()) {
                    val blockText = lines.joinToString("\n") { it.text }
                    val blockBounds = lines.fold(lines.first().boundingBox) { acc, line ->
                        Rect(
                            minOf(acc.left, line.boundingBox.left),
                            minOf(acc.top, line.boundingBox.top),
                            maxOf(acc.right, line.boundingBox.right),
                            maxOf(acc.bottom, line.boundingBox.bottom)
                        )
                    }

                    textBlocks.add(
                        TextBlock(
                            text = blockText,
                            boundingBox = blockBounds,
                            lines = lines
                        )
                    )
                }
            }

            val imageMeta = loadImageMeta(context.contentResolver, imageId)
            setResult(
                OCRResult(
                    text = cached.text,
                    language = cached.detectedLanguage,
                    boundingBoxes = textBlocks,
                    imageWidth = imageMeta?.width ?: 1920, // fallback dimensions
                    imageHeight = imageMeta?.height ?: 1080
                )
            )
            return
        }
    }

    setLoading(true)
    setError(null)
    try {
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId)
        val selectedEngine = settingsRepository?.selectedOcrEngine?.first() ?: OcrEngine.TESSERACT
        val modelCode = preferredLanguage
            ?: settingsRepository?.selectedOcrModelCode?.first()
            ?: OcrLanguageCatalog.DEFAULT_SOURCE_CODE
        val result = runSingleImageOcr(
            context = context,
            uri = uri,
            preferredLanguage = modelCode,
            engine = selectedEngine
        )
        setText(result.text)
        val lang = LanguageDetector.detectLanguage(result.text)
        setLang(lang)
        val enriched = result.copy(language = lang)
        setResult(enriched)
        val groupsMap = mutableMapOf<String, MutableList<TextElement>>()
        result.boundingBoxes.forEach { block ->
            block.lines.forEach { line ->
                val lineLang = LanguageDetector.detectLanguage(line.text)
                line.elements.forEach { el ->
                    groupsMap.getOrPut(lineLang) { mutableListOf() }.add(el)
                }
            }
        }
        val cachedGroups = groupsMap.map { (languageCode, elements) ->
            CachedLanguageGroup(
                language = languageCode, elements = elements.map { e ->
                    CachedTextElement(text = e.text, boundingBox = e.boundingBox)
                })
        }
        // Only cache if cache storage is enabled
        if (enableCacheStorage) {
            repository.cacheResult(
                CachedOCRResult(
                    imageId = imageId,
                    text = enriched.text,
                    detectedLanguage = enriched.language ?: lang,
                    languageGroups = cachedGroups,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    } catch (e: Exception) {
        setError(e.message ?: "OCR failed")
    } finally {
        setLoading(false)
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private suspend fun runSingleImageOcr(
    context: Context,
    uri: Uri,
    preferredLanguage: String? = null,
    engine: OcrEngine = OcrEngine.TESSERACT
): OCRResult = withContext(Dispatchers.IO) {
    if (engine == OcrEngine.TESSERACT) {
        val model = OcrLanguageCatalog.sourceOrDefault(preferredLanguage)
        if (!TesseractModelStore.isModelDownloaded(context, model)) {
            throw IllegalStateException(
                context.getString(R.string.model_required_for_ocr, model.displayName)
            )
        }
        TesseractOcrRepository(context).processUri(uri, model)
    } else {
        val mlKitRepository = MLKitOCRRepository(context)
        if (preferredLanguage == null || preferredLanguage == "auto") {
            mlKitRepository.processImageWithAutoDetection(uri)
        } else {
            mlKitRepository.processImageFromUri(uri, preferredLanguage)
        }
    }
}


/**
 * Dialog for displaying image metadata details
 */
@Composable
private fun ImageDetailsDialog(
    onDismiss: () -> Unit,
    imageId: Long,
    imageMeta: ImageMeta?
) {
    AppModalBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.image_details),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            imageMeta?.let { m ->
                InfoRow("ID", imageId.toString())
                InfoRow("Display Name", m.displayName ?: "-")
                InfoRow("Mime Type", m.mimeType ?: "-")
                InfoRow("Width", m.width?.let { "$it px" } ?: "-")
                InfoRow("Height", m.height?.let { "$it px" } ?: "-")
                InfoRow("Size (KB)", m.size?.let { (it / 1024).toString() } ?: "-")
                InfoRow("Date Added", m.dateAdded?.let {
                    java.text.SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        java.util.Locale.getDefault()
                    )
                        .format(java.util.Date(it * 1000))
                } ?: "-")
                InfoRow("Orientation", m.orientation?.let {
                    when (it) {
                        0 -> "Normal (0°)"
                        90 -> "Rotate 90°"
                        180 -> "Rotate 180°"
                        270 -> "Rotate 270°"
                        else -> "$it°"
                    }
                } ?: "-")
            } ?: run {
                Text(
                    "Image metadata not available",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            label,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.weight(0.6f)
        )
    }
}
