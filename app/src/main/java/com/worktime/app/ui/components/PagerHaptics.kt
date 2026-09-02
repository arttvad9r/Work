package com.worktime.app.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs

/**
 * A light detent for direct pager manipulation.
 *
 * The haptic is tied to the finger crossing the same positional threshold used by pager snapping,
 * never to settledPage or animation completion. The modifier only observes pointer input and does
 * not consume it, so HorizontalPager remains the owner of drag/fling physics.
 */
@Composable
internal fun Modifier.pagerSwipeHapticFeedback(): Modifier {
    val haptics = LocalHapticFeedback.current
    return pointerInput(haptics) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = down.position
            val gate = PagerHapticGate(
                thresholdFraction = AppMotion.PagerSnapPositionalThreshold,
            )

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                val dx = change.position.x - start.x
                val dy = change.position.y - start.y
                val pageWidth = size.width.toFloat()
                if (pageWidth <= 0f) continue

                if (
                    gate.update(
                        horizontalFraction = dx / pageWidth,
                        horizontalDominant = abs(dx) > abs(dy),
                    )
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
            }
        }
    }
}

internal class PagerHapticGate(
    private val thresholdFraction: Float,
    private val rearmFraction: Float = thresholdFraction * 0.55f,
) {
    private var armed = true

    fun update(
        horizontalFraction: Float,
        horizontalDominant: Boolean,
    ): Boolean {
        val distance = abs(horizontalFraction)

        if (distance <= rearmFraction) {
            armed = true
        }
        if (!horizontalDominant || !armed || distance < thresholdFraction) {
            return false
        }

        armed = false
        return true
    }
}
