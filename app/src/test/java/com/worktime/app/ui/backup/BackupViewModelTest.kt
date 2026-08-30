package com.worktime.app.ui.backup

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BackupViewModelTest {
    @Test
    fun `export backup writes entries and preferences and emits success`() = runTest {
        val entries = listOf(
            WorkEntry(LocalDate.of(2026, 7, 30), 300, 9_000_000L),
            WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L),
        )
        val workRepository = FakeWorkEntryRepository(entries)
        val preferencesRepository = FakeUserPreferencesRepository(
            initial = UserPreferences(12_500_000L, ThemeMode.DARK),
            initialized = true,
        )
        val viewModel = BackupViewModel(workRepository, preferencesRepository)
        val output = ByteArrayOutputStream()

        viewModel.exportBackup(output)
        assertEquals(BackupOperationEvent.Success.EXPORTED, viewModel.events.first())

        val restored = BackupCodec.decode(output.toString("UTF-8"))
        assertEquals(entries, restored.entries)
        assertEquals(UserPreferences(12_500_000L, ThemeMode.DARK), restored.preferences)
    }

    @Test
    fun `failed export reports export error`() = runTest {
        val viewModel = BackupViewModel(FakeWorkEntryRepository(emptyList()), FakeUserPreferencesRepository())
        val failingOutput = object : OutputStream() {
            override fun write(b: Int) = error("io failure")
        }

        viewModel.exportBackup(failingOutput)

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.EXPORT),
            viewModel.events.first(),
        )
        assertEquals(BackupOperationError.EXPORT, viewModel.state.first { it.error != null }.error)
    }

    @Test
    fun `import waits for confirmation then replaces entries and preferences`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val preferencesRepository = FakeUserPreferencesRepository()
        val viewModel = BackupViewModel(workRepository, preferencesRepository)
        val payload = BackupCodec.encode(
            entries = listOf(importedEntry),
            preferences = UserPreferences(7_000_000L, ThemeMode.LIGHT),
            defaultRateInitialized = true,
        )

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        assertEquals(1, viewModel.state.first { it.pendingImportCount != null }.pendingImportCount)
        assertEquals(0, workRepository.replaceAllCalls)

        viewModel.confirmImport()
        assertEquals(BackupOperationEvent.Success.IMPORTED, viewModel.events.first())
        assertEquals(listOf(importedEntry), workRepository.entries.value)
        assertEquals(UserPreferences(7_000_000L, ThemeMode.LIGHT), preferencesRepository.preferences.first())
        assertEquals(true, preferencesRepository.initialized)
        assertNull(viewModel.state.first { it.pendingImportCount == null }.pendingImportCount)
    }

    @Test
    fun `zero imported default rate keeps initialization flag`() = runTest {
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val preferencesRepository = FakeUserPreferencesRepository()
        val viewModel = BackupViewModel(FakeWorkEntryRepository(emptyList()), preferencesRepository)
        val payload = BackupCodec.encode(
            entries = listOf(importedEntry),
            preferences = UserPreferences(),
            defaultRateInitialized = true,
        )

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        viewModel.state.first { it.pendingImportCount != null }
        viewModel.confirmImport()

        assertEquals(BackupOperationEvent.Success.IMPORTED, viewModel.events.first())
        assertEquals(0L, preferencesRepository.preferences.first().defaultHourlyRateMicros)
        assertEquals(true, preferencesRepository.initialized)
    }

    @Test
    fun `cancel import clears pending without replacing data`() = runTest {
        val workRepository = FakeWorkEntryRepository(emptyList())
        val viewModel = BackupViewModel(workRepository, FakeUserPreferencesRepository())
        val payload = BackupCodec.encode(
            entries = listOf(WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)),
            preferences = UserPreferences(),
        )

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        viewModel.state.first { it.pendingImportCount != null }
        viewModel.cancelImport()

        assertNull(viewModel.state.first { it.pendingImportCount == null }.pendingImportCount)
        assertEquals(0, workRepository.replaceAllCalls)
    }

    @Test
    fun `failed replace keeps pending import for retry`() = runTest {
        val workRepository = FakeWorkEntryRepository(emptyList()).apply {
            replaceAllFailuresRemaining = 1
        }
        val viewModel = BackupViewModel(workRepository, FakeUserPreferencesRepository())
        val payload = BackupCodec.encode(
            entries = listOf(WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)),
            preferences = UserPreferences(),
        )

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        viewModel.state.first { it.pendingImportCount != null }
        viewModel.confirmImport()

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT),
            viewModel.events.first(),
        )
        assertEquals(1, viewModel.state.value.pendingImportCount)
    }

    @Test
    fun `preference failure rolls entries back and keeps pending import`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val preferencesRepository = FakeUserPreferencesRepository().apply {
            updateFailuresRemaining = 1
        }
        val viewModel = BackupViewModel(workRepository, preferencesRepository)
        val payload = BackupCodec.encode(listOf(importedEntry), UserPreferences())

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        viewModel.state.first { it.pendingImportCount != null }
        viewModel.confirmImport()

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT),
            viewModel.events.first(),
        )
        assertEquals(listOf(oldEntry), workRepository.entries.value)
        assertEquals(1, viewModel.state.value.pendingImportCount)
    }

    @Test
    fun `cancellation after replace rolls old state back without error event`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val preferencesRepository = FakeUserPreferencesRepository().apply {
            cancelUpdatesRemaining = 1
        }
        val viewModel = BackupViewModel(workRepository, preferencesRepository)
        val payload = BackupCodec.encode(listOf(importedEntry), UserPreferences())

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        viewModel.state.first { it.pendingImportCount != null }
        viewModel.confirmImport()
        assertEquals(
            Unit,
            withTimeoutOrNull(1_000) {
                while (workRepository.entries.value != listOf(oldEntry) || preferencesRepository.updates.isEmpty()) {
                    kotlinx.coroutines.yield()
                }
            },
        )

        assertEquals(listOf(oldEntry), workRepository.entries.value)
        assertEquals(UserPreferences(), preferencesRepository.preferences.first())
        assertEquals(false, preferencesRepository.initialized)
        assertEquals(1, viewModel.state.value.pendingImportCount)
        assertNull(viewModel.state.value.error)
        assertNull(withTimeoutOrNull(50) { viewModel.events.first() })
    }

    @Test
    fun `rollback cancellation reports rollback error`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val preferencesRepository = FakeUserPreferencesRepository().apply {
            cancelUpdatesRemaining = 2
        }
        val viewModel = BackupViewModel(workRepository, preferencesRepository)
        val payload = BackupCodec.encode(
            entries = listOf(WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)),
            preferences = UserPreferences(),
        )

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        viewModel.state.first { it.pendingImportCount != null }
        viewModel.confirmImport()

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT_ROLLBACK),
            viewModel.events.first(),
        )
        assertEquals(listOf(oldEntry), workRepository.entries.value)
        assertEquals(1, viewModel.state.value.pendingImportCount)
    }

    @Test
    fun `rollback preference failure reports rollback error`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val preferencesRepository = FakeUserPreferencesRepository().apply {
            updateFailuresRemaining = 2
        }
        val viewModel = BackupViewModel(workRepository, preferencesRepository)
        val payload = BackupCodec.encode(listOf(importedEntry), UserPreferences())

        viewModel.importBackup(ByteArrayInputStream(payload.toByteArray()))
        viewModel.state.first { it.pendingImportCount != null }
        viewModel.confirmImport()

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT_ROLLBACK),
            viewModel.events.first(),
        )
        assertEquals(listOf(oldEntry), workRepository.entries.value)
        assertEquals(1, viewModel.state.value.pendingImportCount)
    }

    @Test
    fun `malformed import reports import error without replacing data`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val viewModel = BackupViewModel(workRepository, FakeUserPreferencesRepository())

        viewModel.importBackup(ByteArrayInputStream("garbage".toByteArray()))

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT),
            viewModel.events.first(),
        )
        assertEquals(0, workRepository.replaceAllCalls)
        assertNull(viewModel.state.value.pendingImportCount)
    }
}

