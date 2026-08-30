package com.worktime.app.domain.backup

import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.UserPreferences

data class BackupPayload(
    val entries: List<WorkEntry>,
    val preferences: UserPreferences,
    val defaultRateInitialized: Boolean,
)

/**
 * Domain-facing port for user-controlled backup/import documents.
 *
 * JSON/CSV implementation details stay in the data layer. UI state holders depend only on
 * this contract and domain models, keeping file-format infrastructure behind an explicit boundary.
 */
interface BackupDocumentSerializer {
    val maxBackupSizeBytes: Int

    fun encodeBackup(
        entries: List<WorkEntry>,
        preferences: UserPreferences,
        defaultRateInitialized: Boolean,
    ): String

    fun decodeBackup(text: String): BackupPayload

    fun encodeCsv(entries: List<WorkEntry>): String
}
