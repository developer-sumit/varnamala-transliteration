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

        // TTS preferences
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_AUTO_SPEAK = booleanPreferencesKey("tts_auto_speak")
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

    val ttsEnabled: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TTS_ENABLED] ?: false
        }

    val ttsAutoSpeak: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TTS_AUTO_SPEAK] ?: false
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

    suspend fun setTtsEnabled(value: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_ENABLED] = value
        }
    }
}
