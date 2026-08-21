package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
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
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (WorkEntry) -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    var duration by rememberSaveable(date.toEpochDay(), existing) {
        mutableStateOf(formatDurationInput(existing?.workedMinutes ?: 0))
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
    var focusBonusOnExpand by remember { mutableStateOf(false) }
    var focusPenaltyOnExpand by remember { mutableStateOf(false) }
    val bonusFocusRequester = remember { FocusRequester() }
    val penaltyFocusRequester = remember { FocusRequester() }
    val showBonus = {
        bonusVisible = true
        focusBonusOnExpand = true
    }
    val showPenalty = {
        penaltyVisible = true
        focusPenaltyOnExpand = true
    }

    LaunchedEffect(focusBonusOnExpand) {
        if (focusBonusOnExpand) {
            bonusFocusRequester.requestFocus()
            focusBonusOnExpand = false
        }
    }
    LaunchedEffect(focusPenaltyOnExpand) {
        if (focusPenaltyOnExpand) {
            penaltyFocusRequester.requestFocus()
            focusPenaltyOnExpand = false
        }
    }

    var confirmDelete by rememberSaveable(date.toEpochDay(), existing) { mutableStateOf(false) }

    val parsedDuration = parseDurationInput(duration)
    val parsedHours = parsedDuration?.hours
    val parsedMinutes = parsedDuration?.minutes
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
    val durationError = when {
        !hoursValid -> stringResource(R.string.hours_range_error)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = date.format(
                    DateTimeFormatter.ofPattern("EEEE, d MMMM", LocalLocale.current.platformLocale),
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DurationField(
                    value = duration,
                    onValueChange = { duration = sanitizeDurationInput(it) },
                    label = stringResource(R.string.worked),
                    isError = durationError != null,
                    modifier = Modifier.weight(1f),
                )
                MoneyField(
                    value = rate,
                    onValueChange = { rate = sanitizeMoneyInput(it) },
                    label = stringResource(R.string.hourly_rate),
                    isError = rateError != null,
                    modifier = Modifier.weight(1f),
                )
            }

            if (!bonusVisible && !penaltyVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = showBonus,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                    ) {
                        Text(stringResource(R.string.add_bonus), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = showPenalty,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                    ) {
                        Text(stringResource(R.string.add_penalty), maxLines = 1)
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (bonusVisible) {
                        MoneyField(
                            value = bonus,
                            onValueChange = { bonus = sanitizeMoneyInput(it) },
                            label = stringResource(R.string.bonus),
                            isError = bonusError != null,
                            modifier = Modifier.focusRequester(bonusFocusRequester),
                        )
                    } else {
                        OutlinedButton(
                            onClick = showBonus,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp),
                        ) {
                            Text(stringResource(R.string.add_bonus), maxLines = 1)
                        }
                    }

                    if (penaltyVisible) {
                        MoneyField(
                            value = penalty,
                            onValueChange = { penalty = sanitizeMoneyInput(it) },
                            label = stringResource(R.string.penalty),
                            isError = penaltyError != null,
                            modifier = Modifier.focusRequester(penaltyFocusRequester),
                        )
                    } else {
                        OutlinedButton(
                            onClick = showPenalty,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp),
                        ) {
                            Text(stringResource(R.string.add_penalty), maxLines = 1)
                        }
                    }
                }
            }

            ValidationMessage(
                message = durationError
                    ?: rateError
                    ?: (if (bonusVisible) bonusError else null)
                    ?: (if (penaltyVisible) penaltyError else null),
            )

            CalculationSummary(
                draft = draft,
                totalMicros = totalMicros,
            )

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
                    .heightIn(min = 50.dp),
            ) {
                Text(stringResource(R.string.save))
            }
            if (existing != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
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
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.calculation),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (draft != null && totalMicros != null) {
                if (draft.bonusMicros > 0L || draft.penaltyMicros > 0L) {
                    val entryPay = SalaryCalculator.entryPay(draft)
                    CalculationRow(
                        label = stringResource(R.string.calculation_base),
                        value = formatAmountMicros(entryPay.basePayMicros),
                    )
                    if (draft.bonusMicros > 0L) {
                        CalculationRow(
                            label = "+ ${stringResource(R.string.calculation_bonus)}",
                            value = formatAmountMicros(draft.bonusMicros),
                        )
                    }
                    if (draft.penaltyMicros > 0L) {
                        CalculationRow(
                            label = "− ${stringResource(R.string.calculation_penalty)}",
                            value = formatAmountMicros(draft.penaltyMicros),
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.14f),
                    )
                }
                CalculationRow(
                    label = stringResource(R.string.calculation_total),
                    value = formatAmountMicros(totalMicros),
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

private data class DurationInput(val hours: Int, val minutes: Int)

private fun formatDurationInput(workedMinutes: Int): String {
    return formatDurationCompact(workedMinutes)
}

internal fun sanitizeDurationInput(value: String): String {
    val filtered = value.filter { it.isDigit() || it == ':' }
    val firstColon = filtered.indexOf(':')
    if (firstColon >= 0) {
        val hours = filtered.take(firstColon).filter(Char::isDigit).take(2)
        val minutes = filtered.drop(firstColon + 1).filter(Char::isDigit).take(2)
        return "$hours:$minutes"
    }

    val digits = filtered.filter(Char::isDigit).take(4)
    return when (digits.length) {
        0, 1 -> digits
        2 -> {
            if (digits.toInt() <= 24) digits else "${digits.take(1)}:${digits.drop(1)}"
        }
        3 -> {
            val twoDigitHours = digits.take(2).toInt()
            if (twoDigitHours <= 24) {
                "${digits.take(2)}:${digits.drop(2)}"
            } else {
                "${digits.take(1)}:${digits.drop(1)}"
            }
        }
        else -> "${digits.take(2)}:${digits.drop(2)}"
    }
}

private fun parseDurationInput(text: String): DurationInput? {
    if (text.isBlank()) return DurationInput(hours = 0, minutes = 0)
    val parts = text.split(':')
    if (parts.size > 2) return null
    val hours = parts[0].ifBlank { "0" }.toIntOrNull() ?: return null
    val minutes = parts.getOrNull(1)?.ifBlank { "0" }?.toIntOrNull() ?: 0
    return DurationInput(hours = hours, minutes = minutes)
}

private fun parseMoneyOrNull(text: String): Long? = runCatching { parseDecimalMicros(text) }.getOrNull()

@Composable
private fun DurationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = { updated ->
            val sanitized = sanitizeDurationInput(updated.text)
            fieldValue = TextFieldValue(
                text = sanitized,
                selection = TextRange(sanitized.length),
            )
            onValueChange(sanitized)
        },
        label = { Text(label, maxLines = 1) },
        placeholder = {
            Text(
                text = stringResource(R.string.duration_placeholder),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        },
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.titleMedium.fontSize,
        ),
        isError = isError,
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused && fieldValue.text == "0") {
                fieldValue = TextFieldValue("", TextRange(0))
                onValueChange("")
            } else if (!focusState.isFocused && fieldValue.text.isBlank()) {
                fieldValue = TextFieldValue("0", TextRange(1))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = { updated ->
            val sanitized = sanitizeMoneyInput(updated.text)
            fieldValue = TextFieldValue(
                text = sanitized,
                selection = TextRange(sanitized.length),
            )
            onValueChange(sanitized)
        },
        label = { Text(label) },
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.titleMedium.fontSize,
        ),
        isError = isError,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (focusState.isFocused && fieldValue.text == "0") {
                    fieldValue = fieldValue.copy(
                        selection = TextRange(0, fieldValue.text.length),
                    )
                }
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun ValidationMessage(message: String?) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
