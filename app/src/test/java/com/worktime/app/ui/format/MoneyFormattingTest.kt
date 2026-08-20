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
    fun `formatting does not convert money through double`() {
        val formatted = formatMoneyMicros(
            micros = 12_345_678L,
            currencyCode = "EUR",
            locale = Locale.US,
        )
        assertEquals("€12.35", formatted)
    }

    @Test
    fun `invalid currency codes fail instead of silently using locale currency`() {
        assertThrows(IllegalArgumentException::class.java) {
            formatMoneyMicros(1_000_000L, "ZZZ", Locale.US)
        }
    }
}
