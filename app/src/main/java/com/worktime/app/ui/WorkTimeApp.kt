package com.worktime.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worktime.app.AppContainer
import com.worktime.app.R
import com.worktime.app.ui.calendar.CalendarOperationError
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarViewModel
import com.worktime.app.ui.dayeditor.DayEditorSheet
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
    var previewTheme by remember { mutableStateOf(state.themeMode) }

    LaunchedEffect(state.isSettingsOpen, state.themeMode) {
        previewTheme = state.themeMode
    }

    WorkTimeTheme(
        themeMode = if (state.isSettingsOpen) previewTheme else state.themeMode,
    ) {
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
            )
        }
    }
}
