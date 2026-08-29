package com.worktime.app.ui.calendar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppNavigationRow
import com.worktime.app.ui.components.AppSheetShape
import com.worktime.app.ui.components.LabelValueRow
import com.worktime.app.ui.components.PlainDragHandle
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.formatWholeAmountMicros
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val CalendarFadeMillis = 100
private const val PagerSnapMillis = 135
private const val PagerProgrammaticMillis = 190
private const val PagerPageCount = 24_001
private const val PagerAnchorPage = PagerPageCount / 2

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
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var monthPickerOpen by rememberSaveable { mutableStateOf(false) }

    val originMonthIndex = rememberSaveable { state.visibleMonth.toMonthIndex() }
    val pagerState = rememberPagerState(
        initialPage = PagerAnchorPage,
        pageCount = { PagerPageCount },
    )
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = tween(PagerSnapMillis, easing = FastOutSlowInEasing),
        snapPositionalThreshold = 0.35f,
    )
    var programmaticPage by remember { mutableStateOf<Int?>(null) }
    var programmaticScrollJob by remember { mutableStateOf<Job?>(null) }

    fun monthForPage(page: Int): YearMonth =
        yearMonthFromIndex(originMonthIndex + page - PagerAnchorPage)

    fun pageForMonth(month: YearMonth): Int =
        PagerAnchorPage + month.toMonthIndex() - originMonthIndex

    val summarySheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = summarySheetState,
    )
    val summaryTargetExpanded = summarySheetState.targetValue == SheetValue.Expanded
    val closeSummaryBehind: (() -> Unit) -> Unit = { action ->
        val shouldHide = summarySheetState.currentValue != SheetValue.Hidden ||
            summarySheetState.targetValue != SheetValue.Hidden

        action()

        if (shouldHide) {
            scope.launch {
                runCatching { summarySheetState.hide() }
            }
        }
    }
    val toggleSummary: () -> Unit = {
        scope.launch {
            if (summarySheetState.currentValue == SheetValue.Expanded ||
                summarySheetState.targetValue == SheetValue.Expanded
            ) {
                summarySheetState.hide()
            } else {
                summarySheetState.expand()
            }
        }
    }

    val animateToPage: (Int) -> Unit = { requestedPage ->
        val targetPage = requestedPage.coerceIn(0, PagerPageCount - 1)
        programmaticPage = targetPage
        programmaticScrollJob?.cancel()
        programmaticScrollJob = scope.launch {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(PagerProgrammaticMillis, easing = FastOutSlowInEasing),
            )
        }
    }

    // External month changes (month picker, restored state) move the pager to the same month.
    // Adjacent changes use the same short directional travel as the arrows; large jumps are immediate.
    LaunchedEffect(state.visibleMonth) {
        val targetPage = pageForMonth(state.visibleMonth)
        if (targetPage !in 0 until PagerPageCount || targetPage == pagerState.settledPage) {
            return@LaunchedEffect
        }
        programmaticPage = targetPage
        programmaticScrollJob?.cancel()
        if (abs(targetPage - pagerState.currentPage) <= 1) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(PagerProgrammaticMillis, easing = FastOutSlowInEasing),
            )
        } else {
            pagerState.scrollToPage(targetPage)
        }
    }

    // A drag follows the finger through HorizontalPager. Commit business state only once
    // the page has settled; user-driven snaps get one restrained tactile tick.
    LaunchedEffect(pagerState, state.visibleMonth) {
        var previousSettledPage = pagerState.settledPage
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            if (settledPage == previousSettledPage) return@collect

            val userDriven = programmaticPage != settledPage
            if (userDriven) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            } else {
                programmaticPage = null
            }

            val settledMonth = monthForPage(settledPage)
            if (settledMonth != state.visibleMonth) {
                onSelectMonth(settledMonth)
            }
            previousSettledPage = settledPage
        }
    }

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetDragHandle = null,
        sheetSwipeEnabled = true,
        sheetShape = AppSheetShape,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 0.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        sheetContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (summaryTargetExpanded) {
                    PlainDragHandle(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = toggleSummary,
                        accessibilityLabel = stringResource(R.string.monthly_summary),
                    )
                }
                MonthlySummaryPanel(
                    state = state,
                    onOpenYearSummary = { closeSummaryBehind(onOpenYearSummary) },
                    locale = locale,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 10.dp),
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
            val displayedMonth = monthForPage(pagerState.currentPage)
            CalendarHeader(
                visibleMonth = displayedMonth,
                isReady = state.isReady,
                locale = locale,
                onPreviousMonth = {
                    closeSummaryBehind {
                        val target = pagerState.settledPage - 1
                        if (target >= 0) animateToPage(target) else onPreviousMonth()
                    }
                },
                onNextMonth = {
                    closeSummaryBehind {
                        val target = pagerState.settledPage + 1
                        if (target < PagerPageCount) animateToPage(target) else onNextMonth()
                    }
                },
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
                val today = LocalDate.now()
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(calendarGridHeight())
                        .testTag("calendar-pager"),
                    beyondViewportPageCount = 1,
                    flingBehavior = pagerFlingBehavior,
                    key = { page -> monthForPage(page).toString() },
                ) { page ->
                    val month = monthForPage(page)
                    val entries = state.monthEntries[month]
                        ?: if (month == state.visibleMonth) state.entries else emptyMap()
                    CalendarGrid(
                        state = state.copy(
                            visibleMonth = month,
                            entries = entries,
                        ),
                        onDayClick = { date -> closeSummaryBehind { onDayClick(date) } },
                        locale = locale,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (
                    shouldShowTodayEntryPrompt(
                        visibleMonth = state.visibleMonth,
                        entryDates = state.entries.keys,
                        today = today,
                    )
                ) {
                    TodayEntryPrompt(
                        onClick = { closeSummaryBehind { onDayClick(today) } },
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                SummaryStrip(
                    state = state,
                    locale = locale,
                    expanded = summaryTargetExpanded,
                    onClick = toggleSummary,
                    onSwipeUp = {
                        scope.launch { summarySheetState.expand() }
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(
                            start = AppDimens.screenHorizontalPadding,
                            end = AppDimens.screenHorizontalPadding,
                            bottom = 8.dp,
                        ),
                )
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
private fun TodayEntryPrompt(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.rowMinHeight),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .height(AppDimens.rowMinHeight)
                .testTag("today-entry-prompt"),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.fill_today),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CalendarHeader(
    visibleMonth: YearMonth,
    isReady: Boolean,
    locale: Locale,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val monthTitle = visibleMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val titleFontSize = if (largeFont) 18.sp else 22.sp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    modifier = Modifier.size(22.dp),
                    contentDescription = stringResource(R.string.previous_month),
                )
            }
            Text(
                text = monthTitle,
                modifier = Modifier
                    .clickable(onClick = onSelectMonth, onClickLabel = stringResource(R.string.select_month))
                    .padding(horizontal = 4.dp)
                    .testTag("calendar-month-title")
                    .then(if (largeFont) Modifier.widthIn(max = 140.dp) else Modifier),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = titleFontSize),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    modifier = Modifier.size(22.dp),
                    contentDescription = stringResource(R.string.next_month),
                )
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            IconButton(onClick = onSettingsClick, enabled = isReady) {
                Icon(
                    Icons.Filled.Settings,
                    modifier = Modifier.size(22.dp),
                    contentDescription = stringResource(R.string.settings),
                )
            }
        }
    }
}

