package com.worktime.app.ui.calendar

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CalendarAmountVisibilityTest {
    @Test
    fun `day amount is hidden when total is zero or unavailable`() {
        assertFalse(shouldShowDayAmount(null))
        assertFalse(shouldShowDayAmount(0L))
    }

    @Test
    fun `day amount is shown for non-zero totals`() {
        assertTrue(shouldShowDayAmount(1L))
        assertTrue(shouldShowDayAmount(-1L))
    }
}
