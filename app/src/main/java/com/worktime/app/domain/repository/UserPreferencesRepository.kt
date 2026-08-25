package com.worktime.app.domain.repository

import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun update(
        defaultHourlyRateMicros: Long,
        themeMode: ThemeMode,
    )

    suspend fun updateThemeMode(themeMode: ThemeMode)

    suspend fun updateDefaultHourlyRate(defaultHourlyRateMicros: Long)

    suspend fun adoptDefaultHourlyRateIfUnset(defaultHourlyRateMicros: Long): Boolean
}
