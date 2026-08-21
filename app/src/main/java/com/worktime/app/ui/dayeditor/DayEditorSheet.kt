package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import java.util.Locale

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
    var note by rememberSaveable(date.toEpochDay(), existing) { mutableStateOf(existing?.note.orEmpty()) }
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
                note = note.trim(),
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", LocalLocale.current.platformLocale)),
                style = MaterialTheme.typography.titleLarge,
            )

            Text(stringResource(R.string.worked), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(4, 6, 8, 10, 12).forEach { quickHours ->
                    AssistChip(
                        onClick = {
                            hours = quickHours.toString()
                            minutes = "0"
                        },
                        label = { Text(stringResource(R.string.quick_hours, quickHours)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            MoneyField(
                value = bonus,
                onValueChange = { bonus = sanitizeMoneyInput(it) },
                label = stringResource(R.string.bonus),
                currencyCode = currencyCode,
                isError = bonusError != null,
                supportingText = bonusError,
            )
            MoneyField(
                value = penalty,
                onValueChange = { penalty = sanitizeMoneyInput(it) },
                label = stringResource(R.string.penalty),
                currencyCode = currencyCode,
                isError = penaltyError != null,
                supportingText = penaltyError,
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(MoneyLimits.MAX_NOTE_LENGTH) },
                label = { Text(stringResource(R.string.note)) },
                supportingText = {
                    Text(stringResource(R.string.note_length, note.length, MoneyLimits.MAX_NOTE_LENGTH))
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            HorizontalDivider()
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
            if (existing != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.calculation),
            style = MaterialTheme.typography.labelLarge,
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
            HorizontalDivider()
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
        Text(text = label, style = textStyle, fontWeight = if (emphasized) FontWeight.SemiBold else null)
        Text(text = value, style = textStyle, fontWeight = if (emphasized) FontWeight.SemiBold else null)
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
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
