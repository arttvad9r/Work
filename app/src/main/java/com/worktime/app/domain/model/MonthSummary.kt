package com.worktime.app.domain.model

data class MonthSummary(
    val workedMinutes: Int,
    val shiftCount: Int,
    val basePayMicros: Long,
    val bonusMicros: Long,
    val penaltyMicros: Long,
    val totalPayMicros: Long,
)
