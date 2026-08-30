package com.worktime.app.ui.yearsummary

import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppMotion
import com.worktime.app.ui.components.AppTopBar
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import java.time.Month
import java.time.format.TextStyle as JavaTextStyle

@Composable
fun YearSummaryScreen(
    selectedYear: Int,
    summaries: Map<Int, YearSummary>,
    onDismiss: () -> Unit,
    onSelectYear: (Int) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val scope = rememberCoroutineScope()
    val pager = rememberYearSummaryPagerState(selectedYear)
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pager.pagerState,
        snapAnimationSpec = spring(
            dampingRatio = AppMotion.NoBounceDampingRatio,
            stiffness = YearSummaryPagerStiffness,
        ),
        snapPositionalThreshold = 0.25f,
    )
    YearSummaryPagerEffects(
        pager = pager,
        selectedYear = selectedYear,
        scope = scope,
        onSelectYear = onSelectYear,
    )

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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.rowMinHeight),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { pager.navigatePrevious(scope) },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.previous_year),
                        )
                    }
                    Text(
                        text = pager.displayedYear.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    IconButton(
                        onClick = { pager.navigateNext(scope) },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.next_year),
                        )
                    }
                }
            }

            HorizontalPager(
                state = pager.pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("year-summary-pager"),
                beyondViewportPageCount = 1,
                flingBehavior = pagerFlingBehavior,
                key = { page -> pager.yearForPage(page) },
            ) { page ->
                val pageYear = pager.yearForPage(page)
                val summary = summaries[pageYear]
                if (summary == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    YearSummaryContent(
                        summary = summary,
                        locale = locale,
                    )
                }
            }
        }
    }
}

@Composable
private fun YearSummaryContent(
    summary: YearSummary,
    locale: java.util.Locale,
) {
    val compactText = LocalDensity.current.fontScale >= 1.4f
    val metricStyle = if (compactText) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val monthStyle = if (compactText) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.bodyMedium
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.screenHorizontalPadding)
            .navigationBarsPadding()
            .padding(bottom = AppDimens.rowGap)
            .testTag("year-summary-content"),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        YearMetricRow(
            label = stringResource(R.string.year_income),
            value = stringResource(
                R.string.amount_with_currency,
                formatAmountMicros(summary.total.totalPayMicros, locale),
            ),
            style = metricStyle,
            valueColor = if (summary.total.totalPayMicros < 0L) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            emphasized = true,
        )
        YearMetricRow(
            label = stringResource(R.string.shift_count_label),
            value = summary.total.shiftCount.toString(),
            style = metricStyle,
        )
        YearMetricRow(
            label = stringResource(R.string.worked_duration),
            value = formatDurationCompact(summary.total.workedMinutes),
            style = metricStyle,
        )
        if (summary.monthsWithData > 0) {
            YearMetricRow(
                label = stringResource(R.string.average_working_month),
                value = formatAmountMicros(
                    summary.total.totalPayMicros / summary.monthsWithData,
                    locale,
                ),
                style = metricStyle,
            )
            if (summary.total.shiftCount > 0) {
                YearMetricRow(
                    label = stringResource(R.string.average_shift),
                    value = formatDurationCompact(
                        summary.total.workedMinutes / summary.total.shiftCount,
                    ),
                    style = metricStyle,
                )
            }
        }
        if (summary.total.bonusMicros > 0L) {
            YearMetricRow(
                label = stringResource(R.string.year_bonuses),
                value = "+${formatAmountMicros(summary.total.bonusMicros, locale)}",
                style = metricStyle,
            )
        }
        if (summary.total.penaltyMicros > 0L) {
            YearMetricRow(
                label = stringResource(R.string.calculation_penalty),
                value = "−${formatAmountMicros(summary.total.penaltyMicros, locale)}",
                style = metricStyle,
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        MonthSectionHeader(compactText = compactText)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("year-summary-months"),
        ) {
            Month.entries.forEachIndexed { index, month ->
                val monthTotal = summary.months[month.value - 1]
                val empty = !summary.monthHasData.getOrElse(index) { false }
                MonthLine(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("year-summary-month-${month.value}"),
                    label = monthDisplayName(month, locale),
                    detail = if (empty) {
                        null
                    } else {
                        "${monthTotal.shiftCount} · ${formatDurationCompact(monthTotal.workedMinutes)}"
                    },
                    amount = formatAmountMicros(monthTotal.totalPayMicros, locale),
                    dimmed = empty,
                    style = monthStyle,
                )
            }
        }
    }
}

@Composable
private fun YearMetricRow(
    label: String,
    value: String,
    style: TextStyle,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = style,
            fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MonthSectionHeader(compactText: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.by_month),
            modifier = Modifier.weight(1f),
            style = if (compactText) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.year_month_detail_header),
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.year_month_income_header),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthLine(
    modifier: Modifier = Modifier,
    label: String,
    detail: String?,
    amount: String,
    dimmed: Boolean,
    style: TextStyle,
) {
    val alpha = if (dimmed) 0.38f else 1f
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.alpha(alpha).weight(1f),
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detail ?: "—",
            modifier = Modifier.weight(1.2f).alpha(alpha),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (detail == null) TextAlign.Center else TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (dimmed) "—" else amount,
            modifier = Modifier.weight(1f).alpha(alpha),
            style = style,
            fontWeight = FontWeight.Medium,
            textAlign = if (dimmed) TextAlign.Center else TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun monthDisplayName(month: Month, locale: java.util.Locale): String =
    month.getDisplayName(JavaTextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }
