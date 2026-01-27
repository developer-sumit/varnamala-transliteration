package com.sumit.varnamala.data.ocr

import android.content.Context
import android.graphics.Rect
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.ocrDataStore by preferencesDataStore("ocr_cache")

class OCRCacheRepository(private val context: Context) {
    private val indexKey = stringSetPreferencesKey("index_ids")

    private fun keyFor(imageId: Long) = stringPreferencesKey("ocr_$imageId")

    suspend fun getCachedResult(imageId: Long): CachedOCRResult? = withContext(Dispatchers.IO) {
        try {
            val prefs = context.ocrDataStore.data.first()
            val raw = prefs[keyFor(imageId)] ?: return@withContext null
            val json = JSONObject(raw)

            val languageGroupsJson = json.optJSONArray("languageGroups")
            val languageGroups = mutableListOf<CachedLanguageGroup>()
            if (languageGroupsJson != null) {
                for (i in 0 until languageGroupsJson.length()) {
                    val groupJson = languageGroupsJson.optJSONObject(i) ?: continue
                    val elementsJson = groupJson.optJSONArray("elements")
                    val elements = mutableListOf<CachedTextElement>()
                    if (elementsJson != null) {
                        for (j in 0 until elementsJson.length()) {
                            val elementJson = elementsJson.optJSONObject(j) ?: continue
                            val bbox = elementJson.optJSONObject("boundingBox")
                            val rect = if (bbox != null) {
                                Rect(
                                    bbox.optInt("left", 0),
                                    bbox.optInt("top", 0),
                                    bbox.optInt("right", 0),
                                    bbox.optInt("bottom", 0)
                                )
                            } else Rect()
                            elements.add(
                                CachedTextElement(
                                    text = elementJson.optString("text", ""),
                                    boundingBox = rect
                                )
                            )
                        }
                    }
                    languageGroups.add(
                        CachedLanguageGroup(
                            language = groupJson.optString("language", "unknown"),
                            elements = elements
                        )
                    )
                }
            }

            CachedOCRResult(
                imageId = json.getLong("imageId"),
                text = json.optString("text", ""),
                detectedLanguage = json.optString("detectedLanguage", "unknown"),
                languageGroups = languageGroups,
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun cacheResult(result: CachedOCRResult) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("imageId", result.imageId)
                put("text", result.text)
                put("detectedLanguage", result.detectedLanguage)
                put("timestamp", result.timestamp)

                val groupsArray = JSONArray()
                result.languageGroups.forEach { group ->
                    val groupJson = JSONObject().apply {
                        put("language", group.language)
                        val elementsArray = JSONArray()
                        group.elements.forEach { element ->
                            val elementJson = JSONObject().apply {
                                put("text", element.text)
                                val bbox = JSONObject().apply {
                                    put("left", element.boundingBox.left)
                                    put("top", element.boundingBox.top)
                                    put("right", element.boundingBox.right)
                                    put("bottom", element.boundingBox.bottom)
                                }
                                put("boundingBox", bbox)
                            }
                            elementsArray.put(elementJson)
                        }
                        put("elements", elementsArray)
                    }
                    groupsArray.put(groupJson)
                }
                put("languageGroups", groupsArray)
            }
            context.ocrDataStore.edit { prefs ->
                prefs[keyFor(result.imageId)] = json.toString()
                val existing = prefs[indexKey]?.toMutableSet() ?: mutableSetOf()
                existing.add(result.imageId.toString())
                prefs[indexKey] = existing
            }
        } catch (e: Exception) {
            // swallow
        }
    }

    suspend fun clearCache(imageId: Long? = null) = withContext(Dispatchers.IO) {
        if (imageId != null) {
            context.ocrDataStore.edit { prefs ->
                prefs.remove(keyFor(imageId))
                val existing = prefs[indexKey]?.toMutableSet() ?: mutableSetOf()
                existing.remove(imageId.toString())
                prefs[indexKey] = existing
            }
        } else {
            context.ocrDataStore.edit { prefs ->
                val ids = prefs[indexKey] ?: emptySet()
                ids.forEach { id -> prefs.remove(stringPreferencesKey("ocr_$id")) }
                prefs.remove(indexKey)
            }
        }
    }
}