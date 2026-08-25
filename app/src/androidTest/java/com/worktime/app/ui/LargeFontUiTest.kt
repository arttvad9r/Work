package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarUiState
import com.worktime.app.ui.calendar.RatePeriodUi
import com.worktime.app.ui.calendar.YearSummary
import com.worktime.app.ui.settings.RateHistoryScreen
import com.worktime.app.ui.settings.YearSummaryScreen
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LargeFontUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun yearSummaryKeepsEssentialActionsAtLargeFontInNarrowLayout() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                Box(Modifier.size(240.dp, 800.dp)) {
                    YearSummaryScreen(
                        summary = summary(),
                        onDismiss = {},
                        onPreviousYear = {},
                        onNextYear = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("2026").assertIsDisplayed()
        composeRule.onNodeWithText("By month").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Previous year",
        ).assertIsDisplayed()
    }

    @Test
    fun rateHistoryKeepsRecordedEntryActionAtLargeFontInNarrowLayout() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                Box(Modifier.size(240.dp, 800.dp)) {
                    RateHistoryScreen(
                        periods = listOf(
                            RatePeriodUi(
                                start = LocalDate.of(2026, 1, 1),
                                end = LocalDate.of(2026, 6, 1),
                                rateMicros = 500_000_000L,
                                entryCount = 2,
                            ),
                        ),
                        onDismiss = {},
                        onEditPeriod = {},
                        onAddPeriod = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Rates in recorded entries").assertIsDisplayed()
        composeRule.onNodeWithText("Change rate for period").assertIsDisplayed()
    }

    @Test
    fun calendarKeepsNavigationAndPopulatedDayAtLargeFontInNarrowLayout() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                Box(Modifier.size(280.dp, 800.dp)) {
                    CalendarScreen(
                        state = CalendarUiState(
                            visibleMonth = YearMonth.of(2026, 1),
                            entries = mapOf(
                                LocalDate.of(2026, 1, 15) to WorkEntry(
                                    date = LocalDate.of(2026, 1, 15),
                                    workedMinutes = 480,
                                    hourlyRateMicros = 10_000_000L,
                                ),
                            ),
                            isReady = true,
                        ),
                        onPreviousMonth = {},
                        onNextMonth = {},
                        onSelectMonth = {},
                        onDayClick = {},
                        onSettingsClick = {},
                        onOpenYearSummary = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Previous month").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Next month").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar-month-title").assertIsDisplayed()
        composeRule.onNodeWithText("15", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("entry recorded", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("8 h", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("80", substring = true).assertIsDisplayed()
    }

    private fun summary() = YearSummary(
        year = 2026,
        total = MonthSummary(480, 1, 8_000_000L, 0L, 0L, 8_000_000L),
        months = List(12) { MonthSummary(480, 1, 8_000_000L, 0L, 0L, 8_000_000L) },
        monthHasData = List(12) { true },
    )
}
