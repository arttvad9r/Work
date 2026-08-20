package com.worktime.app.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun sanitizeMoneyInput(value: String): String = value
    .replace(',', '.')
    .filter { it.isDigit() || it == '.' }
    .let { filtered ->
        val firstDot = filtered.indexOf('.')
        if (firstDot < 0) filtered else {
            filtered.take(firstDot + 1) + filtered.drop(firstDot + 1).replace(".", "").take(6)
        }
    }
    .take(18)

fun parseDecimalMicros(text: String): Long {
    if (text.isBlank()) return 0L
    return BigDecimal(text.replace(',', '.'))
        .movePointRight(6)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
        .also { require(it >= 0L) }
}

fun formatDecimalMicros(micros: Long): String = BigDecimal.valueOf(micros, 6)
    .stripTrailingZeros()
    .toPlainString()

fun formatMoneyMicros(
    micros: Long,
    currencyCode: String,
    locale: Locale = Locale.getDefault(),
): String {
    val formatter = NumberFormat.getCurrencyInstance(locale)
    runCatching { formatter.currency = Currency.getInstance(currencyCode) }
    return formatter.format(BigDecimal.valueOf(micros, 6))
}
