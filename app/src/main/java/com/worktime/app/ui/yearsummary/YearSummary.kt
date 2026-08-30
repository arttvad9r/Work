package com.worktime.app.ui.yearsummary

import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.domain.model.WorkEntry
import java.time.YearMonth

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

internal fun buildYearSummary(year: Int, entries: List<WorkEntry>): YearSummary {
    val byMonth = entries.groupBy { YearMonth.from(it.date) }
    val months = (1..12).map { month ->
        SalaryCalculator.monthSummary(byMonth[YearMonth.of(year, month)].orEmpty())
    }
    return YearSummary(
        year = year,
        total = SalaryCalculator.monthSummary(entries),
        months = months,
        monthHasData = (1..12).map { month ->
            byMonth[YearMonth.of(year, month)].orEmpty().isNotEmpty()
        },
    )
}
