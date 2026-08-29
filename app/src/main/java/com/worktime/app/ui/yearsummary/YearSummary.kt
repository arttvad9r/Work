package com.worktime.app.ui.yearsummary

import com.worktime.app.domain.model.MonthSummary

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
