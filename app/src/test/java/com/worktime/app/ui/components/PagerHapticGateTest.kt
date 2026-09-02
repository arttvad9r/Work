package com.worktime.app.ui.components

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PagerHapticGateTest {
    @Test
    fun emitsExactlyWhenHorizontalDragCrossesSnapThreshold() {
        val gate = PagerHapticGate(thresholdFraction = 0.35f)

        assertFalse(gate.update(horizontalFraction = 0.20f, horizontalDominant = true))
        assertFalse(gate.update(horizontalFraction = 0.34f, horizontalDominant = true))
        assertTrue(gate.update(horizontalFraction = 0.35f, horizontalDominant = true))
        assertFalse(gate.update(horizontalFraction = 0.48f, horizontalDominant = true))
    }

    @Test
    fun verticalDominantMovementDoesNotEmit() {
        val gate = PagerHapticGate(thresholdFraction = 0.35f)

        assertFalse(gate.update(horizontalFraction = 0.50f, horizontalDominant = false))
        assertTrue(gate.update(horizontalFraction = 0.50f, horizontalDominant = true))
    }

    @Test
    fun returningTowardAnchorRearmsTheDetent() {
        val gate = PagerHapticGate(thresholdFraction = 0.35f)

        assertTrue(gate.update(horizontalFraction = -0.40f, horizontalDominant = true))
        assertFalse(gate.update(horizontalFraction = -0.30f, horizontalDominant = true))
        assertFalse(gate.update(horizontalFraction = -0.18f, horizontalDominant = true))
        assertTrue(gate.update(horizontalFraction = 0.40f, horizontalDominant = true))
    }
}
