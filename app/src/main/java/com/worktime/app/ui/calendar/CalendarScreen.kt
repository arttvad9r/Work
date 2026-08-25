package com.worktime.app.ui.calendar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.components.LabelValueRow
import com.worktime.app.ui.components.PlainDragHandle
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.formatWholeAmountMicros
import com.worktime.app.ui.theme.LocalWorkTimeColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenYearSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    var monthPickerOpen by rememberSaveable { mutableStateOf(false) }
    val summarySheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = summarySheetState,
    )
    val scope = rememberCoroutineScope()
    val summaryTargetExpanded = summarySheetState.targetValue == SheetValue.Expanded
    val summaryContentAlpha by animateFloatAsState(
        targetValue = if (summaryTargetExpanded) 1f else 0f,
        animationSpec = if (summaryTargetExpanded) {
            tween(durationMillis = 160, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "summaryContentAlpha",
    )
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
    val toggleSummary: () -> Unit = {
        scope.launch {
            if (
                summarySheetState.currentValue == SheetValue.Expanded ||
                summarySheetState.targetValue == SheetValue.Expanded
            ) {
                summarySheetState.partialExpand()
            } else {
                summarySheetState.expand()
            }
        }
    }

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 88.dp,
        // Material wraps every non-null handle slot in DragHandleWithTooltip. Keep
        // the slot null and render our handle as stable sheet content instead.
        sheetDragHandle = null,
        sheetSwipeEnabled = true,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 0.dp,
        // Collapsed: transparent sheet so the summary strip floats on the calendar
        // background. Expanded: opaque surface for the full report.
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(
            alpha = summaryContentAlpha,
        ),
        sheetContent = {
            // The strip and the report stay composed in both states. Only visibility
            // and semantics change, so the measured sheet height and swipe anchors
            // never jump while a drag is in progress.
            Column(modifier = Modifier.fillMaxWidth()) {
                PlainDragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = toggleSummary,
                    accessibilityLabel = stringResource(R.string.monthly_summary),
                )
                SummaryStrip(
                    state = state,
                    expanded = summaryTargetExpanded,
                    locale = locale,
                    onClick = toggleSummary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                MonthlySummaryPanel(
                    state = state,
                    onOpenYearSummary = { closeSummaryBehind(onOpenYearSummary) },
                    locale = locale,
                    modifier = Modifier
                        .alpha(summaryContentAlpha)
                        .then(
                            if (summaryTargetExpanded) {
                                Modifier
                            } else {
                                Modifier.clearAndSetSemantics { }
                            },
                        ),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CalendarHeader(
                state = state,
                locale = locale,
                onPreviousMonth = { closeSummaryBehind(onPreviousMonth) },
                onNextMonth = { closeSummaryBehind(onNextMonth) },
                onSelectMonth = { monthPickerOpen = true },
                onSettingsClick = { closeSummaryBehind(onSettingsClick) },
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
                CalendarGrid(
                    state = state,
                    onDayClick = { date -> closeSummaryBehind { onDayClick(date) } },
                    onSwipeToPrevious = { closeSummaryBehind(onPreviousMonth) },
                    onSwipeToNext = { closeSummaryBehind(onNextMonth) },
                    locale = locale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(max = 480.dp),
                )
                if (state.entries.isEmpty()) {
                    EmptyMonthPrompt(
                        onOpenToday = { closeSummaryBehind { onDayClick(LocalDate.now()) } },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    if (monthPickerOpen) {
        MonthPickerDialog(
            visibleMonth = state.visibleMonth,
            locale = locale,
            onSelect = { month ->
                monthPickerOpen = false
                closeSummaryBehind { onSelectMonth(month) }
            },
            onDismiss = { monthPickerOpen = false },
        )
    }
}

@Composable
private fun CalendarHeader(
    state: CalendarUiState,
    locale: Locale,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val monthTitle = state.visibleMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_month),
                )
            }
            Text(
                text = monthTitle,
                modifier = Modifier
                    .clickable(onClick = onSelectMonth, onClickLabel = stringResource(R.string.select_month))
                    .padding(horizontal = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_month),
                )
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            IconButton(onClick = onSettingsClick, enabled = state.isReady) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings),
                )
            }
        }
    }
}

