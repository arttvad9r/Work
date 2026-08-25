package com.worktime.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worktime.app.R
import com.worktime.app.ui.calendar.YearSummary
import com.worktime.app.ui.components.AppTopBar
import com.worktime.app.ui.components.LabelValueRow
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.formatWholeAmountMicros
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun YearSummaryScreen(
    summary: YearSummary?,
    onDismiss: () -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale

    BackHandler(onBack = onDismiss)

    // Surface sets LocalContentColor=onSurface so titles/icons follow the theme.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            AppTopBar(
                title = stringResource(R.string.year_summary),
                onBack = onDismiss,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousYear, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_year),
                    )
                }
                Text(
                    text = (summary?.year ?: "").toString(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onNextYear, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.year_income),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(
                                R.string.amount_with_currency,
                                formatWholeAmountMicros(summary.total.totalPayMicros, locale),
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.Medium,
                            color = if (summary.total.totalPayMicros < 0L) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                        )
                    }

                    LabelValueRow(
                        label = stringResource(R.string.shift_count_label),
                        value = summary.total.shiftCount.toString(),
                        modifier = Modifier.height(28.dp),
                    )
                    LabelValueRow(
                        label = stringResource(R.string.worked_duration),
                        value = formatDurationCompact(summary.total.workedMinutes),
                        modifier = Modifier.height(28.dp),
                    )
                    if (summary.monthsWithData > 0) {
                        LabelValueRow(
                            label = stringResource(R.string.average_working_month),
                            value = formatWholeAmountMicros(
                                summary.total.totalPayMicros / summary.monthsWithData,
                                locale,
                            ),
                            modifier = Modifier.height(28.dp),
                        )
                        if (summary.total.shiftCount > 0) {
                            LabelValueRow(
                                label = stringResource(R.string.average_shift),
                                value = formatDurationCompact(
                                    summary.total.workedMinutes / summary.total.shiftCount,
                                ),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                    if (summary.total.bonusMicros > 0L) {
                        LabelValueRow(
                            label = stringResource(R.string.year_bonuses),
                            value = "+${formatWholeAmountMicros(summary.total.bonusMicros, locale)}",
                            modifier = Modifier.height(28.dp),
                        )
                    }
                    if (summary.total.penaltyMicros > 0L) {
                        LabelValueRow(
                            label = stringResource(R.string.calculation_penalty),
                            value = "−${formatWholeAmountMicros(summary.total.penaltyMicros, locale)}",
                            modifier = Modifier.height(28.dp),
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        text = stringResource(R.string.by_month),
                        modifier = Modifier.padding(top = 0.dp, bottom = 0.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        Month.entries.forEachIndexed { index, month ->
                            val monthTotal = summary.months[month.value - 1]
                            val empty = !summary.monthHasData.getOrElse(index) { false }
                            MonthLine(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 28.dp),
                                label = monthDisplayName(month, locale),
                                detail = if (empty) {
                                    null
                                } else {
                                    "${monthTotal.shiftCount} · ${formatDurationCompact(monthTotal.workedMinutes)}"
                                },
                                amount = formatWholeAmountMicros(monthTotal.totalPayMicros, locale),
                                dimmed = empty,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthLine(
    modifier: Modifier = Modifier,
    label: String,
    detail: String?,
    amount: String,
    dimmed: Boolean,
) {
    val alpha = if (dimmed) 0.45f else 1f
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.alpha(alpha).weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            maxLines = 1,
        )
        Text(
            text = detail ?: "—",
            modifier = Modifier.width(100.dp).alpha(alpha),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (detail == null) TextAlign.Center else TextAlign.End,
            maxLines = 1,
        )
        Text(
            text = if (dimmed) "—" else amount,
            modifier = Modifier.width(100.dp).alpha(alpha),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            fontWeight = FontWeight.Medium,
            textAlign = if (dimmed) TextAlign.Center else TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

private fun monthDisplayName(month: Month, locale: java.util.Locale): String =
    month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }
