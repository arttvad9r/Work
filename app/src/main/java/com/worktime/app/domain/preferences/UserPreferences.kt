package com.worktime.app.domain.preferences

import com.worktime.app.domain.model.MoneyLimits

data class UserPreferences(
    val defaultHourlyRateMicros: Long = 0L,
    val currencyCode: String = "EUR",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    init {
        require(defaultHourlyRateMicros in 0..MoneyLimits.MAX_COMPONENT_MICROS)
        require(currencyCode.length == 3)
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
