package com.worktime.app.ui.yearsummary

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
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val YearPagerPageCount = 24_001
private const val YearPagerAnchorPage = YearPagerPageCount / 2
private const val PagerPositionResyncTolerance = 0.08f

/** Screen-local interaction state for the effectively unbounded year pager. */
internal class YearSummaryPagerState(
    val pagerState: PagerState,
    private val originYear: Int,
) {
    private val programmaticPosition = Animatable(YearPagerAnchorPage.toFloat())
    private var programmaticPage by mutableStateOf<Int?>(null)
    private var hapticPage by mutableStateOf<Int?>(null)
    private var programmaticScrollJob: Job? = null

    val displayedYear: Int
        get() = yearForPage(pagerState.currentPage)

    fun yearForPage(page: Int): Int = originYear + page - YearPagerAnchorPage

    fun navigatePrevious(scope: CoroutineScope): Boolean = navigateBy(scope, delta = -1)

    fun navigateNext(scope: CoroutineScope): Boolean = navigateBy(scope, delta = 1)

    suspend fun syncToSelectedYear(
        scope: CoroutineScope,
        selectedYear: Int,
    ) {
        val targetPage = pageForYear(selectedYear)
        if (targetPage !in 0 until YearPagerPageCount || targetPage == pagerState.settledPage) {
            return
        }

        hapticPage = null
        if (abs(targetPage - pagerState.currentPage) <= 1) {
            animateToPage(scope, targetPage)
        } else {
            programmaticPage = targetPage
            programmaticScrollJob?.cancel()
            pagerState.scrollToPage(targetPage)
            programmaticPosition.snapTo(targetPage.toFloat())
        }
    }

    fun consumeSettledPage(settledPage: Int): SettledYearPage {
        val expectedProgrammaticPage = programmaticPage
        val userDriven = expectedProgrammaticPage == null || expectedProgrammaticPage != settledPage
        val emitHaptic = userDriven || hapticPage == settledPage
        programmaticPage = null
        hapticPage = null
        return SettledYearPage(
            year = yearForPage(settledPage),
            emitHaptic = emitHaptic,
        )
    }

    private fun pageForYear(year: Int): Int = YearPagerAnchorPage + year - originYear

    private fun navigateBy(scope: CoroutineScope, delta: Int): Boolean {
        val basePage = programmaticPage ?: pagerState.currentPage
        val targetPage = basePage + delta
        if (targetPage !in 0 until YearPagerPageCount) return false
        hapticPage = targetPage
        animateToPage(scope, targetPage)
        return true
    }

    /** Match calendar arrow physics: interruptible retargeting preserves current velocity. */
    private fun animateToPage(scope: CoroutineScope, requestedPage: Int) {
        val targetPage = requestedPage.coerceIn(0, YearPagerPageCount - 1)
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

internal data class SettledYearPage(
    val year: Int,
    val emitHaptic: Boolean,
)

@Composable
internal fun rememberYearSummaryPagerState(initialYear: Int): YearSummaryPagerState {
    val originYear = rememberSaveable { initialYear }
    val pagerState = rememberPagerState(
        initialPage = YearPagerAnchorPage,
        pageCount = { YearPagerPageCount },
    )
    return remember(pagerState, originYear) {
        YearSummaryPagerState(
            pagerState = pagerState,
            originYear = originYear,
        )
    }
}

@Composable
internal fun YearSummaryPagerEffects(
    pager: YearSummaryPagerState,
    selectedYear: Int,
    scope: CoroutineScope,
    onSelectYear: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(pager, selectedYear) {
        pager.syncToSelectedYear(scope, selectedYear)
    }

    LaunchedEffect(pager, selectedYear) {
        var previousSettledPage = pager.pagerState.settledPage
        snapshotFlow { pager.pagerState.settledPage }.collect { settledPage ->
            if (settledPage == previousSettledPage) return@collect

            val settled = pager.consumeSettledPage(settledPage)
            if (settled.emitHaptic) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
            if (settled.year != selectedYear) {
                onSelectYear(settled.year)
            }
            previousSettledPage = settledPage
        }
    }
}
