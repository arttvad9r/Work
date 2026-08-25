package com.worktime.app.ui.calendar

import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val entries: Map<LocalDate, WorkEntry> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val defaultHourlyRateMicros: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isSettingsOpen: Boolean = false,
    val isRateHistoryOpen: Boolean = false,
    val ratePeriods: List<RatePeriodUi> = emptyList(),
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

/**
 * Fixed 12-slot monthly breakdown for one year; `total` aggregates the same rows.
 */
data class YearSummary(
    val year: Int,
    val total: MonthSummary,
    val months: List<MonthSummary>,
    val monthHasData: List<Boolean> = emptyList(),
) {
    val monthsWithData: Int
        get() = monthHasData.count { it }
}

/** A grouped view of recorded entries sharing one hourly rate, not an effective period. */
data class RatePeriodUi(
    val start: LocalDate,
    val end: LocalDate,
    val rateMicros: Long,
    val entryCount: Int,
)

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
        ENTRY_DELETED,
        RATE_UPDATED,
        OPERATION_UNDONE,
        BACKUP_EXPORTED,
        BACKUP_IMPORTED,
        NO_OP,
    }

    data class Error(val kind: CalendarOperationError) : CalendarOperationEvent
}

/**
 * Groups chronologically sorted entries into same-rate runs for display.
 * The dates are the first and last recorded entries in each group; gaps are not
 * treated as evidence that the rate was continuously effective.
 */
internal fun buildRatePeriods(entries: List<WorkEntry>): List<RatePeriodUi> {
    if (entries.isEmpty()) return emptyList()
    val periods = mutableListOf<RatePeriodUi>()
    for (entry in entries.sortedBy { it.date }) {
        val last = periods.lastOrNull()
        if (last != null && last.rateMicros == entry.hourlyRateMicros) {
            periods[periods.lastIndex] = last.copy(end = entry.date, entryCount = last.entryCount + 1)
        } else {
            periods += RatePeriodUi(entry.date, entry.date, entry.hourlyRateMicros, entryCount = 1)
        }
    }
    return periods
}
