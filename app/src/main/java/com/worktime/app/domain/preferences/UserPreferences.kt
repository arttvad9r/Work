package com.worktime.app.domain.preferences

import com.worktime.app.domain.model.MoneyLimits
import java.util.Currency
import java.util.Locale

data class UserPreferences(
    val defaultHourlyRateMicros: Long = 0L,
    val currencyCode: String = "EUR",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    init {
        require(defaultHourlyRateMicros in 0..MoneyLimits.MAX_COMPONENT_MICROS)
        require(currencyCode.length == 3)
        require(currencyCode == currencyCode.uppercase(Locale.ROOT))
        require(runCatching { Currency.getInstance(currencyCode) }.isSuccess) {
            "currencyCode must be a valid ISO 4217 code"
        }
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
