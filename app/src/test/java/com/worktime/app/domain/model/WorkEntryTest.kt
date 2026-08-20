package com.worktime.app.domain.model

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WorkEntryTest {
    private val date = LocalDate.of(2026, 8, 20)

    @Test
    fun `rejects more than 24 hours`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(date = date, workedMinutes = 1_441, hourlyRateMicros = 1_000_000)
        }
    }

    @Test
    fun `rejects negative money inputs`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(date = date, workedMinutes = 60, hourlyRateMicros = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(date = date, workedMinutes = 60, hourlyRateMicros = 1, bonusMicros = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(date = date, workedMinutes = 60, hourlyRateMicros = 1, penaltyMicros = -1)
        }
    }
}
