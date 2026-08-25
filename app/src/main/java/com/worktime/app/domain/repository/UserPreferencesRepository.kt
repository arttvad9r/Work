package com.worktime.app.domain.repository

import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    val defaultRateInitialized: Flow<Boolean>

    suspend fun update(
        defaultHourlyRateMicros: Long,
        themeMode: ThemeMode,
        defaultRateInitialized: Boolean = true,
    )

    suspend fun updateThemeMode(themeMode: ThemeMode)

    suspend fun updateDefaultHourlyRate(defaultHourlyRateMicros: Long)

    suspend fun adoptDefaultHourlyRateIfUninitialized(defaultHourlyRateMicros: Long): Boolean
}
