package com.worktime.app.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.parseDecimalMicros
import com.worktime.app.ui.format.sanitizeMoneyInput
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    defaultHourlyRateMicros: Long,
    currencyCode: String,
    themeMode: ThemeMode,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (Long, String, ThemeMode) -> Unit,
) {
    var rate by rememberSaveable(defaultHourlyRateMicros) {
        mutableStateOf(formatDecimalMicros(defaultHourlyRateMicros))
    }
    var currency by rememberSaveable(currencyCode) { mutableStateOf(currencyCode) }
    var selectedTheme by rememberSaveable(themeMode) { mutableStateOf(themeMode) }

    val parsedRate = runCatching { parseDecimalMicros(rate) }.getOrNull()
    val rateValid = parsedRate != null && parsedRate <= MoneyLimits.MAX_COMPONENT_MICROS
    val normalizedCurrency = currency.trim().uppercase(Locale.ROOT)
    val validCurrency = normalizedCurrency.length == 3 && runCatching {
        Currency.getInstance(normalizedCurrency)
    }.isSuccess
    val canSave = rateValid && validCurrency
    val rateError = when {
        parsedRate == null -> stringResource(R.string.invalid_money_value)
        parsedRate > MoneyLimits.MAX_COMPONENT_MICROS -> stringResource(
            R.string.money_value_too_large,
            formatDecimalMicros(MoneyLimits.MAX_COMPONENT_MICROS),
        )
        else -> null
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = rate,
                onValueChange = { rate = sanitizeMoneyInput(it) },
                label = { Text(stringResource(R.string.default_hourly_rate)) },
                suffix = { Text(normalizedCurrency.ifBlank { currencyCode }) },
                supportingText = {
                    Text(rateError ?: stringResource(R.string.default_rate_help))
                },
                isError = rateError != null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            OutlinedTextField(
                value = currency,
                onValueChange = {
                    currency = it.filter(Char::isLetter).uppercase(Locale.ROOT).take(3)
                },
                label = { Text(stringResource(R.string.currency_code)) },
                supportingText = {
                    Text(
                        if (!validCurrency) {
                            stringResource(R.string.currency_code_hint)
                        } else {
                            stringResource(R.string.currency_no_conversion)
                        },
                    )
                },
                isError = !validCurrency,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedTheme == mode,
                            onClick = { selectedTheme = mode },
                            label = { Text(themeLabel(mode)) },
                        )
                    }
                }
            }

            if (operationErrorMessage != null) {
                Text(
                    text = operationErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = {
                    val safeRate = parsedRate ?: return@Button
                    onSave(safeRate, normalizedCurrency, selectedTheme)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_settings))
            }
        }
    }
}

@Composable
private fun themeLabel(themeMode: ThemeMode): String = when (themeMode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}
