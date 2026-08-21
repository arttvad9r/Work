package com.worktime.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.calendar.MonthGrid
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.format.formatMoneyMicros
import java.time.DayOfWeek
import java.time.LocalDate
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
            TopAppBar(
                title = { Text(monthTitle, fontWeight = FontWeight.SemiBold) },
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
                    IconButton(
                        onClick = onSettingsClick,
                        enabled = state.isReady,
                    ) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                MonthSummaryCard(state = state, onSetHourlyRate = onSettingsClick)
                if (state.entries.isEmpty()) {
                    EmptyMonthState(onSetHourlyRate = onSettingsClick, showRateAction = state.defaultHourlyRateMicros == 0L)
                }
                CalendarGrid(state = state, onDayClick = onDayClick, locale = locale)
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(
    state: CalendarUiState,
    onSetHourlyRate: () -> Unit,
) {
    val summary = state.summary
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.monthly_income),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatMoneyMicros(summary.totalPayMicros, state.currencyCode),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                KpiMetric(
                    label = stringResource(R.string.worked_duration),
                    value = formatDuration(summary.workedMinutes),
                    modifier = Modifier.weight(1f),
                )
                KpiMetric(
                    label = stringResource(R.string.shift_count_label),
                    value = summary.shiftCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            if (summary.bonusMicros > 0L || summary.penaltyMicros > 0L) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(
                            R.string.base_pay_value,
                            formatMoneyMicros(summary.basePayMicros, state.currencyCode),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (summary.bonusMicros > 0L) {
                        Text(
                            stringResource(
                                R.string.bonus_value,
                                formatMoneyMicros(summary.bonusMicros, state.currencyCode),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (summary.penaltyMicros > 0L) {
                        Text(
                            stringResource(
                                R.string.penalty_value,
                                formatMoneyMicros(summary.penaltyMicros, state.currencyCode),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun KpiMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyMonthState(
    onSetHourlyRate: () -> Unit,
    showRateAction: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.empty_month_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.empty_month_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showRateAction) {
                TextButton(onClick = onSetHourlyRate) {
                    Text(stringResource(R.string.set_hourly_rate))
                }
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
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, locale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }

    val cells = MonthGrid.build(state.visibleMonth)
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                entry = state.entries[date],
                                isToday = date == today,
                                isSelected = date == state.selectedDate,
                                onClick = { onDayClick(date) },
                                locale = locale,
                            )
                        } else {
                            Spacer(Modifier.aspectRatio(0.9f))
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
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    locale: Locale,
) {
    val shape = RoundedCornerShape(14.dp)
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        entry != null -> MaterialTheme.colorScheme.secondaryContainer
        isToday -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surface
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
        if (entry?.bonusMicros ?: 0L > 0L) append(", ").append(stringResource(R.string.has_bonus))
        if (entry?.penaltyMicros ?: 0L > 0L) append(", ").append(stringResource(R.string.has_penalty))
    }

    Column(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(0.9f)
            .clip(shape)
            .background(background)
            .semantics(mergeDescendants = true) { contentDescription = a11yDescription }
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
        if (entry != null) {
            Column {
                if (entry.workedMinutes > 0) {
                    Text(
                        text = formatDuration(entry.workedMinutes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.bonusMicros > 0) Marker("+")
                    if (entry.penaltyMicros > 0) Marker("−")
                }
            }
        }
    }
}

@Composable
private fun Marker(text: String) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(14.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
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
