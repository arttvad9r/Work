package com.worktime.app.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class PreferencesUiState(
    val defaultHourlyRateMicros: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isReady: Boolean = false,
    val saveFailed: Boolean = false,
)

internal class PreferencesViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val saveFailed = MutableStateFlow(false)
    private val mutationMutex = Mutex()

    val state: StateFlow<PreferencesUiState> = combine(
        userPreferencesRepository.preferences,
        saveFailed,
    ) { preferences, failed ->
        PreferencesUiState(
            defaultHourlyRateMicros = preferences.defaultHourlyRateMicros,
            themeMode = preferences.themeMode,
            isReady = true,
            saveFailed = failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = PreferencesUiState(),
    )

    fun updateThemeMode(themeMode: ThemeMode) {
        runMutation {
            userPreferencesRepository.updateThemeMode(themeMode)
        }
    }

    fun updateDefaultRate(defaultHourlyRateMicros: Long) {
        runMutation {
            userPreferencesRepository.updateDefaultHourlyRate(defaultHourlyRateMicros)
        }
    }

    private fun runMutation(block: suspend () -> Unit) {
        saveFailed.value = false
        viewModelScope.launch {
            try {
                mutationMutex.withLock { block() }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                saveFailed.value = true
            }
        }
    }

    companion object {
        fun factory(
            userPreferencesRepository: UserPreferencesRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { PreferencesViewModel(userPreferencesRepository) }
        }
    }
}
