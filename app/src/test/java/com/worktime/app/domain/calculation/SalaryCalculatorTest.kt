package com.worktime.app.domain.calculation

import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class SalaryCalculatorTest {
    @ParameterizedTest
    @MethodSource("goldenCases")
    fun `salary golden cases`(
        workedMinutes: Int,
        hourlyRateMicros: Long,
        bonusMicros: Long,
        penaltyMicros: Long,
        expectedTotalMicros: Long,
    ) {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 8, 20),
            workedMinutes = workedMinutes,
            hourlyRateMicros = hourlyRateMicros,
            bonusMicros = bonusMicros,
            penaltyMicros = penaltyMicros,
        )

        assertEquals(expectedTotalMicros, SalaryCalculator.entryPay(entry).totalPayMicros)
    }

    companion object {
        @JvmStatic
        fun goldenCases() = listOf(
            Arguments.of(8 * 60, 10_000_000L, 0L, 0L, 80_000_000L),
            Arguments.of(8 * 60 + 30, 10_000_000L, 0L, 0L, 85_000_000L),
            Arguments.of(8 * 60, 10_000_000L, 20_000_000L, 0L, 100_000_000L),
            Arguments.of(8 * 60, 10_000_000L, 0L, 15_000_000L, 65_000_000L),
            Arguments.of(8 * 60, 10_000_000L, 20_000_000L, 15_000_000L, 85_000_000L),
            Arguments.of(0, 10_000_000L, 50_000_000L, 0L, 50_000_000L),
            Arguments.of(8 * 60, 10_000L, 0L, 0L, 80_000L),
            Arguments.of(1, 60_000_000L, 0L, 0L, 1_000_000L),
        )
    }
}
