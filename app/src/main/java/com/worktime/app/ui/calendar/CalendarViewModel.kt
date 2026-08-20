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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val settingsOpen = MutableStateFlow(false)

    private val monthEntries = visibleMonth.flatMapLatest(workEntryRepository::observeMonth)

    val state: StateFlow<CalendarUiState> = combine(
        visibleMonth,
        monthEntries,
        userPreferencesRepository.preferences,
        selectedDate,
        settingsOpen,
    ) { month, entries, preferences, selected, isSettingsOpen ->
        CalendarUiState(
            visibleMonth = month,
            entries = entries.associateBy(WorkEntry::date),
            selectedDate = selected,
            currencyCode = preferences.currencyCode,
            defaultHourlyRateMicros = preferences.defaultHourlyRateMicros,
            themeMode = preferences.themeMode,
            isSettingsOpen = isSettingsOpen,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = CalendarUiState(),
    )

    fun previousMonth() = visibleMonth.update { it.minusMonths(1) }

    fun nextMonth() = visibleMonth.update { it.plusMonths(1) }

    fun selectDate(date: LocalDate) = selectedDate.update { date }

    fun dismissEditor() = selectedDate.update { null }

    fun openSettings() = settingsOpen.update { true }

    fun dismissSettings() = settingsOpen.update { false }

    fun saveEntry(entry: WorkEntry) {
        viewModelScope.launch {
            workEntryRepository.save(entry)
            selectedDate.value = null
        }
    }

    fun deleteEntry(date: LocalDate) {
        viewModelScope.launch {
            workEntryRepository.delete(date)
            selectedDate.value = null
        }
    }

    fun updatePreferences(
        defaultHourlyRateMicros: Long,
        currencyCode: String,
        themeMode: ThemeMode,
    ) {
        viewModelScope.launch {
            userPreferencesRepository.update(
                defaultHourlyRateMicros = defaultHourlyRateMicros,
                currencyCode = currencyCode,
                themeMode = themeMode,
            )
            settingsOpen.value = false
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
