package com.worktime.app.ui.yearsummary

import androidx.lifecycle.SavedStateHandle
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.repository.WorkEntryRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YearSummaryViewModelTest {
    @Test
    fun `next and previous year update saved state`() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = YearSummaryViewModel(
            workEntryRepository = EmptyWorkEntryRepository,
            savedStateHandle = savedStateHandle,
            initialYear = 2026,
        )

        viewModel.showPreviousYear()
        assertEquals(2025, savedStateHandle.get<Int>("year"))

        viewModel.showNextYear()
        assertEquals(2026, savedStateHandle.get<Int>("year"))
    }

    @Test
    fun `restored year takes precedence over route initial year`() {
        val savedStateHandle = SavedStateHandle(mapOf("year" to 2024))
        val viewModel = YearSummaryViewModel(
            workEntryRepository = EmptyWorkEntryRepository,
            savedStateHandle = savedStateHandle,
            initialYear = 2026,
        )

        viewModel.showNextYear()

        assertEquals(2025, savedStateHandle.get<Int>("year"))
    }
}

private object EmptyWorkEntryRepository : WorkEntryRepository {
    override fun observeMonth(month: YearMonth): Flow<List<WorkEntry>> = flowOf(emptyList())

    override fun observeDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<WorkEntry>> = flowOf(emptyList())

    override suspend fun getAll(): List<WorkEntry> = emptyList()

    override suspend fun save(entry: WorkEntry) = Unit

    override suspend fun delete(date: LocalDate) = Unit

    override suspend fun restore(entries: List<WorkEntry>) = Unit

    override suspend fun replaceAll(entries: List<WorkEntry>) = Unit

    override suspend fun updateHourlyRate(
        startDate: LocalDate,
        endDate: LocalDate,
        hourlyRateMicros: Long,
    ): List<WorkEntry> = emptyList()
}
