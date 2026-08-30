package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
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
class SettingsThemeSelectionUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun themeChangeCommitsBeforeSegmentIndicatorAnimationAdvances() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        var changedMode: ThemeMode? = null
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            WorkTimeTheme {
                Box(Modifier.size(320.dp, 800.dp)) {
                    SettingsScreen(
                        defaultHourlyRateMicros = 370_000_000L,
                        themeMode = ThemeMode.SYSTEM,
                        operationErrorMessage = null,
                        onDismiss = {},
                        onThemeChange = { changedMode = it },
                        onRateChange = {},
                        onOpenChangeRate = {},
                        onExportData = {},
                        onExportCsv = {},
                        onImportData = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.theme_dark)).performClick()

        check(changedMode == ThemeMode.DARK) {
            "theme change waited for segmented indicator animation: $changedMode"
        }

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
    }
}
