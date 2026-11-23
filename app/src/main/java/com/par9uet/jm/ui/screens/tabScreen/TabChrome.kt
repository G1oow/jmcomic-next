package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.theme.AppMotionPolicy
import com.par9uet.jm.ui.theme.LocalAppMotion
import com.par9uet.jm.ui.theme.motionEasing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 顶栏与底栏的显隐状态。
 *
 * 收起进度跟着手指连续变化：上滑多少就收起多少，下滑多少就展开多少，松手后再按位置和
 * 甩动方向补一段短动画收尾，而不是攒够一小段位移就直接跳到底。只有手指拖动
 * （[NestedScrollSource.UserInput]）改变进度，惯性滑动不参与，避免快速甩动列表时栏位抖动。
 */
@Stable
class TabChromeState internal constructor(
    private val scope: CoroutineScope,
    private val motion: AppMotionPolicy,
    private val collapseDistancePx: Float,
    private val flingVelocityPx: Float,
) {
    /** 已收起的位移，范围 0f（完全展开）到 [collapseDistancePx]（完全收起）。 */
    private var offsetPx by mutableFloatStateOf(0f)

    private var settleJob: Job? = null

    /** 0f = 完全展开，1f = 完全收起。 */
    val progress: Float
        get() = (offsetPx / collapseDistancePx).coerceIn(0f, 1f)

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput) dragBy(-available.y)
            // 只观察，不消费，滚动行为完全交给列表。
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            // 手指离开时按当前位置和甩动方向补齐剩余位移。
            settle(available.y)
            return Velocity.Zero
        }
    }

    /** 切页、回到前台等场景重新展开栏位。 */
    fun show() {
        animateTo(0f)
    }

    /** [deltaPx] 为正表示收起方向：手指上滑，内容向前翻。 */
    private fun dragBy(deltaPx: Float) {
        settleJob?.cancel()
        settleJob = null
        offsetPx = (offsetPx + deltaPx).coerceIn(0f, collapseDistancePx)
    }

    private fun settle(velocityY: Float) {
        val target = when {
            // 继续上滑的惯性：直接收起。
            velocityY <= -flingVelocityPx -> collapseDistancePx
            // 向下回看的惯性：直接展开。
            velocityY >= flingVelocityPx -> 0f
            offsetPx >= collapseDistancePx / 2f -> collapseDistancePx
            else -> 0f
        }
        animateTo(target)
    }

    private fun animateTo(target: Float) {
        settleJob?.cancel()
        settleJob = null
        if (target == offsetPx) return
        if (!motion.enabled) {
            offsetPx = target
            return
        }
        val duration = if (target > offsetPx) motion.exitMs else motion.enterMs
        settleJob = scope.launch {
            animate(
                initialValue = offsetPx,
                targetValue = target,
                animationSpec = tween(durationMillis = duration, easing = motionEasing),
            ) { value, _ ->
                offsetPx = value
            }
        }
    }
}

@Composable
fun rememberTabChromeState(): TabChromeState {
    val density = LocalDensity.current
    val motion = LocalAppMotion.current
    val scope = rememberCoroutineScope()
    return remember(density, motion, scope) {
        TabChromeState(
            scope = scope,
            motion = motion,
            collapseDistancePx = with(density) { ChromeCollapseDistance.toPx() },
            flingVelocityPx = with(density) { ChromeSettleVelocity.toPx() },
        )
    }
}

/**
 * 栏位容器：栏位高度与内容位移同步收缩，[androidx.compose.material3.Scaffold] 的内边距
 * 随之变化，因此内容不会在动画结束后突然跳动。
 *
 * @param fromTop true 表示顶栏（向上收起），false 表示底栏（向下收起）。
 * @param minVisible 收起后仍为系统栏预留的高度，避免内容滑到状态栏/导航栏手势区下面。
 */
@Composable
fun CollapsingChromeSlot(
    state: TabChromeState,
    fromTop: Boolean,
    modifier: Modifier = Modifier,
    minVisible: Dp = 0.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    val minVisiblePx = with(LocalDensity.current) { minVisible.roundToPx() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(containerColor)
    ) {
        SubcomposeLayout { constraints ->
            val placeables = subcompose(ChromeSlotContent, content).map {
                it.measure(Constraints(maxWidth = constraints.maxWidth))
            }
            val fullHeight = placeables.maxOfOrNull { it.height } ?: 0
            // 进度在布局阶段读取：拖动过程中只有栏位重新测量，页面主体不参与重组。
            val shift = (fullHeight * state.progress).roundToInt()
            val visibleHeight = (fullHeight - shift)
                .coerceAtLeast(minVisiblePx)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(constraints.maxWidth, visibleHeight) {
                val offsetY = if (fromTop) -shift else shift
                placeables.forEach { it.placeRelative(0, offsetY) }
            }
        }
    }
}

/** 手指滑过这段距离，栏位正好收起或展开到底。 */
private val ChromeCollapseDistance = 72.dp

/** 松手时超过这个速度（按 dp/s 折算）就顺着甩动方向收起或展开。 */
private val ChromeSettleVelocity = 600.dp

private const val ChromeSlotContent = "tabChromeSlot"
