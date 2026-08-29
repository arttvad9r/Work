package com.worktime.app.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
    fun settingsExitTravelsBeyondLegacyPartialWidthTarget() {
        val settings = composeRule.activity.getString(R.string.settings)

        composeRule
            .onNodeWithContentDescription(settings)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        assertFullScreenExitTravelsFarEnough(
            composeRule.onNodeWithText(settings),
        )
    }

    @Test
    fun yearSummaryExitTravelsBeyondLegacyPartialWidthTarget() {
        composeRule
            .onNodeWithTag("monthly-summary-strip")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.year_stats_title))
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        assertFullScreenExitTravelsFarEnough(
            composeRule.onNodeWithText(composeRule.activity.getString(R.string.year_summary)),
        )
    }

    private fun assertFullScreenExitTravelsFarEnough(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        val initialLeft = node.fetchSemanticsNode().boundsInRoot.left
        val rootWidth = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.width

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        // With the intended full-width target, the critically damped navigation spring has
        // already travelled well over one quarter of the viewport after three 16 ms frames.
        // The old width/5 regression could never cross that threshold before disappearing.
        composeRule.mainClock.advanceTimeBy(48L)
        val travelled = node.fetchSemanticsNode().boundsInRoot.left - initialLeft
        assertTrue(
            "Expected full-screen exit to travel beyond the old width/5 target; " +
                "travelled=$travelled rootWidth=$rootWidth",
            travelled > rootWidth * 0.25f,
        )

        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        node.assertDoesNotExist()
    }
}
