package com.worktime.app.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worktime.app.AppContainer
import com.worktime.app.R
import com.worktime.app.ui.calendar.CalendarOperationError
import com.worktime.app.ui.calendar.CalendarOperationEvent
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarViewModel
import com.worktime.app.ui.components.AppMotion
import com.worktime.app.ui.dayeditor.DayEditorSheet
import com.worktime.app.ui.settings.ChangeRateSheet
import com.worktime.app.ui.settings.SettingsScreen
import com.worktime.app.ui.settings.YearSummaryScreen
import com.worktime.app.ui.theme.WorkTimeTheme
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun WorkTimeApp(
    container: AppContainer,
    openTodayRequest: Long = 0L,
) {
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.factory(
            workEntryRepository = container.workEntryRepository,
            userPreferencesRepository = container.userPreferencesRepository,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(viewModel, openTodayRequest) {
        if (openTodayRequest > 0L) {
            // The widget add action is explicit navigation, not a passive app reopen.
            // Clear transient overlays so the requested editor is actually visible.
            viewModel.dismissChangeRateSheet()
            viewModel.dismissYearSummary()
            viewModel.cancelImport()
            viewModel.selectDate(LocalDate.now())
        }
    }

    val entryDeletedMessage = stringResource(R.string.entry_deleted)
    val rateChangedMessage = stringResource(R.string.rate_changed)
    val noEntriesInPeriodMessage = stringResource(R.string.no_entries_in_period)
    val undoLabel = stringResource(R.string.undo)
    val undoFailedMessage = stringResource(R.string.undo_failed)
    val backupExportedMessage = stringResource(R.string.backup_exported)
    val backupImportedMessage = stringResource(R.string.backup_imported)
    val defaultRateAdoptionFailedMessage = stringResource(R.string.default_rate_adoption_failed)
    // Channel has no replay, so restarting this collector (e.g. on locale change)
    // cannot redisplay an already-consumed event.
    LaunchedEffect(
        viewModel,
        haptics,
        entryDeletedMessage,
        rateChangedMessage,
        noEntriesInPeriodMessage,
        undoLabel,
        undoFailedMessage,
        backupExportedMessage,
        backupImportedMessage,
        defaultRateAdoptionFailedMessage,
    ) {
        viewModel.operationEvents.collect { event ->
            when (event) {
                CalendarOperationEvent.Success.ENTRY_SAVED ->
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                CalendarOperationEvent.Success.ENTRY_DELETED -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    showUndoSnackbar(snackbarHostState, entryDeletedMessage, undoLabel, viewModel)
                }
                CalendarOperationEvent.Success.RATE_UPDATED -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    showUndoSnackbar(snackbarHostState, rateChangedMessage, undoLabel, viewModel)
                }
                CalendarOperationEvent.Success.NO_OP ->
                    snackbarHostState.showSnackbar(noEntriesInPeriodMessage)
                CalendarOperationEvent.Success.BACKUP_EXPORTED ->
                    snackbarHostState.showSnackbar(backupExportedMessage)
                CalendarOperationEvent.Success.BACKUP_IMPORTED ->
                    snackbarHostState.showSnackbar(backupImportedMessage)
                // Undo and default-rate adoption can surface after their originating sheet
                // has closed, so root feedback owns those errors.
                is CalendarOperationEvent.Error ->
                    when (event.kind) {
                        CalendarOperationError.UNDO ->
                            snackbarHostState.showSnackbar(undoFailedMessage, duration = SnackbarDuration.Long)
                        CalendarOperationError.DEFAULT_RATE_ADOPTION ->
                            snackbarHostState.showSnackbar(
                                defaultRateAdoptionFailedMessage,
                                duration = SnackbarDuration.Long,
                            )
                        else -> Unit
                    }
                else -> Unit
            }
        }
    }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openOutputStream(uri) }
                .getOrNull()
                ?.let(viewModel::exportBackup)
                ?: viewModel.reportOperationError(CalendarOperationError.BACKUP_EXPORT)
        }
    }
    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openOutputStream(uri) }
                .getOrNull()
                ?.let(viewModel::exportCsv)
                ?: viewModel.reportOperationError(CalendarOperationError.BACKUP_EXPORT)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri) }
                .getOrNull()
                ?.let(viewModel::importBackup)
                ?: viewModel.reportOperationError(CalendarOperationError.BACKUP_IMPORT)
        }
    }

    var settingsPredictiveExit by remember { mutableStateOf(false) }
    var yearSummaryPredictiveExit by remember { mutableStateOf(false) }
    LaunchedEffect(state.isSettingsOpen) {
        if (state.isSettingsOpen) settingsPredictiveExit = false
    }
    LaunchedEffect(state.isYearSummaryOpen) {
        if (state.isYearSummaryOpen) yearSummaryPredictiveExit = false
    }

    WorkTimeTheme(themeMode = state.themeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            CalendarScreen(
                state = state,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onSelectMonth = viewModel::showMonth,
                onDayClick = viewModel::selectDate,
                onSettingsClick = viewModel::openSettings,
                onOpenYearSummary = viewModel::openYearSummary,
            )

            state.selectedDate?.let { date ->
                val operationErrorMessage = when (state.operationError) {
                    CalendarOperationError.SAVE_ENTRY -> stringResource(R.string.save_entry_failed)
                    CalendarOperationError.DELETE_ENTRY -> stringResource(R.string.delete_entry_failed)
                    CalendarOperationError.BULK_RATE -> stringResource(R.string.bulk_rate_failed)
                    CalendarOperationError.UNDO -> stringResource(R.string.undo_failed)
                    else -> null
                }
                DayEditorSheet(
                    date = date,
                    existing = state.entries[date],
                    defaultHourlyRateMicros = state.defaultHourlyRateMicros,
                    operationErrorMessage = operationErrorMessage,
                    onDismiss = viewModel::dismissEditor,
                    onSave = viewModel::saveEntry,
                    onDelete = viewModel::deleteEntry,
                )
            }

            // Full-screen destinations stay fully opaque. Entry remains a restrained depth cue,
            // while exit travels completely off-screen before composition removes the destination;
            // partial-width exits visibly snapped away on device recordings.
            AnimatedVisibility(
                visible = state.isSettingsOpen,
                enter = slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = AppMotion.NoBounceDampingRatio,
                        stiffness = AppMotion.NavigationStiffness,
                    ),
                    initialOffsetX = { width -> width / 5 },
                ),
                exit = if (settingsPredictiveExit) {
                    ExitTransition.None
                } else {
                    slideOutHorizontally(
                        animationSpec = spring(
                            dampingRatio = AppMotion.NoBounceDampingRatio,
                            stiffness = AppMotion.NavigationStiffness,
                        ),
                        targetOffsetX = { width -> width },
                    )
                },
            ) {
                val operationErrorMessage = when (state.operationError) {
                    CalendarOperationError.SAVE_SETTINGS -> stringResource(R.string.save_settings_failed)
                    CalendarOperationError.BACKUP_EXPORT -> stringResource(R.string.backup_export_failed)
                    CalendarOperationError.BACKUP_IMPORT -> stringResource(R.string.backup_import_failed)
                    CalendarOperationError.BACKUP_IMPORT_ROLLBACK ->
                        stringResource(R.string.backup_import_rollback_failed)
                    else -> null
                }
                PredictiveBackLayer(
                    enabled = state.isSettingsOpen,
                    onPredictiveCommit = { settingsPredictiveExit = true },
                    onDismiss = viewModel::dismissSettings,
                ) {
                    SettingsScreen(
                        defaultHourlyRateMicros = state.defaultHourlyRateMicros,
                        themeMode = state.themeMode,
                        operationErrorMessage = operationErrorMessage,
                        onDismiss = viewModel::dismissSettings,
                        onThemeChange = viewModel::updateThemeMode,
                        onRateChange = viewModel::updateDefaultRate,
                        onOpenChangeRate = { viewModel.openChangeRate(null) },
                        onExportData = {
                            exportLauncher.launch("worktime-backup-" + LocalDate.now() + ".json")
                        },
                        onExportCsv = {
                            csvExportLauncher.launch("worktime-" + LocalDate.now() + ".csv")
                        },
                        onImportData = {
                            importLauncher.launch(
                                arrayOf("application/json", "application/octet-stream", "text/plain"),
                            )
                        },
                    )
                }
            }

            AnimatedVisibility(
                visible = state.isYearSummaryOpen,
                enter = slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = AppMotion.NoBounceDampingRatio,
                        stiffness = AppMotion.NavigationStiffness,
                    ),
                    initialOffsetX = { width -> width / 5 },
                ),
                exit = if (yearSummaryPredictiveExit) {
                    ExitTransition.None
                } else {
                    slideOutHorizontally(
                        animationSpec = spring(
                            dampingRatio = AppMotion.NoBounceDampingRatio,
                            stiffness = AppMotion.NavigationStiffness,
                        ),
                        targetOffsetX = { width -> width },
                    )
                },
            ) {
                PredictiveBackLayer(
                    enabled = state.isYearSummaryOpen,
                    onPredictiveCommit = { yearSummaryPredictiveExit = true },
                    onDismiss = viewModel::dismissYearSummary,
                ) {
                    YearSummaryScreen(
                        summary = state.yearSummary,
                        onDismiss = viewModel::dismissYearSummary,
                        onPreviousYear = viewModel::showPreviousYear,
                        onNextYear = viewModel::showNextYear,
                    )
                }
            }

            if (state.isChangeRateSheetOpen) {
                val operationErrorMessage = when (state.operationError) {
                    CalendarOperationError.BULK_RATE -> stringResource(R.string.bulk_rate_failed)
                    else -> null
                }
                ChangeRateSheet(
                    visibleMonth = state.visibleMonth,
                    initialRange = state.changeRateInitialRange,
                    operationErrorMessage = operationErrorMessage,
                    onDismiss = viewModel::dismissChangeRateSheet,
                    onChangeRate = viewModel::changeRateForPeriod,
                )
            }

            state.pendingImportCount?.let { pendingCount ->
                ImportConfirmationDialog(
                    pendingCount = pendingCount,
                    onConfirm = viewModel::confirmImport,
                    onDismiss = viewModel::cancelImport,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun PredictiveBackLayer(
    enabled: Boolean,
    onPredictiveCommit: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val backOffsetFraction = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(enabled) {
        if (enabled && backOffsetFraction.value != 0f) {
            backOffsetFraction.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = size.width * backOffsetFraction.value
            },
    ) {
        content()
    }

    // Composed after the destination's legacy BackHandler so this callback owns system back.
    // Three-button/hardware back keeps the existing spring exit; only an edge gesture seeks
    // the destination directly with progress and finishes the remaining travel on commit.
    PredictiveBackHandler(enabled = enabled) { progress ->
        var hasEdgeGesture = false
        try {
            progress.collect { backEvent ->
                if (backEvent.swipeEdge != BackEventCompat.EDGE_NONE) {
                    hasEdgeGesture = true
                    backOffsetFraction.snapTo(backEvent.progress)
                }
            }
            if (hasEdgeGesture) {
                backOffsetFraction.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = AppMotion.NoBounceDampingRatio,
                        stiffness = AppMotion.NavigationStiffness,
                    ),
                )
                onPredictiveCommit()
            }
            onDismiss()
        } catch (cancellation: CancellationException) {
            if (hasEdgeGesture) {
                scope.launch {
                    backOffsetFraction.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = AppMotion.NoBounceDampingRatio,
                            stiffness = AppMotion.NavigationStiffness,
                        ),
                    )
                }
            }
            throw cancellation
        }
    }
}

private suspend fun showUndoSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
    actionLabel: String,
    viewModel: CalendarViewModel,
) {
    val result = snackbarHostState.showSnackbar(
        message = message,
        actionLabel = actionLabel,
        duration = SnackbarDuration.Long,
    )
    if (result == SnackbarResult.ActionPerformed) {
        viewModel.undoLastOperation()
    }
}

@Composable
private fun ImportConfirmationDialog(
    pendingCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_confirmation_title)) },
        text = {
            Text(pluralStringResource(R.plurals.import_confirmation_text, pendingCount, pendingCount))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.replace)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
