package com.sumit.varnamala.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")

class OnboardingRepository(private val context: Context) {
    private object PreferencesKeys {
        val HAS_SEEN_CAMERA_TUTORIAL = booleanPreferencesKey("has_seen_camera_tutorial")
        val HAS_SELECTED_APP_LANGUAGE = booleanPreferencesKey("has_selected_app_language")
        val HAS_REQUIRED_OCR_RESOURCES = booleanPreferencesKey("has_required_ocr_resources")
    }

    val hasSeenCameraTutorial: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_CAMERA_TUTORIAL] ?: false
        }

    val hasSeenCameraTutorialLoaded: Flow<Boolean?> = hasSeenCameraTutorial
        .map<Boolean, Boolean?> { it }

    val hasSelectedAppLanguage: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HAS_SELECTED_APP_LANGUAGE] ?: false
        }

    val hasSelectedAppLanguageLoaded: Flow<Boolean?> = hasSelectedAppLanguage
        .map<Boolean, Boolean?> { it }
    val hasRequiredOcrResources: Flow<Boolean> = context.onboardingDataStore.data
        .map { it[PreferencesKeys.HAS_REQUIRED_OCR_RESOURCES] ?: false }
    suspend fun setHasSeenCameraTutorial(value: Boolean) {
        context.onboardingDataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_CAMERA_TUTORIAL] = value
        }
    }

    suspend fun setHasSelectedAppLanguage(value: Boolean) {
        context.onboardingDataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SELECTED_APP_LANGUAGE] = value
        }
    }
    suspend fun setHasRequiredOcrResources(value: Boolean) {
        context.onboardingDataStore.edit { it[PreferencesKeys.HAS_REQUIRED_OCR_RESOURCES] = value }
    }
}
