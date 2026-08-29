package com.worktime.app.ui.yearsummary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worktime.app.domain.repository.WorkEntryRepository
import com.worktime.app.ui.calendar.YearSummary
import com.worktime.app.ui.calendar.buildYearSummary
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal data class YearSummaryUiState(
    val summary: YearSummary? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class YearSummaryViewModel(
    private val workEntryRepository: WorkEntryRepository,
    initialYear: Int,
) : ViewModel() {
    private val year = MutableStateFlow(initialYear)

    val state: StateFlow<YearSummaryUiState> = year
        .flatMapLatest { selectedYear ->
            workEntryRepository.observeDateRange(
                LocalDate.of(selectedYear, 1, 1),
                LocalDate.of(selectedYear, 12, 31),
            )
                .map { entries ->
                    YearSummaryUiState(
                        summary = buildYearSummary(selectedYear, entries),
                    )
                }
                .onStart { emit(YearSummaryUiState()) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = YearSummaryUiState(),
        )

    fun showPreviousYear() = year.update { it - 1 }

    fun showNextYear() = year.update { it + 1 }

    companion object {
        fun factory(
            workEntryRepository: WorkEntryRepository,
            initialYear: Int,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                YearSummaryViewModel(
                    workEntryRepository = workEntryRepository,
                    initialYear = initialYear,
                )
            }
        }
    }
}
