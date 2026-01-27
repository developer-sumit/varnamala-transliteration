package com.sumit.varnamala.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
    val source: String = "camera" // "camera", "gallery", "text_input"
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
            source = json.optString("source", "camera")
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
        private const val MAX_HISTORY_SIZE = 100
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

            // Remove duplicate if exists (same original text and languages)
            currentEntries.removeAll {
                it.originalText == entry.originalText &&
                        it.sourceLanguage == entry.sourceLanguage &&
                        it.targetLanguage == entry.targetLanguage
            }

            // Add new entry at the beginning
            currentEntries.add(0, entry)

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
}
