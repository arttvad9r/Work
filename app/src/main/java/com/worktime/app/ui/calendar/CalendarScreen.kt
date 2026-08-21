package com.worktime.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.format.formatMoneyMicros
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val monthTitle = state.visibleMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.previous_month),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = stringResource(R.string.next_month),
                        )
                    }
                    IconButton(onClick = onSettingsClick, enabled = state.isReady) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!state.isReady) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                CalendarCard(
                    state = state,
                    onDayClick = onDayClick,
                    locale = locale,
                    modifier = Modifier.weight(1f),
                )
                MonthSummaryCard(state = state, onSetHourlyRate = onSettingsClick)
                if (state.entries.isEmpty()) {
                    EmptyMonthState(
                        onSetHourlyRate = onSettingsClick,
                        showRateAction = state.defaultHourlyRateMicros == 0L,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    state: CalendarUiState,
    onDayClick: (LocalDate) -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CalendarGrid(state = state, onDayClick = onDayClick, locale = locale)
        }
    }
}

@Composable
private fun MonthSummaryCard(
    state: CalendarUiState,
    onSetHourlyRate: () -> Unit,
) {
    val summary = state.summary
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.monthly_income),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = formatMoneyMicros(summary.totalPayMicros, state.currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KpiMetric(
                    label = stringResource(R.string.shift_count_label),
                    value = summary.shiftCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                KpiMetric(
                    label = stringResource(R.string.worked_duration),
                    value = formatDuration(summary.workedMinutes),
                    modifier = Modifier.weight(1f),
                )
            }

            if (summary.bonusMicros > 0L || summary.penaltyMicros > 0L) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SummaryLine(
                        stringResource(
                            R.string.base_pay_value,
                            formatMoneyMicros(summary.basePayMicros, state.currencyCode),
                        ),
                    )
                    if (summary.bonusMicros > 0L) {
                        SummaryLine(
                            stringResource(
                                R.string.bonus_value,
                                formatMoneyMicros(summary.bonusMicros, state.currencyCode),
                            ),
                        )
                    }
                    if (summary.penaltyMicros > 0L) {
                        SummaryLine(
                            stringResource(
                                R.string.penalty_value,
                                formatMoneyMicros(summary.penaltyMicros, state.currencyCode),
                            ),
                        )
                    }
                }
            }

            if (state.defaultHourlyRateMicros == 0L) {
                TextButton(onClick = onSetHourlyRate) {
                    Text(stringResource(R.string.set_hourly_rate))
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
    )
}

@Composable
private fun KpiMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyMonthState(
    onSetHourlyRate: () -> Unit,
    showRateAction: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.empty_month_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.empty_month_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (showRateAction) {
            TextButton(onClick = onSetHourlyRate) {
                Text(stringResource(R.string.set_hourly_rate))
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    state: CalendarUiState,
    onDayClick: (LocalDate) -> Unit,
    locale: Locale,
) {
    val weekdays = (0 until 7).map { DayOfWeek.MONDAY.plus(it.toLong()) }
    val firstDay = state.visibleMonth.atDay(1)
    val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
    val cells = (0L until 42L).map { offset -> gridStart.plusDays(offset) }
    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    week.forEach { date ->
                        val isInVisibleMonth = YearMonth.from(date) == state.visibleMonth
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        ) {
                            DayCell(
                                date = date,
                                entry = state.entries[date],
                                currencyCode = state.currencyCode,
                                isInVisibleMonth = isInVisibleMonth,
                                isToday = date == today,
                                isSelected = date == state.selectedDate,
                                onClick = { onDayClick(date) },
                                locale = locale,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    entry: WorkEntry?,
    currencyCode: String,
    isInVisibleMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    locale: Locale,
) {
    val shape = RoundedCornerShape(9.dp)
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        entry != null -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val foreground = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        entry != null -> MaterialTheme.colorScheme.onSecondaryContainer
        !isInVisibleMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val totalMicros = entry?.let {
        runCatching { SalaryCalculator.entryPay(it).totalPayMicros }.getOrNull()
    }
    val dateLabel = date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale),
    )
    val a11yDescription = buildString {
        append(dateLabel)
        if (isToday) append(", ").append(stringResource(R.string.today))
        if (isSelected) append(", ").append(stringResource(R.string.day_selected))
        if (entry != null && entry.workedMinutes > 0) {
            append(", ").append(formatDuration(entry.workedMinutes))
        }
        if (totalMicros != null) {
            append(", ").append(formatMoneyMicros(totalMicros, currencyCode, locale))
        }
        if (entry?.bonusMicros ?: 0L > 0L) append(", ").append(stringResource(R.string.has_bonus))
        if (entry?.penaltyMicros ?: 0L > 0L) append(", ").append(stringResource(R.string.has_penalty))
    }

    Column(
        modifier = Modifier
            .padding(1.dp)
            .fillMaxSize()
            .clip(shape)
            .background(background)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = if (isInVisibleMonth) 0.42f else 0.18f,
                ),
                shape = shape,
            )
            .then(
                if (isToday) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) { contentDescription = a11yDescription }
            .clickable(enabled = isInVisibleMonth, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = foreground,
                fontWeight = if (isToday || entry != null) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
            if (entry != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.bonusMicros > 0L) Marker("+")
                    if (entry.penaltyMicros > 0L) Marker("−")
                }
            }
        }
        if (entry != null) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                if (entry.workedMinutes > 0) {
                    Text(
                        text = formatDuration(entry.workedMinutes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                if (totalMicros != null) {
                    Text(
                        text = formatCellMoney(totalMicros, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = foreground.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun Marker(text: String) {
    Box(
        modifier = Modifier
            .padding(start = 1.dp)
            .size(11.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
        )
    }
}

private fun formatCellMoney(micros: Long, locale: Locale): String {
    return NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        minimumFractionDigits = 0
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }.format(BigDecimal.valueOf(micros, 6))
}

@Composable
private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) {
        stringResource(R.string.duration_hours, hours)
    } else {
        stringResource(R.string.duration_hours_minutes, hours, remainder)
    }
}
