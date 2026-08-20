package com.worktime.app.domain.preferences

data class UserPreferences(
    val defaultHourlyRateMicros: Long = 0L,
    val currencyCode: String = "EUR",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    init {
        require(defaultHourlyRateMicros >= 0L)
        require(currencyCode.length == 3)
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
