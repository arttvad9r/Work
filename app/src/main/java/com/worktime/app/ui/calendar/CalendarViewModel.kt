package com.worktime.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worktime.app.data.backup.BackupCodec
import com.worktime.app.data.backup.BackupData
import com.worktime.app.data.backup.WorkEntryCsv
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val changeRateSheetOpen = MutableStateFlow(false)
    private val changeRateInitialRange = MutableStateFlow<ClosedRange<LocalDate>?>(null)
    private val operationError = MutableStateFlow<CalendarOperationError?>(null)
    private val undoSnapshot = MutableStateFlow<UndoSnapshot?>(null)
    private val pendingImport = MutableStateFlow<BackupData?>(null)
    private var operationGeneration = 0L
    private val operationMutex = Mutex()
    private val _operationEvents = Channel<CalendarOperationEvent>(Channel.BUFFERED)
    val operationEvents: Flow<CalendarOperationEvent> = _operationEvents.receiveAsFlow()

    /**
     * Keep the visible month and both neighbours under one Room observation. A horizontal
     * pager can therefore reveal either adjacent page immediately while the next window
     * subscription is being established after the page settles.
     */
    private val monthWindow = visibleMonth.flatMapLatest { center ->
        val start = center.minusMonths(1).atDay(1)
        val end = center.plusMonths(1).atEndOfMonth()
        workEntryRepository.observeDateRange(start, end).map { rows ->
            MonthWindow(
                center = center,
                entriesByMonth = rows
                    .groupBy { YearMonth.from(it.date) }
                    .mapValues { (_, entries) -> entries.associateBy(WorkEntry::date) },
            )
        }
    }

    private val visibleMonthEntries = combine(
        visibleMonth,
        monthWindow,
    ) { requestedMonth, window ->
        MonthUi(
            requestedMonth = requestedMonth,
            entries = window.entriesByMonth[requestedMonth].orEmpty(),
            entriesByMonth = window.entriesByMonth,
        )
    }

    private val baseState = combine(
        visibleMonthEntries,
        userPreferencesRepository.preferences,
        selectedDate,
        operationError,
    ) { monthUi, preferences, selected, error ->
        CalendarUiState(
            visibleMonth = monthUi.requestedMonth,
            entries = monthUi.entries,
            monthEntries = monthUi.entriesByMonth,
            selectedDate = selected,
            defaultHourlyRateMicros = preferences.defaultHourlyRateMicros,
            themeMode = preferences.themeMode,
            isReady = true,
            operationError = error,
        )
    }

    private data class ChangeRateUi(
        val open: Boolean,
        val initialRange: ClosedRange<LocalDate>?,
    )

    private val changeRateUi = combine(
        changeRateSheetOpen,
        changeRateInitialRange,
    ) { open, range -> ChangeRateUi(open, range) }

    private val overlayState = combine(
        changeRateUi,
        undoSnapshot,
        pendingImport,
    ) { changeRate, snapshot, import ->
        OverlayState(
            isChangeRateSheetOpen = changeRate.open,
            changeRateInitialRange = changeRate.initialRange,
            canUndo = snapshot != null,
            pendingImportCount = import?.entries?.size,
        )
    }

    private data class OverlayState(
        val isChangeRateSheetOpen: Boolean,
        val changeRateInitialRange: ClosedRange<LocalDate>?,
        val canUndo: Boolean,
        val pendingImportCount: Int?,
    )

    val state: StateFlow<CalendarUiState> = combine(
        baseState,
        overlayState,
    ) { base, overlay ->
        base.copy(
            isChangeRateSheetOpen = overlay.isChangeRateSheetOpen,
            changeRateInitialRange = overlay.changeRateInitialRange,
            canUndo = overlay.canUndo,
            pendingImportCount = overlay.pendingImportCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = CalendarUiState(),
    )

    fun previousMonth() = visibleMonth.update { it.minusMonths(1) }

    fun nextMonth() = visibleMonth.update { it.plusMonths(1) }

    fun showMonth(month: YearMonth) = visibleMonth.update { month }

    fun selectDate(date: LocalDate) {
        operationError.value = null
        selectedDate.value = date
    }

    fun dismissEditor() {
        operationError.value = null
        selectedDate.value = null
    }

    fun openChangeRate(range: ClosedRange<LocalDate>?) {
        operationError.value = null
        selectedDate.value = null
        changeRateInitialRange.value = range
        changeRateSheetOpen.value = true
    }

    fun dismissChangeRateSheet() {
        operationError.value = null
        changeRateSheetOpen.value = false
        changeRateInitialRange.value = null
    }

    fun saveEntry(entry: WorkEntry) {
        runOperation(CalendarOperationError.SAVE_ENTRY, body = {
            workEntryRepository.save(entry)
            selectedDate.value = null
            try {
                adoptDefaultRateFrom(entry)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                reportOperationError(CalendarOperationError.DEFAULT_RATE_ADOPTION)
            }
        }, onSuccess = {
            _operationEvents.send(CalendarOperationEvent.Success.ENTRY_SAVED)
        })
    }

    // First ever entry sets the default rate; an existing default is never overwritten.
    private suspend fun adoptDefaultRateFrom(entry: WorkEntry) {
        if (entry.hourlyRateMicros <= 0L) return
        userPreferencesRepository.adoptDefaultHourlyRateIfUninitialized(entry.hourlyRateMicros)
    }

    fun deleteEntry(date: LocalDate) {
        val deletedEntry = state.value.entries[date]
        runOperation(CalendarOperationError.DELETE_ENTRY, body = {
            workEntryRepository.delete(date)
            selectedDate.value = null
        }, onSuccess = {
            if (deletedEntry != null) undoSnapshot.value = UndoSnapshot.Deleted(deletedEntry)
            _operationEvents.send(CalendarOperationEvent.Success.ENTRY_DELETED)
        })
    }

    fun changeRateForPeriod(startDate: LocalDate, endDate: LocalDate, newRateMicros: Long) {
        operationError.value = null
        if (startDate > endDate || newRateMicros <= 0L) {
            operationError.value = CalendarOperationError.BULK_RATE
            _operationEvents.trySend(CalendarOperationEvent.Error(CalendarOperationError.BULK_RATE))
            return
        }
        var originals: List<WorkEntry> = emptyList()
        runOperation(CalendarOperationError.BULK_RATE, body = {
            originals = workEntryRepository.updateHourlyRate(startDate, endDate, newRateMicros)
        }, onSuccess = {
            if (originals.isEmpty()) {
                _operationEvents.send(CalendarOperationEvent.Success.NO_OP)
            } else {
                undoSnapshot.value = UndoSnapshot.Bulk(originals)
                _operationEvents.send(CalendarOperationEvent.Success.RATE_UPDATED)
            }
            changeRateSheetOpen.value = false
            changeRateInitialRange.value = null
        })
    }

    fun undoLastOperation() {
        val snapshot = undoSnapshot.value ?: return
        runOperation(CalendarOperationError.UNDO, body = {
            when (snapshot) {
                is UndoSnapshot.Deleted -> workEntryRepository.restore(listOf(snapshot.entry))
                is UndoSnapshot.Bulk -> workEntryRepository.restore(snapshot.entries)
            }
        }, onSuccess = {
            undoSnapshot.value = null
            _operationEvents.send(CalendarOperationEvent.Success.OPERATION_UNDONE)
        }, invalidateUndo = false)
    }

    /** Autosaves the theme as soon as the user picks it; settings stay open. */
    fun updateThemeMode(themeMode: ThemeMode) {
        operationError.value = null
        viewModelScope.launch {
            try {
                operationMutex.withLock {
                    userPreferencesRepository.updateThemeMode(themeMode)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                operationError.value = CalendarOperationError.SAVE_SETTINGS
            }
        }
    }

    /** Autosaves a valid default rate as soon as it is entered; settings stay open. */
    fun updateDefaultRate(defaultHourlyRateMicros: Long) {
        operationError.value = null
        viewModelScope.launch {
            try {
                operationMutex.withLock {
                    userPreferencesRepository.updateDefaultHourlyRate(defaultHourlyRateMicros)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                operationError.value = CalendarOperationError.SAVE_SETTINGS
            }
        }
    }

    fun exportBackup(stream: OutputStream) {
        runOperation(CalendarOperationError.BACKUP_EXPORT, body = {
            withContext(Dispatchers.IO) {
                val data = BackupData(
                    entries = workEntryRepository.getAll(),
                    preferences = userPreferencesRepository.preferences.first(),
                    defaultRateInitialized = userPreferencesRepository.defaultRateInitialized.first(),
                )
                stream.use {
                    it.write(
                        BackupCodec.encode(
                            data.entries,
                            data.preferences,
                            data.defaultRateInitialized,
                        ).toByteArray(),
                    )
                }
            }
        }, onSuccess = {
            _operationEvents.send(CalendarOperationEvent.Success.BACKUP_EXPORTED)
        }, invalidateUndo = false)
    }

    fun exportCsv(stream: OutputStream) {
        runOperation(CalendarOperationError.BACKUP_EXPORT, body = {
            withContext(Dispatchers.IO) {
                stream.use { it.write(WorkEntryCsv.encode(workEntryRepository.getAll()).toByteArray()) }
            }
        }, onSuccess = {
            _operationEvents.send(CalendarOperationEvent.Success.BACKUP_EXPORTED)
        }, invalidateUndo = false)
    }

    fun importBackup(stream: InputStream) {
        runOperation(CalendarOperationError.BACKUP_IMPORT, body = {
            pendingImport.value = withContext(Dispatchers.IO) {
                stream.use { input ->
                    val bytes = input.readBounded(BackupCodec.MAX_BACKUP_SIZE_BYTES)
                    BackupCodec.decode(bytes.decodeToString())
                }
            }
        }, invalidateUndo = false)
    }

    fun confirmImport() {
        val data = pendingImport.value ?: return
        runOperation(CalendarOperationError.BACKUP_IMPORT, body = {
            val oldEntries = workEntryRepository.getAll()
            val oldPreferences = userPreferencesRepository.preferences.first()
            val oldDefaultRateInitialized = userPreferencesRepository.defaultRateInitialized.first()
            var replaced = false
            try {
                workEntryRepository.replaceAll(data.entries)
                replaced = true
                userPreferencesRepository.update(
                    defaultHourlyRateMicros = data.preferences.defaultHourlyRateMicros,
                    themeMode = data.preferences.themeMode,
                    defaultRateInitialized = data.defaultRateInitialized,
                )
            } catch (error: Exception) {
                if (!replaced) throw error
                try {
                    withContext(NonCancellable) {
                        workEntryRepository.replaceAll(oldEntries)
                        userPreferencesRepository.update(
                            defaultHourlyRateMicros = oldPreferences.defaultHourlyRateMicros,
                            themeMode = oldPreferences.themeMode,
                            defaultRateInitialized = oldDefaultRateInitialized,
                        )
                    }
                } catch (rollbackError: Throwable) {
                    throw ImportRollbackException(rollbackError)
                }
                throw error
            }
        }, onSuccess = {
            // Cleared only on success so a failed replace can be retried.
            pendingImport.value = null
            _operationEvents.send(CalendarOperationEvent.Success.BACKUP_IMPORTED)
        })
    }

    /**
     * Runs one repository operation: clears stale errors, invalidates prior undo state,
     * and reports failure as [errorKind] unless a newer operation superseded this one.
     * Undo opts out of invalidating: a failed undo must stay retryable.
     */
    private fun runOperation(
        errorKind: CalendarOperationError,
        body: suspend () -> Unit,
        onSuccess: suspend () -> Unit = {},
        invalidateUndo: Boolean = true,
    ) {
        operationError.value = null
        if (invalidateUndo) supersedeOperation()
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                operationMutex.withLock { body() }
                if (generation == operationGeneration) onSuccess()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == operationGeneration) {
                    val actualError = if (error is ImportRollbackException) {
                        CalendarOperationError.BACKUP_IMPORT_ROLLBACK
                    } else {
                        errorKind
                    }
                    operationError.value = actualError
                    _operationEvents.send(CalendarOperationEvent.Error(actualError))
                }
            }
        }
    }

    fun cancelImport() {
        operationError.value = null
        pendingImport.value = null
    }

    fun reportOperationError(error: CalendarOperationError) {
        operationError.value = error
        _operationEvents.trySend(CalendarOperationEvent.Error(error))
    }

    private fun supersedeOperation() {
        operationGeneration++
        undoSnapshot.value = null
    }

    companion object {
        fun factory(
            workEntryRepository: WorkEntryRepository,
            userPreferencesRepository: UserPreferencesRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { CalendarViewModel(workEntryRepository, userPreferencesRepository) }
        }
    }

    private sealed interface UndoSnapshot {
        data class Deleted(val entry: WorkEntry) : UndoSnapshot
        data class Bulk(val entries: List<WorkEntry>) : UndoSnapshot
    }

    private class ImportRollbackException(cause: Throwable) : RuntimeException(
        "Import rollback failed",
        cause,
    )
}

private data class MonthWindow(
    val center: YearMonth,
    val entriesByMonth: Map<YearMonth, Map<LocalDate, WorkEntry>>,
)

private data class MonthUi(
    val requestedMonth: YearMonth,
    val entries: Map<LocalDate, WorkEntry>,
    val entriesByMonth: Map<YearMonth, Map<LocalDate, WorkEntry>>,
)

private fun InputStream.readBounded(maxBytes: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) return output.toByteArray()
        total += read
        if (total > maxBytes) throw IllegalArgumentException("Backup file is too large")
        output.write(buffer, 0, read)
    }
}
