package com.worktime.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

internal data class BackupDocumentActions(
    val exportBackup: () -> Unit,
    val exportCsv: () -> Unit,
    val importBackup: () -> Unit,
)

@Composable
internal fun rememberBackupDocumentActions(
    viewModel: BackupViewModel,
): BackupDocumentActions {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openOutputStream(uri) }
                .getOrNull()
                ?.let(viewModel::exportBackup)
                ?: viewModel.reportOperationError(BackupOperationError.EXPORT)
        }
    }
    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openOutputStream(uri) }
                .getOrNull()
                ?.let(viewModel::exportCsv)
                ?: viewModel.reportOperationError(BackupOperationError.EXPORT)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri) }
                .getOrNull()
                ?.let(viewModel::importBackup)
                ?: viewModel.reportOperationError(BackupOperationError.IMPORT)
        }
    }

    return remember(exportLauncher, csvExportLauncher, importLauncher) {
        BackupDocumentActions(
            exportBackup = {
                exportLauncher.launch("worktime-backup-${LocalDate.now()}.json")
            },
            exportCsv = {
                csvExportLauncher.launch("worktime-${LocalDate.now()}.csv")
            },
            importBackup = {
                importLauncher.launch(
                    arrayOf("application/json", "application/octet-stream", "text/plain"),
                )
            },
        )
    }
}
