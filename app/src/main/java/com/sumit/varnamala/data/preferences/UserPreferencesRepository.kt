package com.sumit.varnamala.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
        val TTS_VOICE_ID = intPreferencesKey("tts_voice_id")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val TTS_STYLE = intPreferencesKey("tts_style")
        val TTS_SPEAK_ORIGINAL = booleanPreferencesKey("tts_speak_original")
        val READING_MODE = booleanPreferencesKey("reading_mode")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast_overlay")
        val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
        val APP_LOCK = booleanPreferencesKey("app_lock_enabled")
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
            preferences[PreferencesKeys.TTS_ENABLED] ?: true
        }

    val ttsAutoSpeak: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TTS_AUTO_SPEAK] ?: false
        }

    suspend fun setTtsVoiceId(value: Int) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_VOICE_ID] = value
        }
    }

    val readingMode: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[PreferencesKeys.READING_MODE] ?: false }
    val highContrast: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[PreferencesKeys.HIGH_CONTRAST] ?: false }
    /** Scans older than this many days are deleted when the app opens; 0 keeps everything. */
    val historyRetentionDays: Flow<Int> = context.userPreferencesDataStore.data.map { it[PreferencesKeys.HISTORY_RETENTION_DAYS] ?: 0 }
    val appLockEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[PreferencesKeys.APP_LOCK] ?: false }

    /** Everything the speaker needs, as one flow. */
    val speechSettings: Flow<com.sumit.varnamala.tts.SpeechSettings> = context.userPreferencesDataStore.data.map { p ->
        com.sumit.varnamala.tts.SpeechSettings(
            speakerId = p[PreferencesKeys.TTS_VOICE_ID] ?: com.sumit.varnamala.tts.RasaVoices.default.speakerId,
            emotionId = p[PreferencesKeys.TTS_STYLE] ?: 0,
            speed = p[PreferencesKeys.TTS_SPEED] ?: 1f,
            pitch = p[PreferencesKeys.TTS_PITCH] ?: 1f,
            speakOriginal = p[PreferencesKeys.TTS_SPEAK_ORIGINAL] ?: false,
        )
    }

    suspend fun setTtsSpeed(value: Float) = context.userPreferencesDataStore.edit { it[PreferencesKeys.TTS_SPEED] = value.coerceIn(0.5f, 2f) }
    suspend fun setTtsPitch(value: Float) = context.userPreferencesDataStore.edit { it[PreferencesKeys.TTS_PITCH] = value.coerceIn(0.7f, 1.4f) }
    suspend fun setTtsStyleId(value: Int) = context.userPreferencesDataStore.edit { it[PreferencesKeys.TTS_STYLE] = value }
    suspend fun setTtsSpeakOriginal(value: Boolean) = context.userPreferencesDataStore.edit { it[PreferencesKeys.TTS_SPEAK_ORIGINAL] = value }
    suspend fun setTtsAutoSpeak(value: Boolean) = context.userPreferencesDataStore.edit { it[PreferencesKeys.TTS_AUTO_SPEAK] = value }
    suspend fun setReadingMode(value: Boolean) = context.userPreferencesDataStore.edit { it[PreferencesKeys.READING_MODE] = value }
    suspend fun setHighContrast(value: Boolean) = context.userPreferencesDataStore.edit { it[PreferencesKeys.HIGH_CONTRAST] = value }
    suspend fun setHistoryRetentionDays(value: Int) = context.userPreferencesDataStore.edit { it[PreferencesKeys.HISTORY_RETENTION_DAYS] = value }
    suspend fun setAppLockEnabled(value: Boolean) = context.userPreferencesDataStore.edit { it[PreferencesKeys.APP_LOCK] = value }

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
