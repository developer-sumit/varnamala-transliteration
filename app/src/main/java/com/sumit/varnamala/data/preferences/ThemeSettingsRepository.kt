package com.sumit.varnamala.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

class ThemeSettingsRepository(private val context: Context) {
    private object PreferencesKeys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    val isDarkTheme: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] ?: false // Default to false (light theme)
        }

    suspend fun setIsDarkTheme(value: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] = value
        }
    }
}