package com.worktime.app.ui.calendar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalMaterial3Api::class)
class SummarySheetToggleTest {
    @Test
    fun hiddenTargetRequestsExpansion() {
        assertTrue(shouldExpandSummaryAfterToggle(SheetValue.Hidden))
    }

    @Test
    fun expandedTargetRequestsHide() {
        assertFalse(shouldExpandSummaryAfterToggle(SheetValue.Expanded))
    }

    @Test
    fun closingAnimationCanBeReversedImmediately() {
        // During closing, currentValue may still be Expanded while targetValue is already Hidden.
        // The next tap must follow the latest target and reopen instead of issuing hide() again.
        assertTrue(shouldExpandSummaryAfterToggle(SheetValue.Hidden))
    }
}
