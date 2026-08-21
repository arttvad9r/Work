package com.worktime.app.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object MonthGrid {
    fun build(
        month: YearMonth,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): List<LocalDate?> {
        val first = month.atDay(1)
        val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        return List(42) { index ->
            val day = index - leading + 1
            if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
        }
    }
}
