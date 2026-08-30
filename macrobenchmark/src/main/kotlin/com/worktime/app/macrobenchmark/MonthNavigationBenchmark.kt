package com.worktime.app.macrobenchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TargetPackage = "com.worktime.app"
private const val NextMonthResourceName = "next_month"

@LargeTest
@RunWith(AndroidJUnit4::class)
class MonthNavigationBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val targetResources by lazy {
        InstrumentationRegistry.getInstrumentation()
            .context
            .packageManager
            .getResourcesForApplication(TargetPackage)
    }

    private val locale: Locale by lazy {
        targetResources.configuration.locales[0]
    }

    private val nextMonthDescription: String by lazy {
        val resourceId = targetResources.getIdentifier(
            NextMonthResourceName,
            "string",
            TargetPackage,
        )
        check(resourceId != 0) {
            "Target app does not expose string resource $NextMonthResourceName"
        }
        targetResources.getString(resourceId)
    }

    private val expectedNextMonthTitle: String by lazy {
        YearMonth.now()
            .plusMonths(1)
            .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    }

    @Test
    fun nextMonthFrameTimingWithBaselineProfile() = benchmarkRule.measureRepeated(
        packageName = TargetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require,
            warmupIterations = 0,
        ),
        iterations = 10,
        setupBlock = {
            killProcess()
            startActivityAndWait()
            uiAutomator {
                onElement {
                    contentDescription?.toString() == nextMonthDescription
                }
            }
        },
    ) {
        uiAutomator {
            onElement {
                contentDescription?.toString() == nextMonthDescription
            }.click()

            // CalendarViewModel commits the new business month only after the pager settles.
            // Waiting for the localized next-month title therefore keeps the full pager
            // transition inside the FrameTimingMetric measurement window without sleeps.
            onElement {
                textAsString() == expectedNextMonthTitle
            }
        }
    }
}