@Composable
private fun SummaryStrip(
    state: CalendarUiState,
    locale: Locale,
    expanded: Boolean,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    val summaryText = summaryLine(
        shiftCount = summary.shiftCount,
        workedMinutes = summary.workedMinutes,
        totalPayMicros = summary.totalPayMicros,
        locale = locale,
    )
    val haptics = LocalHapticFeedback.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(CalendarFadeMillis),
        label = "summary chevron",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(haptics) {
                    val threshold = 40.dp.toPx()
                    var totalDrag = 0f
                    var thresholdActive = false
                    detectDragGestures(
                        onDragStart = {
                            totalDrag = 0f
                            thresholdActive = false
                        },
                        onDrag = { change, dragAmount ->
                            totalDrag += dragAmount.y
                            val isEligible = totalDrag <= -threshold
                            if (isEligible && !thresholdActive) {
                                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                thresholdActive = true
                            } else if (!isEligible) {
                                thresholdActive = false
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            if (totalDrag <= -threshold) onSwipeUp()
                            totalDrag = 0f
                            thresholdActive = false
                        },
                    )
                }
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.76f))
                .clickable(
                    onClickLabel = stringResource(R.string.monthly_summary),
                    onClick = onClick,
                )
                .testTag("monthly-summary-strip")
                .padding(horizontal = AppDimens.screenHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summaryText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Icon(
                Icons.Filled.KeyboardArrowUp,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun summaryLine(
    shiftCount: Int,
    workedMinutes: Int,
    locale: Locale,
    totalPayMicros: Long? = null,
): String {
    val shifts = pluralStringResource(R.plurals.shifts_short, shiftCount, shiftCount)
    val hours = stringResource(R.string.hours_short, formatDurationCompact(workedMinutes))
    val line = "$shifts · $hours"
    if (totalPayMicros == null) return line
    val money = stringResource(
        R.string.amount_with_currency,
        formatWholeAmountMicros(totalPayMicros, locale),
    )
    return "$line · $money"
}

@Composable
private fun MonthlySummaryPanel(
    state: CalendarUiState,
    onOpenYearSummary: () -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    val totalText = stringResource(
        R.string.amount_with_currency,
        formatAmountMicros(summary.totalPayMicros, locale),
    )
    val detailText = summaryLine(
        shiftCount = summary.shiftCount,
        workedMinutes = summary.workedMinutes,
        locale = locale,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly-report-panel"),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = AppDimens.screenHorizontalPadding,
                vertical = 4.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.visibleMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = totalText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = if (shouldUseErrorColorForTotal(summary.totalPayMicros)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
            )
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            LabelValueRow(
                label = stringResource(R.string.calculation_base),
                value = formatAmountMicros(summary.basePayMicros, locale),
            )
            if (summary.bonusMicros > 0L) {
                LabelValueRow(
                    label = stringResource(R.string.calculation_bonus),
                    value = "+${formatAmountMicros(summary.bonusMicros, locale)}",
                )
            }
            if (summary.penaltyMicros > 0L) {
                LabelValueRow(
                    label = stringResource(R.string.calculation_penalty),
                    value = "−${formatAmountMicros(summary.penaltyMicros, locale)}",
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

            AppNavigationRow(
                label = stringResource(R.string.year_stats_title),
                onClick = onOpenYearSummary,
            )
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.Transparent,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun CalendarGrid(
    state: CalendarUiState,
    onDayClick: (LocalDate) -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val weekRowHeight = WeekRowHeight
    val weekdayRowHeight = 28.dp
    val dateAreaHeight = 28.dp
    Box(
        modifier = modifier
            .height(calendarGridHeight())
            .testTag("calendar-grid"),
    ) {
        val weekdays = (0 until 7).map { DayOfWeek.MONDAY.plus(it.toLong()) }
        val firstDay = state.visibleMonth.atDay(1)
        val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
        val cells = (0L until 42L).map { offset -> gridStart.plusDays(offset) }
        val today = LocalDate.now()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(weekdayRowHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                weekdays.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, locale),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
            )

            cells.chunked(7).forEachIndexed { weekIndex, week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(weekRowHeight),
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
                                dateAreaHeight = dateAreaHeight,
                                isLastGridRow = weekIndex == CalendarWeekCount - 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val CalendarWeekCount = 6
private val WeekRowHeight = 64.dp

internal fun calendarGridHeight(): Dp =
    WeekRowHeight * CalendarWeekCount + 28.dp + 8.dp

@Composable
private fun DayCell(
    date: LocalDate,
    entry: WorkEntry?,
    isInVisibleMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    locale: Locale,
    dateAreaHeight: Dp,
    isLastGridRow: Boolean,
) {
    val visibleEntry = entry.takeIf { isInVisibleMonth }
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val interactionSource = remember { MutableInteractionSource() }
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
        !isInVisibleMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    }
    val amountColor = if ((totalMicros ?: 0L) < 0L) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)
    }
    val borderColor = if (isToday && isInVisibleMonth) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    }
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        visibleEntry != null -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.58f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = if (isToday && isInVisibleMonth) 1.dp else 0.5.dp,
                brush = SolidColor(borderColor),
                shape = RectangleShape,
            )
            .background(backgroundColor)
            .semantics(mergeDescendants = true) { contentDescription = a11yDescription }
            .then(
                if (isLastGridRow && isInVisibleMonth) Modifier.testTag("calendar-last-row-day")
                else Modifier,
            )
            .clickable(
                enabled = isInVisibleMonth,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dateAreaHeight)
                    .padding(top = 3.dp, end = 4.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = dateColor,
                    maxLines = 1,
                )
            }
            if (visibleEntry != null) {
                if (largeFont) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = dateAreaHeight, start = 2.dp, end = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (visibleEntry.workedMinutes > 0) {
                                formatDurationCompact(visibleEntry.workedMinutes)
                            } else {
                                ""
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (totalMicros != null && shouldShowDayAmount(totalMicros)) {
                                formatWholeAmountMicros(totalMicros, locale)
                            } else {
                                ""
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.sp,
                                lineHeight = 8.sp,
                            ),
                            fontWeight = FontWeight.Medium,
                            color = amountColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            text = if (visibleEntry.workedMinutes > 0) {
                                formatDurationCompact(visibleEntry.workedMinutes)
                            } else {
                                ""
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(bottom = 3.dp)
                                .padding(horizontal = 2.dp),
                            text = if (totalMicros != null && shouldShowDayAmount(totalMicros)) {
                                formatWholeAmountMicros(totalMicros, locale)
                            } else {
                                ""
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                            ),
                            fontWeight = FontWeight.Medium,
                            color = amountColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun YearMonth.toMonthIndex(): Int = year * 12 + (monthValue - 1)

private fun yearMonthFromIndex(index: Int): YearMonth = YearMonth.of(
    Math.floorDiv(index, 12),
    Math.floorMod(index, 12) + 1,
)

internal fun shouldShowDayAmount(totalMicros: Long?): Boolean = totalMicros != null && totalMicros != 0L

internal fun shouldUseErrorColorForTotal(totalPayMicros: Long): Boolean = totalPayMicros < 0L

internal fun shouldShowTodayEntryPrompt(
    visibleMonth: YearMonth,
    entryDates: Set<LocalDate>,
    today: LocalDate,
): Boolean = visibleMonth == YearMonth.from(today) && today !in entryDates

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
private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) {
        stringResource(R.string.duration_hours, hours)
    } else {
        stringResource(R.string.duration_hours_minutes, hours, remainder)
    }
}
