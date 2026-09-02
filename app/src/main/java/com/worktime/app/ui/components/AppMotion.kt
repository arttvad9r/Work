package com.worktime.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Shared refresh-rate-independent motion vocabulary.
 *
 * Motion is expressed in real time or critically damped physics, never in a fixed frame
 * count. Compose/Choreographer samples the same animation at whatever cadence the display
 * currently exposes (120/90/60 Hz, including dynamic switches), so interaction speed stays
 * stable while higher refresh rates simply receive more intermediate frames.
 *
 * Tweens are reserved for non-spatial properties such as color/alpha. Spatial motion uses
 * the spring constants below so it can be interrupted and re-targeted without restarting a
 * frame-counted sequence or overshooting routine controls.
 */
object AppMotion {
    // Non-spatial feedback. At 120 Hz these are roughly 9/12/18/23 frames; at 60 Hz the
    // same durations remain perceptually short rather than becoming half-speed.
    const val MicroMillis = 75
    const val FastMillis = 100
    const val StandardMillis = 150
    const val EmphasizedMillis = 190

    // Critically damped positional motion. Routine interactions intentionally use a softer
    // settle than the first feedback pass: the previous high stiffness read as abrupt on-device.
    const val NoBounceDampingRatio = 1f
    const val ControlStiffness = 900f
    const val NavigationStiffness = 420f
    const val PagerStiffness = 500f
    const val PagerSnapPositionalThreshold = 0.35f

    // Direct-manipulation feedback stays deliberately quiet. Large scale/tone changes compound
    // with platform indications and make frequent interactions feel busy rather than responsive.
    const val PressedScale = 0.995f
    const val PressedStateAlpha = 0.04f
    const val DragScaleReduction = 0.005f

    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
