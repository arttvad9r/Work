package com.worktime.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Shared motion vocabulary for direct UI feedback.
 *
 * Micro is for color/alpha response, Fast for compact controls, Standard for local
 * state changes and Emphasized for directional navigation. The same non-overshooting
 * easing keeps motion responsive without making controls feel mechanical.
 */
object AppMotion {
    const val MicroMillis = 90
    const val FastMillis = 130
    const val StandardMillis = 180
    const val EmphasizedMillis = 230

    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
