package com.worktime.app.domain.model

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
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
    fun `rejects negative or unsupported money inputs`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(date = date, workedMinutes = 60, hourlyRateMicros = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(date = date, workedMinutes = 60, hourlyRateMicros = 1, bonusMicros = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(date = date, workedMinutes = 60, hourlyRateMicros = 1, penaltyMicros = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(
                date = date,
                workedMinutes = 60,
                hourlyRateMicros = MoneyLimits.MAX_COMPONENT_MICROS + 1,
            )
        }
    }

    @Test
    fun `rejects worked time with zero hourly rate`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(date = date, workedMinutes = 60, hourlyRateMicros = 0)
        }
    }

    @Test
    fun `allows adjustment only entry with zero hourly rate`() {
        assertDoesNotThrow {
            WorkEntry(
                date = date,
                workedMinutes = 0,
                hourlyRateMicros = 0,
                bonusMicros = 1,
            )
        }
    }

    @Test
    fun `rejects empty entries`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(date = date, workedMinutes = 0, hourlyRateMicros = 0)
        }
    }

    @Test
    fun `rejects notes above storage limit`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkEntry(
                date = date,
                workedMinutes = 60,
                hourlyRateMicros = 1_000_000,
                note = "x".repeat(MoneyLimits.MAX_NOTE_LENGTH + 1),
            )
        }
    }
}
