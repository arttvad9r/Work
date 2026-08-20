package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    val draft = runCatching {
        WorkEntry(
            date = date,
            workedMinutes = (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0),
            hourlyRateMicros = parseDecimalMicros(rate),
            bonusMicros = parseDecimalMicros(bonus),
            penaltyMicros = parseDecimalMicros(penalty),
            note = note.trim(),
        )
    }.getOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = MaterialTheme.typography.titleLarge,
            )

            Text("Worked", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 6, 8, 10, 12).forEach { quickHours ->
                    AssistChip(
                        onClick = { hours = quickHours.toString(); minutes = "0" },
                        label = { Text("${quickHours}h") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = hours,
                    onValueChange = { hours = it.filter(Char::isDigit).take(2) },
                    label = "Hours",
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                    label = "Minutes",
                    modifier = Modifier.weight(1f),
                )
            }

            MoneyField(value = rate, onValueChange = { rate = sanitizeMoneyInput(it) }, label = "Hourly rate")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyField(
                    value = bonus,
                    onValueChange = { bonus = sanitizeMoneyInput(it) },
                    label = "Bonus",
                    modifier = Modifier.weight(1f),
                )
                MoneyField(
                    value = penalty,
                    onValueChange = { penalty = sanitizeMoneyInput(it) },
                    label = "Penalty",
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            HorizontalDivider()
            val total = draft?.let { SalaryCalculator.entryPay(it).totalPayMicros }
            Text(
                text = "Total: ${total?.let(::formatDecimalMicros) ?: "—"} $currencyCode",
                style = MaterialTheme.typography.titleMedium,
            )

            Button(
                onClick = { draft?.let(onSave) },
                enabled = draft != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
            if (existing != null) {
                OutlinedButton(
                    onClick = { onDelete(date) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete entry")
                }
            }
        }
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
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

private fun sanitizeMoneyInput(value: String): String = value
    .replace(',', '.')
    .filter { it.isDigit() || it == '.' }
    .let { filtered ->
        val firstDot = filtered.indexOf('.')
        if (firstDot < 0) filtered else {
            filtered.take(firstDot + 1) + filtered.drop(firstDot + 1).replace(".", "").take(6)
        }
    }
    .take(18)

private fun parseDecimalMicros(text: String): Long {
    if (text.isBlank()) return 0L
    val normalized = text.replace(',', '.')
    return BigDecimal(normalized)
        .movePointRight(6)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
        .also { require(it >= 0) }
}

private fun formatDecimalMicros(micros: Long): String = BigDecimal.valueOf(micros, 6)
    .stripTrailingZeros()
    .toPlainString()
