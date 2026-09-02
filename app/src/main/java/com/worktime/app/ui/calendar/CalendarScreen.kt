package com.worktime.app.ui.calendar

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppMotion
import com.worktime.app.ui.components.AppSheetShape
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal enum class CalendarLayoutMode {
    Compact,
    CompactShort,
    SupportingPane,
}

private val SummaryStripFootprint = 64.dp

internal fun calendarLayoutMode(
    maxHorizontalPartitions: Int,
    isHeightAtLeastMedium: Boolean,
): CalendarLayoutMode = when {
    !isHeightAtLeastMedium -> CalendarLayoutMode.CompactShort
    maxHorizontalPartitions >= 2 -> CalendarLayoutMode.SupportingPane
    else -> CalendarLayoutMode.Compact
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun shouldExpandSummaryAfterToggle(targetValue: SheetValue): Boolean =
    targetValue != SheetValue.Expanded

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

    val pager = rememberCalendarPagerState(state.visibleMonth)
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pager.pagerState,
        snapAnimationSpec = spring(
            dampingRatio = AppMotion.NoBounceDampingRatio,
            stiffness = AppMotion.PagerStiffness,
        ),
        snapPositionalThreshold = AppMotion.PagerSnapPositionalThreshold,
    )
    CalendarPagerEffects(
        pager = pager,
        visibleMonth = state.visibleMonth,
        scope = scope,
        onSelectMonth = onSelectMonth,
    )

    val summaryMayHide by rememberUpdatedState(layoutMode == CalendarLayoutMode.SupportingPane)
    val summarySheetState = rememberStandardBottomSheetState(
        initialValue = if (layoutMode == CalendarLayoutMode.SupportingPane) {
            SheetValue.Hidden
        } else {
            SheetValue.PartiallyExpanded
        },
        skipHiddenState = false,
        confirmValueChange = { target ->
            target != SheetValue.Hidden || summaryMayHide
        },
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = summarySheetState,
    )
    val summaryTargetExpanded = summarySheetState.targetValue == SheetValue.Expanded
    val closeSummaryBehind: (() -> Unit) -> Unit = { action ->
        val shouldCollapse = summarySheetState.currentValue == SheetValue.Expanded ||
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
            if (shouldExpandSummaryAfterToggle(summarySheetState.targetValue)) {
                summarySheetState.expand()
            } else {
                summarySheetState.partialExpand()
            }
        }
    }

    LaunchedEffect(layoutMode) {
        when {
            layoutMode == CalendarLayoutMode.SupportingPane &&
                (summarySheetState.currentValue != SheetValue.Hidden ||
                    summarySheetState.targetValue != SheetValue.Hidden) -> {
                runCatching { summarySheetState.hide() }
            }

            layoutMode != CalendarLayoutMode.SupportingPane &&
                (summarySheetState.currentValue == SheetValue.Hidden ||
                    summarySheetState.targetValue == SheetValue.Hidden) -> {
                runCatching { summarySheetState.partialExpand() }
            }
        }
    }

    val navigationBottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val compactSheetPeekHeight = SummaryStripFootprint + navigationBottomPadding

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val collapsedSheetOffsetPx = with(density) {
            (maxHeight - compactSheetPeekHeight).toPx()
        }

        BottomSheetScaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = scaffoldState,
            sheetPeekHeight = if (layoutMode == CalendarLayoutMode.SupportingPane) {
                0.dp
            } else {
                compactSheetPeekHeight
            },
            sheetDragHandle = null,
            sheetSwipeEnabled = layoutMode != CalendarLayoutMode.SupportingPane,
            sheetShape = AppSheetShape,
            sheetTonalElevation = 0.dp,
            sheetShadowElevation = 0.dp,
            sheetContainerColor = Color.Transparent,
            sheetContent = {
                if (layoutMode == CalendarLayoutMode.SupportingPane) {
                    Spacer(modifier = Modifier.height(1.dp))
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SummaryStrip(
                            state = state,
                            locale = locale,
                            expanded = summaryTargetExpanded,
                            onClick = toggleSummary,
                            modifier = Modifier
                                .offset {
                                    val sheetOffset = runCatching {
                                        summarySheetState.requireOffset()
                                    }.getOrDefault(collapsedSheetOffsetPx)
                                    IntOffset(
                                        x = 0,
                                        y = (collapsedSheetOffsetPx - sheetOffset).roundToInt(),
                                    )
                                }
                                .zIndex(1f)
                                .padding(
                                    start = AppDimens.screenHorizontalPadding,
                                    end = AppDimens.screenHorizontalPadding,
                                    bottom = 8.dp,
                                ),
                        )
                        Spacer(modifier = Modifier.height(navigationBottomPadding))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppSheetShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
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
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            val today = LocalDate.now()

            val primaryPane: @Composable (Modifier, Boolean) -> Unit = {
                    paneModifier,
                    useFlexibleSpacer,
                ->
                Column(
                    modifier = paneModifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CalendarHeader(
                        visibleMonth = state.visibleMonth,
                        isReady = state.isReady,
                        locale = locale,
                        onPreviousMonth = {
                            closeSummaryBehind {
                                if (pager.navigatePrevious(scope)) {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                } else {
                                    onPreviousMonth()
                                }
                            }
                        },
                        onNextMonth = {
                            closeSummaryBehind {
                                if (pager.navigateNext(scope)) {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                } else {
                                    onNextMonth()
                                }
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
                            state = pager.pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(calendarGridHeight())
                                .testTag("calendar-pager"),
                            beyondViewportPageCount = 1,
                            flingBehavior = pagerFlingBehavior,
                            key = { page -> pager.monthForPage(page).toString() },
                        ) { page ->
                            val month = pager.monthForPage(page)
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
                        if (useFlexibleSpacer) {
                            Spacer(modifier = Modifier.weight(1f))
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
                    )
                }

                CalendarLayoutMode.CompactShort -> {
                    primaryPane(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(innerPadding)
                            .verticalScroll(shortScrollState),
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
                                    state = state,
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
