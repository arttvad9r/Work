package com.worktime.app.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.MainActivity
import com.worktime.app.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullScreenMotionUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsExitReturnsCleanlyToCalendar() {
        val settings = composeRule.activity.getString(R.string.settings)
        val settingsTitle = composeRule.onNodeWithText(settings)

        composeRule
            .onNodeWithContentDescription(settings)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        settingsTitle.assertIsDisplayed()

        // Settings now uses Navigation3's maintained default back transition. Do not pin it to a
        // custom travel fraction again: the previous app-owned pop transition alternated visible
        // Settings/Calendar frames on a physical device.
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runCatching { settingsTitle.fetchSemanticsNode() }.isFailure
        }
        settingsTitle.assertDoesNotExist()
        composeRule.onNodeWithContentDescription(settings).assertIsDisplayed()
    }

    @Test
    fun yearSummaryExitMovesDown() {
        val summaryStrip = composeRule.onNodeWithTag("monthly-summary-strip")
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            runCatching { summaryStrip.fetchSemanticsNode() }.isSuccess
        }
        summaryStrip.performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.year_stats_title))
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        assertVerticalExitMovesDown(
            composeRule.onNodeWithContentDescription(
                composeRule.activity.getString(R.string.previous_year),
            ),
        )
    }

    private fun assertVerticalExitMovesDown(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        val initialTop = node.fetchSemanticsNode().boundsInRoot.top
        val rootHeight = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.height

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        var maxTravelled = 0f
        repeat(60) {
            composeRule.mainClock.advanceTimeByFrame()
            val top = runCatching { node.fetchSemanticsNode().boundsInRoot.top }.getOrNull()
                ?: return@repeat
            maxTravelled = maxOf(maxTravelled, top - initialTop)
        }

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertTrue(
            "Expected a readable but restrained Year Summary downward exit; " +
                "maxTravelled=$maxTravelled rootHeight=$rootHeight",
            maxTravelled >= rootHeight * 0.08f && maxTravelled <= rootHeight * 0.12f,
        )
        node.assertDoesNotExist()
    }
}
