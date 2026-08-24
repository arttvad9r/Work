package com.worktime.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worktime.app.data.backup.BackupCodec
import com.worktime.app.data.backup.BackupData
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val settingsOpen = MutableStateFlow(false)
    private val changeRateSheetOpen = MutableStateFlow(false)
    private val operationError = MutableStateFlow<CalendarOperationError?>(null)
    private val undoSnapshot = MutableStateFlow<UndoSnapshot?>(null)
    private val pendingImport = MutableStateFlow<BackupData?>(null)
    private var operationGeneration = 0L
    private val _operationEvents = Channel<CalendarOperationEvent>(Channel.BUFFERED)
    val operationEvents: Flow<CalendarOperationEvent> = _operationEvents.receiveAsFlow()

    private val monthSnapshot = visibleMonth.flatMapLatest { month ->
        workEntryRepository.observeMonth(month).map { entries -> month to entries }
    }

    private val visibleMonthEntries = combine(
        visibleMonth,
        monthSnapshot,
    ) { requestedMonth, monthAndEntries ->
        val (loadedMonth, loadedEntries) = monthAndEntries
        requestedMonth to if (loadedMonth == requestedMonth) loadedEntries else emptyList()
    }

    private val baseState = combine(
        visibleMonthEntries,
        userPreferencesRepository.preferences,
        selectedDate,
        settingsOpen,
        operationError,
    ) { monthAndEntries, preferences, selected, isSettingsOpen, error ->
        val (requestedMonth, entries) = monthAndEntries
        CalendarUiState(
            // Navigation updates immediately on arrow press. Room can emit the new
            // month's rows a moment later without holding the title/date grid back.
            visibleMonth = requestedMonth,
            entries = entries.associateBy(WorkEntry::date),
            selectedDate = selected,
            defaultHourlyRateMicros = preferences.defaultHourlyRateMicros,
            themeMode = preferences.themeMode,
            isSettingsOpen = isSettingsOpen,
            isReady = true,
            operationError = error,
        )
    }

    val state: StateFlow<CalendarUiState> = combine(
        baseState,
        changeRateSheetOpen,
        undoSnapshot,
        pendingImport,
    ) { base, isChangeRateSheetOpen, snapshot, import ->
        base.copy(
            isChangeRateSheetOpen = isChangeRateSheetOpen,
            canUndo = snapshot != null,
            pendingImportCount = import?.entries?.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = CalendarUiState(),
    )

    fun previousMonth() = visibleMonth.update { it.minusMonths(1) }

    fun nextMonth() = visibleMonth.update { it.plusMonths(1) }

    fun selectDate(date: LocalDate) {
        operationError.value = null
        settingsOpen.value = false
        selectedDate.value = date
    }

    fun dismissEditor() {
        operationError.value = null
        selectedDate.value = null
    }

    fun openSettings() {
        operationError.value = null
        selectedDate.value = null
        settingsOpen.value = true
    }

    fun dismissSettings() {
        operationError.value = null
        settingsOpen.value = false
    }

    fun openChangeRateSheet() {
        operationError.value = null
        selectedDate.value = null
        settingsOpen.value = false
        changeRateSheetOpen.value = true
    }

    fun dismissChangeRateSheet() {
        operationError.value = null
        changeRateSheetOpen.value = false
    }

    fun saveEntry(entry: WorkEntry) {
        operationError.value = null
        supersedeOperation()
        viewModelScope.launch {
            try {
                workEntryRepository.save(entry)
                adoptDefaultRateFrom(entry)
                selectedDate.value = null
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                operationError.value = CalendarOperationError.SAVE_ENTRY
            }
        }
    }

    // First ever entry sets the default rate; an existing default is never overwritten.
    private suspend fun adoptDefaultRateFrom(entry: WorkEntry) {
        if (entry.hourlyRateMicros <= 0L) return
        val preferences = userPreferencesRepository.preferences.first()
        if (preferences.defaultHourlyRateMicros == 0L) {
            userPreferencesRepository.update(
                defaultHourlyRateMicros = entry.hourlyRateMicros,
                themeMode = preferences.themeMode,
            )
        }
    }

    fun deleteEntry(date: LocalDate) {
        operationError.value = null
        val deletedEntry = state.value.entries[date]
        supersedeOperation()
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                workEntryRepository.delete(date)
                selectedDate.value = null
                if (generation == operationGeneration) {
                    if (deletedEntry != null) undoSnapshot.value = UndoSnapshot.Deleted(deletedEntry)
                    _operationEvents.send(CalendarOperationEvent.Success.ENTRY_DELETED)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == operationGeneration) {
                    operationError.value = CalendarOperationError.DELETE_ENTRY
                    _operationEvents.send(CalendarOperationEvent.Error.DELETE_ENTRY)
                }
            }
        }
    }

    fun changeRateForPeriod(startDate: LocalDate, endDate: LocalDate, newRateMicros: Long) {
        operationError.value = null
        supersedeOperation()
        if (startDate > endDate || newRateMicros <= 0L) {
            operationError.value = CalendarOperationError.BULK_RATE
            _operationEvents.trySend(CalendarOperationEvent.Error.BULK_RATE)
            return
        }
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                val originals = workEntryRepository.updateHourlyRate(startDate, endDate, newRateMicros)
                if (generation == operationGeneration) {
                    if (originals.isEmpty()) {
                        _operationEvents.send(CalendarOperationEvent.Success.NO_OP)
                    } else {
                        undoSnapshot.value = UndoSnapshot.Bulk(originals)
                        _operationEvents.send(CalendarOperationEvent.Success.RATE_UPDATED)
                    }
                    changeRateSheetOpen.value = false
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == operationGeneration) {
                    operationError.value = CalendarOperationError.BULK_RATE
                    _operationEvents.send(CalendarOperationEvent.Error.BULK_RATE)
                }
            }
        }
    }

    fun undoLastOperation() {
        operationError.value = null
        val snapshot = undoSnapshot.value ?: return
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                when (snapshot) {
                    is UndoSnapshot.Deleted -> workEntryRepository.restore(listOf(snapshot.entry))
                    is UndoSnapshot.Bulk -> workEntryRepository.restore(snapshot.entries)
                }
                if (generation == operationGeneration) {
                    undoSnapshot.value = null
                    _operationEvents.send(CalendarOperationEvent.Success.OPERATION_UNDONE)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == operationGeneration) {
                    operationError.value = CalendarOperationError.UNDO
                    _operationEvents.send(CalendarOperationEvent.Error.UNDO)
                }
            }
        }
    }

    fun updatePreferences(
        defaultHourlyRateMicros: Long,
        themeMode: ThemeMode,
    ) {
        operationError.value = null
        supersedeOperation()
        viewModelScope.launch {
            try {
                userPreferencesRepository.update(
                    defaultHourlyRateMicros = defaultHourlyRateMicros,
                    themeMode = themeMode,
                )
                settingsOpen.value = false
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                operationError.value = CalendarOperationError.SAVE_SETTINGS
            }
        }
    }

    fun exportBackup(stream: OutputStream) {
        operationError.value = null
        supersedeOperation()
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                val data = BackupData(
                    entries = workEntryRepository.getAll(),
                    preferences = userPreferencesRepository.preferences.first(),
                )
                stream.use { it.write(BackupCodec.encode(data.entries, data.preferences).toByteArray()) }
                if (generation == operationGeneration) {
                    _operationEvents.send(CalendarOperationEvent.Success.BACKUP_EXPORTED)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == operationGeneration) {
                    operationError.value = CalendarOperationError.BACKUP_EXPORT
                    _operationEvents.send(CalendarOperationEvent.Error.BACKUP_EXPORT)
                }
            }
        }
    }

    fun importBackup(stream: InputStream) {
        operationError.value = null
        supersedeOperation()
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                val data = BackupCodec.decode(stream.use { it.readBytes().decodeToString() })
                if (generation == operationGeneration) {
                    pendingImport.value = data
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == operationGeneration) {
                    operationError.value = CalendarOperationError.BACKUP_IMPORT
                    _operationEvents.send(CalendarOperationEvent.Error.BACKUP_IMPORT)
                }
            }
        }
    }

    fun confirmImport() {
        operationError.value = null
        val data = pendingImport.value ?: return
        supersedeOperation()
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                workEntryRepository.replaceAll(data.entries)
                userPreferencesRepository.update(
                    defaultHourlyRateMicros = data.preferences.defaultHourlyRateMicros,
                    themeMode = data.preferences.themeMode,
                )
                if (generation == operationGeneration) {
                    pendingImport.value = null
                    _operationEvents.send(CalendarOperationEvent.Success.BACKUP_IMPORTED)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == operationGeneration) {
                    // Keep the parsed backup so the user can retry the replace.
                    operationError.value = CalendarOperationError.BACKUP_IMPORT
                    _operationEvents.send(CalendarOperationEvent.Error.BACKUP_IMPORT)
                }
            }
        }
    }

    fun cancelImport() {
        operationError.value = null
        pendingImport.value = null
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
}
