package com.worktime.app.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worktime.app.domain.operation.DataMutationCoordinator
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class PreferencesUiState(
    val defaultHourlyRateMicros: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isReady: Boolean = false,
    val saveFailed: Boolean = false,
)

internal class PreferencesViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dataMutationCoordinator: DataMutationCoordinator = DataMutationCoordinator(),
) : ViewModel() {
    private val saveFailed = MutableStateFlow(false)
    private val pendingThemeMode = MutableStateFlow<ThemeMode?>(null)
    private var themeMutationGeneration = 0L

    val state: StateFlow<PreferencesUiState> = combine(
        userPreferencesRepository.preferences,
        saveFailed,
        pendingThemeMode,
    ) { preferences, failed, optimisticThemeMode ->
        PreferencesUiState(
            defaultHourlyRateMicros = preferences.defaultHourlyRateMicros,
            themeMode = optimisticThemeMode ?: preferences.themeMode,
            isReady = true,
            saveFailed = failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = PreferencesUiState(),
    )

    /**
     * Theme selection is optimistic: the visible palette follows the tap immediately while the
     * durable DataStore write remains serialized with other app mutations. If persistence fails,
     * the optimistic value is cleared and repository state becomes authoritative again.
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        val generation = ++themeMutationGeneration
        pendingThemeMode.value = themeMode
        saveFailed.value = false
        viewModelScope.launch {
            try {
                dataMutationCoordinator.run {
                    userPreferencesRepository.updateThemeMode(themeMode)
                }
                // Keep the optimistic value until the repository flow has caught up so clearing it
                // cannot expose a one-frame palette rollback after the DataStore edit completes.
                userPreferencesRepository.preferences.first { it.themeMode == themeMode }
                if (generation == themeMutationGeneration) {
                    pendingThemeMode.value = null
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == themeMutationGeneration) {
                    pendingThemeMode.value = null
                    saveFailed.value = true
                }
            }
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
                dataMutationCoordinator.run { block() }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                saveFailed.value = true
            }
        }
    }

    companion object {
        fun factory(
            userPreferencesRepository: UserPreferencesRepository,
            dataMutationCoordinator: DataMutationCoordinator = DataMutationCoordinator(),
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PreferencesViewModel(
                    userPreferencesRepository = userPreferencesRepository,
                    dataMutationCoordinator = dataMutationCoordinator,
                )
            }
        }
    }
}
