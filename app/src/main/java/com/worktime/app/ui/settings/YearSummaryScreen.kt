package com.worktime.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.worktime.app.R
import com.worktime.app.ui.calendar.YearSummary
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppTopBar
import com.worktime.app.ui.components.LabelValueRow
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.formatWholeAmountMicros
import java.time.Month
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
                    .heightIn(min = AppDimens.rowMinHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousYear) {
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
                    fontWeight = FontWeight.Medium,
                )
                IconButton(onClick = onNextYear) {
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
                        .padding(vertical = AppDimens.sectionSpacing * 3),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = AppDimens.screenHorizontalPadding)
                        .navigationBarsPadding()
                        .padding(bottom = AppDimens.rowGap),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 30.dp),
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
                            style = MaterialTheme.typography.bodyLarge,
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
                        modifier = Modifier.heightIn(min = 28.dp),
                    )
                    LabelValueRow(
                        label = stringResource(R.string.worked_duration),
                        value = formatDurationCompact(summary.total.workedMinutes),
                        modifier = Modifier.heightIn(min = 28.dp),
                    )
                    if (summary.monthsWithData > 0) {
                        LabelValueRow(
                            label = stringResource(R.string.average_working_month),
                            value = formatWholeAmountMicros(
                                summary.total.totalPayMicros / summary.monthsWithData,
                                locale,
                            ),
                            modifier = Modifier.heightIn(min = 28.dp),
                        )
                        if (summary.total.shiftCount > 0) {
                            LabelValueRow(
                                label = stringResource(R.string.average_shift),
                                value = formatDurationCompact(
                                    summary.total.workedMinutes / summary.total.shiftCount,
                                ),
                                modifier = Modifier.heightIn(min = 28.dp),
                            )
                        }
                    }
                    if (summary.total.bonusMicros > 0L) {
                        LabelValueRow(
                            label = stringResource(R.string.year_bonuses),
                            value = "+${formatWholeAmountMicros(summary.total.bonusMicros, locale)}",
                            modifier = Modifier.heightIn(min = 28.dp),
                        )
                    }
                    if (summary.total.penaltyMicros > 0L) {
                        LabelValueRow(
                            label = stringResource(R.string.calculation_penalty),
                            value = "−${formatWholeAmountMicros(summary.total.penaltyMicros, locale)}",
                            modifier = Modifier.heightIn(min = 28.dp),
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        text = stringResource(R.string.by_month),
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
                                modifier = Modifier.weight(1f),
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
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.alpha(alpha).weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detail ?: "—",
            modifier = Modifier.weight(1.2f).alpha(alpha),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (detail == null) TextAlign.Center else TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (dimmed) "—" else amount,
            modifier = Modifier.weight(1f).alpha(alpha),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = if (dimmed) TextAlign.Center else TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun monthDisplayName(month: Month, locale: java.util.Locale): String =
    month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }
