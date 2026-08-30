package com.worktime.app.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.ui.components.AppSegmentedControl
import com.worktime.app.ui.components.PlainDragHandle
import com.worktime.app.ui.theme.WorkTimeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickableDragHandleReservesMinimumInteractiveSize() {
        composeRule.setContent {
            WorkTimeTheme {
                PlainDragHandle(
                    onClick = {},
                    accessibilityLabel = "Monthly summary",
                )
            }
        }

        composeRule.onNodeWithContentDescription("Monthly summary")
            .assertHasClickAction()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun segmentedControlOptionsMeetMinimumInteractiveHeight() {
        composeRule.setContent {
            WorkTimeTheme {
                AppSegmentedControl(
                    options = listOf("System", "Light", "Dark"),
                    selectedIndex = 0,
                    onSelect = {},
                )
            }
        }

        composeRule.onNodeWithText("Dark")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
    }
}
