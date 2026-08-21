package com.worktime.app.ui.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DurationFormattingTest {
    @Test
    fun `zero duration has no redundant minutes`() {
        assertEquals("0", formatDurationCompact(0))
    }

    @Test
    fun `whole hours have no redundant minutes`() {
        assertEquals("15", formatDurationCompact(15 * 60))
    }

    @Test
    fun `non-zero minutes remain visible`() {
        assertEquals("15:30", formatDurationCompact(15 * 60 + 30))
    }

    @Test
    fun `negative duration is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            formatDurationCompact(-1)
        }
    }
}
