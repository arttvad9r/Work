package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.formatMoneyMicros
import com.worktime.app.ui.format.parseDecimalMicros
import com.worktime.app.ui.format.sanitizeMoneyInput
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorSheet(
    date: LocalDate,
    existing: WorkEntry?,
    defaultHourlyRateMicros: Long,
    currencyCode: String,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (WorkEntry) -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    var hours by rememberSaveable(date.toEpochDay(), existing) {
        mutableStateOf(((existing?.workedMinutes ?: 0) / 60).toString())
    }
    var minutes by rememberSaveable(date.toEpochDay(), existing) {
        mutableStateOf(((existing?.workedMinutes ?: 0) % 60).toString())
    }
    var rate by rememberSaveable(date.toEpochDay(), existing) {
        mutableStateOf(formatDecimalMicros(existing?.hourlyRateMicros ?: defaultHourlyRateMicros))
    }
    var bonus by rememberSaveable(date.toEpochDay(), existing) {
        mutableStateOf(formatDecimalMicros(existing?.bonusMicros ?: 0L))
    }
    var penalty by rememberSaveable(date.toEpochDay(), existing) {
        mutableStateOf(formatDecimalMicros(existing?.penaltyMicros ?: 0L))
    }
    var bonusVisible by rememberSaveable(date.toEpochDay(), existing) {
        mutableStateOf((existing?.bonusMicros ?: 0L) > 0L)
    }
    var penaltyVisible by rememberSaveable(date.toEpochDay(), existing) {
        mutableStateOf((existing?.penaltyMicros ?: 0L) > 0L)
    }
    var confirmDelete by rememberSaveable(date.toEpochDay(), existing) { mutableStateOf(false) }

    val parsedHours = parseWholeNumberOrZero(hours)
    val parsedMinutes = parseWholeNumberOrZero(minutes)
    val hoursValid = parsedHours != null && parsedHours in 0..24
    val minutesValid = parsedMinutes != null && parsedMinutes in 0..59
    val durationValid = hoursValid && minutesValid && !(parsedHours == 24 && parsedMinutes != 0)
    val workedMinutes = if (durationValid) parsedHours * 60 + parsedMinutes else null

    val parsedRate = parseMoneyOrNull(rate)
    val parsedBonus = parseMoneyOrNull(bonus)
    val parsedPenalty = parseMoneyOrNull(penalty)
    val rateWithinLimit = parsedRate != null && parsedRate <= MoneyLimits.MAX_COMPONENT_MICROS
    val bonusWithinLimit = parsedBonus != null && parsedBonus <= MoneyLimits.MAX_COMPONENT_MICROS
    val penaltyWithinLimit = parsedPenalty != null && parsedPenalty <= MoneyLimits.MAX_COMPONENT_MICROS
    val positiveRateRequired = (workedMinutes ?: 0) > 0
    val rateValid = rateWithinLimit && (!positiveRateRequired || parsedRate > 0L)
    val hasEffectiveData = workedMinutes != null && (
        workedMinutes > 0 || (parsedBonus ?: 0L) > 0L || (parsedPenalty ?: 0L) > 0L
    )

    val draft = if (
        durationValid && rateValid && bonusWithinLimit && penaltyWithinLimit && hasEffectiveData
    ) {
        runCatching {
            WorkEntry(
                date = date,
                workedMinutes = workedMinutes,
                hourlyRateMicros = parsedRate,
                bonusMicros = parsedBonus,
                penaltyMicros = parsedPenalty,
                note = existing?.note.orEmpty(),
            )
        }.getOrNull()
    } else {
        null
    }

    val maxMoneyLabel = formatDecimalMicros(MoneyLimits.MAX_COMPONENT_MICROS)
    val hoursError = when {
        !hoursValid -> stringResource(R.string.hours_range_error)
        !durationValid -> stringResource(R.string.duration_24h_error)
        else -> null
    }
    val minutesError = when {
        !minutesValid -> stringResource(R.string.minutes_range_error)
        !durationValid -> stringResource(R.string.duration_24h_error)
        else -> null
    }
    val rateError = when {
        parsedRate == null -> stringResource(R.string.invalid_money_value)
        parsedRate > MoneyLimits.MAX_COMPONENT_MICROS -> stringResource(R.string.money_value_too_large, maxMoneyLabel)
        positiveRateRequired && parsedRate == 0L -> stringResource(R.string.hourly_rate_required)
        else -> null
    }
    val bonusError = when {
        parsedBonus == null -> stringResource(R.string.invalid_money_value)
        parsedBonus > MoneyLimits.MAX_COMPONENT_MICROS -> stringResource(R.string.money_value_too_large, maxMoneyLabel)
        else -> null
    }
    val penaltyError = when {
        parsedPenalty == null -> stringResource(R.string.invalid_money_value)
        parsedPenalty > MoneyLimits.MAX_COMPONENT_MICROS -> stringResource(R.string.money_value_too_large, maxMoneyLabel)
        else -> null
    }
    val totalMicros = draft?.let { runCatching { SalaryCalculator.entryPay(it).totalPayMicros }.getOrNull() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = date.format(
                    DateTimeFormatter.ofPattern("EEEE, d MMMM", LocalLocale.current.platformLocale),
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.worked),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(
                            value = hours,
                            onValueChange = { hours = it.filter(Char::isDigit).take(2) },
                            label = stringResource(R.string.hours),
                            isError = hoursError != null,
                            supportingText = hoursError,
                            modifier = Modifier.weight(1f),
                        )
                        NumberField(
                            value = minutes,
                            onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                            label = stringResource(R.string.minutes),
                            isError = minutesError != null,
                            supportingText = minutesError,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    MoneyField(
                        value = rate,
                        onValueChange = { rate = sanitizeMoneyInput(it) },
                        label = stringResource(R.string.hourly_rate),
                        currencyCode = currencyCode,
                        isError = rateError != null,
                        supportingText = rateError,
                    )
                }
            }

            if (!bonusVisible || !penaltyVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!bonusVisible) {
                        OutlinedButton(
                            onClick = { bonusVisible = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.add_bonus))
                        }
                    }
                    if (!penaltyVisible) {
                        OutlinedButton(
                            onClick = { penaltyVisible = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.add_penalty))
                        }
                    }
                }
            }

            if (bonusVisible || penaltyVisible) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (bonusVisible) {
                            MoneyField(
                                value = bonus,
                                onValueChange = { bonus = sanitizeMoneyInput(it) },
                                label = stringResource(R.string.bonus),
                                currencyCode = currencyCode,
                                isError = bonusError != null,
                                supportingText = bonusError,
                            )
                        }
                        if (penaltyVisible) {
                            MoneyField(
                                value = penalty,
                                onValueChange = { penalty = sanitizeMoneyInput(it) },
                                label = stringResource(R.string.penalty),
                                currencyCode = currencyCode,
                                isError = penaltyError != null,
                                supportingText = penaltyError,
                            )
                        }
                    }
                }
            }

            CalculationSummary(
                draft = draft,
                totalMicros = totalMicros,
                currencyCode = currencyCode,
            )

            if (durationValid && rateValid && bonusWithinLimit && penaltyWithinLimit && !hasEffectiveData) {
                Text(
                    text = stringResource(R.string.empty_entry_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (operationErrorMessage != null) {
                Text(
                    text = operationErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = { draft?.let(onSave) },
                enabled = draft != null && totalMicros != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.save))
            }
            if (existing != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete_entry))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_entry)) },
            text = { Text(stringResource(R.string.delete_entry_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete(date)
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CalculationSummary(
    draft: WorkEntry?,
    totalMicros: Long?,
    currencyCode: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.calculation),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (draft != null && totalMicros != null) {
                val entryPay = SalaryCalculator.entryPay(draft)
                CalculationRow(
                    label = stringResource(R.string.calculation_base),
                    value = formatMoneyMicros(entryPay.basePayMicros, currencyCode),
                )
                CalculationRow(
                    label = "+ ${stringResource(R.string.calculation_bonus)}",
                    value = formatMoneyMicros(draft.bonusMicros, currencyCode),
                )
                CalculationRow(
                    label = "− ${stringResource(R.string.calculation_penalty)}",
                    value = formatMoneyMicros(draft.penaltyMicros, currencyCode),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.14f))
                CalculationRow(
                    label = stringResource(R.string.calculation_total),
                    value = formatMoneyMicros(totalMicros, currencyCode),
                    emphasized = true,
                )
            } else {
                Text(
                    text = stringResource(R.string.total_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalculationRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    val textStyle = if (emphasized) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = textStyle, fontWeight = if (emphasized) FontWeight.Bold else null)
        Text(text = value, style = textStyle, fontWeight = if (emphasized) FontWeight.Bold else null)
    }
}

private fun parseWholeNumberOrZero(text: String): Int? = if (text.isBlank()) 0 else text.toIntOrNull()

private fun parseMoneyOrNull(text: String): Long? = runCatching { parseDecimalMicros(text) }.getOrNull()

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    supportingText: String?,
    modifier: Modifier = Modifier,
) {
    val supportingContent: @Composable (() -> Unit)? = supportingText?.let { message ->
        { Text(message) }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supportingContent,
        isError = isError,
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    currencyCode: String,
    isError: Boolean,
    supportingText: String?,
    modifier: Modifier = Modifier,
) {
    val supportingContent: @Composable (() -> Unit)? = supportingText?.let { message ->
        { Text(message) }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(currencyCode) },
        supportingText = supportingContent,
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
