package com.worktime.app.ui.backup

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worktime.app.domain.backup.BackupDocumentSerializer
import com.worktime.app.domain.backup.BackupPayload
import com.worktime.app.domain.operation.DataMutationCoordinator
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository
import java.io.InputStream
import java.io.OutputStream
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class BackupUiState(
    val pendingImportCount: Int? = null,
    val error: BackupOperationError? = null,
    val lastExportAtMillis: Long? = null,
    val lastExportEntryCount: Int? = null,
)

internal enum class BackupOperationError {
    EXPORT,
    IMPORT,
    IMPORT_ROLLBACK,
}

internal sealed interface BackupOperationEvent {
    enum class Success : BackupOperationEvent {
        EXPORTED,
        IMPORTED,
    }

    data class Error(val kind: BackupOperationError) : BackupOperationEvent
}

internal class BackupViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val backupDocumentSerializer: BackupDocumentSerializer,
    private val dataMutationCoordinator: DataMutationCoordinator = DataMutationCoordinator(),
    private val metadataPreferences: SharedPreferences? = null,
    private val internalBackupFile: File? = null,
) : ViewModel() {
    private val pendingImport = MutableStateFlow<BackupPayload?>(null)
    private val operationError = MutableStateFlow<BackupOperationError?>(null)
    private val lastExportAtMillis = MutableStateFlow(metadataPreferences?.getLong(KEY_LAST_EXPORT_AT, 0L)?.takeIf { it > 0L })
    private val lastExportEntryCount = MutableStateFlow(metadataPreferences?.getInt(KEY_LAST_EXPORT_COUNT, -1)?.takeIf { it >= 0 })
    private var operationGeneration = 0L
    private val _events = Channel<BackupOperationEvent>(Channel.BUFFERED)
    val events: Flow<BackupOperationEvent> = _events.receiveAsFlow()

    val state: StateFlow<BackupUiState> = combine(
        pendingImport,
        operationError,
        lastExportAtMillis,
        lastExportEntryCount,
    ) { pending, error, exportedAt, exportedCount ->
        BackupUiState(
            pendingImportCount = pending?.entries?.size,
            error = error,
            lastExportAtMillis = exportedAt,
            lastExportEntryCount = exportedCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = BackupUiState(),
    )

    fun exportBackup(stream: OutputStream) {
        var exportedCount = 0
        runOperation(BackupOperationError.EXPORT, body = {
            withContext(Dispatchers.IO) {
                val entries = workEntryRepository.getAll()
                exportedCount = entries.size
                val encoded = backupDocumentSerializer.encodeBackup(
                    entries = entries,
                    preferences = userPreferencesRepository.preferences.first(),
                    defaultRateInitialized = userPreferencesRepository.defaultRateInitialized.first(),
                )
                stream.use { it.write(encoded.encodeToByteArray()) }
            }
        }, onSuccess = {
            recordExport(exportedCount)
            _events.send(BackupOperationEvent.Success.EXPORTED)
        })
    }

    fun exportCsv(stream: OutputStream) {
        var exportedCount = 0
        runOperation(BackupOperationError.EXPORT, body = {
            withContext(Dispatchers.IO) {
                val entries = workEntryRepository.getAll()
                exportedCount = entries.size
                val encoded = backupDocumentSerializer.encodeCsv(entries)
                stream.use { it.write(encoded.encodeToByteArray()) }
            }
        }, onSuccess = {
            recordExport(exportedCount)
            _events.send(BackupOperationEvent.Success.EXPORTED)
        })
    }

    fun importBackup(stream: InputStream) {
        runOperation(BackupOperationError.IMPORT, body = {
            pendingImport.value = withContext(Dispatchers.IO) {
                stream.use { input ->
                    val bytes = input.readBounded(backupDocumentSerializer.maxBackupSizeBytes)
                    backupDocumentSerializer.decodeBackup(bytes.decodeToString())
                }
            }
        })
    }

    fun confirmImport() {
        val data = pendingImport.value ?: return
        runOperation(BackupOperationError.IMPORT, supersede = true, body = {
            val oldEntries = workEntryRepository.getAll()
            val oldPreferences = userPreferencesRepository.preferences.first()
            val oldDefaultRateInitialized = userPreferencesRepository.defaultRateInitialized.first()
            saveInternalBackup(oldEntries, oldPreferences, oldDefaultRateInitialized)
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
            pendingImport.value = null
            _events.send(BackupOperationEvent.Success.IMPORTED)
        })
    }

    fun cancelImport() {
        operationError.value = null
        pendingImport.value = null
    }

    fun reportOperationError(error: BackupOperationError) {
        operationError.value = error
        _events.trySend(BackupOperationEvent.Error(error))
    }

    private fun recordExport(entryCount: Int) {
        val exportedAt = System.currentTimeMillis()
        lastExportAtMillis.value = exportedAt
        lastExportEntryCount.value = entryCount
        metadataPreferences?.edit()
            ?.putLong(KEY_LAST_EXPORT_AT, exportedAt)
            ?.putInt(KEY_LAST_EXPORT_COUNT, entryCount)
            ?.apply()
    }

    private suspend fun saveInternalBackup(
        entries: List<com.worktime.app.domain.model.WorkEntry>,
        preferences: com.worktime.app.domain.preferences.UserPreferences,
        defaultRateInitialized: Boolean,
    ) {
        val file = internalBackupFile ?: return
        val encoded = backupDocumentSerializer.encodeBackup(entries, preferences, defaultRateInitialized)
        withContext(Dispatchers.IO) {
            val temporaryFile = File(file.parentFile, "${file.name}.tmp")
            temporaryFile.writeText(encoded)
            temporaryFile.copyTo(file, overwrite = true)
            check(temporaryFile.delete()) { "Could not clean internal backup temporary file" }
        }
    }

    private fun runOperation(
        errorKind: BackupOperationError,
        body: suspend () -> Unit,
        onSuccess: suspend () -> Unit = {},
        supersede: Boolean = false,
    ) {
        operationError.value = null
        if (supersede) operationGeneration += 1
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                dataMutationCoordinator.run { body() }
                if (generation == operationGeneration) onSuccess()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == operationGeneration) {
                    val actualError = if (error is ImportRollbackException) {
                        BackupOperationError.IMPORT_ROLLBACK
                    } else {
                        errorKind
                    }
                    operationError.value = actualError
                    _events.send(BackupOperationEvent.Error(actualError))
                }
            }
        }
    }

    companion object {
        private const val KEY_LAST_EXPORT_AT = "last_export_at_millis"
        private const val KEY_LAST_EXPORT_COUNT = "last_export_entry_count"
        fun factory(
            workEntryRepository: WorkEntryRepository,
            userPreferencesRepository: UserPreferencesRepository,
            backupDocumentSerializer: BackupDocumentSerializer,
            dataMutationCoordinator: DataMutationCoordinator = DataMutationCoordinator(),
            metadataPreferences: SharedPreferences? = null,
            internalBackupFile: File? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BackupViewModel(
                    workEntryRepository = workEntryRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    backupDocumentSerializer = backupDocumentSerializer,
                    dataMutationCoordinator = dataMutationCoordinator,
                    metadataPreferences = metadataPreferences,
                    internalBackupFile = internalBackupFile,
                )
            }
        }
    }

    private class ImportRollbackException(cause: Throwable) : RuntimeException(
        "Import rollback failed",
        cause,
    )
}

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
