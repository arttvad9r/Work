package com.worktime.app.ui.yearsummary

import androidx.lifecycle.SavedStateHandle
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.repository.WorkEntryRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `zero net bonus and penalty month stays populated`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 8, 10),
            workedMinutes = 0,
            hourlyRateMicros = 0,
            bonusMicros = 100_000_000,
            penaltyMicros = 100_000_000,
        )

        val summary = buildYearSummary(2026, listOf(entry))

        assertEquals(1, summary.monthsWithData)
        assertTrue(summary.monthHasData[7])
    }

    @Test
    fun `year summary aggregates totals and keeps all twelve month slots`() {
        val entries = listOf(
            WorkEntry(LocalDate.of(2026, 1, 10), 480, 350_000_000L),
            WorkEntry(LocalDate.of(2026, 2, 5), 840, 370_000_000L, bonusMicros = 1_500_000_000L),
        )

        val summary = buildYearSummary(2026, entries)

        assertEquals(2026, summary.year)
        assertEquals(12, summary.months.size)
        assertEquals(2, summary.total.shiftCount)
        assertEquals(1320, summary.total.workedMinutes)
        assertEquals(
            480 * 350_000_000L / 60 + 840 * 370_000_000L / 60 + 1_500_000_000L,
            summary.total.totalPayMicros,
        )
        assertEquals(1, summary.months[0].shiftCount)
        assertEquals(0, summary.months[2].shiftCount)
        assertEquals(2, summary.monthsWithData)
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
