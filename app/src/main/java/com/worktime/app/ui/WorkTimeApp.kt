package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worktime.app.AppContainer
import com.worktime.app.R
import com.worktime.app.ui.calendar.CalendarOperationError
import com.worktime.app.ui.calendar.CalendarOperationEvent
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarViewModel
import com.worktime.app.ui.dayeditor.DayEditorSheet
import com.worktime.app.ui.settings.ChangeRateSheet
import com.worktime.app.ui.settings.SettingsSheet
import com.worktime.app.ui.theme.WorkTimeTheme

@Composable
fun WorkTimeApp(container: AppContainer) {
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.factory(
            workEntryRepository = container.workEntryRepository,
            userPreferencesRepository = container.userPreferencesRepository,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var previewTheme by remember { mutableStateOf(state.themeMode) }

    LaunchedEffect(state.isSettingsOpen, state.themeMode) {
        previewTheme = state.themeMode
    }

    val entryDeletedMessage = stringResource(R.string.entry_deleted)
    val rateChangedMessage = stringResource(R.string.rate_changed)
    val undoLabel = stringResource(R.string.undo)
    val undoFailedMessage = stringResource(R.string.undo_failed)
    // Channel has no replay, so restarting this collector (e.g. on locale change)
    // cannot redisplay an already-consumed event.
    LaunchedEffect(viewModel, entryDeletedMessage, rateChangedMessage, undoLabel, undoFailedMessage) {
        viewModel.operationEvents.collect { event ->
            when (event) {
                CalendarOperationEvent.Success.ENTRY_DELETED ->
                    showUndoSnackbar(snackbarHostState, entryDeletedMessage, undoLabel, viewModel)
                CalendarOperationEvent.Success.RATE_UPDATED ->
                    showUndoSnackbar(snackbarHostState, rateChangedMessage, undoLabel, viewModel)
                // Undo fires from a consumed root snackbar after every sheet is gone,
                // so it is the only error with no owning surface to display it.
                CalendarOperationEvent.Error.UNDO ->
                    snackbarHostState.showSnackbar(undoFailedMessage, duration = SnackbarDuration.Long)
                else -> Unit
            }
        }
    }

    WorkTimeTheme(
        themeMode = if (state.isSettingsOpen) previewTheme else state.themeMode,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CalendarScreen(
                state = state,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onDayClick = viewModel::selectDate,
                onSettingsClick = viewModel::openSettings,
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

            if (state.isSettingsOpen) {
                val operationErrorMessage = when (state.operationError) {
                    CalendarOperationError.SAVE_SETTINGS -> stringResource(R.string.save_settings_failed)
                    else -> null
                }
                SettingsSheet(
                    defaultHourlyRateMicros = state.defaultHourlyRateMicros,
                    themeMode = state.themeMode,
                    operationErrorMessage = operationErrorMessage,
                    onDismiss = viewModel::dismissSettings,
                    onSave = viewModel::updatePreferences,
                    onPreviewTheme = { previewTheme = it },
                    onChangeRateForPeriod = viewModel::openChangeRateSheet,
                )
            }

            if (state.isChangeRateSheetOpen) {
                val operationErrorMessage = when (state.operationError) {
                    CalendarOperationError.BULK_RATE -> stringResource(R.string.bulk_rate_failed)
                    else -> null
                }
                ChangeRateSheet(
                    visibleMonth = state.visibleMonth,
                    operationErrorMessage = operationErrorMessage,
                    onDismiss = viewModel::dismissChangeRateSheet,
                    onChangeRate = viewModel::changeRateForPeriod,
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
