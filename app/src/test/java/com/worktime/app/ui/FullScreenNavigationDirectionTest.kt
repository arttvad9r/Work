package com.worktime.app.ui

import androidx.compose.ui.unit.LayoutDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FullScreenNavigationDirectionTest {
    @Test
    fun ltrUsesPositiveHorizontalDirection() {
        assertEquals(1, fullScreenNavigationDirection(LayoutDirection.Ltr))
    }

    @Test
    fun rtlUsesNegativeHorizontalDirection() {
        assertEquals(-1, fullScreenNavigationDirection(LayoutDirection.Rtl))
    }
}
