package com.sumit.varnamala.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private val Context.appLanguageDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_language_settings")

class AppLanguageRepository(private val context: Context) {
    private object PreferencesKeys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val appLanguage: Flow<String> = context.appLanguageDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] ?: "en"
        }

    val hasExplicitAppLanguage: Flow<Boolean> = context.appLanguageDataStore.data
        .map { preferences ->
            preferences.contains(PreferencesKeys.APP_LANGUAGE)
        }

    val hasExplicitAppLanguageLoaded: Flow<Boolean?> = hasExplicitAppLanguage
        .map { it }
        .onStart { emit(false) }

    suspend fun setAppLanguage(value: String) {
        context.appLanguageDataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = value
        }
    }

    companion object {
        private const val ENGLISH = "en"
        private const val HINDI = "hi"
        private const val PUNJABI = "pa"
        private const val BENGALI = "bn"
        private const val TELUGU = "te"
        private const val TAMIL = "ta"
        private const val MARATHI = "mr"
        private const val GUJARATI = "gu"
        private const val KANNADA = "kn"
        private const val MALAYALAM = "ml"
        private const val ODIA = "or"
        private const val ASSAMESE = "as"
        private const val URDU = "ur"
        private const val SANSKRIT = "sa"
        private const val NEPALI = "ne"

        fun getLanguageDisplayName(code: String): String {
            return when (code) {
                ENGLISH -> "English"
                HINDI -> "हिन्दी"
                PUNJABI -> "ਪੰਜਾਬੀ"
                BENGALI -> "বাংলা"
                TELUGU -> "తెలుగు"
                TAMIL -> "தமிழ்"
                MARATHI -> "मराठी"
                GUJARATI -> "ગુજરાતી"
                KANNADA -> "ಕನ್ನಡ"
                MALAYALAM -> "മലയാളം"
                ODIA -> "ଓଡ଼ିଆ"
                ASSAMESE -> "অসমীয়া"
                URDU -> "اردو"
                SANSKRIT -> "संस्कृतम्"
                NEPALI -> "नेपाली"
                else -> code.uppercase()
            }
        }
    }
}