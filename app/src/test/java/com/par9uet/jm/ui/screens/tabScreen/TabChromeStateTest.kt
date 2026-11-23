package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
        dragDistancePx = 200f,
        settleVelocityPx = 1_000f,
    )

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
    @Test
    fun pagerPresenceExtendsTheCollapseTravel() {
        val density = Density(2f)
        val withoutPager = chromeStackTravelPx(density, inset = 8.dp, pagerPresent = false)
        val withPager = chromeStackTravelPx(density, inset = 8.dp, pagerPresent = true)
        // 分页胶囊叠在底栏上方，行程要整块多出它的高度与间距，两层才会一起滑出屏幕。
        assertEquals(with(density) { ChromeCapsulePagerBlock.toPx() }, withPager - withoutPager, 0.001f)
        assertEquals(true, withPager > withoutPager)
    }

    @Test
    fun pagerPresenceIsTrackedAndIdempotent() {
        val state = chromeState()
        assertEquals(false, state.pagerPresent)
        state.setPagerPresent(true)
        assertEquals(true, state.pagerPresent)
        state.setPagerPresent(true)
        assertEquals(true, state.pagerPresent)
        state.setPagerPresent(false)
        assertEquals(false, state.pagerPresent)
    }

    @Test
    fun pagerPresenceLengthensTheDragDistanceToKeepTheRatio() {
        val state = TabChromeState(
            scope = CoroutineScope(Dispatchers.Unconfined),
            motion = AppMotionPolicy(enabled = false, enterMs = 0, exitMs = 0),
            dragDistancePx = 200f,
            settleVelocityPx = 1_000f,
            pagerBlockPx = 200f,
        )
        // 整叠胶囊行程更长，手指也要多滑一段才收到底，栏位才不会被放大成两倍速度。
        state.setPagerPresent(true)
        state.scroll(-100f)
        assertEquals(0.25f, state.progress, 0.001f)
    }
}
