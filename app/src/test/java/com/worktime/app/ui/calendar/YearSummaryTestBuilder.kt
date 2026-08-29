package com.worktime.app.ui.calendar

import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.yearsummary.YearSummary

internal fun buildYearSummary(year: Int, entries: List<WorkEntry>): YearSummary =
    com.worktime.app.ui.yearsummary.buildYearSummary(year, entries)
