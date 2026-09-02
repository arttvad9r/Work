package com.worktime.app.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.MainActivity
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SummaryGestureUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tapExpandsSummaryReportWithoutMovingPersistentStrip() {
        val strip = waitForSummaryStrip()
        val collapsedTop = strip.fetchSemanticsNode().boundsInRoot.top

        strip.performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("monthly-report-panel").assertIsDisplayed()
        assertStripStayedAt(collapsedTop, strip)
    }

    @Test
    fun upwardDragExpandsSummaryReportWithoutMovingPersistentStrip() {
        val strip = waitForSummaryStrip()
        val collapsedTop = strip.fetchSemanticsNode().boundsInRoot.top

        // The gesture still starts on the persistent strip and is owned by Material sheet physics;
        // only the report body moves. The strip itself must remain at the same screen position.
        strip.performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("monthly-report-panel").assertIsDisplayed()
        assertStripStayedAt(collapsedTop, strip)
    }

    @Test
    fun persistentStripStaysAboveSystemNavigation() {
        val strip = waitForSummaryStrip()
        val stripBottom = strip.fetchSemanticsNode().boundsInRoot.bottom
        val decorView = composeRule.activity.window.decorView
        val navigationBottom = ViewCompat.getRootWindowInsets(decorView)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            ?.bottom
            ?: 0

        assertTrue(
            "Summary strip must remain above system navigation; " +
                "stripBottom=$stripBottom viewHeight=${decorView.height} navBottom=$navigationBottom",
            stripBottom <= decorView.height - navigationBottom + 1f,
        )
    }

    private fun assertStripStayedAt(
        expectedTop: Float,
        strip: SemanticsNodeInteraction,
    ) {
        val expandedTop = strip.fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            "Persistent summary strip moved with the report; " +
                "collapsedTop=$expectedTop expandedTop=$expandedTop",
            abs(expandedTop - expectedTop) <= 2f,
        )
    }

    private fun waitForSummaryStrip() = composeRule.onNodeWithTag("monthly-summary-strip").also {
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            runCatching { it.fetchSemanticsNode() }.isSuccess
        }
        it.assertIsDisplayed()
    }
}
