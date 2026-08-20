package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
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
    onDismiss: () -> Unit,
    onSave: (WorkEntry) -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    var hours by remember(date, existing) { mutableStateOf(((existing?.workedMinutes ?: 0) / 60).toString()) }
    var minutes by remember(date, existing) { mutableStateOf(((existing?.workedMinutes ?: 0) % 60).toString()) }
    var rate by remember(date, existing) {
        mutableStateOf(formatDecimalMicros(existing?.hourlyRateMicros ?: defaultHourlyRateMicros))
    }
    var bonus by remember(date, existing) { mutableStateOf(formatDecimalMicros(existing?.bonusMicros ?: 0L)) }
    var penalty by remember(date, existing) { mutableStateOf(formatDecimalMicros(existing?.penaltyMicros ?: 0L)) }
    var note by remember(date, existing) { mutableStateOf(existing?.note.orEmpty()) }
    var confirmDelete by remember(date, existing) { mutableStateOf(false) }

    val draft = runCatching {
        val parsedHours = hours.toIntOrNull() ?: 0
        val parsedMinutes = minutes.toIntOrNull() ?: 0
        require(parsedHours in 0..24)
        require(parsedMinutes in 0..59)
        require(parsedHours < 24 || parsedMinutes == 0)

        WorkEntry(
            date = date,
            workedMinutes = parsedHours * 60 + parsedMinutes,
            hourlyRateMicros = parseDecimalMicros(rate),
            bonusMicros = parseDecimalMicros(bonus),
            penaltyMicros = parseDecimalMicros(penalty),
            note = note.trim(),
        )
    }.getOrNull()?.takeIf { entry ->
        entry.workedMinutes > 0 || entry.bonusMicros > 0L || entry.penaltyMicros > 0L
    }

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
                date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                style = MaterialTheme.typography.titleLarge,
            )

            Text(stringResource(R.string.worked), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                    label = stringResource(R.string.minutes),
                    modifier = Modifier.weight(1f),
                )
            }

            MoneyField(
                value = rate,
                onValueChange = { rate = sanitizeMoneyInput(it) },
                label = stringResource(R.string.hourly_rate),
                currencyCode = currencyCode,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyField(
                    value = bonus,
                    onValueChange = { bonus = sanitizeMoneyInput(it) },
                    label = stringResource(R.string.bonus),
                    currencyCode = currencyCode,
                    modifier = Modifier.weight(1f),
                )
                MoneyField(
                    value = penalty,
                    onValueChange = { penalty = sanitizeMoneyInput(it) },
                    label = stringResource(R.string.penalty),
                    currencyCode = currencyCode,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text(stringResource(R.string.note)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            HorizontalDivider()
            val total = draft?.let { SalaryCalculator.entryPay(it).totalPayMicros }
            Text(
                text = total?.let {
                    stringResource(R.string.total_value, formatMoneyMicros(it, currencyCode))
                } ?: stringResource(R.string.total_unavailable),
                style = MaterialTheme.typography.titleMedium,
            )

            Button(
                onClick = { draft?.let(onSave) },
                enabled = draft != null,
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
                ) {
                    Text(stringResource(R.string.delete))
                }
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
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
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
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(currencyCode) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
