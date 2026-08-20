package com.worktime.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarViewModel
import com.worktime.app.ui.dayeditor.DayEditorSheet

@Composable
fun WorkTimeApp(viewModel: CalendarViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CalendarScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onDayClick = viewModel::selectDate,
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
}
