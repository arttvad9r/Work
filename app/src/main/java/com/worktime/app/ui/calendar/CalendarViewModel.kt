package com.worktime.app.ui.calendar

import androidx.lifecycle.ViewModel
import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CalendarViewModel : ViewModel() {
    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    fun previousMonth() = _state.update { it.copy(visibleMonth = it.visibleMonth.minusMonths(1)) }

    fun nextMonth() = _state.update { it.copy(visibleMonth = it.visibleMonth.plusMonths(1)) }

    fun selectDate(date: LocalDate) = _state.update { it.copy(selectedDate = date) }

    fun dismissEditor() = _state.update { it.copy(selectedDate = null) }

    fun saveEntry(entry: WorkEntry) = _state.update { state ->
        state.copy(
            entries = state.entries + (entry.date to entry),
            selectedDate = null,
        )
    }

    fun deleteEntry(date: LocalDate) = _state.update { state ->
        state.copy(entries = state.entries - date, selectedDate = null)
    }
}
