package com.worktime.app.ui.calendar

import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RatePeriodsTest {
    private fun entry(date: LocalDate, rateMicros: Long) = WorkEntry(
        date = date,
        workedMinutes = 480,
        hourlyRateMicros = rateMicros,
    )

    @Test
    fun `empty entries produce no periods`() {
        assertEquals(emptyList<RatePeriodUi>(), buildRatePeriods(emptyList()))
    }

    @Test
    fun `single entry forms one period`() {
        val periods = buildRatePeriods(listOf(entry(LocalDate.of(2026, 8, 3), 370_000_000)))
        assertEquals(listOf(RatePeriodUi(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3), 370_000_000)), periods)
    }

    @Test
    fun `consecutive same-rate entries merge into one period`() {
        val periods = buildRatePeriods(
            listOf(
                entry(LocalDate.of(2026, 8, 3), 370_000_000),
                entry(LocalDate.of(2026, 8, 10), 370_000_000),
                entry(LocalDate.of(2026, 8, 17), 370_000_000),
            ),
        )
        assertEquals(
            listOf(RatePeriodUi(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 17), 370_000_000)),
            periods,
        )
    }

    @Test
    fun `rate changes split periods chronologically`() {
        val periods = buildRatePeriods(
            listOf(
                entry(LocalDate.of(2026, 4, 2), 320_000_000),
                entry(LocalDate.of(2026, 5, 4), 350_000_000),
                entry(LocalDate.of(2026, 7, 30), 350_000_000),
                entry(LocalDate.of(2026, 8, 3), 370_000_000),
            ),
        )
        assertEquals(
            listOf(
                RatePeriodUi(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 2), 320_000_000),
                RatePeriodUi(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 7, 30), 350_000_000),
                RatePeriodUi(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3), 370_000_000),
            ),
            periods,
        )
    }

    @Test
    fun `unsorted input is ordered by date before grouping`() {
        val periods = buildRatePeriods(
            listOf(
                entry(LocalDate.of(2026, 8, 3), 370_000_000),
                entry(LocalDate.of(2026, 5, 4), 350_000_000),
            ),
        )
        assertEquals(
            listOf(
                RatePeriodUi(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 4), 350_000_000),
                RatePeriodUi(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3), 370_000_000),
            ),
            periods,
        )
    }

    @Test
    fun `returning to an earlier rate starts a new period`() {
        val periods = buildRatePeriods(
            listOf(
                entry(LocalDate.of(2026, 1, 5), 300_000_000),
                entry(LocalDate.of(2026, 2, 5), 350_000_000),
                entry(LocalDate.of(2026, 3, 5), 300_000_000),
            ),
        )
        assertEquals(3, periods.size)
        assertEquals(300_000_000L, periods.first().rateMicros)
        assertEquals(300_000_000L, periods.last().rateMicros)
    }
}
