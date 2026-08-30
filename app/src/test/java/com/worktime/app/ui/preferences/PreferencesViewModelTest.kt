package com.worktime.app.ui.preferences

import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import com.worktime.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PreferencesViewModelTest {
    @Test
    fun `state reflects repository preferences`() = runTest {
        val repository = FakeUserPreferencesRepository(
            UserPreferences(
                defaultHourlyRateMicros = 125_000_000L,
                themeMode = ThemeMode.DARK,
            ),
        )
        val viewModel = PreferencesViewModel(repository)

        val state = viewModel.state.first { it.isReady }

        assertEquals(125_000_000L, state.defaultHourlyRateMicros)
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertFalse(state.saveFailed)
    }

    @Test
    fun `theme and rate mutations preserve both values`() = runTest {
        val repository = FakeUserPreferencesRepository()
        val viewModel = PreferencesViewModel(repository)
        viewModel.state.first { it.isReady }

        viewModel.updateThemeMode(ThemeMode.DARK)
        viewModel.updateDefaultRate(420_000_000L)

        val state = viewModel.state.first {
            it.themeMode == ThemeMode.DARK && it.defaultHourlyRateMicros == 420_000_000L
        }
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(420_000_000L, state.defaultHourlyRateMicros)
        assertFalse(state.saveFailed)
    }

    @Test
    fun `failed mutation is exposed and a successful retry clears the error`() = runTest {
        val repository = FakeUserPreferencesRepository().apply {
            themeFailure = IllegalStateException("datastore")
        }
        val viewModel = PreferencesViewModel(repository)
        viewModel.state.first { it.isReady }

        viewModel.updateThemeMode(ThemeMode.DARK)
        assertTrue(viewModel.state.first { it.saveFailed }.saveFailed)

        repository.themeFailure = null
        viewModel.updateThemeMode(ThemeMode.DARK)

        val recovered = viewModel.state.first {
            !it.saveFailed && it.themeMode == ThemeMode.DARK
        }
        assertFalse(recovered.saveFailed)
    }
}

private class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {
    private val values = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = values
    override val defaultRateInitialized: Flow<Boolean> = flowOf(false)
    var themeFailure: Exception? = null
    var rateFailure: Exception? = null

    override suspend fun update(
        defaultHourlyRateMicros: Long,
        themeMode: ThemeMode,
        defaultRateInitialized: Boolean,
    ) {
        values.value = UserPreferences(defaultHourlyRateMicros, themeMode)
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        themeFailure?.let { throw it }
        values.value = values.value.copy(themeMode = themeMode)
    }

    override suspend fun updateDefaultHourlyRate(defaultHourlyRateMicros: Long) {
        rateFailure?.let { throw it }
        values.value = values.value.copy(defaultHourlyRateMicros = defaultHourlyRateMicros)
    }

    override suspend fun adoptDefaultHourlyRateIfUninitialized(defaultHourlyRateMicros: Long): Boolean = false
}
