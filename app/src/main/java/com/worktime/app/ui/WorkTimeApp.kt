package com.worktime.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worktime.app.AppContainer
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarViewModel
import com.worktime.app.ui.dayeditor.DayEditorSheet
import com.worktime.app.ui.settings.SettingsSheet
import com.worktime.app.ui.theme.WorkTimeTheme

@Composable
fun WorkTimeApp(container: AppContainer) {
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.Factory(
            workEntryRepository = container.workEntryRepository,
            userPreferencesRepository = container.userPreferencesRepository,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    WorkTimeTheme(themeMode = state.themeMode) {
        CalendarScreen(
            state = state,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onDayClick = viewModel::selectDate,
            onSettingsClick = viewModel::openSettings,
        )

        state.selectedDate?.let { date ->
            DayEditorSheet(
                date = date,
                existing = state.entries[date],
                defaultHourlyRateMicros = state.defaultHourlyRateMicros,
                currencyCode = state.currencyCode,
                onDismiss = viewModel::dismissEditor,
                onSave = viewModel::saveEntry,
                onDelete = viewModel::deleteEntry,
            )
        }

        if (state.isSettingsOpen) {
            SettingsSheet(
                defaultHourlyRateMicros = state.defaultHourlyRateMicros,
                currencyCode = state.currencyCode,
                themeMode = state.themeMode,
                onDismiss = viewModel::dismissSettings,
                onSave = viewModel::updatePreferences,
            )
        }
    }
}
