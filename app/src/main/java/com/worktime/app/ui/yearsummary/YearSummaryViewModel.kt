package com.worktime.app.ui.yearsummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worktime.app.domain.repository.WorkEntryRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

internal data class YearSummaryUiState(
    val summary: YearSummary? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class YearSummaryViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val savedStateHandle: SavedStateHandle,
    initialYear: Int,
) : ViewModel() {
    private val year = savedStateHandle.getStateFlow(KEY_YEAR, initialYear)

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

    fun showPreviousYear() {
        savedStateHandle[KEY_YEAR] = year.value - 1
    }

    fun showNextYear() {
        savedStateHandle[KEY_YEAR] = year.value + 1
    }

    companion object {
        private const val KEY_YEAR = "year"

        fun factory(
            workEntryRepository: WorkEntryRepository,
            initialYear: Int,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                YearSummaryViewModel(
                    workEntryRepository = workEntryRepository,
                    savedStateHandle = createSavedStateHandle(),
                    initialYear = initialYear,
                )
            }
        }
    }
}
