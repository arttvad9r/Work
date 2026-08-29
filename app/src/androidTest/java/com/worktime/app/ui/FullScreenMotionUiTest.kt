package com.worktime.app.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
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
        // On compact managed devices the summary strip can sit just below the visible viewport.
        // Its semantics action is still the canonical app action, so invoke it directly instead
        // of making this motion regression depend on calendar viewport geometry.
        composeRule
            .onNodeWithTag("monthly-summary-strip")
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

        // Sample the whole transition instead of assuming a device-specific position at one
        // timestamp. The old regression targeted exactly width/5, so while composed it could
        // never travel beyond 20% of the viewport. The intended full-width exit must cross it.
        var maxTravelled = 0f
        repeat(60) {
            composeRule.mainClock.advanceTimeByFrame()
            val left = runCatching { node.fetchSemanticsNode().boundsInRoot.left }.getOrNull()
                ?: return@repeat
            maxTravelled = maxOf(maxTravelled, left - initialLeft)
        }

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertTrue(
            "Expected full-screen exit to travel beyond the old width/5 target; " +
                "maxTravelled=$maxTravelled rootWidth=$rootWidth",
            maxTravelled > rootWidth * 0.21f,
        )
        node.assertDoesNotExist()
    }
}
