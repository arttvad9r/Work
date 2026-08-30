package com.worktime.app.data.backup

import com.worktime.app.domain.backup.BackupDocumentSerializer
import com.worktime.app.domain.backup.BackupPayload
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.UserPreferences

class DefaultBackupDocumentSerializer : BackupDocumentSerializer {
    override val maxBackupSizeBytes: Int = BackupCodec.MAX_BACKUP_SIZE_BYTES

    override fun encodeBackup(
        entries: List<WorkEntry>,
        preferences: UserPreferences,
        defaultRateInitialized: Boolean,
    ): String = BackupCodec.encode(
        entries = entries,
        preferences = preferences,
        defaultRateInitialized = defaultRateInitialized,
    )

    override fun decodeBackup(text: String): BackupPayload = BackupCodec.decode(text)

    override fun encodeCsv(entries: List<WorkEntry>): String = WorkEntryCsv.encode(entries)
}
