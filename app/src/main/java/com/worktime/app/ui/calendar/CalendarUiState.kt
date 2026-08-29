package com.worktime.app.ui.calendar

import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.yearsummary.YearSummary
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val entries: Map<LocalDate, WorkEntry> = emptyMap(),
    /**
     * Loaded month window used by the horizontal calendar pager. The current repository
     * subscription keeps the visible month plus its immediate neighbours warm so a drag
     * never reveals an empty page simply because Room has not switched queries yet.
     */
    val monthEntries: Map<YearMonth, Map<LocalDate, WorkEntry>> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val defaultHourlyRateMicros: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isChangeRateSheetOpen: Boolean = false,
    val changeRateInitialRange: ClosedRange<LocalDate>? = null,
    val isYearSummaryOpen: Boolean = false,
    val yearSummary: YearSummary? = null,
    val isReady: Boolean = false,
    val operationError: CalendarOperationError? = null,
    val canUndo: Boolean = false,
    val pendingImportCount: Int? = null,
) {
    val summary: MonthSummary
        get() = SalaryCalculator.monthSummary(entries.values)
}

enum class CalendarOperationError {
    SAVE_ENTRY,
    DELETE_ENTRY,
    SAVE_SETTINGS,
    BULK_RATE,
    UNDO,
    BACKUP_EXPORT,
    BACKUP_IMPORT,
    BACKUP_IMPORT_ROLLBACK,
    DEFAULT_RATE_ADOPTION,
}

sealed interface CalendarOperationEvent {
    enum class Success : CalendarOperationEvent {
        ENTRY_SAVED,
        ENTRY_DELETED,
        RATE_UPDATED,
        OPERATION_UNDONE,
        BACKUP_EXPORTED,
        BACKUP_IMPORTED,
        NO_OP,
    }

    data class Error(val kind: CalendarOperationError) : CalendarOperationEvent
}
