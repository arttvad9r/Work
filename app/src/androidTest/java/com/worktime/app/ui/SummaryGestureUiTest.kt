package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.ui.calendar.CalendarUiState
import com.worktime.app.ui.calendar.SummaryStrip
import com.worktime.app.ui.theme.WorkTimeTheme
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SummaryGestureUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shortUpwardDragStaysBelowSummaryThreshold() {
        var swipeUps = 0
        setSummaryStrip(onSwipeUp = { swipeUps += 1 })

        composeRule.onNodeWithTag("monthly-summary-strip")
            .performTouchInput {
                // The strip is 56dp high. Half-height is 28dp, intentionally below the
                // production 40dp activation threshold.
                swipeUp(startY = bottom, endY = centerY)
            }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(0, swipeUps)
        }
    }

    @Test
    fun fullUpwardDragCrossesSummaryThresholdOnce() {
        var swipeUps = 0
        setSummaryStrip(onSwipeUp = { swipeUps += 1 })

        composeRule.onNodeWithTag("monthly-summary-strip")
            .performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, swipeUps)
        }
    }

    private fun setSummaryStrip(onSwipeUp: () -> Unit) {
        composeRule.setContent {
            WorkTimeTheme {
                Box(Modifier.size(320.dp, 120.dp)) {
                    SummaryStrip(
                        state = CalendarUiState(
                            visibleMonth = YearMonth.of(2026, 8),
                            isReady = true,
                        ),
                        locale = Locale.US,
                        expanded = false,
                        onClick = {},
                        onSwipeUp = onSwipeUp,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }
}
