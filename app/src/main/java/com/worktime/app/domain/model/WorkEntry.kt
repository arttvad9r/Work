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
        require(hourlyRateMicros >= 0L) { "hourlyRateMicros must be non-negative" }
        require(bonusMicros >= 0L) { "bonusMicros must be non-negative" }
        require(penaltyMicros >= 0L) { "penaltyMicros must be non-negative" }
    }
}
