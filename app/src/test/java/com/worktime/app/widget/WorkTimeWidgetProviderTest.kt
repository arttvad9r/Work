package com.worktime.app.widget

import java.time.YearMonth
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorkTimeWidgetProviderTest {
    @Test
    fun `current month flow advances after the clock crosses a month`() = runTest {
        var month = YearMonth.of(2026, 8)

        val months = currentMonthFlow(
            now = { month },
            waitUntilNextMonth = { month = month.plusMonths(1) },
        ).take(2).toList()

        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9)), months)
    }
}
