package com.worktime.app.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MonthGridTest {
    @ParameterizedTest
    @CsvSource(
        "2025,9,0",
        "2025,4,1",
        "2025,1,2",
        "2025,5,3",
        "2025,8,4",
        "2025,2,5",
        "2025,6,6",
    )
    fun `monday-first grid supports every possible month start weekday`(
        year: Int,
        monthNumber: Int,
        expectedLeadingCells: Int,
    ) {
        val month = YearMonth.of(year, monthNumber)
        val cells = MonthGrid.build(month)

        assertEquals(42, cells.size)
        assertEquals(month.atDay(1), cells[expectedLeadingCells])
        if (expectedLeadingCells > 0) assertNull(cells[expectedLeadingCells - 1])
    }

    @Test
    fun `leap february contains day 29`() {
        val cells = MonthGrid.build(YearMonth.of(2024, 2))
        assertEquals(LocalDate.of(2024, 2, 29), cells.filterNotNull().last())
        assertEquals(29, cells.filterNotNull().size)
    }

    @Test
    fun `custom sunday first day shifts august 2026 correctly`() {
        val cells = MonthGrid.build(
            month = YearMonth.of(2026, 8),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )
        assertEquals(LocalDate.of(2026, 8, 1), cells[6])
    }
}
