package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.ui.theme.WorkTimeTheme
import com.worktime.app.ui.yearsummary.YearSummary
import com.worktime.app.ui.yearsummary.YearSummaryScreen
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YearSummaryLayoutUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allTwelveMonthsFitAndVerticalSwipeDoesNotMoveReport() {
        val summary = populatedYearSummary()
        composeRule.setContent {
            WorkTimeTheme {
                Box(Modifier.size(360.dp, 800.dp)) {
                    YearSummaryScreen(
                        selectedYear = summary.year,
                        summaries = mapOf(summary.year to summary),
                        onDismiss = {},
                        onSelectYear = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        (1..12).forEach { month ->
            composeRule.onNodeWithTag("year-summary-month-$month").assertIsDisplayed()
        }

        val pagerBottom = composeRule.onNodeWithTag("year-summary-pager")
            .fetchSemanticsNode().boundsInRoot.bottom
        val decemberBefore = composeRule.onNodeWithTag("year-summary-month-12")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "December must fit inside the year-summary pager: ${decemberBefore.bottom} > $pagerBottom",
            decemberBefore.bottom <= pagerBottom + 1f,
        )

        composeRule.onNodeWithTag("year-summary-content")
            .performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        val decemberAfter = composeRule.onNodeWithTag("year-summary-month-12")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Year summary moved vertically after swipe: ${decemberBefore.top} -> ${decemberAfter.top}",
            abs(decemberBefore.top - decemberAfter.top) <= 1f,
        )
    }

    private fun populatedYearSummary(): YearSummary {
        val months = List(12) { index ->
            monthSummary(
                workedMinutes = 7_200 + index * 30,
                shifts = 15 + index % 3,
                payRubles = 55_000L + index * 1_000L,
            )
        }
        return YearSummary(
            year = 2026,
            total = MonthSummary(
                workedMinutes = months.sumOf { it.workedMinutes },
                shiftCount = months.sumOf { it.shiftCount },
                basePayMicros = months.sumOf { it.basePayMicros },
                bonusMicros = 0L,
                penaltyMicros = 0L,
                totalPayMicros = months.sumOf { it.totalPayMicros },
            ),
            months = months,
            monthHasData = List(12) { true },
        )
    }

    private fun monthSummary(
        workedMinutes: Int,
        shifts: Int,
        payRubles: Long,
    ): MonthSummary {
        val payMicros = payRubles * 1_000_000L
        return MonthSummary(
            workedMinutes = workedMinutes,
            shiftCount = shifts,
            basePayMicros = payMicros,
            bonusMicros = 0L,
            penaltyMicros = 0L,
            totalPayMicros = payMicros,
        )
    }
}
