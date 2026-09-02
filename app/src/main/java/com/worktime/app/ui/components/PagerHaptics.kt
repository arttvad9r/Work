package com.worktime.app.ui.components

import kotlin.math.abs

/**
 * One deterministic pager detent per committed-direction threshold crossing.
 *
 * The caller feeds actual pager position rather than raw pointer distance. That matters for fast
 * flings: a swipe can be released before the finger itself reaches the positional threshold while
 * pager physics still commits the next page. Observing pager position lets feedback fire as soon as
 * the moving page crosses the snap threshold, during the fling instead of after settledPage.
 */
internal class PagerHapticGate(
    private val thresholdFraction: Float,
    private val rearmFraction: Float = thresholdFraction * 0.55f,
) {
    private var armed = true

    fun update(positionDeltaPages: Float): Boolean {
        val distance = abs(positionDeltaPages)

        if (distance <= rearmFraction) {
            armed = true
        }
        if (!armed || distance < thresholdFraction) {
            return false
        }

        armed = false
        return true
    }

    fun reset() {
        armed = true
    }
}