@Composable
private fun SummaryStrip(
    state: CalendarUiState,
    expanded: Boolean,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        // Collapsed state: the tappable month summary strip.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (expanded) 0f else 1f)
                .then(if (expanded) Modifier.clearAndSetSemantics { } else Modifier)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(
                    enabled = !expanded,
                    onClickLabel = stringResource(R.string.monthly_summary),
                    onClick = onClick,
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summaryLine(
                    shiftCount = summary.shiftCount,
                    workedMinutes = summary.workedMinutes,
                    totalPayMicros = summary.totalPayMicros,
                    locale = locale,
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        // Expanded state: the report header in the same slot.
        Text(
            text = state.visibleMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 12.dp)
                .alpha(if (expanded) 1f else 0f)
                .then(if (expanded) Modifier else Modifier.clearAndSetSemantics { }),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun summaryLine(
    shiftCount: Int,
    workedMinutes: Int,
    totalPayMicros: Long,
    locale: Locale,
): String {
    val shifts = pluralStringResource(R.plurals.shifts_short, shiftCount, shiftCount)
    val hours = stringResource(R.string.hours_short, formatDurationCompact(workedMinutes))
    val money = stringResource(
        R.string.amount_with_currency,
        formatWholeAmountMicros(totalPayMicros, locale),
    )
    return "$shifts · $hours · $money"
}

@Composable
private fun MonthlySummaryPanel(
    state: CalendarUiState,
    onOpenYearSummary: () -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.amount_with_currency,
                    formatWholeAmountMicros(summary.totalPayMicros, locale),
                ),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (shouldUseErrorColorForTotal(summary.totalPayMicros)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
            )
            Text(
                text = summaryLine(
                    shiftCount = summary.shiftCount,
                    workedMinutes = summary.workedMinutes,
                    totalPayMicros = summary.totalPayMicros,
                    locale = locale,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(4.dp))
            LabelValueRow(
                label = stringResource(R.string.calculation_base),
                value = formatWholeAmountMicros(summary.basePayMicros, locale),
            )
            if (summary.bonusMicros > 0L) {
                LabelValueRow(
                    label = stringResource(R.string.calculation_bonus),
                    value = "+${formatWholeAmountMicros(summary.bonusMicros, locale)}",
                )
            }
            if (summary.penaltyMicros > 0L) {
                LabelValueRow(
                    label = stringResource(R.string.calculation_penalty),
                    value = "−${formatWholeAmountMicros(summary.penaltyMicros, locale)}",
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (summary.shiftCount > 0) {
                LabelValueRow(
                    label = stringResource(R.string.average_shift),
                    value = formatDurationCompact(summary.workedMinutes / summary.shiftCount),
                )
                LabelValueRow(
                    label = stringResource(R.string.average_shift_income),
                    value = formatAmountMicros(summary.totalPayMicros / summary.shiftCount, locale),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            YearNavRow(
                year = state.visibleMonth.year,
                onClick = onOpenYearSummary,
            )
        }
    }
}

@Composable
private fun YearNavRow(year: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.year_stats_title, year),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun MonthPickerDialog(
    visibleMonth: YearMonth,
    locale: Locale,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var shownYear by rememberSaveable(visibleMonth.year) { mutableStateOf(visibleMonth.year) }
    val monthLabels = remember(locale) {
        (1..12).map { month ->
            month to Month.of(month).getDisplayName(TextStyle.SHORT, locale)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { shownYear -= 1 },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_year),
                    )
                }
                Text(
                    text = shownYear.toString(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = { shownYear += 1 },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_year),
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                monthLabels.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { (month, label) ->
                            val selected = shownYear == visibleMonth.year &&
                                month == visibleMonth.monthValue
                            FilterChip(
                                selected = selected,
                                onClick = { onSelect(YearMonth.of(shownYear, month)) },
                                label = {
                                    Text(
                                        text = label.replaceFirstChar { it.uppercase(locale) },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

// Horizontal distance a drag must cover before it counts as a month switch.
private val MonthSwipeThreshold = 48.dp

@Composable
private fun CalendarGrid(
    state: CalendarUiState,
    onDayClick: (LocalDate) -> Unit,
    onSwipeToPrevious: () -> Unit,
    onSwipeToNext: () -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val currentOnSwipeToPrevious by rememberUpdatedState(onSwipeToPrevious)
    val currentOnSwipeToNext by rememberUpdatedState(onSwipeToNext)
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                val swipeThresholdPx = MonthSwipeThreshold.toPx()
                var totalDrag = Offset.Zero
                detectDragGestures(
                    onDragStart = { totalDrag = Offset.Zero },
                    onDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        when (resolveMonthSwipe(totalDrag.x, totalDrag.y, swipeThresholdPx)) {
                            MonthSwipe.TO_PREVIOUS -> currentOnSwipeToPrevious()
                            MonthSwipe.TO_NEXT -> currentOnSwipeToNext()
                            MonthSwipe.NONE -> Unit
                        }
                        totalDrag = Offset.Zero
                    },
                )
            },
    ) {
        val weekdays = (0 until 7).map { DayOfWeek.MONDAY.plus(it.toLong()) }
        val firstDay = state.visibleMonth.atDay(1)
        val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
        val cells = (0L until 42L).map { offset -> gridStart.plusDays(offset) }
        val today = LocalDate.now()

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

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp),
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

private val DayCellShape = RoundedCornerShape(10.dp)

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
    val totalMicros = visibleEntry?.let {
        runCatching { SalaryCalculator.entryPay(it).totalPayMicros }.getOrNull()
    }
    val dateLabel = date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale),
    )
    val a11yDescription = buildDayCellDescription(
        dateLabel = dateLabel,
        todayLabel = if (isToday) stringResource(R.string.today) else null,
        selectedLabel = if (isSelected) stringResource(R.string.day_selected) else null,
        entryLabel = if (visibleEntry != null) stringResource(R.string.has_entry) else null,
        durationText = if (visibleEntry != null && visibleEntry.workedMinutes > 0) {
            formatDuration(visibleEntry.workedMinutes)
        } else {
            null
        },
        amountText = if (totalMicros != null && shouldShowDayAmount(totalMicros)) {
            formatAmountMicros(totalMicros, locale)
        } else {
            null
        },
        bonusText = if ((visibleEntry?.bonusMicros ?: 0L) > 0L) {
            stringResource(R.string.has_bonus)
        } else {
            null
        },
        penaltyText = if ((visibleEntry?.penaltyMicros ?: 0L) > 0L) {
            stringResource(R.string.has_penalty)
        } else {
            null
        },
    )
    val dateColor = when {
        !isInVisibleMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .padding(1.dp)
            .fillMaxSize()
            .clip(DayCellShape)
            .background(
                when {
                    visibleEntry == null -> Color.Transparent
                    else -> LocalWorkTimeColors.current.workedDayContainer
                },
            )
            .then(
                if (isSelected) {
                    Modifier.border(1.25.dp, MaterialTheme.colorScheme.primary, DayCellShape)
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) { contentDescription = a11yDescription }
            .clickable(enabled = isInVisibleMonth, onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 3.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = dateColor,
                maxLines = 1,
            )
            if (visibleEntry != null) {
                if (visibleEntry.workedMinutes > 0) {
                    Spacer(modifier = Modifier.weight(0.9f))
                    Text(
                        text = formatDurationCompact(visibleEntry.workedMinutes),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (totalMicros != null && shouldShowDayAmount(totalMicros)) {
                    Text(
                        text = formatWholeAmountMicros(totalMicros, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }

        AdjustmentDots(
            hasBonus = (visibleEntry?.bonusMicros ?: 0L) > 0L,
            hasPenalty = (visibleEntry?.penaltyMicros ?: 0L) > 0L,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 3.dp),
        )
    }
}

/** Tiny non-interactive markers for days with bonus/penalty adjustments. */
@Composable
private fun AdjustmentDots(
    hasBonus: Boolean,
    hasPenalty: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!hasBonus && !hasPenalty) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (hasBonus) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        if (hasPenalty) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.error),
            )
        }
    }
}

internal fun shouldShowDayAmount(totalMicros: Long?): Boolean = totalMicros != null && totalMicros != 0L

internal fun shouldUseErrorColorForTotal(totalPayMicros: Long): Boolean = totalPayMicros < 0L

internal enum class MonthSwipe { NONE, TO_PREVIOUS, TO_NEXT }

internal fun resolveMonthSwipe(deltaX: Float, deltaY: Float, thresholdPx: Float): MonthSwipe = when {
    abs(deltaY) >= abs(deltaX) -> MonthSwipe.NONE
    deltaX <= -thresholdPx -> MonthSwipe.TO_NEXT
    deltaX >= thresholdPx -> MonthSwipe.TO_PREVIOUS
    else -> MonthSwipe.NONE
}

internal fun buildDayCellDescription(
    dateLabel: String,
    todayLabel: String?,
    selectedLabel: String?,
    entryLabel: String?,
    durationText: String?,
    amountText: String?,
    bonusText: String?,
    penaltyText: String?,
): String = listOfNotNull(
    dateLabel,
    todayLabel,
    selectedLabel,
    entryLabel,
    durationText,
    amountText,
    bonusText,
    penaltyText,
).joinToString(", ")

@Composable
private fun EmptyMonthPrompt(
    onOpenToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.empty_month_prompt),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            TextButton(onClick = onOpenToday) {
                Text(stringResource(R.string.open_today))
            }
        }
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
