package com.worktime.app.ui.dayeditor

import com.worktime.app.domain.model.WorkEntry
import java.time.DayOfWeek
import java.time.LocalDate

private const val MinimumSamplesForPersonalSuggestion = 3
private const val PersonalSuggestionLimit = 2

internal fun durationSuggestions(
    date: LocalDate,
    entries: Collection<WorkEntry>,
): List<Int> {
    val weekendShift = date.dayOfWeek == DayOfWeek.FRIDAY || date.dayOfWeek == DayOfWeek.SATURDAY
    val frequent = entries.asSequence()
        .filter { entry ->
            val entryWeekendShift = entry.date.dayOfWeek == DayOfWeek.FRIDAY ||
                entry.date.dayOfWeek == DayOfWeek.SATURDAY
            entryWeekendShift == weekendShift && entry.workedMinutes > 0
        }
        .groupingBy(WorkEntry::workedMinutes)
        .eachCount()
        .filterValues { it >= MinimumSamplesForPersonalSuggestion }
        .entries
        .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key })
        .map { it.key }

    return (listOf(8 * 60, 12 * 60) + frequent.take(PersonalSuggestionLimit))
        .distinct()
        .take(4)
}