private class FakeWorkEntryRepository(initial: List<WorkEntry>) : WorkEntryRepository {
    val entries = MutableStateFlow(initial)
    var replaceAllCalls = 0
    var replaceAllFailuresRemaining = 0

    override fun observeMonth(month: YearMonth): Flow<List<WorkEntry>> = entries.map { list ->
        list.filter { YearMonth.from(it.date) == month }
    }

    override fun observeDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<WorkEntry>> = entries.map { list ->
        list.filter { it.date in startDate..endDate }
    }

    override suspend fun getAll(): List<WorkEntry> = entries.value

    override suspend fun save(entry: WorkEntry) {
        entries.value = entries.value.filterNot { it.date == entry.date } + entry
    }

    override suspend fun delete(date: LocalDate) {
        entries.value = entries.value.filterNot { it.date == date }
    }

    override suspend fun restore(entries: List<WorkEntry>) {
        this.entries.value = this.entries.value.filterNot { old -> entries.any { it.date == old.date } } + entries
    }

    override suspend fun replaceAll(entries: List<WorkEntry>) {
        replaceAllCalls += 1
        if (replaceAllFailuresRemaining > 0) {
            replaceAllFailuresRemaining -= 1
            throw IllegalStateException("database")
        }
        this.entries.value = entries
    }

    override suspend fun updateHourlyRate(
        startDate: LocalDate,
        endDate: LocalDate,
        hourlyRateMicros: Long,
    ): List<WorkEntry> = emptyList()
}

private class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
    initialized: Boolean = false,
) : UserPreferencesRepository {
    private val values = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = values
    override val defaultRateInitialized: Flow<Boolean>
        get() = flowOf(this.initialized)
    var initialized = initialized
    var updateFailuresRemaining = 0
    var cancelUpdatesRemaining = 0
    val updates = mutableListOf<UserPreferences>()

    override suspend fun update(
        defaultHourlyRateMicros: Long,
        themeMode: ThemeMode,
        defaultRateInitialized: Boolean,
    ) {
        if (cancelUpdatesRemaining > 0) {
            cancelUpdatesRemaining -= 1
            throw CancellationException("cancelled")
        }
        if (updateFailuresRemaining > 0) {
            updateFailuresRemaining -= 1
            throw IllegalStateException("datastore")
        }
        val updated = UserPreferences(defaultHourlyRateMicros, themeMode)
        values.value = updated
        initialized = defaultRateInitialized
        updates += updated
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        values.value = values.value.copy(themeMode = themeMode)
    }

    override suspend fun updateDefaultHourlyRate(defaultHourlyRateMicros: Long) {
        values.value = values.value.copy(defaultHourlyRateMicros = defaultHourlyRateMicros)
        initialized = true
    }

    override suspend fun adoptDefaultHourlyRateIfUninitialized(defaultHourlyRateMicros: Long): Boolean = false
}
