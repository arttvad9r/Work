package com.worktime.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.ui.calendar.YearSummary
import com.worktime.app.ui.components.PlainDragHandle
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearSummarySheet(
    summary: YearSummary?,
    onDismiss: () -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
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
                text = stringResource(R.string.year_summary),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousYear, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.previous_year),
                    )
                }
                Text(
                    text = (summary?.year ?: "").toString(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onNextYear, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.next_year),
                    )
                }
            }

            if (summary == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "…",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = formatAmountMicros(summary.total.totalPayMicros, locale),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        SummaryLine(
                            label = stringResource(R.string.shift_count_label),
                            value = summary.total.shiftCount.toString(),
                        )
                        SummaryLine(
                            label = stringResource(R.string.worked_duration),
                            value = formatDurationCompact(summary.total.workedMinutes),
                        )
                        if (summary.monthsWithData > 0) {
                            SummaryLine(
                                label = stringResource(R.string.average_monthly_income),
                                value = formatAmountMicros(
                                    summary.total.totalPayMicros / summary.monthsWithData,
                                    locale,
                                ),
                            )
                            if (summary.total.shiftCount > 0) {
                                SummaryLine(
                                    label = stringResource(R.string.average_shift),
                                    value = formatDurationCompact(
                                        summary.total.workedMinutes / summary.total.shiftCount,
                                    ),
                                )
                            }
                        }
                        if (summary.total.bonusMicros > 0L) {
                            SummaryLine(
                                label = stringResource(R.string.bonus),
                                value = formatAmountMicros(summary.total.bonusMicros, locale),
                            )
                        }
                        if (summary.total.penaltyMicros > 0L) {
                            SummaryLine(
                                label = stringResource(R.string.penalty),
                                value = formatAmountMicros(summary.total.penaltyMicros, locale),
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.by_month),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Month.entries.forEach { month ->
                        val monthTotal = summary.months[month.value - 1]
                        val empty = monthTotal.workedMinutes == 0 && monthTotal.totalPayMicros == 0L
                        MonthLine(
                            label = monthDisplayName(month, locale),
                            amount = formatAmountMicros(monthTotal.totalPayMicros, locale),
                            detail = if (empty) {
                                null
                            } else {
                                "${monthTotal.shiftCount} · ${formatDurationCompact(monthTotal.workedMinutes)}"
                            },
                            dimmed = empty,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MonthLine(
    label: String,
    detail: String?,
    amount: String,
    dimmed: Boolean,
) {
    val alpha = if (dimmed) 0.45f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(alpha),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        Text(
            text = detail ?: "—",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Text(
            text = amount,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

private fun monthDisplayName(month: Month, locale: java.util.Locale): String =
    month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }
