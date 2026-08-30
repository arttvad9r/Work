package com.worktime.app.macrobenchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.worktime.app.benchmark.shared.WorkTimeJourneys
import com.worktime.app.benchmark.shared.WorkTimePackage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MonthNavigationBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val journeys = WorkTimeJourneys()

    @Test
    fun nextMonthFrameTimingWithBaselineProfile() = benchmarkRule.measureRepeated(
        packageName = WorkTimePackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require,
            warmupIterations = 0,
        ),
        iterations = 10,
        setupBlock = {
            killProcess()
            journeys.launchCalendar(this)
        },
    ) {
        journeys.navigateToNextMonth(this)
    }
}
