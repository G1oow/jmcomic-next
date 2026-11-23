package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.par9uet.jm.ui.theme.AppMotionPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TabChromeStateTest {

    private fun chromeState(): TabChromeState = TabChromeState(
        scope = CoroutineScope(Dispatchers.Unconfined),
        motion = AppMotionPolicy(enabled = false, enterMs = 0, exitMs = 0),
        fallbackDragDistancePx = 100f,
        settleVelocityPx = 1_000f,
    ).apply {
        updateDragDistance(200f)
    }

    private fun TabChromeState.scroll(deltaY: Float, source: NestedScrollSource = NestedScrollSource.UserInput) {
        nestedScrollConnection.onPreScroll(Offset(0f, deltaY), source)
    }

    private fun TabChromeState.release(velocityY: Float) = runBlocking {
        nestedScrollConnection.onPreFling(Velocity(0f, velocityY))
    }

    @Test
    fun draggingUpCollapsesChrome() {
        val state = chromeState()
        state.scroll(-50f)
        assertEquals(0.25f, state.progress, 0.001f)
    }

    @Test
    fun draggingDownExpandsChromeAndClampsBetweenEnds() {
        val state = chromeState()
        state.scroll(-1_000f)
        assertEquals(1f, state.progress, 0.001f)
        state.scroll(60f)
        assertEquals(0.7f, state.progress, 0.001f)
        state.scroll(1_000f)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun scrollDeltaIsNeverConsumed() {
        val state = chromeState()
        // 栏位只做视觉位移，滚动量必须原样留给列表，否则滑动会被加减速。
        assertEquals(Offset.Zero, state.nestedScrollConnection.onPreScroll(Offset(0f, -30f), NestedScrollSource.UserInput))
    }

    @Test
    fun flingAndProgrammaticScrollDoNotMoveChrome() {
        val state = chromeState()
        state.scroll(-40f, NestedScrollSource.SideEffect)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun slowReleaseSettlesToNearestEnd() {
        val state = chromeState()
        state.scroll(-30f)
        state.release(0f)
        assertEquals(0f, state.progress, 0.001f)

        state.scroll(-120f)
        state.release(0f)
        assertEquals(1f, state.progress, 0.001f)
    }

    @Test
    fun fastReleaseFollowsLastDragDirectionRatherThanVelocitySign() {
        val state = chromeState()
        state.scroll(-10f)
        state.release(-5_000f)
        assertEquals(1f, state.progress, 0.001f)

        state.scroll(10f)
        state.release(5_000f)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun showExpandsChromeAgain() {
        val state = chromeState()
        state.scroll(-1_000f)
        state.show()
        assertEquals(0f, state.progress, 0.001f)
    }
}
