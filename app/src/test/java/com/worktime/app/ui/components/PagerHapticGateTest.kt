package com.worktime.app.ui.components

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PagerHapticGateTest {
    @Test
    fun emitsExactlyWhenPagerPositionCrossesSnapThreshold() {
        val gate = PagerHapticGate(thresholdFraction = 0.35f)

        assertFalse(gate.update(positionDeltaPages = 0.20f))
        assertFalse(gate.update(positionDeltaPages = 0.34f))
        assertTrue(gate.update(positionDeltaPages = 0.35f))
        assertFalse(gate.update(positionDeltaPages = 0.48f))
    }

    @Test
    fun flingCanEmitAfterPointerReleaseWhenPagerCrossesThreshold() {
        val gate = PagerHapticGate(thresholdFraction = 0.35f)

        // The finger may release at 20%, then pager fling physics continues toward the next page.
        assertFalse(gate.update(positionDeltaPages = 0.20f))
        assertTrue(gate.update(positionDeltaPages = 0.37f))
    }

    @Test
    fun returningTowardAnchorRearmsTheDetent() {
        val gate = PagerHapticGate(thresholdFraction = 0.35f)

        assertTrue(gate.update(positionDeltaPages = -0.40f))
        assertFalse(gate.update(positionDeltaPages = -0.30f))
        assertFalse(gate.update(positionDeltaPages = -0.18f))
        assertTrue(gate.update(positionDeltaPages = 0.40f))
    }

    @Test
    fun explicitResetRearmsAfterProgrammaticNavigation() {
        val gate = PagerHapticGate(thresholdFraction = 0.35f)

        assertTrue(gate.update(positionDeltaPages = 0.40f))
        gate.reset()
        assertTrue(gate.update(positionDeltaPages = 0.40f))
    }
}
