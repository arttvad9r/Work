package com.worktime.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val settingsOpen = MutableStateFlow(false)
    private val operationError = MutableStateFlow<CalendarOperationError?>(null)

    private val monthSnapshot = visibleMonth.flatMapLatest { month ->
        workEntryRepository.observeMonth(month).map { entries -> month to entries }
    }

    private val visibleMonthEntries = combine(
        visibleMonth,
        monthSnapshot,
    ) { requestedMonth, monthAndEntries ->
        val (loadedMonth, loadedEntries) = monthAndEntries
        requestedMonth to if (loadedMonth == requestedMonth) loadedEntries else emptyList()
    }

    val state: StateFlow<CalendarUiState> = combine(
        visibleMonthEntries,
        userPreferencesRepository.preferences,
        selectedDate,
        settingsOpen,
        operationError,
    ) { monthAndEntries, preferences, selected, isSettingsOpen, error ->
        val (requestedMonth, entries) = monthAndEntries
        CalendarUiState(
            // Navigation updates immediately on arrow press. Room can emit the new
            // month's rows a moment later without holding the title/date grid back.
            visibleMonth = requestedMonth,
            entries = entries.associateBy(WorkEntry::date),
            selectedDate = selected,
            defaultHourlyRateMicros = preferences.defaultHourlyRateMicros,
            themeMode = preferences.themeMode,
            isSettingsOpen = isSettingsOpen,
            isReady = true,
            operationError = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = CalendarUiState(),
    )

    fun previousMonth() = visibleMonth.update { it.minusMonths(1) }

    fun nextMonth() = visibleMonth.update { it.plusMonths(1) }

    fun selectDate(date: LocalDate) {
        operationError.value = null
        settingsOpen.value = false
        selectedDate.value = date
    }

    fun dismissEditor() {
        operationError.value = null
        selectedDate.value = null
    }

    fun openSettings() {
        operationError.value = null
        selectedDate.value = null
        settingsOpen.value = true
    }

    fun dismissSettings() {
        operationError.value = null
        settingsOpen.value = false
    }

    fun saveEntry(entry: WorkEntry) {
        operationError.value = null
        viewModelScope.launch {
            try {
                workEntryRepository.save(entry)
                selectedDate.value = null
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                operationError.value = CalendarOperationError.SAVE_ENTRY
            }
        }
    }

    fun deleteEntry(date: LocalDate) {
        operationError.value = null
        viewModelScope.launch {
            try {
                workEntryRepository.delete(date)
                selectedDate.value = null
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                operationError.value = CalendarOperationError.DELETE_ENTRY
            }
        }
    }

    fun updatePreferences(
        defaultHourlyRateMicros: Long,
        themeMode: ThemeMode,
    ) {
        operationError.value = null
        viewModelScope.launch {
            try {
                userPreferencesRepository.update(
                    defaultHourlyRateMicros = defaultHourlyRateMicros,
                    themeMode = themeMode,
                )
                settingsOpen.value = false
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                operationError.value = CalendarOperationError.SAVE_SETTINGS
            }
        }
    }

    class Factory(
        private val workEntryRepository: WorkEntryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                return CalendarViewModel(workEntryRepository, userPreferencesRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
