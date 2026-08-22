package com.worktime.app.domain.model

import java.time.LocalDate

data class WorkEntry(
    val date: LocalDate,
    val workedMinutes: Int,
    val hourlyRateMicros: Long,
    val bonusMicros: Long = 0L,
    val penaltyMicros: Long = 0L,
    val note: String = "",
) {
    init {
        require(workedMinutes in 0..24 * 60) { "workedMinutes must be in 0..1440" }
        require(hourlyRateMicros in 0..MoneyLimits.MAX_COMPONENT_MICROS) {
            "hourlyRateMicros is outside the supported range"
        }
        require(bonusMicros in 0..MoneyLimits.MAX_COMPONENT_MICROS) {
            "bonusMicros is outside the supported range"
        }
        require(penaltyMicros in 0..MoneyLimits.MAX_COMPONENT_MICROS) {
            "penaltyMicros is outside the supported range"
        }
        require(workedMinutes == 0 || hourlyRateMicros > 0L) {
            "worked time requires a positive hourly rate"
        }
        require(workedMinutes > 0 || bonusMicros > 0L || penaltyMicros > 0L) {
            "work entry must contain worked time, a bonus, or a penalty"
        }
    }
}
