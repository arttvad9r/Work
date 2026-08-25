package com.worktime.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.ui.components.PlainDragHandle
import com.worktime.app.ui.components.CompactMoneyField
import com.worktime.app.ui.format.parseDecimalMicros
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class RatePeriod { CURRENT_MONTH, CUSTOM }

private enum class DateField { Start, End }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeRateSheet(
    visibleMonth: YearMonth,
    initialRange: ClosedRange<LocalDate>?,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onChangeRate: (LocalDate, LocalDate, Long) -> Unit,
) {
    var period by rememberSaveable(initialRange) {
        mutableStateOf(if (initialRange != null) RatePeriod.CUSTOM else RatePeriod.CURRENT_MONTH)
    }
    var customStart by rememberSaveable(initialRange) {
        mutableStateOf(initialRange?.start)
    }
    var customEnd by rememberSaveable(initialRange) {
        mutableStateOf(initialRange?.endInclusive)
    }
    var rate by rememberSaveable { mutableStateOf("") }
    var pickingDate by rememberSaveable { mutableStateOf<DateField?>(null) }
    var confirmChange by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationErrorMessage) {
        if (!operationErrorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(operationErrorMessage)
        }
    }

    val startDate: LocalDate?
    val endDate: LocalDate?
    if (period == RatePeriod.CURRENT_MONTH) {
        startDate = visibleMonth.atDay(1)
        endDate = visibleMonth.atEndOfMonth()
    } else {
        startDate = customStart
        endDate = customEnd
    }

    val parsedRate = runCatching { parseDecimalMicros(rate) }.getOrNull()
    val rateValid = parsedRate != null && parsedRate > 0L && parsedRate <= MoneyLimits.MAX_COMPONENT_MICROS
    val rangeValid = startDate != null && endDate != null && startDate <= endDate
    val canChange = rateValid && rangeValid

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
    val rateLabel = stringResource(R.string.hourly_rate)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlainDragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

                Text(
                    text = stringResource(R.string.rate_for_period),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = period == RatePeriod.CURRENT_MONTH,
                        onClick = { period = RatePeriod.CURRENT_MONTH },
                        label = {
                            Text(
                                text = stringResource(R.string.current_month),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = period == RatePeriod.CUSTOM,
                        onClick = { period = RatePeriod.CUSTOM },
                        label = {
                            Text(
                                text = stringResource(R.string.custom_period),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (period == RatePeriod.CUSTOM) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            DateRow(
                                label = stringResource(R.string.start_date),
                                value = customStart?.format(dateFormatter),
                                onClick = { pickingDate = DateField.Start },
                            )
                            DateRow(
                                label = stringResource(R.string.end_date),
                                value = customEnd?.format(dateFormatter),
                                onClick = { pickingDate = DateField.End },
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = rateLabel,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        CompactMoneyField(
                            text = rate,
                            onTextChange = { rate = it },
                            isError = rate.isNotEmpty() && !rateValid,
                            contentDescription = rateLabel,
                        )
                    }
                }

                Button(
                    onClick = { confirmChange = true },
                    enabled = canChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                ) {
                    Text(stringResource(R.string.change_rate))
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }
    }

    when (val target = pickingDate) {
        DateField.Start -> DatePickerSheetDialog(
            initialDate = customStart ?: visibleMonth.atDay(1),
            onSelect = {
                customStart = it
                pickingDate = null
            },
            onDismiss = { pickingDate = null },
        )

        DateField.End -> DatePickerSheetDialog(
            initialDate = customEnd ?: visibleMonth.atEndOfMonth(),
            onSelect = {
                customEnd = it
                pickingDate = null
            },
            onDismiss = { pickingDate = null },
        )

        null -> Unit
    }

    if (confirmChange) {
        AlertDialog(
            onDismissRequest = { confirmChange = false },
            title = { Text(stringResource(R.string.change_rate_confirmation_title)) },
            text = { Text(stringResource(R.string.change_rate_confirmation_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmChange = false
                        val start = startDate
                        val end = endDate
                        val safeRate = parsedRate
                        if (start != null && end != null && safeRate != null && safeRate > 0L) {
                            onChangeRate(start, end, safeRate)
                        }
                    },
                ) { Text(stringResource(R.string.change_rate)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmChange = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun DateRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheetDialog(
    initialDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    pickerState.selectedDateMillis?.let { onSelect(it.toLocalDate()) }
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
