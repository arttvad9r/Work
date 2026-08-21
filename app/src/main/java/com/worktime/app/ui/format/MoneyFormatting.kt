package com.worktime.app.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val DECIMAL_INPUT_PATTERN = Regex("^(?:\\d+(?:\\.\\d*)?|\\.\\d+)$")

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
    val normalized = text.replace(',', '.')
    require(DECIMAL_INPUT_PATTERN.matches(normalized)) { "Invalid decimal amount" }
    return BigDecimal(normalized)
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
    val currency = Currency.getInstance(currencyCode)
    val formatter = NumberFormat.getCurrencyInstance(locale)
    formatter.currency = currency
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = currency.defaultFractionDigits.takeIf { it >= 0 } ?: 2
    formatter.roundingMode = RoundingMode.HALF_UP
    return formatter.format(BigDecimal.valueOf(micros, 6))
}

fun formatAmountMicros(
    micros: Long,
    currencyCode: String,
    locale: Locale = Locale.getDefault(),
): String {
    val currency = Currency.getInstance(currencyCode)
    val formatter = NumberFormat.getNumberInstance(locale)
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = currency.defaultFractionDigits.takeIf { it >= 0 } ?: 2
    formatter.roundingMode = RoundingMode.HALF_UP
    return formatter.format(BigDecimal.valueOf(micros, 6))
}
