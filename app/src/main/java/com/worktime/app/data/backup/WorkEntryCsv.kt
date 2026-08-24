package com.worktime.app.data.backup

import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry

/**
 * CSV export for spreadsheets. Every value is generated (ISO date, `H:MM`
 * duration, dot-decimal amounts), so no quoting or escaping is required.
 */
object WorkEntryCsv {
    fun encode(entries: List<WorkEntry>): String = buildString {
        appendLine("date,duration,hourly_rate,bonus,penalty,total")
        entries.forEach { entry ->
            val totalMicros = SalaryCalculator.entryPay(entry).totalPayMicros
            appendLine(
                listOf(
                    entry.date.toString(),
                    duration(entry.workedMinutes),
                    money(entry.hourlyRateMicros),
                    money(entry.bonusMicros),
                    money(entry.penaltyMicros),
                    money(totalMicros),
                ).joinToString(","),
            )
        }
    }

    private fun duration(workedMinutes: Int): String =
        "%d:%02d".format(workedMinutes / 60, workedMinutes % 60)

    private fun money(micros: Long): String {
        val negative = micros < 0
        val absolute = if (negative) -micros else micros
        val whole = absolute / 1_000_000L
        val hundredths = (absolute % 1_000_000L) / 10_000L
        val fraction = when {
            hundredths == 0L -> ""
            hundredths % 10L == 0L -> ".${hundredths / 10L}"
            else -> ".$hundredths"
        }
        return buildString {
            if (negative) append('-')
            append(whole)
            append(fraction)
        }
    }
}
