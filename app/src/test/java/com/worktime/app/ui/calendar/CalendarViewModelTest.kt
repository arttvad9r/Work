package com.worktime.app.ui.calendar

import com.worktime.app.data.backup.BackupCodec
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Month
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CalendarViewModelTest {
    @Test
    fun `export backup writes all entries and preferences and reports success`() = runTest {
        val entries = listOf(
            WorkEntry(LocalDate.of(2026, 7, 30), 300, 9_000_000),
            WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000),
        )
        val repository = FakeWorkEntryRepository(entries)
        val preferencesRepository = FakeUserPreferencesRepository()
        preferencesRepository.update(defaultHourlyRateMicros = 12_500_000L, themeMode = ThemeMode.DARK)
        val viewModel = CalendarViewModel(repository, preferencesRepository)
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        val stream = ByteArrayOutputStream()
        viewModel.exportBackup(stream)
        assertEquals(CalendarOperationEvent.Success.BACKUP_EXPORTED, viewModel.operationEvents.first())

        val restored = BackupCodec.decode(stream.toString("UTF-8"))
        assertEquals(entries, restored.entries)
        assertEquals(UserPreferences(12_500_000L, ThemeMode.DARK), restored.preferences)
        stateJob.cancel()
    }

    @Test
    fun `failed export reports error event`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }
        val failingStream = object : OutputStream() {
            override fun write(b: Int) = error("io failure")
        }

        viewModel.exportBackup(failingStream)
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.BACKUP_EXPORT), viewModel.operationEvents.first())
        viewModel.state.first { it.operationError == CalendarOperationError.BACKUP_EXPORT }
        stateJob.cancel()
    }

    @Test
    fun `import awaits confirmation then replaceAll applies backup`() = runTest {
        val repository = FakeWorkEntryRepository(listOf(WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)))
        val preferencesRepository = FakeUserPreferencesRepository()
        val viewModel = CalendarViewModel(repository, preferencesRepository)
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }
        val backupEntries = listOf(WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000))
        val payload = BackupCodec.encode(backupEntries, UserPreferences(7_000_000L, ThemeMode.LIGHT))

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        assertEquals(backupEntries.size, viewModel.state.first { it.pendingImportCount != null }.pendingImportCount)
        assertEquals(0, repository.replaceAllCalls)

        viewModel.confirmImport()
        assertEquals(CalendarOperationEvent.Success.BACKUP_IMPORTED, viewModel.operationEvents.first())
        assertEquals(backupEntries, repository.observeMonth(YearMonth.of(2026, 7)).first())
        assertEquals(UserPreferences(7_000_000L, ThemeMode.LIGHT), preferencesRepository.updates.single())
        assertEquals(null, viewModel.state.first { it.pendingImportCount == null }.pendingImportCount)
        stateJob.cancel()
    }

    @Test
    fun `malformed import reports error without writing`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.importBackup(ByteArrayInputStream("garbage".toByteArray()))
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.BACKUP_IMPORT), viewModel.operationEvents.first())
        assertEquals(null, viewModel.state.value.pendingImportCount)
        assertEquals(0, repository.replaceAllCalls)
        stateJob.cancel()
    }

    @Test
    fun `cancel import clears pending without writing`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }
        val payload = BackupCodec.encode(listOf(WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000)), UserPreferences())

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        viewModel.state.first { it.pendingImportCount != null }
        viewModel.cancelImport()
        assertEquals(null, viewModel.state.first { it.pendingImportCount == null }.pendingImportCount)
        assertEquals(0, repository.replaceAllCalls)
        stateJob.cancel()
    }

    @Test
    fun `failed replaceAll keeps pending import for retry and reports error`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList()).apply { replaceAllError = IllegalStateException("db") }
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }
        val backupEntries = listOf(WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000))
        val payload = BackupCodec.encode(backupEntries, UserPreferences())

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        viewModel.state.first { it.pendingImportCount != null }
        viewModel.confirmImport()
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.BACKUP_IMPORT), viewModel.operationEvents.first())

        assertEquals(backupEntries.size, viewModel.state.value.pendingImportCount)
        stateJob.cancel()
    }

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
        assertEquals(CalendarOperationEvent.Success.ENTRY_DELETED, viewModel.operationEvents.first())
        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Success.OPERATION_UNDONE, viewModel.operationEvents.first())

        assertEquals(listOf(entry), repository.restoredEntries)
        assertFalse(viewModel.state.first { !it.canUndo }.canUndo)
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
        assertEquals(CalendarOperationEvent.Success.RATE_UPDATED, viewModel.operationEvents.first())
        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Success.OPERATION_UNDONE, viewModel.operationEvents.first())

        assertEquals(entries.subList(1, 3).toList(), repository.restoredEntries.toList())
        assertFalse(viewModel.state.first { !it.canUndo }.canUndo)
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
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.BULK_RATE), viewModel.operationEvents.first())
        assertEquals(null, viewModel.state.value.selectedDate)
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

    @Test
    fun `successful delete emits one root event without replay`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        val repository = FakeWorkEntryRepository(listOf(entry))
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.entries[entry.date] == entry }

        viewModel.deleteEntry(entry.date)

        assertEquals(CalendarOperationEvent.Success.ENTRY_DELETED, viewModel.operationEvents.first())
        assertEquals(null, withTimeoutOrNull(50) { viewModel.operationEvents.first() })
        stateJob.cancel()
    }

    @Test
    fun `failed delete emits root error and leaves undo unavailable`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList()).apply { deleteError = IllegalStateException() }
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.deleteEntry(LocalDate.of(2026, 8, 10))

        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.DELETE_ENTRY), viewModel.operationEvents.first())
        assertFalse(viewModel.state.value.canUndo)
        stateJob.cancel()
    }

    @Test
    fun `empty bulk update is a no-op without undo`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10), 20_000_000)
        assertEquals(CalendarOperationEvent.Success.NO_OP, viewModel.operationEvents.first())
        assertFalse(viewModel.state.value.canUndo)
        stateJob.cancel()
    }

    @Test
    fun `new save supersedes undo snapshot`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        val replacement = entry.copy(note = "replacement")
        val repository = FakeWorkEntryRepository(listOf(entry))
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.entries[entry.date] == entry }

        viewModel.deleteEntry(entry.date)
        advanceUntilIdle()
        viewModel.operationEvents.first()
        viewModel.saveEntry(replacement)
        advanceUntilIdle()
        viewModel.undoLastOperation()
        assertEquals(null, withTimeoutOrNull(50) { viewModel.operationEvents.first() })
        assertFalse(viewModel.state.value.canUndo)
        stateJob.cancel()
    }

    @Test
    fun `saving an entry with empty default rate adopts the entry rate as default`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val preferencesRepository = FakeUserPreferencesRepository()
        val viewModel = CalendarViewModel(repository, preferencesRepository)
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.saveEntry(WorkEntry(LocalDate.of(2026, 8, 10), 720, 370_000_000L))

        // viewModelScope does not run on the test scheduler, so await the outcome.
        assertEquals(
            UserPreferences(370_000_000L, ThemeMode.SYSTEM),
            preferencesRepository.preferences.first { it.defaultHourlyRateMicros > 0L },
        )
        assertEquals(
            listOf(UserPreferences(370_000_000L, ThemeMode.SYSTEM)),
            preferencesRepository.updates,
        )
        stateJob.cancel()
    }

    @Test
    fun `saving an entry keeps an existing default rate untouched`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val preferencesRepository = FakeUserPreferencesRepository()
        preferencesRepository.update(defaultHourlyRateMicros = 300_000_000L, themeMode = ThemeMode.DARK)
        val viewModel = CalendarViewModel(repository, preferencesRepository)
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.saveEntry(WorkEntry(LocalDate.of(2026, 8, 10), 720, 370_000_000L))
        // Wait for the entry itself; the rate guard runs right after it in the same job.
        repository.observeMonth(YearMonth.now()).first { it.isNotEmpty() }

        // Only the setup update exists; the entry's different rate must not replace it.
        assertEquals(
            listOf(UserPreferences(300_000_000L, ThemeMode.DARK)),
            preferencesRepository.updates,
        )
        assertEquals(300_000_000L, preferencesRepository.preferences.first().defaultHourlyRateMicros)
        stateJob.cancel()
    }

    @Test
    fun `year summary aggregates totals and keeps all twelve month slots`() = runTest {
        val repository = FakeWorkEntryRepository(
            listOf(
                WorkEntry(LocalDate.of(2026, 1, 10), 480, 350_000_000L),
                WorkEntry(LocalDate.of(2026, 2, 5), 840, 370_000_000L, bonusMicros = 1_500_000_000L),
                WorkEntry(LocalDate.of(2025, 6, 20), 600, 300_000_000L),
            ),
        )
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.openYearSummary()
        val summary = viewModel.state.first { it.isYearSummaryOpen && it.yearSummary != null }.yearSummary!!

        assertEquals(2026, summary.year)
        assertEquals(12, summary.months.size)
        // Only the two 2026 entries count; the 2025 one stays out.
        assertEquals(2, summary.total.shiftCount)
        assertEquals(1320, summary.total.workedMinutes)
        assertEquals(
            480 * 350_000_000L / 60 + 840 * 370_000_000L / 60 + 1_500_000_000L,
            summary.total.totalPayMicros,
        )
        assertEquals(1, summary.months[Month.JANUARY.value - 1].shiftCount)
        assertEquals(0, summary.months[Month.MARCH.value - 1].shiftCount)
        assertEquals(2, summary.monthsWithData)

        viewModel.dismissYearSummary()
        assertFalse(viewModel.state.first { !it.isYearSummaryOpen }.isYearSummaryOpen)
        stateJob.cancel()
    }

    @Test
    fun `bulk undo keeps snapshot when atomic restore fails and can retry`() = runTest {
        val entries = listOf(
            WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000),
            WorkEntry(LocalDate.of(2026, 8, 11), 480, 11_000_000),
        )
        val repository = FakeWorkEntryRepository(entries)
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }
        viewModel.changeRateForPeriod(entries.first().date, entries.last().date, 20_000_000)
        viewModel.operationEvents.first()
        repository.restoreError = IllegalStateException()

        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.UNDO), viewModel.operationEvents.first())
        assertTrue(viewModel.state.value.canUndo)
        repository.restoreError = null
        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Success.OPERATION_UNDONE, viewModel.operationEvents.first())
        assertFalse(viewModel.state.first { !it.canUndo }.canUndo)
        assertEquals(entries, repository.restoredEntries)
        stateJob.cancel()
    }

    @Test
    fun `bulk and undo errors are exposed when editor is closed`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList()).apply {
            bulkError = IllegalStateException()
        }
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10), 20_000_000)
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.BULK_RATE), viewModel.operationEvents.first())
        assertEquals(null, viewModel.state.value.selectedDate)

        repository.bulkError = null
        repository.restoreError = IllegalStateException()
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        repository.replaceEntries(listOf(entry))
        viewModel.changeRateForPeriod(entry.date, entry.date, 20_000_000)
        assertEquals(CalendarOperationEvent.Success.RATE_UPDATED, viewModel.operationEvents.first())
        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.UNDO), viewModel.operationEvents.first())
        assertEquals(CalendarOperationError.UNDO, viewModel.state.first { it.operationError == CalendarOperationError.UNDO }.operationError)
        assertEquals(null, viewModel.state.value.selectedDate)
        stateJob.cancel()
    }

    @Test
    fun `opening rate period editor keeps settings and history open`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.openSettings()
        viewModel.openRateHistory()
        viewModel.openRatePeriodEditor(null)
        advanceUntilIdle()

        // Flags propagate through separate combine chains; wait for all of them.
        val state = viewModel.state.first {
            it.isChangeRateSheetOpen && it.isRateHistoryOpen && it.isSettingsOpen
        }
        assertTrue(state.isSettingsOpen)
        assertTrue(state.isRateHistoryOpen)
        assertTrue(state.isChangeRateSheetOpen)

        viewModel.dismissChangeRateSheet()
        assertFalse(viewModel.state.first { !it.isChangeRateSheetOpen }.isChangeRateSheetOpen)
        stateJob.cancel()
    }

    @Test
    fun `successful bulk rate update closes the change rate sheet`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        val repository = FakeWorkEntryRepository(listOf(entry))
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }
        viewModel.openRatePeriodEditor(null)
        advanceUntilIdle()

        viewModel.changeRateForPeriod(entry.date, entry.date, 20_000_000)
        assertEquals(CalendarOperationEvent.Success.RATE_UPDATED, viewModel.operationEvents.first())
        assertFalse(viewModel.state.first { !it.isChangeRateSheetOpen }.isChangeRateSheetOpen)
        stateJob.cancel()
    }

    @Test
    fun `failed bulk rate update keeps the change rate sheet open`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList()).apply { bulkError = IllegalStateException() }
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }
        viewModel.openRatePeriodEditor(null)
        advanceUntilIdle()

        viewModel.changeRateForPeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10), 20_000_000)
        advanceUntilIdle()

        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.BULK_RATE), viewModel.operationEvents.first())
        assertTrue(viewModel.state.first { it.isChangeRateSheetOpen }.isChangeRateSheetOpen)
        stateJob.cancel()
    }

    @Test
    fun `in flight old undo cannot clear or replace newer undo snapshot`() = runTest {
        val first = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        val second = WorkEntry(LocalDate.of(2026, 8, 11), 480, 11_000_000)
        val repository = FakeWorkEntryRepository(listOf(first, second))
        val restoreStarted = CompletableDeferred<Unit>()
        val releaseRestore = CompletableDeferred<Unit>()
        repository.restoreStarted = restoreStarted
        repository.releaseRestore = releaseRestore
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(first.date, first.date, 20_000_000)
        assertEquals(CalendarOperationEvent.Success.RATE_UPDATED, viewModel.operationEvents.first())
        viewModel.undoLastOperation()
        restoreStarted.await()

        viewModel.changeRateForPeriod(second.date, second.date, 30_000_000)
        releaseRestore.complete(Unit)
        advanceUntilIdle()

        assertEquals(CalendarOperationEvent.Success.RATE_UPDATED, viewModel.operationEvents.first())
        assertTrue(viewModel.state.first { it.canUndo }.canUndo)
        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Success.OPERATION_UNDONE, viewModel.operationEvents.first())
        assertEquals(listOf(second), repository.restoredEntries)
        stateJob.cancel()
    }
}

