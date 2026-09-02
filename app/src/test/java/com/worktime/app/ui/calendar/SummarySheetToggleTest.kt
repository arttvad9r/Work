package com.worktime.app.ui.calendar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalMaterial3Api::class)
class SummarySheetToggleTest {
    @Test
    fun collapsedPeekRequestsExpansion() {
        assertTrue(shouldExpandSummaryAfterToggle(SheetValue.PartiallyExpanded))
    }

    @Test
    fun expandedTargetRequestsCollapse() {
        assertFalse(shouldExpandSummaryAfterToggle(SheetValue.Expanded))
    }

    @Test
    fun collapsingAnimationCanBeReversedImmediately() {
        // During collapse, currentValue may still be Expanded while targetValue is already the
        // persistent peek. The next tap follows the latest target and reopens immediately.
        assertTrue(shouldExpandSummaryAfterToggle(SheetValue.PartiallyExpanded))
    }

    @Test
    fun hiddenSupportingPaneStateCanExpandWhenCompactReturns() {
        assertTrue(shouldExpandSummaryAfterToggle(SheetValue.Hidden))
    }
}
