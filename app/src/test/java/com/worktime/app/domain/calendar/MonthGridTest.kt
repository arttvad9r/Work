package com.worktime.app.domain.calendar

import java.time.LocalDate
import java.time.YearMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MonthGridTest {
    @Test
    fun `august 2026 is represented as a fixed 6 by 7 grid`() {
        val cells = MonthGrid.build(YearMonth.of(2026, 8))

        assertEquals(42, cells.size)
        assertEquals(LocalDate.of(2026, 8, 1), cells[5])
        assertEquals(LocalDate.of(2026, 8, 31), cells[35])
    }
}
