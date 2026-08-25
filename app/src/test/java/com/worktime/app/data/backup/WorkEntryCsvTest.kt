package com.worktime.app.data.backup

import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorkEntryCsvTest {
    @Test
    fun `encode writes header and one line per entry with plain numbers`() {
        val csv = WorkEntryCsv.encode(
            listOf(
                WorkEntry(LocalDate.of(2026, 2, 1), 840, 350_000_000L),
                WorkEntry(
                    LocalDate.of(2026, 5, 29),
                    workedMinutes = 795,
                    hourlyRateMicros = 370_500_000L,
                    bonusMicros = 1_500_000_000L,
                    penaltyMicros = 250_000_000L,
                    note = "ignored, notes are not exported",
                ),
            ),
        )

        val lines = csv.trimEnd('\n').split('\n')
        assertEquals("date,duration,hourly_rate,bonus,penalty,total", lines[0])
        assertEquals("2026-02-01,14:00,350,0,0,4900", lines[1])
        assertEquals("2026-05-29,13:15,370.5,1500,250,6159.13", lines[2])
    }

    @Test
    fun `negative totals keep their sign`() {
        val csv = WorkEntryCsv.encode(
            listOf(
                WorkEntry(
                    LocalDate.of(2026, 1, 5),
                    workedMinutes = 0,
                    hourlyRateMicros = 0L,
                    penaltyMicros = 125_500_000L,
                ),
            ),
        )

        assertEquals("2026-01-05,0:00,0,0,125.5,-125.5", csv.trimEnd('\n').split('\n')[1])
    }
}
