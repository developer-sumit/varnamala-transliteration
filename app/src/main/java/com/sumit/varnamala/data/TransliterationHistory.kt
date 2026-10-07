package com.sumit.varnamala.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "transliteration_history")

/**
 * Data class representing a single transliteration history entry
 */
data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val originalText: String,
    val transliteratedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val source: String = "camera", // "camera", "gallery", "text_input"
    /** Trip or topic this scan is filed under; empty when it is not in a collection. */
    val collection: String = "",
    /** Thumbnail shown in lists: a file path or content URI that Coil can load; empty for typed text. */
    val imageRef: String = "",
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("originalText", originalText)
        put("transliteratedText", transliteratedText)
        put("sourceLanguage", sourceLanguage)
        put("targetLanguage", targetLanguage)
        put("timestamp", timestamp)
        put("isFavorite", isFavorite)
        put("source", source)
        put("collection", collection)
        put("imageRef", imageRef)
    }

    companion object {
        fun fromJson(json: JSONObject): HistoryEntry = HistoryEntry(
            id = json.optString("id", UUID.randomUUID().toString()),
            originalText = json.optString("originalText", ""),
            transliteratedText = json.optString("transliteratedText", ""),
            sourceLanguage = json.optString("sourceLanguage", ""),
            targetLanguage = json.optString("targetLanguage", ""),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            isFavorite = json.optBoolean("isFavorite", false),
            source = json.optString("source", "camera"),
            collection = json.optString("collection", ""),
            imageRef = json.optString("imageRef", "")
        )
    }
}

/**
 * Repository for managing transliteration history and favorites
 */
class TransliterationHistoryRepository(private val context: Context) {
    private object PreferencesKeys {
        val HISTORY_JSON = stringPreferencesKey("history_json")
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 500
    }

    val historyEntries: Flow<List<HistoryEntry>> = context.historyDataStore.data
        .map { preferences ->
            val jsonString = preferences[PreferencesKeys.HISTORY_JSON] ?: "[]"
            parseHistoryJson(jsonString)
        }

    val favoriteEntries: Flow<List<HistoryEntry>> = historyEntries
        .map { entries -> entries.filter { it.isFavorite } }

    val recentEntries: Flow<List<HistoryEntry>> = historyEntries
        .map { entries -> entries.sortedByDescending { it.timestamp }.take(10) }

    private fun parseHistoryJson(jsonString: String): List<HistoryEntry> {
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { index ->
                HistoryEntry.fromJson(jsonArray.getJSONObject(index))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun historyToJson(entries: List<HistoryEntry>): String {
        val jsonArray = JSONArray()
        entries.forEach { entry ->
            jsonArray.put(entry.toJson())
        }
        return jsonArray.toString()
    }

    suspend fun addEntry(entry: HistoryEntry) {
        context.historyDataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.HISTORY_JSON] ?: "[]"
            val currentEntries = parseHistoryJson(currentJson).toMutableList()

            // Replace an earlier copy of the same scan, but keep what the user did to it:
            // its favourite mark, collection and picture.
            val earlier = currentEntries.firstOrNull {
                it.originalText == entry.originalText &&
                        it.sourceLanguage == entry.sourceLanguage &&
                        it.targetLanguage == entry.targetLanguage
            }
            currentEntries.removeAll {
                it.originalText == entry.originalText &&
                        it.sourceLanguage == entry.sourceLanguage &&
                        it.targetLanguage == entry.targetLanguage
            }

            // Add new entry at the beginning
            currentEntries.add(
                0,
                if (earlier == null) entry else entry.copy(
                    isFavorite = earlier.isFavorite || entry.isFavorite,
                    collection = entry.collection.ifEmpty { earlier.collection },
                    imageRef = entry.imageRef.ifEmpty { earlier.imageRef },
                ),
            )

            // Trim to max size (keep favorites even if over limit)
            val favorites = currentEntries.filter { it.isFavorite }
            val nonFavorites = currentEntries.filter { !it.isFavorite }
            val trimmedNonFavorites = nonFavorites.take(MAX_HISTORY_SIZE - favorites.size)
            val finalList = (favorites + trimmedNonFavorites).sortedByDescending { it.timestamp }

            preferences[PreferencesKeys.HISTORY_JSON] = historyToJson(finalList)
        }
    }

    suspend fun toggleFavorite(entryId: String) {
        context.historyDataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.HISTORY_JSON] ?: "[]"
            val currentEntries = parseHistoryJson(currentJson).toMutableList()