private class FakeWorkEntryRepository(initialEntries: List<WorkEntry>) : WorkEntryRepository {
    private val entries = MutableStateFlow(initialEntries)
    val savedEntries = mutableListOf<WorkEntry>()
    var bulkOriginals = emptyList<WorkEntry>()
    var bulkError: Exception? = null
    var deleteError: Exception? = null
    var restoreError: Exception? = null
    var restoredEntries = emptyList<WorkEntry>()
    var restoreStarted: CompletableDeferred<Unit>? = null
    var releaseRestore: CompletableDeferred<Unit>? = null
    var bulkCalls = 0

    override fun observeMonth(month: YearMonth): Flow<List<WorkEntry>> = entries

    override fun observeDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<WorkEntry>> = entries.map { list ->
        list.filter { it.date in startDate..endDate }.sortedBy(WorkEntry::date)
    }

    fun replaceEntries(updated: List<WorkEntry>) { entries.value = updated }
    var getAllError: Exception? = null
    override suspend fun getAll(): List<WorkEntry> {
        getAllError?.let { throw it }
        return entries.value.sortedBy(WorkEntry::date)
    }
    override suspend fun save(entry: WorkEntry) {
        savedEntries += entry
        entries.value = entries.value.filterNot { it.date == entry.date } + entry
    }
    override suspend fun delete(date: LocalDate) {
        deleteError?.let { throw it }
        entries.value = entries.value.filterNot { it.date == date }
    }
    override suspend fun restore(entries: List<WorkEntry>) {
        restoreError?.let { throw it }
        restoreStarted?.complete(Unit)
        releaseRestore?.await()
        restoredEntries = entries
        entries.forEach { entry -> save(entry) }
    }
    var replaceAllCalls = 0
    var replaceAllError: Exception? = null
    override suspend fun replaceAll(entries: List<WorkEntry>) {
        replaceAllCalls++
        replaceAllError?.let { throw it }
        this.entries.value = entries
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
    private val _preferences = MutableStateFlow(UserPreferences())
    override val preferences: Flow<UserPreferences> = _preferences
    val updates = mutableListOf<UserPreferences>()
    override suspend fun update(defaultHourlyRateMicros: Long, themeMode: ThemeMode) {
        val value = UserPreferences(defaultHourlyRateMicros, themeMode)
        updates += value
        _preferences.value = value
    }
}
