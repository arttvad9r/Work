package com.worktime.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SummaryGestureUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tapExpandsPersistentSummarySheet() {
        val strip = waitForSummaryStrip()

        strip.performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("monthly-report-panel").assertIsDisplayed()
    }

    @Test
    fun upwardDragExpandsPersistentSummarySheet() {
        val strip = waitForSummaryStrip()

        // The strip is the real BottomSheetScaffold peek. This gesture is handled by the
        // Material sheet itself rather than converted into a second animation after release.
        strip.performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("monthly-report-panel").assertIsDisplayed()
    }

    private fun waitForSummaryStrip() = composeRule.onNodeWithTag("monthly-summary-strip").also {
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            runCatching { it.fetchSemanticsNode() }.isSuccess
        }
        it.assertIsDisplayed()
    }
}
