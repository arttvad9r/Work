package com.worktime.app.data.db

import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorkEntryEntityTest {
    @Test
    fun `entity round trip preserves work entry`() {
        val original = WorkEntry(
            date = LocalDate.of(2026, 8, 20),
            workedMinutes = 510,
            hourlyRateMicros = 12_500_000,
            bonusMicros = 15_000_000,
            penaltyMicros = 2_500_000,
            note = "Late shift",
        )

        assertEquals(original, original.toEntity().toDomain())
    }
}
