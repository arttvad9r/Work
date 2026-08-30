package com.worktime.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarUiState
import com.worktime.app.ui.yearsummary.YearSummary
import com.worktime.app.ui.yearsummary.YearSummaryScreen
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MotionUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calendarPagerCommitsMonthAfterGestureSettles() {
        var visibleMonth by mutableStateOf(YearMonth.of(2026, 8))

        composeRule.setContent {
            CalendarScreen(
                state = CalendarUiState(
                    visibleMonth = visibleMonth,
                    isReady = true,
                ),
                onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                onSelectMonth = { visibleMonth = it },
                onDayClick = {},
                onSettingsClick = {},
                onOpenYearSummary = {},
            )
        }

        composeRule.onNodeWithTag("calendar-pager")
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(YearMonth.of(2026, 9), visibleMonth)
        }
    }

    @Test
    fun yearSummaryPagerCommitsYearAfterGestureSettles() {
        var selectedYear by mutableStateOf(2026)
        val summaries = mapOf(
            2026 to emptyYearSummary(2026),
            2027 to emptyYearSummary(2027),
        )

        composeRule.setContent {
            YearSummaryScreen(
                selectedYear = selectedYear,
                summaries = summaries,
                onDismiss = {},
                onSelectYear = { selectedYear = it },
            )
        }

        composeRule.onNodeWithTag("year-summary-pager")
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(2027, selectedYear)
        }
    }

    private fun emptyYearSummary(year: Int) = YearSummary(
        year = year,
        total = MonthSummary(0, 0, 0L, 0L, 0L, 0L),
        months = List(12) { MonthSummary(0, 0, 0L, 0L, 0L, 0L) },
        monthHasData = List(12) { false },
    )
}
