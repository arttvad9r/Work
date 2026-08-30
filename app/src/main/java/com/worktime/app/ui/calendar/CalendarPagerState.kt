package com.worktime.app.ui.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.worktime.app.ui.components.AppMotion
import java.time.YearMonth
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val PagerPageCount = 24_001
private const val PagerAnchorPage = PagerPageCount / 2
private const val PagerPositionResyncTolerance = 0.08f

/**
 * Screen-local state holder for the calendar's effectively unbounded month pager.
 *
 * It owns only interaction/animation state. The selected business month remains owned by
 * [CalendarViewModel] and is committed through [onSelectMonth] after a user drag settles.
 */
internal class CalendarPagerState(
    val pagerState: PagerState,
    private val originMonthIndex: Int,
) {
    private val programmaticPosition = Animatable(PagerAnchorPage.toFloat())
    private var programmaticPage by mutableStateOf<Int?>(null)
    private var programmaticScrollJob: Job? = null

    val displayedMonth: YearMonth
        get() = monthForPage(pagerState.currentPage)

    fun monthForPage(page: Int): YearMonth =
        yearMonthFromIndex(originMonthIndex + page - PagerAnchorPage)

    fun navigatePrevious(scope: CoroutineScope): Boolean = navigateBy(scope, delta = -1)

    fun navigateNext(scope: CoroutineScope): Boolean = navigateBy(scope, delta = 1)

    suspend fun syncToVisibleMonth(
        scope: CoroutineScope,
        visibleMonth: YearMonth,
    ) {
        val targetPage = pageForMonth(visibleMonth)
        if (targetPage !in 0 until PagerPageCount || targetPage == pagerState.settledPage) {
            return
        }

        if (abs(targetPage - pagerState.currentPage) <= 1) {
            animateToPage(scope, targetPage)
        } else {
            programmaticPage = targetPage
            programmaticScrollJob?.cancel()
            pagerState.scrollToPage(targetPage)
            programmaticPosition.snapTo(targetPage.toFloat())
        }
    }

    fun consumeSettledPage(settledPage: Int): SettledCalendarPage {
        val expectedProgrammaticPage = programmaticPage
        val userDriven = expectedProgrammaticPage == null || expectedProgrammaticPage != settledPage
        programmaticPage = null
        return SettledCalendarPage(
            month = monthForPage(settledPage),
            userDriven = userDriven,
        )
    }

    private fun pageForMonth(month: YearMonth): Int =
        PagerAnchorPage + month.toMonthIndex() - originMonthIndex

    private fun navigateBy(scope: CoroutineScope, delta: Int): Boolean {
        val basePage = programmaticPage ?: pagerState.currentPage
        val targetPage = basePage + delta
        if (targetPage !in 0 until PagerPageCount) return false
        animateToPage(scope, targetPage)
        return true
    }

    /**
     * Re-target one virtual pager position instead of starting independent fixed-time animations.
     * Keeping the current spring velocity makes repeated arrow taps accumulate naturally.
     */
    private fun animateToPage(scope: CoroutineScope, requestedPage: Int) {
        val targetPage = requestedPage.coerceIn(0, PagerPageCount - 1)
        if (programmaticScrollJob?.isActive == true && programmaticPage == targetPage) return

        val wasRunning = programmaticScrollJob?.isActive == true
        val carriedVelocity = if (wasRunning) programmaticPosition.velocity else 0f
        programmaticPage = targetPage
        programmaticScrollJob?.cancel()
        programmaticScrollJob = scope.launch {
            val actualPosition = pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction
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

internal data class SettledCalendarPage(
    val month: YearMonth,
    val userDriven: Boolean,
)

@Composable
internal fun rememberCalendarPagerState(initialMonth: YearMonth): CalendarPagerState {
    val originMonthIndex = rememberSaveable { initialMonth.toMonthIndex() }
    val pagerState = rememberPagerState(
        initialPage = PagerAnchorPage,
        pageCount = { PagerPageCount },
    )
    return remember(pagerState, originMonthIndex) {
        CalendarPagerState(
            pagerState = pagerState,
            originMonthIndex = originMonthIndex,
        )
    }
}

@Composable
internal fun CalendarPagerEffects(
    pager: CalendarPagerState,
    visibleMonth: YearMonth,
    scope: CoroutineScope,
    onSelectMonth: (YearMonth) -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    // Month picker/restored state drives the pager. Adjacent changes use the same interruptible
    // spring as arrow navigation; large jumps are immediate.
    LaunchedEffect(pager, visibleMonth) {
        pager.syncToVisibleMonth(scope, visibleMonth)
    }

    // Gesture progress is rendered by HorizontalPager itself. Business state is committed only
    // after settling, and user-driven snaps receive one restrained tactile tick.
    LaunchedEffect(pager, visibleMonth) {
        var previousSettledPage = pager.pagerState.settledPage
        snapshotFlow { pager.pagerState.settledPage }.collect { settledPage ->
            if (settledPage == previousSettledPage) return@collect

            val settled = pager.consumeSettledPage(settledPage)
            if (settled.userDriven) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
            if (settled.month != visibleMonth) {
                onSelectMonth(settled.month)
            }
            previousSettledPage = settledPage
        }
    }
}

private fun YearMonth.toMonthIndex(): Int = year * 12 + (monthValue - 1)

private fun yearMonthFromIndex(index: Int): YearMonth = YearMonth.of(
    Math.floorDiv(index, 12),
    Math.floorMod(index, 12) + 1,
)
