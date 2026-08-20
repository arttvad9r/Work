package com.worktime.app.ui.calendar

import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val entries: Map<LocalDate, WorkEntry> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val currencyCode: String = "EUR",
    val defaultHourlyRateMicros: Long = 12_500_000L,
) {
    val summary: MonthSummary
        get() = SalaryCalculator.monthSummary(
            entries.values.filter { YearMonth.from(it.date) == visibleMonth },
        )
}
