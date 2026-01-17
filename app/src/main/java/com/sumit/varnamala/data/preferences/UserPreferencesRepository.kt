package com.sumit.varnamala.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Repository for user-facing preferences that enhance the user experience.
 * These are separate from OCR-specific settings for better organization.
 */
class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        // Display preferences
        val TEXT_DISPLAY_SIZE = floatPreferencesKey("text_display_size")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val AUTO_COPY_ENABLED = booleanPreferencesKey("auto_copy_enabled")
        val SHOW_PRONUNCIATION_HINTS = booleanPreferencesKey("show_pronunciation_hints")
        
        // TTS preferences
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_AUTO_SPEAK = booleanPreferencesKey("tts_auto_speak")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        
        // Quick actions
        val SHOW_QUICK_ACTIONS = booleanPreferencesKey("show_quick_actions")
        val CONFIRM_BEFORE_CLEAR = booleanPreferencesKey("confirm_before_clear")
        
        // Input mode
        val LAST_USED_INPUT_MODE = booleanPreferencesKey("last_used_camera_mode") // true = camera, false = text input
    }

    // Text display size (0.8 = small, 1.0 = normal, 1.2 = large, 1.5 = extra large)
    val textDisplaySize: Flow<Float> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TEXT_DISPLAY_SIZE] ?: 1.0f
        }

    val hapticFeedbackEnabled: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] ?: true
        }

    val autoCopyEnabled: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_COPY_ENABLED] ?: false
        }

    val showPronunciationHints: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_PRONUNCIATION_HINTS] ?: true
        }

    val ttsEnabled: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TTS_ENABLED] ?: false
        }

    val ttsAutoSpeak: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TTS_AUTO_SPEAK] ?: false
        }

    val ttsSpeed: Flow<Float> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TTS_SPEED] ?: 1.0f
        }

    val showQuickActions: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_QUICK_ACTIONS] ?: true
        }

    val confirmBeforeClear: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CONFIRM_BEFORE_CLEAR] ?: true
        }

    val lastUsedCameraMode: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_USED_INPUT_MODE] ?: true
        }

    suspend fun setTextDisplaySize(value: Float) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_DISPLAY_SIZE] = value.coerceIn(0.7f, 1.8f)
        }
    }

    suspend fun setHapticFeedbackEnabled(value: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] = value
        }
    }

    suspend fun setAutoCopyEnabled(value: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_COPY_ENABLED] = value
        }
    }

    suspend fun setShowPronunciationHints(value: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_PRONUNCIATION_HINTS] = value
        }
    }

    suspend fun setTtsEnabled(value: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_ENABLED] = value
        }
    }

    suspend fun setTtsAutoSpeak(value: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_AUTO_SPEAK] = value
        }
    }

    suspend fun setTtsSpeed(value: Float) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_SPEED] = value.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun setShowQuickActions(value: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_QUICK_ACTIONS] = value
        }
    }

    suspend fun setConfirmBeforeClear(value: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.CONFIRM_BEFORE_CLEAR] = value
        }
    }

    suspend fun setLastUsedCameraMode(value: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_USED_INPUT_MODE] = value
        }
    }
}
