package com.worktime.app.ui.yearsummary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YearSummaryLayoutModeTest {
    @Test
    fun `medium height keeps fixed viewport report`() {
        assertEquals(
            YearSummaryLayoutMode.FixedViewport,
            yearSummaryLayoutMode(isHeightAtLeastMedium = true),
        )
    }

    @Test
    fun `short height uses scrollable compact layout`() {
        assertEquals(
            YearSummaryLayoutMode.CompactShort,
            yearSummaryLayoutMode(isHeightAtLeastMedium = false),
        )
    }
}
