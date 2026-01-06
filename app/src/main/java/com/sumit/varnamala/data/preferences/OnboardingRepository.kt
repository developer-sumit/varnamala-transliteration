package com.sumit.varnamala.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")

class OnboardingRepository(private val context: Context) {
    private object PreferencesKeys {
        val HAS_SEEN_CAMERA_TUTORIAL = booleanPreferencesKey("has_seen_camera_tutorial")
        val HAS_SELECTED_APP_LANGUAGE = booleanPreferencesKey("has_selected_app_language")
    }

    val hasSeenCameraTutorial: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_CAMERA_TUTORIAL] ?: false
        }

    val hasSeenCameraTutorialLoaded: Flow<Boolean?> = hasSeenCameraTutorial
        .map { it as Boolean? }
        .onStart { emit(null) }

    val hasSelectedAppLanguage: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HAS_SELECTED_APP_LANGUAGE] ?: false
        }

    val hasSelectedAppLanguageLoaded: Flow<Boolean?> = hasSelectedAppLanguage
        .map { it as Boolean? }
        .onStart { emit(null) }

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
}
