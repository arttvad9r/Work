package com.worktime.app.domain.preferences

import com.worktime.app.domain.model.MoneyLimits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UserPreferencesTest {
    @Test
    fun `accepts supported maximum default rate`() {
        val preferences = UserPreferences(
            defaultHourlyRateMicros = MoneyLimits.MAX_COMPONENT_MICROS,
            currencyCode = "EUR",
        )
        assertEquals(MoneyLimits.MAX_COMPONENT_MICROS, preferences.defaultHourlyRateMicros)
    }

    @Test
    fun `rejects unsupported default rate`() {
        assertThrows(IllegalArgumentException::class.java) {
            UserPreferences(
                defaultHourlyRateMicros = MoneyLimits.MAX_COMPONENT_MICROS + 1,
                currencyCode = "EUR",
            )
        }
    }

    @Test
    fun `rejects malformed or unknown currency code`() {
        assertThrows(IllegalArgumentException::class.java) {
            UserPreferences(currencyCode = "EU")
        }
        assertThrows(IllegalArgumentException::class.java) {
            UserPreferences(currencyCode = "eur")
        }
        assertThrows(IllegalArgumentException::class.java) {
            UserPreferences(currencyCode = "ZZZ")
        }
    }
}
