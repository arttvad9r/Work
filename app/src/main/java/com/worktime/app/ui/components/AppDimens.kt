package com.worktime.app.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Small shared dimension contract for the WorkTime interface.
 * Screens should reach for these before inventing local values.
 */
object AppDimens {
    /** Horizontal padding of every full screen and modal sheet. */
    val screenHorizontalPadding: Dp = 16.dp

    /** Space above a semantic section (and around dividers between sections). */
    val sectionSpacing: Dp = 16.dp

    /** Vertical gap between rows and small blocks. */
    val rowGap: Dp = 8.dp

    /** Standard interactive row height. */
    val rowMinHeight: Dp = 56.dp

    /** Shared compact control height for segmented controls and inline editors. */
    val compactControlHeight: Dp = 44.dp

    /** Compact inline numeric editor slot (fits inside a [rowMinHeight] row). */
    val compactFieldWidth: Dp = 120.dp
    val compactFieldHeight: Dp = compactControlHeight

    /** Minimum height of primary actions. */
    val primaryButtonMinHeight: Dp = 52.dp

    /** Short, non-layout motion used only for visual feedback. */
    const val feedbackAnimationMillis: Int = 120
}
