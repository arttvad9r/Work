package com.worktime.app.ui.format

import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `formatting does not convert money through double`() {
        val formatted = formatMoneyMicros(
            micros = 12_345_678L,
            currencyCode = "EUR",
            locale = Locale.US,
        )
        assertEquals("€12.35", formatted)
    }
}
