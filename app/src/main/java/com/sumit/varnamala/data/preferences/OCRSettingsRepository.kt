package com.sumit.varnamala.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ocrSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ocr_settings")

class OCRSettingsRepository(private val context: Context) {
    private object PreferencesKeys {
        val SHOW_BOUNDING_BOXES = booleanPreferencesKey("show_bounding_boxes")
        val SHOW_TEXT_OVERLAY = booleanPreferencesKey("show_text_overlay")
        val SHOW_WORD_BOUNDING_BOXES = booleanPreferencesKey("show_word_bounding_boxes")
        val SHOW_LINE_BOUNDING_BOXES = booleanPreferencesKey("show_line_bounding_boxes")
        val SHOW_BLOCK_BOUNDING_BOXES = booleanPreferencesKey("show_block_bounding_boxes")
        val TEXT_BACKGROUND_OPACITY = floatPreferencesKey("text_background_opacity")
        val BOUNDING_BOX_STROKE_WIDTH = floatPreferencesKey("bounding_box_stroke_width")
        val ENABLE_CACHE_STORAGE = booleanPreferencesKey("enable_cache_storage")

        val CAMERA_DETECTION_INTERVAL_MS = longPreferencesKey("camera_detection_interval_ms")
        val CAMERA_READING_MS_PER_WORD = intPreferencesKey("camera_reading_ms_per_word")
        val CAMERA_MAX_EXTRA_DELAY_MS = intPreferencesKey("camera_max_extra_delay_ms")
        val CAMERA_SKIP_FRAMES = intPreferencesKey("camera_skip_frames")
    }

    val cameraDetectionIntervalMs: Flow<Long> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CAMERA_DETECTION_INTERVAL_MS] ?: 1000L
        }

    val cameraReadingMsPerWord: Flow<Int> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CAMERA_READING_MS_PER_WORD] ?: 300
        }

    val cameraMaxExtraDelayMs: Flow<Int> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CAMERA_MAX_EXTRA_DELAY_MS] ?: 5000
        }

    val cameraSkipFrames: Flow<Int> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CAMERA_SKIP_FRAMES] ?: 5
        }

    val showBoundingBoxes: Flow<Boolean> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_BOUNDING_BOXES] ?: true
        }

    val showTextOverlay: Flow<Boolean> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_TEXT_OVERLAY] ?: true
        }

    val showWordBoundingBoxes: Flow<Boolean> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_WORD_BOUNDING_BOXES] ?: true
        }

    val showLineBoundingBoxes: Flow<Boolean> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_LINE_BOUNDING_BOXES] ?: true
        }

    val showBlockBoundingBoxes: Flow<Boolean> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_BLOCK_BOUNDING_BOXES] ?: true
        }

    val textBackgroundOpacity: Flow<Float> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TEXT_BACKGROUND_OPACITY] ?: 0.8f
        }

    val boundingBoxStrokeWidth: Flow<Float> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BOUNDING_BOX_STROKE_WIDTH] ?: 2f
        }

    val enableCacheStorage: Flow<Boolean> = context.ocrSettingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ENABLE_CACHE_STORAGE] ?: true
        }

    suspend fun setShowBoundingBoxes(value: Boolean) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_BOUNDING_BOXES] = value
        }
    }

    suspend fun setShowTextOverlay(value: Boolean) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_TEXT_OVERLAY] = value
        }
    }

    suspend fun setShowWordBoundingBoxes(value: Boolean) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_WORD_BOUNDING_BOXES] = value
        }
    }

    suspend fun setShowLineBoundingBoxes(value: Boolean) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_LINE_BOUNDING_BOXES] = value
        }
    }

    suspend fun setShowBlockBoundingBoxes(value: Boolean) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_BLOCK_BOUNDING_BOXES] = value
        }
    }

    suspend fun setTextBackgroundOpacity(value: Float) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_BACKGROUND_OPACITY] = value
        }
    }

    suspend fun setBoundingBoxStrokeWidth(value: Float) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.BOUNDING_BOX_STROKE_WIDTH] = value
        }
    }

    suspend fun setEnableCacheStorage(value: Boolean) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_CACHE_STORAGE] = value
        }
    }

    suspend fun setCameraDetectionIntervalMs(value: Long) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.CAMERA_DETECTION_INTERVAL_MS] = value
        }
    }

    suspend fun setCameraReadingMsPerWord(value: Int) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.CAMERA_READING_MS_PER_WORD] = value
        }
    }

    suspend fun setCameraMaxExtraDelayMs(value: Int) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.CAMERA_MAX_EXTRA_DELAY_MS] = value
        }
    }

    suspend fun setCameraSkipFrames(value: Int) {
        context.ocrSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.CAMERA_SKIP_FRAMES] = value
        }
    }
}