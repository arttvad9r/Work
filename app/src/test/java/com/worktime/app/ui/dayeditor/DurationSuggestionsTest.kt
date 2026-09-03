package com.worktime.app.ui.dayeditor

import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DurationSuggestionsTest {
    @Test
    fun `formats Russian suggestion with hours abbreviation`() {
        assertEquals("8 ч", formatDurationSuggestion(8, "ч"))
    }

    @Test
    fun `uses frequent duration for Friday and Saturday`() {
        val entries = listOf(
            entry(LocalDate.of(2026, 8, 7), 15 * 60),
            entry(LocalDate.of(2026, 8, 14), 15 * 60),
            entry(LocalDate.of(2026, 8, 21), 15 * 60),
            entry(LocalDate.of(2026, 8, 8), 13 * 60),
        )

        assertEquals(
            listOf(8 * 60, 12 * 60, 15 * 60),
            durationSuggestions(LocalDate.of(2026, 8, 28), entries),
        )
    }

    @Test
    fun `uses common duration for other weekdays`() {
        val entries = listOf(
            entry(LocalDate.of(2026, 8, 3), 13 * 60),
            entry(LocalDate.of(2026, 8, 10), 13 * 60),
            entry(LocalDate.of(2026, 8, 17), 13 * 60),
        )

        assertEquals(
            listOf(8 * 60, 12 * 60, 13 * 60),
            durationSuggestions(LocalDate.of(2026, 8, 24), entries),
        )
    }

    @Test
    fun `does not infer a personal duration from fewer than three entries`() {
        assertEquals(
            listOf(8 * 60, 12 * 60),
            durationSuggestions(
                LocalDate.of(2026, 8, 28),
                listOf(entry(LocalDate.of(2026, 8, 7), 15 * 60)),
            ),
        )
    }

    private fun entry(date: LocalDate, workedMinutes: Int) = WorkEntry(
        date = date,
        workedMinutes = workedMinutes,
        hourlyRateMicros = 100_000_000L,
    )
}
