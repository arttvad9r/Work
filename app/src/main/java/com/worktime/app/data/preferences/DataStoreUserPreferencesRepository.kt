package com.worktime.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import com.worktime.app.domain.repository.UserPreferencesRepository
import java.io.IOException
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

class DataStoreUserPreferencesRepository(
    private val context: Context,
) : UserPreferencesRepository {
    override val preferences: Flow<UserPreferences> = context.userPreferencesDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
        }
        .map(::toUserPreferences)

    override suspend fun update(
        defaultHourlyRateMicros: Long,
        currencyCode: String,
        themeMode: ThemeMode,
    ) {
        require(defaultHourlyRateMicros >= 0L)
        val normalizedCurrency = currencyCode.trim().uppercase(Locale.ROOT)
        require(normalizedCurrency.length == 3)
        Currency.getInstance(normalizedCurrency)

        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.DEFAULT_HOURLY_RATE_MICROS] = defaultHourlyRateMicros
            preferences[Keys.CURRENCY_CODE] = normalizedCurrency
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    private fun toUserPreferences(preferences: Preferences): UserPreferences {
        val themeMode = preferences[Keys.THEME_MODE]
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM

        val storedCurrency = preferences[Keys.CURRENCY_CODE]
        val currencyCode = storedCurrency
            ?.takeIf(::isValidCurrencyCode)
            ?: defaultCurrencyCode()

        return UserPreferences(
            defaultHourlyRateMicros = preferences[Keys.DEFAULT_HOURLY_RATE_MICROS] ?: 0L,
            currencyCode = currencyCode,
            themeMode = themeMode,
        )
    }

    private fun isValidCurrencyCode(code: String): Boolean = runCatching {
        Currency.getInstance(code)
    }.isSuccess

    private fun defaultCurrencyCode(): String = runCatching {
        Currency.getInstance(Locale.getDefault()).currencyCode
    }.getOrDefault("EUR")

    private object Keys {
        val DEFAULT_HOURLY_RATE_MICROS = longPreferencesKey("default_hourly_rate_micros")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
