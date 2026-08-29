package com.worktime.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarUiState
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
}
