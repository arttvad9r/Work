package com.worktime.app.ui.dayeditor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DayEditorInputFormattingTest {
    @Test
    fun `three digits are interpreted as one hour digit and two minute digits`() {
        assertEquals("5:30", sanitizeDurationInput("530"))
    }

    @Test
    fun `three digits preserve valid two digit hours while typing`() {
        assertEquals("12:0", sanitizeDurationInput("120"))
    }

    @Test
    fun `four digits are interpreted as two hour digits and two minute digits`() {
        assertEquals("15:30", sanitizeDurationInput("1530"))
    }

    @Test
    fun `explicit separator is preserved while typing`() {
        assertEquals("5:3", sanitizeDurationInput("5:3"))
    }
}
