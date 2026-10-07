package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.HistoryEntry
import com.sumit.varnamala.data.TransliterationHistoryRepository
import com.sumit.varnamala.ui.components.AppModalBottomSheet
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.HighlightedText
import com.sumit.varnamala.ui.components.SpeakButton
import com.sumit.varnamala.ui.components.SpeechController
import com.sumit.varnamala.ui.components.rememberSpeechController
import com.sumit.varnamala.ui.components.speechLanguage
import com.sumit.varnamala.tts.SpeechPart
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil.compose.AsyncImage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(navController: NavController) {
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val historyRepository = remember { TransliterationHistoryRepository(context) }
    val historyEntries by historyRepository.historyEntries.collectAsState(initial = emptyList())
    val favoriteEntries by historyRepository.favoriteEntries.collectAsState(initial = emptyList())

    val collections by historyRepository.collections.collectAsState(initial = emptyList())
    val speech = rememberSpeechController()

    // Filter: null = everything, FAVORITES = hearts, otherwise a collection name.
    var filter by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var collectionTarget by remember { mutableStateOf<HistoryEntry?>(null) }
    var selectedEntry by remember { mutableStateOf<HistoryEntry?>(null) }
    val showFavoritesOnly = filter == FAVORITES

    val displayedEntries = remember(historyEntries, filter, query) {
        val needle = query.trim().lowercase()
        historyEntries.filter { entry ->
            val inFilter = when (filter) {
                null -> true
                FAVORITES -> entry.isFavorite
                else -> entry.collection == filter
            }
            inFilter && (needle.isEmpty() || listOf(entry.originalText, entry.transliteratedText, entry.collection, entry.sourceLanguage, entry.targetLanguage)
                .any { it.lowercase().contains(needle) })
        }
    }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(historyRepository.exportJson().toByteArray()) } }.isSuccess
            Toast.makeText(context, context.getString(if (ok) R.string.backup_saved else R.string.backup_failed), Toast.LENGTH_SHORT).show()
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(historyRepository.exportCsv().toByteArray()) } }.isSuccess
            Toast.makeText(context, context.getString(if (ok) R.string.backup_saved else R.string.backup_failed), Toast.LENGTH_SHORT).show()
        }
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            val added = if (text == null) -1 else historyRepository.importJson(text)
            Toast.makeText(
                context,
                if (added < 0) context.getString(R.string.backup_invalid) else context.resources.getQuantityString(R.plurals.backup_imported, added, added),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, topInset + 10.dp, 16.dp, 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(
                description = stringResource(R.string.back),
                icon = painterResource(R.drawable.ic_chevron_left),
                onClick = { navController.popBackStack() }
            )
            Text(
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    description = stringResource(R.string.a11y_search),
                    icon = painterResource(R.drawable.ic_search),
                    onClick = { showSearch = !showSearch; if (!showSearch) query = "" }
                )
                GlassButton(
                    description = stringResource(R.string.a11y_backup),
                    icon = painterResource(R.drawable.ic_share),
                    onClick = { showBackupSheet = true }
                )
                GlassButton(
                    description = stringResource(R.string.a11y_delete),
                    icon = painterResource(R.drawable.ic_delete_outline),
                    onClick = { showClearDialog = true }
                )
            }
        }

        AnimatedVisibility(visible = showSearch) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.history_search_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Recent scans with thumbnails
        val recents = remember(historyEntries) { historyEntries.sortedByDescending { it.timestamp }.take(6) }
        if (recents.isNotEmpty() && query.isBlank() && filter == null) {
            Text(
                text = stringResource(R.string.recent_scans),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recents, key = { "recent-${it.id}" }) { entry ->
                    RecentScanTile(entry) { selectedEntry = entry }
                }
            }
        }

        // Filter chips: all, favourites, then one per collection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == null,
                onClick = { filter = null },
                label = { Text(text = "${stringResource(R.string.all)} (${historyEntries.size})") }
            )
            FilterChip(
                selected = showFavoritesOnly,
                onClick = { filter = FAVORITES },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_heart_filled),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${stringResource(R.string.favorites)} (${favoriteEntries.size})")
                    }
                }
            )
            collections.forEach { name ->
                FilterChip(
                    selected = filter == name,
                    onClick = { filter = name },
                    label = { Text(name) }
                )
            }
        }

        // History List
        if (displayedEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (showFavoritesOnly) R.drawable.ic_heart_outline
                            else R.drawable.ic_time
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when {
                            query.isNotBlank() -> stringResource(R.string.history_no_matches)
                            showFavoritesOnly -> stringResource(R.string.no_favorites)
                            else -> stringResource(R.string.no_history)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (showFavoritesOnly)
                            stringResource(R.string.no_favorites_hint)
                        else
                            stringResource(R.string.no_history_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = displayedEntries,
                    key = { it.id }
                ) { entry ->
                    HistoryEntryCard(
                        entry = entry,
                        speech = speech,
                        onFileInCollection = { collectionTarget = entry },
                        isExpanded = selectedEntry?.id == entry.id,
                        onClick = {
                            selectedEntry = if (selectedEntry?.id == entry.id) null else entry
                        },
                        onCopy = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    context.getString(R.string.clipboard_label_text),
                                    entry.transliteratedText
                                )
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onToggleFavorite = {
                            scope.launch {
                                historyRepository.toggleFavorite(entry.id)
                            }
                        },
                        onDelete = {
                            scope.launch {
                                historyRepository.removeEntry(entry.id)
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AppModalBottomSheet(onDismiss = { showClearDialog = false }) {
            Text(
                text = stringResource(R.string.clear_history),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.clear_history_confirmation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            historyRepository.clearHistory(keepFavorites = true)
                        }
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.clear_keep_favorites),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            historyRepository.clearHistory(keepFavorites = false)
                        }
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.clear_all),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showBackupSheet) {
        AppModalBottomSheet(onDismiss = { showBackupSheet = false }) {
            Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.backup_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { showBackupSheet = false; exportJson.launch("varnamala-history.json") }) {
                Text(stringResource(R.string.backup_export_json))
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { showBackupSheet = false; exportCsv.launch("varnamala-history.csv") }) {
                Text(stringResource(R.string.backup_export_csv))
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                showBackupSheet = false
                importBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }) {
                Text(stringResource(R.string.backup_import))
            }
        }
    }

    collectionTarget?.let { entry ->
        var newName by remember(entry.id) { mutableStateOf("") }
        AppModalBottomSheet(onDismiss = { collectionTarget = null }) {
            Text(stringResource(R.string.collection), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                collections.forEach { name ->
                    FilterChip(
                        selected = entry.collection == name,
                        onClick = {
                            scope.launch { historyRepository.setCollection(entry.id, if (entry.collection == name) "" else name) }
                            collectionTarget = null
                        },
                        label = { Text(name) }
                    )
                }
            }
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it.take(30) },
                singleLine = true,
                label = { Text(stringResource(R.string.collection_new)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (entry.collection.isNotEmpty()) {
                    TextButton(onClick = {
                        scope.launch { historyRepository.setCollection(entry.id, "") }
                        collectionTarget = null
                    }) { Text(stringResource(R.string.collection_remove)) }
                }
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        scope.launch { historyRepository.setCollection(entry.id, newName) }
                        filter = newName.trim()
                        collectionTarget = null
                    }
                ) { Text(stringResource(R.string.collection_create)) }
            }
        }
    }
}

