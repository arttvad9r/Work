package com.worktime.app.ui.format

import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MoneyFormattingTest {
    @Test
    fun `decimal parser preserves six fractional digits`() {
        assertEquals(12_345_678L, parseDecimalMicros("12.345678"))
    }

    @Test
    fun `decimal parser rounds half up after six digits`() {
        assertEquals(1_234_568L, parseDecimalMicros("1.2345678"))
    }

    @Test
    fun `decimal parser rejects exponent notation`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseDecimalMicros("1e3")
        }
    }

    @Test
    fun `sanitizer keeps one decimal separator and six fractional digits`() {
        assertEquals("12.345678", sanitizeMoneyInput("12,34.567890"))
    }

    @Test
    fun `amount formatter preserves explicitly entered fractional precision`() {
        assertEquals("12.345678", formatAmountMicros(12_345_678L, Locale.US))
    }

    @Test
    fun `amount formatter omits zero fractional part`() {
        assertEquals("12", formatAmountMicros(12_000_000L, Locale.US))
    }
}
