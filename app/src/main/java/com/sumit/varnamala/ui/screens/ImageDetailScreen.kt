package com.sumit.varnamala.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import com.sumit.varnamala.R
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.OCRInfoCard
import com.sumit.varnamala.data.ocr.OCRCacheRepository
import com.sumit.varnamala.data.ocr.CachedOCRResult
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextBlock
import com.sumit.varnamala.data.ocr.TextLine
import com.sumit.varnamala.data.ocr.TextElement
import com.sumit.varnamala.utils.LanguageDetector
import com.sumit.varnamala.data.ocr.CachedLanguageGroup
import com.sumit.varnamala.data.ocr.CachedTextElement
import com.sumit.varnamala.ui.components.OCRInfoSection

import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import android.util.Log
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.graphics.ImageDecoder
import android.graphics.BitmapFactory
import android.os.Build
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.platform.LocalDensity
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi


data class FilterItem(
    val title: String,
    val icon: Int
)

@Composable
fun ImageDetailScreen(navController: NavController, imageId: Long) {
    val context = LocalContext.current
    var meta by remember { mutableStateOf<ImageMeta?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val cacheRepository = remember { OCRCacheRepository(context) }
    var ocrLoading by remember { mutableStateOf(false) }
    var ocrError by remember { mutableStateOf<String?>(null) }
    var rawOcrText by remember { mutableStateOf("") }
    var detectedLanguage by remember { mutableStateOf("unknown") }
    var languageConfidence by remember { mutableStateOf(0f) }
    var lastResult by remember { mutableStateOf<OCRResult?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val filterItems = remember {
        listOf(
            FilterItem("None", R.drawable.ic_none),
            FilterItem("English", R.drawable.ic_lang_english),
            FilterItem("Hindi", R.drawable.ic_lang_hindi),
            FilterItem("Gujrati", R.drawable.ic_lang_gujrati),
            FilterItem("Punjabi", R.drawable.ic_lang_punjabi),
            FilterItem("Bengali", R.drawable.ic_lang_bengali),
            FilterItem("Tamil", R.drawable.ic_lang_tamil),
            FilterItem("Telugu", R.drawable.ic_lang_telugu),
        )
    }
    val listState = rememberLazyListState()
    var centeredIndex by remember { mutableIntStateOf(0) }

    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(imageId) {
        meta = loadImageMeta(context.contentResolver, imageId)
        runOCRWithCache(
            repository = cacheRepository,
            imageId = imageId,
            context = context,
            setLoading = { ocrLoading = it },
            setError = { ocrError = it },
            setText = { rawOcrText = it },
            setLang = { newLang ->
                detectedLanguage = newLang
                languageConfidence =
                    LanguageDetector.getDetectionConfidenceMap(rawOcrText)[newLang] ?: 0f
            },
            setResult = { lastResult = it }
        )
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect {
                val layoutInfo = listState.layoutInfo
                val items = layoutInfo.visibleItemsInfo
                if (items.isEmpty()) return@collect
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                var best = centeredIndex
                var minDist = Int.MAX_VALUE
                items.forEach { info ->
                    val c = info.offset + info.size / 2
                    val d = kotlin.math.abs(c - viewportCenter)
                    if (d < minDist) {
                        minDist = d; best = info.index
                    }
                }
                if (best != centeredIndex) {
                    centeredIndex = best
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    Log.d(
                        "ImageDetailSlider",
                        "Selected item: ${filterItems.getOrNull(best)} (index=$best)"
                    )
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding())
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassButton(
                    icon = painterResource(R.drawable.ic_chevron_left),
                    size = 52.dp,
                    onClick = { navController.popBackStack() })
                GlassButton(
                    icon = painterResource(R.drawable.ic_settings_outline),
                    size = 52.dp,
                    onClick = { showInfoDialog = true })
            }

            meta?.let { m ->
                val targetUri =
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        imageId
                    )
                val aspectRatio =
                    if (m.width != null && m.height != null && m.height != 0) m.width.toFloat() / m.height.toFloat() else 1f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(targetUri).crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(aspectRatio)
                            .border(
                                color = MaterialTheme.colorScheme.onSurface,
                                width = 2.dp,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Fit,
                    )

                    this@Column.AnimatedVisibility(
                        visible = rawOcrText.isNotBlank() || ocrLoading,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        OCRInfoCard(
                            ocrLoading = ocrLoading,
                            ocrError = ocrError,
                            rawOcrText = rawOcrText,
                            detectedLanguage = detectedLanguage,
                            languageConfidence = languageConfidence,
                            lastResult = lastResult,

                            onCopy = {
                                val clip =
                                    ClipData.newPlainText("OCR Text", rawOcrText)
                                ClipboardManager::class.java
                                    .cast(context.getSystemService(Context.CLIPBOARD_SERVICE))
                                    ?.setPrimaryClip(clip)
                            },
                            onShare = {
                                if (rawOcrText.isNotBlank()) {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, rawOcrText)
                                        type = "text/plain"
                                    }
                                    val chooser = Intent.createChooser(
                                        sendIntent,
                                        "Share OCR Text"
                                    )
                                    context.startActivity(chooser)
                                }
                            },
                            onRefresh = {
                                coroutineScope.launch {
                                    runOCRWithCache(
                                        repository = cacheRepository,
                                        imageId = imageId,
                                        context = context,
                                        forceRefresh = true,
                                        setLoading = { ocrLoading = it },
                                        setError = { ocrError = it },
                                        setText = { rawOcrText = it },
                                        setLang = { newLang ->
                                            detectedLanguage = newLang
                                            languageConfidence =
                                                LanguageDetector.getDetectionConfidenceMap(
                                                    rawOcrText
                                                )[newLang] ?: 0f
                                        },
                                        setResult = { lastResult = it }
                                    )
                                }
                            },
                            onClear = {
                                coroutineScope.launch {
                                    cacheRepository.clearCache(imageId)
                                    rawOcrText = ""
                                    detectedLanguage = "unknown"
                                    languageConfidence = 0f
                                    lastResult = null
                                }
                            }
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val maxW = constraints.maxWidth.toFloat()
                    val unselectedMinScale = 0.55f
                    val selectedScale = 1.25f
                    val baseItemSizeDp = 64.dp
                    val ringSizeDp =
                        baseItemSizeDp * selectedScale
                    val density = LocalDensity.current
                    val baseItemSizePx = with(density) { baseItemSizeDp.toPx() }
                    val sidePadPx = ((maxW - baseItemSizePx) / 2f).coerceAtLeast(0f)
                    val sidePadDp = with(density) { sidePadPx.toDp() }

                    // Center ring
                    Box(
                        modifier = Modifier
                            .size(ringSizeDp + 4.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                shape = CircleShape
                            )
                    )

                    val selectedItem = filterItems.getOrNull(centeredIndex)
                    if (selectedItem != null) {
                        this@Column.AnimatedVisibility(visible = true) {
                            Text(
                                text = selectedItem.title.uppercase(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(y = -((ringSizeDp + 4.dp) / 2 + 18.dp))
                            )
                        }
                    }

                    LazyRow(
                        state = listState,
                        flingBehavior = snapBehavior,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(horizontal = sidePadDp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        itemsIndexed(filterItems) { index, item ->
                            val layoutInfo = listState.layoutInfo
                            val viewportCenter =
                                (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                            val info = layoutInfo.visibleItemsInfo.find { it.index == index }
                            val scaleTarget = if (info != null) {
                                val itemCenter = info.offset + info.size / 2
                                val dist = kotlin.math.abs(itemCenter - viewportCenter).toFloat()
                                val halfWidth = maxW / 2f
                                val norm = (dist / halfWidth).coerceIn(0f, 1f)
                                val eased = 1f - FastOutSlowInEasing.transform(norm)
                                var s =
                                    (unselectedMinScale + (selectedScale - unselectedMinScale) * eased)
                                if (dist < 0.75f) s = selectedScale
                                s
                            } else {
                                if (index == centeredIndex) selectedScale else unselectedMinScale
                            }
                            val animatedScale by animateFloatAsState(
                                targetValue = scaleTarget,
                                animationSpec = tween(180, easing = FastOutSlowInEasing),
                                label = "carousel_scale"
                            )
                            val isSelected = index == centeredIndex
                            GlassButton(
                                icon = painterResource(item.icon),
                                size = baseItemSizeDp,
                                background = if (isSelected) MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.85f
                                ) else MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                                textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.65f
                                ),
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(
                                            index
                                        )
                                    }
                                },
                                modifier = Modifier.graphicsLayer {
                                    val s = animatedScale
                                    scaleX = s
                                    scaleY = s
                                }
                            )
                        }
                    }
                }

            } ?: run {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp), contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Loading...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Info dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
                },
                title = { Text("File Info") },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = {
                                val clip =
                                    ClipData.newPlainText("OCR Text", rawOcrText)
                                ClipboardManager::class.java
                                    .cast(context.getSystemService(Context.CLIPBOARD_SERVICE))
                                    ?.setPrimaryClip(clip)
                            }, enabled = rawOcrText.isNotBlank()) { Text("Copy") }
                            OutlinedButton(onClick = {
                                if (rawOcrText.isNotBlank()) {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, rawOcrText)
                                        type = "text/plain"
                                    }
                                    val chooser = Intent.createChooser(
                                        sendIntent,
                                        "Share OCR Text"
                                    )
                                    context.startActivity(chooser)
                                }
                            }, enabled = rawOcrText.isNotBlank()) { Text("Share") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Details", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        meta?.let { m ->
                            InfoRow("ID", imageId.toString())
                            InfoRow("Display Name", m.displayName ?: "-")
                            InfoRow("Mime", m.mimeType ?: "-")
                            InfoRow("Width", m.width?.toString() ?: "-")
                            InfoRow("Height", m.height?.toString() ?: "-")
                            InfoRow("Size (KB)", m.size?.let { (it / 1024).toString() } ?: "-")
                            InfoRow("Date Added (s)", m.dateAdded?.toString() ?: "-")
                            InfoRow("Orientation", m.orientation?.toString() ?: "-")
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("OCR", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OCRInfoSection(
                            ocrLoading = ocrLoading,
                            ocrError = ocrError,
                            rawOcrText = rawOcrText,
                            detectedLanguage = detectedLanguage,
                            languageConfidence = languageConfidence,
                            lastResult = lastResult,
                            onCopy = {
                                val clip =
                                    ClipData.newPlainText("OCR Text", rawOcrText)
                                android.content.ClipboardManager::class.java
                                    .cast(context.getSystemService(Context.CLIPBOARD_SERVICE))
                                    ?.setPrimaryClip(clip)
                            },
                            onShare = {
                                if (rawOcrText.isNotBlank()) {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, rawOcrText)
                                        type = "text/plain"
                                    }
                                    val chooser = Intent.createChooser(
                                        sendIntent,
                                        "Share OCR Text"
                                    )
                                    context.startActivity(chooser)
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                coroutineScope.launch {
                                    runOCRWithCache(
                                        repository = cacheRepository,
                                        imageId = imageId,
                                        context = context,
                                        forceRefresh = true,
                                        setLoading = { ocrLoading = it },
                                        setError = { ocrError = it },
                                        setText = { rawOcrText = it },
                                        setLang = { newLang ->
                                            detectedLanguage = newLang
                                            languageConfidence =
                                                LanguageDetector.getDetectionConfidenceMap(
                                                    rawOcrText
                                                )[newLang] ?: 0f
                                        },
                                        setResult = { lastResult = it }
                                    )
                                }
                            }, enabled = !ocrLoading) { Text("Detect Again") }
                            OutlinedButton(onClick = {
                                coroutineScope.launch {
                                    cacheRepository.clearCache(imageId)
                                    rawOcrText = ""
                                    detectedLanguage = "unknown"
                                    languageConfidence = 0f
                                    lastResult = null
                                }
                            }, enabled = !ocrLoading) { Text("Clear Cache") }
                            OutlinedButton(
                                onClick = { showClearAllDialog = true },
                                enabled = !ocrLoading
                            ) { Text("Clear All") }
                        }
                    }
                }
            )
        }

        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                confirmButton = {
                    TextButton(onClick = {
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
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showClearAllDialog = false
                    }) { Text("Cancel") }
                },
                title = { Text("Clear All OCR Cache") },
                text = { Text("This will remove cached OCR for all images. Continue?") }
            )
        }
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

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(
            value,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
    }
}

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
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    setText: (String) -> Unit,
    setLang: (String) -> Unit,
    setResult: (OCRResult) -> Unit
) {
    if (!forceRefresh) {
        repository.getCachedResult(imageId)?.let { cached ->
            setText(cached.text)
            setLang(cached.detectedLanguage)
            setResult(
                OCRResult(
                    text = cached.text,
                    confidence = cached.confidence,
                    language = cached.detectedLanguage,
                    processingTimeMs = cached.processingTimeMs
                )
            )
            return
        }
    }
    setLoading(true)
    setError(null)
    try {
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId)
        val result = runSingleImageOcr(context, uri)
        setText(result.text)
        val lang = LanguageDetector.detectLanguage(result.text)
        setLang(lang)
        val enriched = result.copy(language = lang)
        setResult(enriched)
        val groupsMap = mutableMapOf<String, MutableList<TextElement>>()
        result.boundingBoxes.forEach { block ->
            block.lines.forEach { line ->
                val lineLang = LanguageDetector.detectLanguage(line.text)
                val key = lineLang
                line.elements.forEach { el ->
                    groupsMap.getOrPut(key) { mutableListOf() }.add(el)
                }
            }
        }
        val cachedGroups = groupsMap.map { (languageCode, elements) ->
            val combinedText = elements.joinToString(" ") { it.text }
            val conf = LanguageDetector.getDetectionConfidenceMap(combinedText)[languageCode] ?: 0f
            CachedLanguageGroup(
                language = languageCode,
                confidence = conf,
                elements = elements.map { e ->
                    CachedTextElement(
                        text = e.text,
                        confidence = e.confidence,
                        boundingBox = e.boundingBox
                    )
                }
            )
        }
        repository.cacheResult(
            CachedOCRResult(
                imageId = imageId,
                text = enriched.text,
                confidence = enriched.confidence,
                detectedLanguage = enriched.language ?: lang,
                languageGroups = cachedGroups,
                processingTimeMs = enriched.processingTimeMs,
                timestamp = System.currentTimeMillis()
            )
        )
    } catch (e: Exception) {
        setError(e.message ?: "OCR failed")
    } finally {
        setLoading(false)
    }
}

