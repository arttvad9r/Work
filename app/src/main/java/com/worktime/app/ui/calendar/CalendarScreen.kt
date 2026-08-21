package com.worktime.app.ui.calendar

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.components.PlainDragHandle
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
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
import kotlinx.coroutines.launch

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
    val summarySheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = summarySheetState,
    )
    val scope = rememberCoroutineScope()
    val summaryTargetExpanded = summarySheetState.targetValue == SheetValue.Expanded
    val closeSummaryBehind: (() -> Unit) -> Unit = { action ->
        val shouldCollapse =
            summarySheetState.currentValue == SheetValue.Expanded ||
                summarySheetState.targetValue == SheetValue.Expanded

        action()

        if (shouldCollapse) {
            scope.launch {
                runCatching { summarySheetState.partialExpand() }
            }
        }
    }
    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 88.dp,
        sheetDragHandle = null,
        // The visual handle lives in sheetContent. Avoid Material's drag-handle
        // slot because it adds an accessibility tooltip on long press.
        sheetSwipeEnabled = true,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 0.dp,
        sheetContainerColor = if (summaryTargetExpanded) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            Color.Transparent
        },
        sheetContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PlainDragHandle()
                }
                // Do not compose summary text while collapsed: it prevents delayed
                // text flashes behind the closed sheet.
                if (summaryTargetExpanded) {
                    FullSummaryPanel(state = state)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(52.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Crossfade(
                        targetState = monthTitle,
                        animationSpec = tween(durationMillis = 180),
                        label = "monthTitle",
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { closeSummaryBehind(onPreviousMonth) }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.previous_month),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { closeSummaryBehind(onNextMonth) }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = stringResource(R.string.next_month),
                        )
                    }
                    IconButton(
                        onClick = { closeSummaryBehind(onSettingsClick) },
                        enabled = state.isReady,
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
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
                    onDayClick = { date -> closeSummaryBehind { onDayClick(date) } },
                    locale = locale,
                    modifier = Modifier.height(392.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                CollapsedSummaryCard(state = state)
                Spacer(modifier = Modifier.height(4.dp))
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
private fun CollapsedSummaryCard(
    state: CalendarUiState,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    val locale = LocalLocale.current.platformLocale
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SummaryRow(stringResource(R.string.shift_count_label), summary.shiftCount.toString())
            SummaryRow(
                stringResource(R.string.worked_duration),
                formatDurationCompact(summary.workedMinutes),
            )
            SummaryRow(
                stringResource(R.string.monthly_income),
                formatAmountMicros(summary.totalPayMicros, locale),
            )
        }
    }
}

@Composable
private fun FullSummaryPanel(
    state: CalendarUiState,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    val locale = LocalLocale.current.platformLocale
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "${stringResource(R.string.monthly_summary)}:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SummaryRow(stringResource(R.string.shift_count_label), summary.shiftCount.toString())
            SummaryRow(
                stringResource(R.string.worked_duration),
                formatDurationCompact(summary.workedMinutes),
            )
            if (summary.bonusMicros > 0L) {
                SummaryRow(
                    stringResource(R.string.calculation_bonus),
                    "+${formatAmountMicros(summary.bonusMicros, locale)}",
                )
            }
            if (summary.penaltyMicros > 0L) {
                SummaryRow(
                    stringResource(R.string.calculation_penalty),
                    "−${formatAmountMicros(summary.penaltyMicros, locale)}",
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow(
                stringResource(R.string.calculation_total),
                formatAmountMicros(summary.totalPayMicros, locale),
                emphasized = true,
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
        )
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
    isInVisibleMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    locale: Locale,
) {
    val visibleEntry = entry.takeIf { isInVisibleMonth }
    val shape = RoundedCornerShape(9.dp)
    val targetBackground = when {
        !isInVisibleMonth -> MaterialTheme.colorScheme.surfaceContainerLowest
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        visibleEntry != null -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(durationMillis = 160),
        label = "dayCellBackground",
    )
    val foreground = when {
        !isInVisibleMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        visibleEntry != null -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val totalMicros = visibleEntry?.let {
        runCatching { SalaryCalculator.entryPay(it).totalPayMicros }.getOrNull()
    }
    val dateLabel = date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale),
    )
    val a11yDescription = buildString {
        append(dateLabel)
        if (isToday) append(", ").append(stringResource(R.string.today))
        if (isSelected) append(", ").append(stringResource(R.string.day_selected))
        if (visibleEntry != null && visibleEntry.workedMinutes > 0) {
            append(", ").append(formatDuration(visibleEntry.workedMinutes))
        }
        if (totalMicros != null) {
            append(", ").append(formatAmountMicros(totalMicros, locale))
        }
        if (visibleEntry?.bonusMicros ?: 0L > 0L) append(", ").append(stringResource(R.string.has_bonus))
        if (visibleEntry?.penaltyMicros ?: 0L > 0L) append(", ").append(stringResource(R.string.has_penalty))
    }

    Box(
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
                if (isToday && isInVisibleMonth) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) { contentDescription = a11yDescription }
            .clickable(enabled = isInVisibleMonth, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            modifier = Modifier.align(Alignment.TopStart),
            style = MaterialTheme.typography.titleSmall,
            color = foreground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )

        if (visibleEntry != null) {
            MarkerGroup(
                hasBonus = visibleEntry.bonusMicros > 0L,
                hasPenalty = visibleEntry.penaltyMicros > 0L,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            if (visibleEntry.workedMinutes > 0) {
                Text(
                    text = formatDurationCompact(visibleEntry.workedMinutes),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            if (totalMicros != null) {
                Text(
                    text = formatCellMoney(totalMicros, locale),
                    modifier = Modifier.align(Alignment.BottomCenter),
                    style = MaterialTheme.typography.labelSmall,
                    color = foreground.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MarkerGroup(
    hasBonus: Boolean,
    hasPenalty: Boolean,
    modifier: Modifier = Modifier,
) {
    if (hasBonus && hasPenalty) {
        Row(
            modifier = modifier
                .width(22.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    } else if (hasBonus) {
        Marker(isBonus = true, modifier = modifier)
    } else if (hasPenalty) {
        Marker(isBonus = false, modifier = modifier)
    }
}

@Composable
private fun Marker(
    isBonus: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(start = 1.dp)
            .size(12.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (isBonus) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isBonus) Icons.Rounded.Add else Icons.Rounded.Remove,
            contentDescription = null,
            modifier = Modifier.size(8.dp),
            tint = if (isBonus) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
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
