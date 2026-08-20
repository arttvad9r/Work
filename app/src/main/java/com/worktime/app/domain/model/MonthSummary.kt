package com.worktime.app.domain.model

data class MonthSummary(
    val workedMinutes: Int,
    val shiftCount: Int,
    val basePayMicros: Long,
    val bonusMicros: Long,
    val penaltyMicros: Long,
    val totalPayMicros: Long,
) {
    companion object {
        val Empty = MonthSummary(
            workedMinutes = 0,
            shiftCount = 0,
            basePayMicros = 0L,
            bonusMicros = 0L,
            penaltyMicros = 0L,
            totalPayMicros = 0L,
        )
    }
}
