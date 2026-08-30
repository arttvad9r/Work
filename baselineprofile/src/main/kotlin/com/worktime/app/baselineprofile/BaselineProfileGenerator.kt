package com.worktime.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.worktime.app.benchmark.shared.WorkTimeJourneys
import com.worktime.app.benchmark.shared.WorkTimePackage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    private val journeys = WorkTimeJourneys()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = WorkTimePackage,
        includeInStartupProfile = true,
    ) {
        startActivityAndWait()
    }

    @Test
    fun calendarMonthNavigation() = baselineProfileRule.collect(
        packageName = WorkTimePackage,
        includeInStartupProfile = false,
    ) {
        journeys.launchCalendar(this)
        journeys.navigateToNextMonth(this)
    }
}
