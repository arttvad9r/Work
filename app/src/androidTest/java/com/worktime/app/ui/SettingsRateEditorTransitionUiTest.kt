package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.R
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.settings.SettingsScreen
import com.worktime.app.ui.theme.WorkTimeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRateEditorTransitionUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun changeRateEndsInlineRateEditingBeforeCallback() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        var opened = false
        setSettingsContent(onOpenChangeRate = { opened = true })

        startRateEditing(context)
        composeRule.onNodeWithText(context.getString(R.string.change_rate_for_period)).performClick()
        composeRule.waitForIdle()

        check(opened)
        activeRateEditor().assertDoesNotExist()
    }

    @Test
    fun importEndsInlineRateEditingBeforeCallback() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        var imported = false
        setSettingsContent(onImportData = { imported = true })

        startRateEditing(context)
        composeRule.onNodeWithText(context.getString(R.string.import_data)).performClick()
        composeRule.waitForIdle()

        check(imported)
        activeRateEditor().assertDoesNotExist()
    }

    @Test
    fun exportDialogOpensAfterInlineRateEditorIsClosed() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        setSettingsContent()

        startRateEditing(context)
        composeRule.onNodeWithText(context.getString(R.string.export_data)).performClick()
        composeRule.waitForIdle()

        activeRateEditor().assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.export_format_title)).assertIsDisplayed()
    }

    private fun setSettingsContent(
        onOpenChangeRate: () -> Unit = {},
        onImportData: () -> Unit = {},
    ) {
        composeRule.setContent {
            WorkTimeTheme {
                Box(Modifier.size(320.dp, 800.dp)) {
                    SettingsScreen(
                        defaultHourlyRateMicros = 370_000_000L,
                        themeMode = ThemeMode.SYSTEM,
                        operationErrorMessage = null,
                        onDismiss = {},
                        onThemeChange = {},
                        onRateChange = {},
                        onOpenChangeRate = onOpenChangeRate,
                        onExportData = {},
                        onExportCsv = {},
                        onImportData = onImportData,
                    )
                }
            }
        }
    }

    private fun startRateEditing(context: android.content.Context) {
        composeRule.onNodeWithText(context.getString(R.string.settings_rate)).performClick()
        composeRule.waitForIdle()
        activeRateEditor().assertIsFocused()
    }

    private fun activeRateEditor(): SemanticsNodeInteraction = composeRule.onNode(
        matcher = hasSetTextAction(),
        useUnmergedTree = true,
    )
}
