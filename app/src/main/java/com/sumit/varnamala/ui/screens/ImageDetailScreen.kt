package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.ui.components.AppModalBottomSheet
import com.sumit.varnamala.ui.components.ConfirmBottomSheet
import com.sumit.varnamala.ui.components.DisplayOptionsDialog
import com.sumit.varnamala.ui.components.DetectionOptionsDialog
import com.sumit.varnamala.ui.components.LanguageSelectionRow
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
import com.sumit.varnamala.transliteration.LanguageDetector
import com.sumit.varnamala.utils.Transliterator
import com.sumit.varnamala.data.ScanThumbnails
import com.sumit.varnamala.tts.SpeechPart
import com.sumit.varnamala.ui.components.rememberSpeechController
import com.sumit.varnamala.ui.components.speechLanguage
import com.sumit.varnamala.data.ocr.CachedLanguageGroup
import com.sumit.varnamala.data.ocr.CachedTextElement
import com.sumit.varnamala.data.preferences.OCRSettingsRepository
import com.sumit.varnamala.data.preferences.UserPreferencesRepository
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.OCROverlayCanvas
import com.sumit.varnamala.ocr.paddle.PaddleOcrRepository

import android.content.ClipData
import android.content.ClipboardManager
import kotlinx.coroutines.flow.first
import android.content.ContentUris
import android.provider.MediaStore
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.requiredSize
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
fun ImageDetailScreen(navController: NavController, imageId: Long, sharedUri: Uri? = null) {
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
    var lastResult by remember { mutableStateOf<OCRResult?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showDisplayOptionsDialog by remember { mutableStateOf(false) }
    var showDetectionOptionsDialog by remember { mutableStateOf(false) }
    var showImageDetailsDialog by remember { mutableStateOf(false) }

    // Language selection state
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

    // Image display state
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var ocrLanguage by remember { mutableStateOf("auto") }
    // Picture for history lists: shared images are copied small; gallery images use their own URI.
    var thumbnailRef by remember { mutableStateOf("") }
    val speech = rememberSpeechController()
    val highContrast by userPrefsRepository.highContrast.collectAsState(initial = false)
    val textDisplaySize by userPrefsRepository.textDisplaySize.collectAsState(initial = 1f)
    val autoSpeak by userPrefsRepository.ttsAutoSpeak.collectAsState(initial = false)

    LaunchedEffect(imageId, sharedUri) {
        meta = if (sharedUri != null) loadSharedMeta(context, sharedUri) else loadImageMeta(context.contentResolver, imageId)
        runOCRWithCache(
            repository = cacheRepository,
            imageId = imageId,
            uriOverride = sharedUri,
            onThumbnail = { thumbnailRef = it },
            onPreview = { preview ->
                lastResult = preview
                rawOcrText = preview.text
                detectedLanguage = LanguageDetector.detectLanguage(preview.text)
            },
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
                uriOverride = sharedUri,
                onThumbnail = { thumbnailRef = it },
                onPreview = { preview ->
                    lastResult = preview
                    rawOcrText = preview.text
                    detectedLanguage = LanguageDetector.detectLanguage(preview.text)
                },
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
        val targetUri = sharedUri ?: ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId
        )
        val transformableState =
            rememberTransformableState { zoomChange, offsetChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 5f)
                offset += offsetChange
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
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
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(targetUri).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            val overlayTextOverrides =
                remember(lastResult, selectedTargetLanguage, detectedLanguage) {
                    val result = lastResult
                    if (result == null) {
                        null
                    } else {
                        result.boundingBoxes
                            .flatMap { it.lines }
                            .flatMap { it.elements }
                            .filter { !it.isLowConfidence }
                            .associate { element ->
                                element.boundingBox to Transliterator.transliterate(
                                    text = element.text,
                                    sourceLanguage = detectedLanguage,
                                    targetLanguage = selectedTargetLanguage.localeCode
                                )
                            }
                    }
                }

            OCROverlayCanvas(
                result = lastResult,
                textOverride = overlayTextOverrides,
                containerSize = imageSize,
                cropToFill = false,
                showBoundingBoxes = showBoundingBoxes,
                showTextOverlay = showTextOverlay,
                showWordBoundingBoxes = showWordBoundingBoxes,
                showLineBoundingBoxes = showLineBoundingBoxes,
                showBlockBoundingBoxes = showBlockBoundingBoxes,
                textBackgroundOpacity = textBackgroundOpacity,
                boundingBoxStrokeWidth = boundingBoxStrokeWidth,
                enableTextCopy = true,
                onWordTap = { word ->
                    speech.start("word", listOf(SpeechPart(word, speechLanguage(detectedLanguage))))
                },
                highContrast = highContrast,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, topInset + 10.dp, 16.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(
                description = stringResource(R.string.back),
                icon = painterResource(R.drawable.ic_chevron_left),
                size = 45.dp,
                onClick = { navController.popBackStack() })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Image details button
                GlassButton(
                    description = stringResource(R.string.a11y_info),
                    icon = painterResource(R.drawable.ic_info),
                    size = 45.dp,
                    onClick = { showImageDetailsDialog = true })

                // Display options button
                GlassButton(
                    description = stringResource(R.string.a11y_display_options),
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
            ScanStatus(
                loading = ocrLoading,
                error = ocrError,
                foundNothing = !ocrLoading && ocrError == null && lastResult != null && lastResult!!.boundingBoxes.isEmpty(),
            )
            if (ocrLoading || ocrError != null || rawOcrText.isNotBlank() || lastResult != null) {
                // Computed once per result, not on every recomposition.
                val transliteratedText = remember(rawOcrText, detectedLanguage, selectedTargetLanguage) {
                    if (rawOcrText.isBlank()) rawOcrText
                    else Transliterator.transliterate(
                        text = rawOcrText,
                        sourceLanguage = detectedLanguage,
                        targetLanguage = selectedTargetLanguage.localeCode
                    )
                }

                LaunchedEffect(rawOcrText, transliteratedText, thumbnailRef) {
                    if (rawOcrText.isNotBlank() && transliteratedText.isNotBlank() && transliteratedText != rawOcrText) {
                        // Save to history
                        historyRepository.addEntry(
                            HistoryEntry(
                                originalText = rawOcrText,
                                transliteratedText = transliteratedText,
                                sourceLanguage = detectedLanguage,
                                targetLanguage = selectedTargetLanguage.localeCode,
                                source = "gallery",
                                imageRef = thumbnailRef.ifEmpty {
                                    if (sharedUri != null) "" else ContentUris.withAppendedId(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId
                                    ).toString()
                                }
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

                // Read the result aloud as soon as it is ready, when auto-speak is on.
                LaunchedEffect(lastResult) {
                    if (autoSpeak && !ocrLoading && transliteratedText.isNotBlank()) {
                        speech.toggleConverted(
                            "scan",
                            SpeechPart(rawOcrText, speechLanguage(detectedLanguage)),
                            SpeechPart(transliteratedText, selectedTargetLanguage.localeCode),
                        )
                    }
                }
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
//                description = stringResource(R.string.a11y_settings), icon = painterResource(R.drawable.ic_settings_outline),
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
                ocrLanguage = code
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
            currentLanguage = ocrLanguage,
            onDetectAgain = {
                coroutineScope.launch {
                    runOCRWithCache(
                        repository = cacheRepository,
                        imageId = imageId,
                        context = context,
                        forceRefresh = true,
                        preferredLanguage = ocrLanguage,
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
                        lastResult = null
                    }
                }
            }
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
    uriOverride: Uri? = null,
    onThumbnail: (String) -> Unit = {},
    onPreview: (OCRResult) -> Unit = {},
    forceRefresh: Boolean = false,
    preferredLanguage: String? = null,
    settingsRepository: OCRSettingsRepository? = null,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    setText: (String) -> Unit,
    setLang: (String) -> Unit,
    setResult: (OCRResult) -> Unit
) {
    // Images shared from other apps have no stable id, so they are never cached.
    val enableCacheStorage = (settingsRepository?.enableCacheStorage?.first() ?: true) && uriOverride == null && imageId >= 0

    if (!forceRefresh && enableCacheStorage) {
        repository.getCachedResult(imageId)?.let { cached ->
            setText(cached.text)
            setLang(cached.detectedLanguage)

            // Put the cached lines back in the order they were read, one block per line.
            val textBlocks = cached.languageGroups
                .flatMap { it.elements }
                .sortedBy { it.order }
                .map { element ->
                    val textElement = TextElement(element.text, element.boundingBox, element.confidence)
                    val line = TextLine(element.text, element.boundingBox, listOf(textElement), element.confidence)
                    TextBlock(element.text, element.boundingBox, listOf(line), element.confidence)
                }

            val imageMeta = loadImageMeta(context.contentResolver, imageId)
            setResult(
                OCRResult(
                    text = cached.text,
                    language = cached.detectedLanguage,
                    boundingBoxes = textBlocks,
                    // Boxes were measured on the rotated picture, so prefer the saved size over
                    // the gallery's un-rotated one.
                    imageWidth = cached.imageWidth.takeIf { it > 0 } ?: imageMeta?.width ?: 1920,
                    imageHeight = cached.imageHeight.takeIf { it > 0 } ?: imageMeta?.height ?: 1080
                )
            )
            return
        }
    }

    setLoading(true)
    setError(null)
    try {
        val uri = uriOverride ?: ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId)
        val modelCode = preferredLanguage
            ?: settingsRepository?.selectedOcrModelCode?.first()
            ?: OcrLanguageCatalog.DEFAULT_SOURCE_CODE
        val result = runSingleImageOcr(
            context = context,
            uri = uri,
            preferredLanguage = modelCode,
            onThumbnail = if (uriOverride != null) onThumbnail else { _ -> },
            onPreview = onPreview,
        )
        setText(result.text)
        val lang = LanguageDetector.detectLanguage(result.text)
        setLang(lang)
        val enriched = result.copy(language = lang)
        setResult(enriched)
        val groupsMap = linkedMapOf<String, MutableList<CachedTextElement>>()
        var order = 0
        result.boundingBoxes.forEach { block ->
            block.lines.forEach { line ->
                val lineLang = LanguageDetector.detectLanguage(line.text)
                line.elements.forEach { el ->
                    groupsMap.getOrPut(lineLang) { mutableListOf() }
                        .add(CachedTextElement(el.text, el.boundingBox, el.confidence, order++))
                }
            }
        }
        val cachedGroups = groupsMap.map { (languageCode, elements) -> CachedLanguageGroup(language = languageCode, elements = elements) }
        // Only cache if cache storage is enabled
        if (enableCacheStorage) {
            repository.cacheResult(
                CachedOCRResult(
                    imageId = imageId,
                    text = enriched.text,
                    detectedLanguage = enriched.language ?: lang,
                    languageGroups = cachedGroups,
                    timestamp = System.currentTimeMillis(),
                    imageWidth = result.imageWidth,
                    imageHeight = result.imageHeight,
                )
            )
        }
    } catch (e: Exception) {
        setError(e.message ?: "OCR failed")
    } finally {
        setLoading(false)
    }
}

private suspend fun runSingleImageOcr(
    context: Context,
    uri: Uri,
    preferredLanguage: String? = null,
    onThumbnail: (String) -> Unit = {},
    onPreview: (OCRResult) -> Unit = {},
): OCRResult = withContext(Dispatchers.IO) {
    val bitmap = decodeUpright(context, uri)
    onThumbnail(ScanThumbnails.save(context, bitmap))
    val repository = PaddleOcrRepository(context)
    try {
        val provisioner = com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner.get(context)
        val accurateInstalled = provisioner.isReady(com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner.Tier.MEDIUM)
        val fastInstalled = provisioner.isReady(com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner.Tier.TINY)
        if (!accurateInstalled && !fastInstalled) {
            throw IllegalStateException("OCR models are not downloaded. Please download OCR resources in Settings.")
        }
        if (accurateInstalled && fastInstalled) {
            onPreview(repository.processBitmap(bitmap, preferredLanguage, com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner.Tier.TINY))
        }
        val targetTier = if (accurateInstalled) com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner.Tier.MEDIUM else com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner.Tier.TINY
        repository.processBitmap(
            bitmap,
            preferredLanguage,
            targetTier,
            enhance = true,
        )
    } finally {
        repository.release()
    }
}

/** Decodes [uri] with its EXIF rotation applied, so boxes line up with what is shown on screen. */
private fun decodeUpright(context: Context, uri: Uri): android.graphics.Bitmap {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return android.graphics.ImageDecoder.decodeBitmap(
                android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            ) { decoder, _, _ -> decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE }
        }
        // Android 7 and 8 have no ImageDecoder: decode, then rotate by the EXIF orientation.
        val bitmap = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("Unable to open image")
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            androidx.exifinterface.media.ExifInterface(it).getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
        } ?: androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        return android.graphics.Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            android.graphics.Matrix().apply { postRotate(degrees) }, true
        )
    } catch (e: Exception) {
        throw IllegalArgumentException("Unable to load image", e)
    }
}

/** What the app is doing, or why there is nothing to show: reading, failed, or no text in the picture. */
@Composable
private fun ScanStatus(loading: Boolean, error: String?, foundNothing: Boolean) {
    val message = when {
        error != null -> stringResource(R.string.ocr_failed, error)
        foundNothing -> stringResource(R.string.ocr_no_text)
        loading -> stringResource(R.string.ocr_reading)
        else -> return
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (loading && error == null && !foundNothing) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.then(Modifier.requiredSize(18.dp)), strokeWidth = 2.dp)
        }
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Details for an image another app shared, which has no gallery row to read them from. */
private suspend fun loadSharedMeta(context: Context, uri: Uri): ImageMeta? = withContext(Dispatchers.IO) {
    try {
        val bitmapSize = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bitmapSize) }
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            androidx.exifinterface.media.ExifInterface(it).getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
        }
        val sideways = orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
        val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
        ImageMeta(
            id = -1, displayName = name, mimeType = context.contentResolver.getType(uri),
            width = if (sideways) bitmapSize.outHeight else bitmapSize.outWidth,
            height = if (sideways) bitmapSize.outWidth else bitmapSize.outHeight,
            size = null, dateAdded = null, orientation = null,
        )
    } catch (_: Exception) {
        null
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
