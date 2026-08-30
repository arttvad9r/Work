package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasPerformImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.ui.dayeditor.DayEditorSheetContent
import com.worktime.app.ui.theme.WorkTimeTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayEditorFocusUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun persistentNumericEditorKeepsFocusAcrossFieldChanges() {
        composeRule.setContent {
            WorkTimeTheme {
                Box(Modifier.size(320.dp, 800.dp)) {
                    DayEditorSheetContent(
                        date = LocalDate.of(2026, 8, 21),
                        existing = null,
                        defaultHourlyRateMicros = 370_000_000L,
                        operationErrorMessage = null,
                        onDismiss = {},
                        onSave = {},
                        onDelete = {},
                    )
                }
            }
        }

        val input = activeInput()
        input.performClick()
        input.assertIsFocused()

        // IME Next moves Duration -> Rate. The same persistent input must keep focus so the
        // platform IME session does not close and reopen while the field changes slots.
        input.performImeAction()
        composeRule.waitForIdle()
        activeInput().assertIsFocused()
        composeRule.onNodeWithTag("day-editor-row-duration").assertExists()

        // Switching by tapping another value row must preserve focus for the same reason.
        composeRule.onNodeWithTag("day-editor-row-bonus").performClick()
        composeRule.waitForIdle()
        activeInput().assertIsFocused()
        composeRule.onNodeWithTag("day-editor-row-rate").assertExists()
    }

    private fun activeInput(): SemanticsNodeInteraction = composeRule.onNode(
        matcher = hasAnyAncestor(hasTestTag("day-editor-active-field")) and
            hasSetTextAction() and
            hasPerformImeAction(),
        useUnmergedTree = true,
    )
}
