package com.worktime.app.data.preferences

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import com.worktime.app.domain.repository.UserPreferencesRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "user_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class DataStoreUserPreferencesRepository(
    private val context: Context,
) : UserPreferencesRepository {
    override val preferences: Flow<UserPreferences> = context.userPreferencesDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::toUserPreferences)

    override val defaultRateInitialized: Flow<Boolean> = context.userPreferencesDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            preferences[Keys.DEFAULT_RATE_INITIALIZED]
                ?: (preferences[Keys.DEFAULT_HOURLY_RATE_MICROS] ?: 0L) != 0L
        }

    override suspend fun update(
        defaultHourlyRateMicros: Long,
        themeMode: ThemeMode,
        defaultRateInitialized: Boolean,
    ) {
        require(defaultHourlyRateMicros in 0..MoneyLimits.MAX_COMPONENT_MICROS)

        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.DEFAULT_HOURLY_RATE_MICROS] = defaultHourlyRateMicros
            preferences[Keys.THEME_MODE] = themeMode.name
            preferences[Keys.DEFAULT_RATE_INITIALIZED] = defaultRateInitialized
        }
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
            preferences[Keys.DEFAULT_RATE_INITIALIZED] = true
        }
    }

    override suspend fun updateDefaultHourlyRate(defaultHourlyRateMicros: Long) {
        require(defaultHourlyRateMicros in 0..MoneyLimits.MAX_COMPONENT_MICROS)
        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.DEFAULT_HOURLY_RATE_MICROS] = defaultHourlyRateMicros
            preferences[Keys.DEFAULT_RATE_INITIALIZED] = true
        }
    }

    override suspend fun adoptDefaultHourlyRateIfUninitialized(defaultHourlyRateMicros: Long): Boolean {
        require(defaultHourlyRateMicros in 1..MoneyLimits.MAX_COMPONENT_MICROS)
        var adopted = false
        context.userPreferencesDataStore.edit { preferences ->
            if (preferences[Keys.DEFAULT_RATE_INITIALIZED] == true) return@edit
            if (preferences[Keys.DEFAULT_HOURLY_RATE_MICROS] != null &&
                preferences[Keys.DEFAULT_HOURLY_RATE_MICROS] != 0L
            ) {
                preferences[Keys.DEFAULT_RATE_INITIALIZED] = true
                return@edit
            }
            if ((preferences[Keys.DEFAULT_HOURLY_RATE_MICROS] ?: 0L) == 0L) {
                preferences[Keys.DEFAULT_HOURLY_RATE_MICROS] = defaultHourlyRateMicros
                preferences[Keys.DEFAULT_RATE_INITIALIZED] = true
                adopted = true
            }
        }
        return adopted
    }

    private fun toUserPreferences(preferences: Preferences): UserPreferences {
        val themeMode = preferences[Keys.THEME_MODE]
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM

        val defaultRate = preferences[Keys.DEFAULT_HOURLY_RATE_MICROS]
            ?.takeIf { it in 0..MoneyLimits.MAX_COMPONENT_MICROS }
            ?: 0L

        return UserPreferences(
            defaultHourlyRateMicros = defaultRate,
            themeMode = themeMode,
        )
    }

    private object Keys {
        val DEFAULT_HOURLY_RATE_MICROS = longPreferencesKey("default_hourly_rate_micros")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_RATE_INITIALIZED = androidx.datastore.preferences.core.booleanPreferencesKey(
            "default_rate_initialized",
        )
    }
}
