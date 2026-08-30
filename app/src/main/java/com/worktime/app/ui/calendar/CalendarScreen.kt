package com.worktime.app.ui.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import com.worktime.app.R
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppMotion
import com.worktime.app.ui.components.AppSheetShape
import com.worktime.app.ui.components.PlainDragHandle
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val PagerPageCount = 24_001
private const val PagerAnchorPage = PagerPageCount / 2
private const val PagerPositionResyncTolerance = 0.08f

internal enum class CalendarLayoutMode {
    Compact,
    CompactShort,
    SupportingPane,
}

internal fun calendarLayoutMode(
    maxHorizontalPartitions: Int,
    isHeightAtLeastMedium: Boolean,
): CalendarLayoutMode = when {
    !isHeightAtLeastMedium -> CalendarLayoutMode.CompactShort
    maxHorizontalPartitions >= 2 -> CalendarLayoutMode.SupportingPane
    else -> CalendarLayoutMode.Compact
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
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
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val paneDirective = calculatePaneScaffoldDirective(adaptiveInfo)
    val layoutMode = calendarLayoutMode(
        maxHorizontalPartitions = paneDirective.maxHorizontalPartitions,
        isHeightAtLeastMedium = adaptiveInfo.windowSizeClass.isHeightAtLeastBreakpoint(
            HEIGHT_DP_MEDIUM_LOWER_BOUND,
        ),
    )
    val shortScrollState = rememberScrollState()
    val supportingPaneScrollState = rememberScrollState()
    var monthPickerOpen by rememberSaveable { mutableStateOf(false) }

    val originMonthIndex = rememberSaveable { state.visibleMonth.toMonthIndex() }
    val pagerState = rememberPagerState(
        initialPage = PagerAnchorPage,
        pageCount = { PagerPageCount },
    )
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            dampingRatio = AppMotion.NoBounceDampingRatio,
            stiffness = AppMotion.PagerStiffness,
        ),
        snapPositionalThreshold = 0.35f,
    )
    val programmaticPosition = remember { Animatable(PagerAnchorPage.toFloat()) }
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

    LaunchedEffect(layoutMode) {
        if (
            layoutMode == CalendarLayoutMode.SupportingPane &&
            (summarySheetState.currentValue != SheetValue.Hidden ||
                summarySheetState.targetValue != SheetValue.Hidden)
        ) {
            runCatching { summarySheetState.hide() }
        }
    }

    // Arrow presses drive one virtual page position rather than starting independent fixed-time
    // animations. Re-targeting keeps the current spring velocity, so repeated taps accumulate
    // naturally instead of being dropped or restarting from rest at every intermediate month.
    val animateToPage: (Int) -> Unit = { requestedPage ->
        val targetPage = requestedPage.coerceIn(0, PagerPageCount - 1)
        if (programmaticScrollJob?.isActive != true || programmaticPage != targetPage) {
            val wasRunning = programmaticScrollJob?.isActive == true
            val carriedVelocity = if (wasRunning) programmaticPosition.velocity else 0f
            programmaticPage = targetPage
            programmaticScrollJob?.cancel()
            programmaticScrollJob = scope.launch {
                val actualPosition =
                    pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction
                if (
                    !wasRunning ||
                    abs(programmaticPosition.value - actualPosition) > PagerPositionResyncTolerance
                ) {
                    programmaticPosition.snapTo(actualPosition)
                }

                val pageSizePx = pagerState.layoutInfo.pageSize.toFloat()
                if (pageSizePx <= 0f) {
                    pagerState.scrollToPage(targetPage)
                    programmaticPosition.snapTo(targetPage.toFloat())
                    return@launch
                }

                pagerState.scroll {
                    val scrollScope = this
                    var consumedPosition = programmaticPosition.value
                    programmaticPosition.animateTo(
                        targetValue = targetPage.toFloat(),
                        animationSpec = spring(
                            dampingRatio = AppMotion.NoBounceDampingRatio,
                            stiffness = AppMotion.PagerStiffness,
                        ),
                        initialVelocity = carriedVelocity,
                    ) {
                        val deltaPages = value - consumedPosition
                        val consumedPx = scrollScope.scrollBy(deltaPages * pageSizePx)
                        consumedPosition += consumedPx / pageSizePx
                    }
                }

                programmaticPosition.snapTo(
                    pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction,
                )
            }
        }
    }

    // External month changes (month picker, restored state) move the pager to the same month.
    // Adjacent changes use the same interruptible spring as the arrows; large jumps stay immediate.
    LaunchedEffect(state.visibleMonth) {
        val targetPage = pageForMonth(state.visibleMonth)
        if (targetPage !in 0 until PagerPageCount || targetPage == pagerState.settledPage) {
            return@LaunchedEffect
        }
        if (abs(targetPage - pagerState.currentPage) <= 1) {
            animateToPage(targetPage)
        } else {
            programmaticPage = targetPage
            programmaticScrollJob?.cancel()
            pagerState.scrollToPage(targetPage)
            programmaticPosition.snapTo(targetPage.toFloat())
        }
    }

    // A drag follows the finger through HorizontalPager. Commit business state only once
    // the page has settled; user-driven snaps get one restrained tactile tick.
    LaunchedEffect(pagerState, state.visibleMonth) {
        var previousSettledPage = pagerState.settledPage
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            if (settledPage == previousSettledPage) return@collect

            val expectedProgrammaticPage = programmaticPage
            val userDriven = expectedProgrammaticPage == null || expectedProgrammaticPage != settledPage
            if (userDriven) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
            programmaticPage = null

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
        sheetSwipeEnabled = layoutMode != CalendarLayoutMode.SupportingPane,
        sheetShape = AppSheetShape,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 0.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        sheetContent = {
            if (layoutMode == CalendarLayoutMode.SupportingPane) {
                Spacer(modifier = Modifier.height(1.dp))
            } else {
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
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val displayedMonth = monthForPage(pagerState.currentPage)
        val displayedEntries = state.monthEntries[displayedMonth]
            ?: if (displayedMonth == state.visibleMonth) state.entries else emptyMap()
        val displayedState = state.copy(
            visibleMonth = displayedMonth,
            entries = displayedEntries,
        )
        val today = LocalDate.now()

        val primaryPane: @Composable (Modifier, Boolean, Boolean) -> Unit = {
                paneModifier,
                showSummaryStrip,
                useFlexibleSpacer,
            ->
            Column(
                modifier = paneModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CalendarHeader(
                    visibleMonth = displayedMonth,
                    isReady = state.isReady,
                    locale = locale,
                    onPreviousMonth = {
                        closeSummaryBehind {
                            val basePage = programmaticPage ?: pagerState.currentPage
                            val target = basePage - 1
                            if (target >= 0) animateToPage(target) else onPreviousMonth()
                        }
                    },
                    onNextMonth = {
                        closeSummaryBehind {
                            val basePage = programmaticPage ?: pagerState.currentPage
                            val target = basePage + 1
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
                            visibleMonth = displayedMonth,
                            entryDates = displayedEntries.keys,
                            today = today,
                        )
                    ) {
                        TodayEntryPrompt(
                            onClick = { closeSummaryBehind { onDayClick(today) } },
                        )
                    }
                    if (useFlexibleSpacer) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (showSummaryStrip) {
                        SummaryStrip(
                            state = displayedState,
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
        }

        when (layoutMode) {
            CalendarLayoutMode.Compact -> {
                primaryPane(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding),
                    true,
                    true,
                )
            }

            CalendarLayoutMode.CompactShort -> {
                primaryPane(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding)
                        .verticalScroll(shortScrollState),
                    true,
                    false,
                )
            }

            CalendarLayoutMode.SupportingPane -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(innerPadding)
                        .padding(
                            start = AppDimens.screenHorizontalPadding,
                            end = AppDimens.screenHorizontalPadding,
                            bottom = 8.dp,
                        ),
                ) {
                    primaryPane(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        false,
                        true,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .testTag("calendar-supporting-pane"),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp,
                    ) {
                        if (state.isReady) {
                            MonthlySummaryPanel(
                                state = displayedState,
                                onOpenYearSummary = onOpenYearSummary,
                                locale = locale,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(supportingPaneScrollState)
                                    .padding(vertical = 8.dp),
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
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

private fun YearMonth.toMonthIndex(): Int = year * 12 + (monthValue - 1)

private fun yearMonthFromIndex(index: Int): YearMonth = YearMonth.of(
    Math.floorDiv(index, 12),
    Math.floorMod(index, 12) + 1,
)