            val index = currentEntries.indexOfFirst { it.id == entryId }
            if (index != -1) {
                val entry = currentEntries[index]
                currentEntries[index] = entry.copy(isFavorite = !entry.isFavorite)
                preferences[PreferencesKeys.HISTORY_JSON] = historyToJson(currentEntries)
            }
        }
    }

    suspend fun removeEntry(entryId: String) {
        context.historyDataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.HISTORY_JSON] ?: "[]"
            val currentEntries = parseHistoryJson(currentJson).toMutableList()

            currentEntries.filter { it.id == entryId }.forEach { ScanThumbnails.delete(it.imageRef) }
            currentEntries.removeAll { it.id == entryId }
            preferences[PreferencesKeys.HISTORY_JSON] = historyToJson(currentEntries)
        }
    }

    suspend fun clearHistory(keepFavorites: Boolean = true) {
        context.historyDataStore.edit { preferences ->
            if (keepFavorites) {
                val currentJson = preferences[PreferencesKeys.HISTORY_JSON] ?: "[]"
                val favorites = parseHistoryJson(currentJson).filter { it.isFavorite }
                preferences[PreferencesKeys.HISTORY_JSON] = historyToJson(favorites)
            } else {
                preferences[PreferencesKeys.HISTORY_JSON] = "[]"
            }
        }
    }

    /** Every collection name in use, alphabetically. */
    val collections: Flow<List<String>> = historyEntries.map { entries ->
        entries.map { it.collection }.filter { it.isNotBlank() }.distinct().sorted()
    }

    suspend fun setCollection(entryId: String, name: String) {
        updateEntry(entryId) { it.copy(collection = name.trim()) }
    }

    private suspend fun updateEntry(entryId: String, change: (HistoryEntry) -> HistoryEntry) {
        context.historyDataStore.edit { preferences ->
            val entries = parseHistoryJson(preferences[PreferencesKeys.HISTORY_JSON] ?: "[]").toMutableList()
            val index = entries.indexOfFirst { it.id == entryId }
            if (index != -1) {
                entries[index] = change(entries[index])
                preferences[PreferencesKeys.HISTORY_JSON] = historyToJson(entries)
            }
        }
    }

    /** Deletes scans older than [days] days, keeping favourites. Returns how many were removed. */
    suspend fun pruneOlderThan(days: Int): Int {
        if (days <= 0) return 0
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        var removed = 0
        context.historyDataStore.edit { preferences ->
            val entries = parseHistoryJson(preferences[PreferencesKeys.HISTORY_JSON] ?: "[]")
            val kept = entries.filter { it.isFavorite || it.timestamp >= cutoff }
            removed = entries.size - kept.size
            if (removed > 0) {
                entries.filter { it !in kept }.forEach { ScanThumbnails.delete(it.imageRef) }
                preferences[PreferencesKeys.HISTORY_JSON] = historyToJson(kept)
            }
        }
        return removed
    }

    /** All scans as JSON, in the same shape [importJson] reads back. */
    suspend fun exportJson(): String {
        val entries = historyEntries.first()
        return JSONObject().apply {
            put("app", "Varnamala")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("entries", JSONArray().also { array -> entries.forEach { array.put(it.toJson()) } })
        }.toString(2)
    }

    /** All scans as CSV for spreadsheets. */
    suspend fun exportCsv(): String {
        fun cell(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
        return buildString {
            appendLine("timestamp,source,source_language,target_language,original,converted,favorite,collection")
            historyEntries.first().sortedBy { it.timestamp }.forEach {
                appendLine(
                    listOf(
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date(it.timestamp)), it.source, it.sourceLanguage,
                        it.targetLanguage, it.originalText, it.transliteratedText, it.isFavorite.toString(), it.collection,
                    ).joinToString(",") { value -> cell(value) }
                )
            }
        }
    }

    /**
     * Adds the scans in [json] (a file made by [exportJson]) to the history, skipping ones that
     * are already there. Returns how many were added, or -1 when the file is not a Varnamala backup.
     */
    suspend fun importJson(json: String): Int {
        val incoming = try {
            val array = JSONObject(json).getJSONArray("entries")
            (0 until array.length()).map { HistoryEntry.fromJson(array.getJSONObject(it)) }
        } catch (e: Exception) {
            return -1
        }
        var added = 0
        context.historyDataStore.edit { preferences ->
            val current = parseHistoryJson(preferences[PreferencesKeys.HISTORY_JSON] ?: "[]").toMutableList()
            val known = current.map { Triple(it.originalText, it.sourceLanguage, it.targetLanguage) }.toMutableSet()
            incoming.forEach { entry ->
                if (known.add(Triple(entry.originalText, entry.sourceLanguage, entry.targetLanguage))) {
                    // Thumbnails are not part of the backup, so a stale file path would only break the list.
                    current += entry.copy(imageRef = if (entry.imageRef.startsWith("content:")) entry.imageRef else "")
                    added++
                }
            }
            preferences[PreferencesKeys.HISTORY_JSON] = historyToJson(current.sortedByDescending { it.timestamp }.take(MAX_HISTORY_SIZE))
        }
        return added
    }
}
