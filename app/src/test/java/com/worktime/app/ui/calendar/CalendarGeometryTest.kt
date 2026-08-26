package com.worktime.app.ui.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CalendarGeometryTest {
    @Test
    fun `large font keeps calendar macro height fixed`() {
        assertEquals(calendarGridHeight(false), calendarGridHeight(true))
    }
}
