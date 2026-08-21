package com.worktime.app.ui.format

/**
 * Formats a non-negative duration without redundant zero minutes.
 *
 * Examples: 0 -> "0", 15h -> "15", 15h 30m -> "15:30".
 */
fun formatDurationCompact(minutes: Int): String {
    require(minutes >= 0) { "minutes must be non-negative" }
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) {
        hours.toString()
    } else {
        "$hours:${remainder.toString().padStart(2, '0')}"
    }
}
