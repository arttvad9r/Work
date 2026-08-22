package com.worktime.app.ui.calendar

import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CalendarViewModelTest {
    @Test
    fun `deleting an existing entry stores exact entry and undo restores it`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000, note = "original")
        val repository = FakeWorkEntryRepository(listOf(entry))
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady && it.entries[entry.date] == entry }

        viewModel.deleteEntry(entry.date)
        advanceUntilIdle()

        assertTrue(viewModel.state.first { it.canUndo }.canUndo)
        viewModel.undoLastOperation()
        viewModel.state.first { it.operationResult == CalendarOperationResult.OPERATION_UNDONE }

        assertEquals(listOf(entry), repository.savedEntries)
        assertFalse(viewModel.state.value.canUndo)
        stateJob.cancel()
    }

    @Test
    fun `bulk rate update stores all original records and undo restores mixed rates`() = runTest {
        val entries = listOf(
            WorkEntry(LocalDate.of(2026, 8, 9), 480, 10_000_000),
            WorkEntry(LocalDate.of(2026, 8, 10), 420, 11_000_000, bonusMicros = 2_000_000),
            WorkEntry(LocalDate.of(2026, 8, 12), 360, 12_000_000, penaltyMicros = 1_000_000, note = "late"),
        )
        val repository = FakeWorkEntryRepository(entries)
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(
            LocalDate.of(2026, 8, 10),
            LocalDate.of(2026, 8, 12),
            20_000_000,
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.first { it.canUndo }.canUndo)
        assertEquals(entries.subList(1, 3), repository.bulkOriginals)
        viewModel.undoLastOperation()
        viewModel.state.first { it.operationResult == CalendarOperationResult.OPERATION_UNDONE }

        assertEquals(entries.subList(1, 3).toList(), repository.savedEntries.toList())
        assertFalse(viewModel.state.value.canUndo)
        stateJob.cancel()
    }

    @Test
    fun `failed bulk operation does not expose undo`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList()).apply { bulkError = IllegalStateException() }
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(
            LocalDate.of(2026, 8, 12),
            LocalDate.of(2026, 8, 10),
            20_000_000,
        )
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canUndo)
        assertEquals(
            CalendarOperationError.BULK_RATE,
            viewModel.state.first { it.operationError == CalendarOperationError.BULK_RATE }.operationError,
        )
        stateJob.cancel()
    }

    @Test
    fun `bulk rate rejects non-positive rates without repository call`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10), 0L)
        advanceUntilIdle()

        assertEquals(0, repository.bulkCalls)
        assertEquals(
            CalendarOperationError.BULK_RATE,
            viewModel.state.first { it.operationError == CalendarOperationError.BULK_RATE }.operationError,
        )
        stateJob.cancel()
    }
}

private class FakeWorkEntryRepository(initialEntries: List<WorkEntry>) : WorkEntryRepository {
    private val entries = MutableStateFlow(initialEntries)
    val savedEntries = mutableListOf<WorkEntry>()
    var bulkOriginals = emptyList<WorkEntry>()
    var bulkError: Exception? = null
    var bulkCalls = 0

    override fun observeMonth(month: YearMonth): Flow<List<WorkEntry>> = entries
    override suspend fun save(entry: WorkEntry) {
        savedEntries += entry
        entries.value = entries.value.filterNot { it.date == entry.date } + entry
    }
    override suspend fun delete(date: LocalDate) {
        entries.value = entries.value.filterNot { it.date == date }
    }
    override suspend fun updateHourlyRate(
        startDate: LocalDate,
        endDate: LocalDate,
        hourlyRateMicros: Long,
    ): List<WorkEntry> {
        bulkCalls++
        bulkError?.let { throw it }
        val originals = entries.value.filter { it.date in startDate..endDate }
        bulkOriginals = originals
        entries.value = entries.value.map { entry ->
            if (entry.date in startDate..endDate) entry.copy(hourlyRateMicros = hourlyRateMicros) else entry
        }
        return originals
    }
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    override val preferences: Flow<UserPreferences> = flowOf(UserPreferences())
    override suspend fun update(defaultHourlyRateMicros: Long, themeMode: ThemeMode) = Unit
}
