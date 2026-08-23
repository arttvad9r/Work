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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.components.PlainDragHandle
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.formatWholeAmountMicros
import java.time.DayOfWeek
import java.time.LocalDate
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
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 0.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = summaryContentAlpha,
        ),
        sheetContent = {
            // The complete report stays composed in both states. Only visibility and
            // semantics change, so the measured sheet height and swipe anchors never
            // jump while a drag is in progress.
            Column(modifier = Modifier.fillMaxWidth()) {
                PlainDragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = toggleSummary,
                    accessibilityLabel = stringResource(R.string.monthly_summary),
                )
                FullSummaryPanel(
                    state = state,
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
            CenterAlignedTopAppBar(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .height(52.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { closeSummaryBehind(onPreviousMonth) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.previous_month),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { closeSummaryBehind(onNextMonth) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.next_month),
                        )
                    }
                    IconButton(
                        onClick = { closeSummaryBehind(onSettingsClick) },
                        enabled = state.isReady,
                    ) {
                        Icon(
                            Icons.Filled.Settings,
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
                    onSwipeToPrevious = { closeSummaryBehind(onPreviousMonth) },
                    onSwipeToNext = { closeSummaryBehind(onNextMonth) },
                    locale = locale,
                    modifier = Modifier.height(392.dp),
                )
                if (state.entries.isEmpty()) {
                    EmptyMonthPrompt(
                        onOpenToday = { closeSummaryBehind { onDayClick(LocalDate.now()) } },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                CollapsedSummaryCard(
                    state = state,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// Horizontal distance a drag must cover before it counts as a month switch.
private val MonthSwipeThreshold = 48.dp

@Composable
private fun CalendarCard(
    state: CalendarUiState,
    onDayClick: (LocalDate) -> Unit,
    onSwipeToPrevious: () -> Unit,
    onSwipeToNext: () -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val currentOnSwipeToPrevious by rememberUpdatedState(onSwipeToPrevious)
    val currentOnSwipeToNext by rememberUpdatedState(onSwipeToNext)
    Surface(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .fillMaxWidth()
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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.monthly_income),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                )
                Text(
                    text = formatAmountMicros(summary.totalPayMicros, locale),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${stringResource(R.string.shift_count_label)}: ${summary.shiftCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                )
                Text(
                    text = "${stringResource(R.string.worked_duration)}: ${formatDurationCompact(summary.workedMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                )
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.calculation_total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = formatAmountMicros(summary.totalPayMicros, locale),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (shouldUseErrorColorForTotal(summary.totalPayMicros)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
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
    val background = when {
        !isInVisibleMonth -> MaterialTheme.colorScheme.surfaceContainerLowest
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        visibleEntry != null -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }
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

    Box(
        modifier = Modifier
            .padding(0.25.dp)
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
            .padding(horizontal = 2.dp, vertical = 2.dp),
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 2.dp),
            style = MaterialTheme.typography.titleSmall,
            color = foreground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )

        if (visibleEntry != null) {
            MarkerGroup(
                hasBonus = visibleEntry.bonusMicros > 0L,
                hasPenalty = visibleEntry.penaltyMicros > 0L,
                modifier = Modifier.align(Alignment.TopStart),
            )
            EntryGlyph(modifier = Modifier.align(Alignment.BottomEnd))
            if (visibleEntry.workedMinutes > 0) {
                Text(
                    text = formatDurationCompact(visibleEntry.workedMinutes),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            if (totalMicros != null && shouldShowDayAmount(totalMicros)) {
                Text(
                    text = formatWholeAmountMicros(totalMicros, locale),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = foreground.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                )
            }
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
            AdjustmentGlyph(isBonus = true)
            AdjustmentGlyph(isBonus = false)
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
        AdjustmentGlyph(isBonus = isBonus)
    }
}

@Composable
private fun AdjustmentGlyph(isBonus: Boolean) {
    val tint = if (isBonus) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Box(
        modifier = Modifier
            .size(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(if (isBonus) 8.dp else 6.dp)
                .height(1.5.dp)
                .background(tint),
        )
        if (isBonus) {
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(8.dp)
                    .background(tint),
            )
        }
    }
}

@Composable
private fun EntryGlyph(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(end = 2.dp)
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(7.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

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
