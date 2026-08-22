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
    fun `two digits become hours and minutes when hours would be invalid`() {
        assertEquals("3:5", sanitizeDurationInput("35"))
    }

    @Test
    fun `explicit separator is preserved while typing`() {
        assertEquals("3:55", sanitizeDurationInput("3:55"))
        assertEquals("5:3", sanitizeDurationInput("5:3"))
    }

    @Test
    fun `sequential four digit input keeps minute order`() {
        assertEquals("12:34", typeDuration("1234"))
    }

    @Test
    fun `sequential one digit hour entry accepts minutes without transient error`() {
        assertEquals("3:55", typeDuration("355"))
    }

    @Test
    fun `leading zero cannot turn twelve hours into zero one colon two`() {
        assertEquals("1", sanitizeDurationInput("01"))
        assertEquals("12", sanitizeDurationInput("012"))
        assertEquals("12:0", sanitizeDurationInput("0120"))
        assertEquals("1:30", sanitizeDurationInput("01:30"))
    }

    private fun typeDuration(digits: String): String {
        return digits.fold("") { current, digit ->
            sanitizeDurationInput(current + digit)
        }
    }
}
