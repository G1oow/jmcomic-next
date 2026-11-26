package com.par9uet.jm.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMotionTest {
    @Test
    fun systemDisabledAnimationsRemainOffOnAllSupportedVersions() {
        listOf(23, 24, 35, 36).forEach {
            val policy = appMotionPolicy(it, 0f)
            assertFalse(policy.enabled)
            assertEquals(0, policy.enterMs)
            assertEquals(0, policy.exitMs)
            assertEquals(0, policy.pageTurnMs)
            assertEquals(0, policy.fadeThroughOutMs)
            assertEquals(0, policy.fadeThroughInMs)
        }
    }

    @Test
    fun androidSixUsesShorterTransitions() {
        val legacy = appMotionPolicy(23, 1f)
        val modern = appMotionPolicy(35, 1f)
        assertTrue(legacy.enabled)
        assertTrue(legacy.enterMs < modern.enterMs)
        assertTrue(legacy.exitMs < legacy.enterMs)
        assertTrue(legacy.pageTurnMs < modern.pageTurnMs)
        assertTrue(modern.enterMs < 300)
    }

    @Test
    fun fadeThroughSeparatesExitBeforeEnter() {
        val policy = appMotionPolicy(35, 1f)
        assertTrue(policy.fadeThroughOutMs > 0)
        assertTrue(policy.fadeThroughInMs > policy.fadeThroughOutMs)
        assertTrue(policy.fadeThroughOutMs + policy.fadeThroughInMs <= 320)
    }

    @Test
    fun animatorDurationScaleAdjustsDurationsWithinBounds() {
        val half = appMotionPolicy(35, 0.5f)
        val normal = appMotionPolicy(35, 1f)
        val slow = appMotionPolicy(35, 2f)
        assertTrue(half.enterMs < normal.enterMs)
        assertTrue(normal.enterMs < slow.enterMs)
        assertEquals(slow.enterMs, appMotionPolicy(35, 5f).enterMs)
    }

    @Test
    fun sharedAxisOffsetScalesWithViewportAndCapsAtMax() {
        assertEquals(0, sharedAxisOffsetPx(width = 0, fraction = 10, maxPx = 120))
        assertEquals(40, sharedAxisOffsetPx(width = 400, fraction = 10, maxPx = 120))
        assertEquals(120, sharedAxisOffsetPx(width = 2400, fraction = 10, maxPx = 120))
    }
}
