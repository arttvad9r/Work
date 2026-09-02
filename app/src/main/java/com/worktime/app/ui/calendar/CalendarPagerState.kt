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
import com.worktime.app.ui.components.PagerHapticGate
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

    val isProgrammaticScrollInProgress: Boolean
        get() = programmaticPage != null

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
            try {
                pagerState.scrollToPage(targetPage)
                programmaticPosition.snapTo(targetPage.toFloat())
            } finally {
                if (programmaticPage == targetPage) {
                    programmaticPage = null
                }
            }
        }
    }

    fun consumeSettledPage(settledPage: Int): YearMonth {
        programmaticPage = null
        return monthForPage(settledPage)
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
            try {
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
            } finally {
                // A user drag can interrupt the arrow animation. Clearing this in finally lets the
                // gesture haptic observer take over immediately instead of remaining suppressed
                // until the next settled-page callback.
                if (programmaticPage == targetPage) {
                    programmaticPage = null
                }
            }
        }
    }
}

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

    // Observe the pager's actual continuous position, not raw pointer travel. This also covers a
    // fast fling released before 35%: when pager physics commits the page and crosses the same snap
    // threshold a threshold haptic is emitted during the movement, never after settledPage.
    LaunchedEffect(pager) {
        val gate = PagerHapticGate(AppMotion.PagerSnapPositionalThreshold)
        snapshotFlow {
            val position =
                pager.pagerState.currentPage.toFloat() + pager.pagerState.currentPageOffsetFraction
            val deltaFromSettled = position - pager.pagerState.settledPage.toFloat()
            deltaFromSettled to pager.isProgrammaticScrollInProgress
        }.collect { (deltaFromSettled, programmatic) ->
            if (programmatic) {
                gate.reset()
            } else if (gate.update(deltaFromSettled)) {
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            }
        }
    }

    // Settling only commits business state; it never emits tactile feedback.
    LaunchedEffect(pager, visibleMonth) {
        var previousSettledPage = pager.pagerState.settledPage
        snapshotFlow { pager.pagerState.settledPage }.collect { settledPage ->
            if (settledPage == previousSettledPage) return@collect

            val settledMonth = pager.consumeSettledPage(settledPage)
            if (settledMonth != visibleMonth) {
                onSelectMonth(settledMonth)
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
