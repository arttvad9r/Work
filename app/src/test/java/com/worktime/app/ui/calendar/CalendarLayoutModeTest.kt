package com.worktime.app.ui.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CalendarLayoutModeTest {
    @Test
    fun `single partition with normal height stays compact`() {
        assertEquals(
            CalendarLayoutMode.Compact,
            calendarLayoutMode(
                maxHorizontalPartitions = 1,
                isHeightAtLeastMedium = true,
            ),
        )
    }

    @Test
    fun `two partitions with normal height use supporting pane`() {
        assertEquals(
            CalendarLayoutMode.SupportingPane,
            calendarLayoutMode(
                maxHorizontalPartitions = 2,
                isHeightAtLeastMedium = true,
            ),
        )
    }

    @Test
    fun `compact height takes precedence over wide partitions`() {
        assertEquals(
            CalendarLayoutMode.CompactShort,
            calendarLayoutMode(
                maxHorizontalPartitions = 2,
                isHeightAtLeastMedium = false,
            ),
        )
    }
}