private const val FAVORITES = "\u0000favorites"

/** Small picture-and-text tile in the "Recent scans" strip. */
@Composable
private fun RecentScanTile(entry: HistoryEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = entry.transliteratedText }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            if (entry.imageRef.isNotEmpty()) {
                AsyncImage(
                    model = entry.imageRef,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(text = "⌨️", fontSize = 22.sp)
            }
        }
        Text(
            text = entry.transliteratedText,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(6.dp)
        )
    }
}

@Composable
private fun HistoryEntryCard(
    entry: HistoryEntry,
    speech: SpeechController,
    onFileInCollection: () -> Unit,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteScale by animateFloatAsState(
        targetValue = if (entry.isFavorite) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "favoriteScale"
    )
    val favoriteColor by animateColorAsState(
        targetValue = if (entry.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.4f
        ),
        label = "favoriteColor"
    )

    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "cardScale"
    )

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onCopy,
                onClickLabel = null,
                onLongClickLabel = null,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isExpanded) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (entry.imageRef.isNotEmpty()) {
                    AsyncImage(
                        model = entry.imageRef,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    val originalKey = "o-${entry.id}"
                    val convertedKey = "c-${entry.id}"
                    val originalPart = SpeechPart(entry.originalText, speechLanguage(entry.sourceLanguage))
                    // Original text preview, with its own speak button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HighlightedText(
                            text = entry.originalText,
                            controller = speech,
                            key = originalKey,
                            partIndex = 0,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = if (isExpanded) 3 else 1,
                            modifier = Modifier.weight(1f)
                        )
                        SpeakButton(
                            controller = speech,
                            key = originalKey,
                            description = stringResource(R.string.tts_speak_original_text),
                            onClick = { speech.toggle(originalKey, listOf(originalPart)) },
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Converted text, with its own speak button (the original is read first when that setting is on)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HighlightedText(
                            text = entry.transliteratedText,
                            controller = speech,
                            key = convertedKey,
                            partIndex = if (speech.settings.speakOriginal) 1 else 0,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (isExpanded) 5 else 2,
                            modifier = Modifier.weight(1f)
                        )
                        SpeakButton(
                            controller = speech,
                            key = convertedKey,
                            description = stringResource(R.string.tts_speak_converted_text),
                            onClick = {
                                speech.toggleConverted(convertedKey, originalPart, SpeechPart(entry.transliteratedText, entry.targetLanguage))
                            },
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Favorite button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.scale(favoriteScale)
                ) {
                    Icon(
                        painter = painterResource(
                            if (entry.isFavorite) R.drawable.ic_heart_filled
                            else R.drawable.ic_heart_outline
                        ),
                        contentDescription = stringResource(R.string.favorites),
                        tint = favoriteColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Metadata Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source indicator
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (entry.source) {
                                "camera" -> "📷"
                                "gallery" -> "🖼️"
                                "text_input" -> "⌨️"
                                else -> "📝"
                            },
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (entry.collection.isNotBlank()) {
                        Text(
                            text = "📁 ${entry.collection}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Languages
                    Text(
                        text = "${entry.sourceLanguage.uppercase()} → ${entry.targetLanguage.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Timestamp
                Text(
                    text = dateFormat.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // Expanded Actions
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.copy))
                    }

                    OutlinedButton(
                        onClick = onFileInCollection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.collection), maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_outline),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}
