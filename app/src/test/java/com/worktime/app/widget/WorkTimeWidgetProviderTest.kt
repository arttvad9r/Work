package com.worktime.app.widget

import java.time.YearMonth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Test
    fun `time invalidation rechecks month without waiting for scheduled wakeup`() = runTest {
        var month = YearMonth.of(2026, 8)
        val invalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val months = async {
            currentMonthFlow(
                now = { month },
                waitUntilNextMonth = { kotlinx.coroutines.awaitCancellation() },
                invalidations = invalidations,
            ).take(2).toList()
        }
        runCurrent()
        month = YearMonth.of(2026, 9)
        invalidations.emit(Unit)

        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9)), months.await())
    }

    @Test
    fun `time invalidation inside same month reschedules rollover timer`() = runTest {
        val invalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val firstStarted = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        var waits = 0
        var firstCancelled = false
        val observer = launch {
            currentMonthFlow(
                now = { YearMonth.of(2026, 1) },
                waitUntilNextMonth = {
                    waits++
                    if (waits == 1) firstStarted.complete(Unit) else secondStarted.complete(Unit)
                    try {
                        kotlinx.coroutines.awaitCancellation()
                    } finally {
                        if (waits == 1) firstCancelled = true
                    }
                },
                invalidations = invalidations,
            ).collect { }
        }
        firstStarted.await()
        invalidations.emit(Unit)
        secondStarted.await()

        assertEquals(2, waits)
        assertTrue(firstCancelled)
        observer.cancelAndJoin()
    }
}