private suspend fun runSingleImageOcr(context: Context, uri: Uri): OCRResult =
    withContext(Dispatchers.IO) {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src)
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: throw IllegalArgumentException("Cannot decode image")
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val devRecognizer =
            TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        val start = System.currentTimeMillis()
        val latin = latinRecognizer.process(image).await()
        val dev = devRecognizer.process(image).await()
        val duration = System.currentTimeMillis() - start

        val allBlocks = mutableListOf<TextBlock>()
        fun collect(from: com.google.mlkit.vision.text.Text) {
            from.textBlocks.forEach { b ->
                val lines = b.lines.map { l ->
                    TextLine(
                        text = l.text,
                        confidence = 0.9f,
                        boundingBox = l.boundingBox ?: android.graphics.Rect(),
                        elements = l.elements.map { e ->
                            TextElement(
                                text = e.text,
                                confidence = 0.9f,
                                boundingBox = e.boundingBox ?: android.graphics.Rect()
                            )
                        }
                    )
                }
                allBlocks.add(
                    TextBlock(
                        text = b.text,
                        confidence = 0.9f,
                        boundingBox = b.boundingBox ?: android.graphics.Rect(),
                        lines = lines
                    )
                )
            }
        }
        collect(latin)
        collect(dev)

        val combined = allBlocks.joinToString("\n") { it.text }.trim()

        OCRResult(
            text = combined,
            confidence = 0.9f,
            language = null,
            boundingBoxes = allBlocks,
            processingTimeMs = duration,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            imageRotation = 0
        )
    }

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { r -> if (cont.isActive) cont.resume(r) {} }
        addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    }
