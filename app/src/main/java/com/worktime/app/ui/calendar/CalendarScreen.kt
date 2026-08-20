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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.domain.calendar.MonthGrid
import com.worktime.app.domain.model.WorkEntry
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.visibleMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy")),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous month")
                    }
                },
                actions = {
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next month")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MonthSummaryCard(state)
            CalendarGrid(state = state, onDayClick = onDayClick)
        }
    }
}

@Composable
private fun MonthSummaryCard(state: CalendarUiState) {
    val summary = state.summary
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = formatMoney(summary.totalPayMicros, state.currencyCode),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(formatDuration(summary.workedMinutes), style = MaterialTheme.typography.bodyMedium)
                Text("${summary.shiftCount} shifts", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    state: CalendarUiState,
    onDayClick: (LocalDate) -> Unit,
) {
    val weekdays = (0 until 7).map { DayOfWeek.MONDAY.plus(it.toLong()) }
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }

    val cells = MonthGrid.build(state.visibleMonth)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                entry = state.entries[date],
                                isToday = date == LocalDate.now(),
                                onClick = { onDayClick(date) },
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
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val background = when {
        entry != null -> MaterialTheme.colorScheme.secondaryContainer
        isToday -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(0.9f)
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
        )
        if (entry != null) {
            Column {
                Text(
                    text = formatDuration(entry.workedMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
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
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "${hours}h" else "${hours}h ${remainder}m"
}

private fun formatMoney(micros: Long, currencyCode: String): String {
    val formatter = NumberFormat.getCurrencyInstance()
    runCatching { formatter.currency = Currency.getInstance(currencyCode) }
    return formatter.format(micros / 1_000_000.0)
}
