package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.theme.AppMotionPolicy
import com.par9uet.jm.ui.theme.LocalAppMotion
import com.par9uet.jm.ui.theme.motionEasing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 顶栏与底栏的显隐状态。
 *
 * 收起进度只驱动绘制阶段的位移，不参与测量：[FloatingChromeCapsule] 的高度恒定，
 * [androidx.compose.material3.Scaffold] 的内容内边距也就不会随手指变化。拖动时页面主体
 * 既不重新测量也不会跟着上下位移，滚动量与手指位移一一对应，不会再出现「收栏时滑动被
 * 加速或减速」的手感。
 *
 * 只有手指拖动（[NestedScrollSource.UserInput]）改变进度，惯性滑动不参与，避免快速甩动
 * 列表时栏位来回抖动。
 */
@Stable
class TabChromeState internal constructor(
    private val scope: CoroutineScope,
    private val motion: AppMotionPolicy,
    private val fallbackDragDistancePx: Float,
    private val settleVelocityPx: Float,
) {
    /** 0f = 完全展开，1f = 完全收起。 */
    private var progressState by mutableFloatStateOf(0f)

    /** 顶栏 / 底栏容纳胶囊所需的总高度（含系统栏内边距与悬浮外边距）。 */
    private var topTravelPx by mutableFloatStateOf(0f)
    private var bottomTravelPx by mutableFloatStateOf(0f)

    private var settleJob: Job? = null

    /** 最近一次拖动的方向：1f 收起，-1f 展开，0f 本次手势尚未拖动。 */
    private var dragDirection = 0f

    /** 供绘制阶段读取：只在 [graphicsLayer] 里读，进度变化只触发重绘。 */
    val progress: Float
        get() = progressState

    /** 收起时需要移动的距离，等于栏位总高度，正好把胶囊推到屏幕外。 */
    fun travelPx(fromTop: Boolean): Float =
        (if (fromTop) topTravelPx else bottomTravelPx).takeIf { it > 0f } ?: fallbackDragDistancePx

    /** 栏位测量完成后回报自身总高度。 */
    fun updateTravel(fromTop: Boolean, travelPx: Float) {
        if (fromTop) {
            if (topTravelPx != travelPx) topTravelPx = travelPx
        } else if (bottomTravelPx != travelPx) {
            bottomTravelPx = travelPx
        }
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // available.y < 0 表示手指上滑、内容向前滚动，此时收起。
            if (source == NestedScrollSource.UserInput) dragBy(-available.y)
            // 只观察，不消费：滚动量全部留给列表，栏位只做视觉位移。
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            // 手指离开时按当前位置和甩动方向补齐剩余位移。
            settle(available.y)
            return Velocity.Zero
        }
    }

    /** 切页、回到前台等场景重新展开栏位。 */
    fun show() = animateTo(0f)

    /** 手指滑过这段距离正好收起到底，位移与手指严格同步。 */
    private val dragDistancePx: Float
        get() = listOf(topTravelPx, bottomTravelPx).filter { it > 0f }.minOrNull()
            ?: fallbackDragDistancePx

    /** [deltaPx] 为正表示收起方向：手指上滑，内容向前翻。 */
    private fun dragBy(deltaPx: Float) {
        if (deltaPx == 0f) return
        settleJob?.cancel()
        settleJob = null
        dragDirection = if (deltaPx > 0f) 1f else -1f
        val next = (progressState + deltaPx / dragDistancePx).coerceIn(0f, 1f)
        if (next != progressState) progressState = next
    }

    /**
     * 松手时按位置和甩动方向补一段短动画收尾。
     *
     * 甩动方向取最近一次拖动的方向而不是速度的正负号：嵌套滚动层给出的速度符号约定与
     * 拖动增量并不一致，用拖动方向判断收放更稳。
     */
    private fun settle(velocityY: Float) {
        val fast = abs(velocityY) >= settleVelocityPx
        val target = when {
            fast && dragDirection > 0f -> 1f
            fast && dragDirection < 0f -> 0f
            progressState >= 0.5f -> 1f
            else -> 0f
        }
        animateTo(target)
    }

    private fun animateTo(target: Float) {
        settleJob?.cancel()
        settleJob = null
        if (target == progressState) return
        if (!motion.enabled) {
            progressState = target
            return
        }
        val duration = if (target > progressState) motion.exitMs else motion.enterMs
        settleJob = scope.launch {
            animate(
                initialValue = progressState,
                targetValue = target,
                animationSpec = tween(durationMillis = duration, easing = motionEasing),
            ) { value, _ ->
                progressState = value
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
            fallbackDragDistancePx = with(density) { ChromeFallbackDragDistance.toPx() },
            settleVelocityPx = with(density) { ChromeSettleVelocity.toPx() },
        )
    }
}

/**
 * 悬浮胶囊栏位：栏位背后留出系统栏内边距与一段间隙，胶囊本身带圆角与阴影浮在上面。
 *
 * 容器高度固定，收起时胶囊用 [graphicsLayer] 平移到屏幕外，测量高度不变，因此不会牵动
 * Scaffold 的内容内边距——这是「滑动不再被加减速」的关键。
 *
 * @param fromTop true 表示顶栏（向上收起），false 表示底栏（向下收起）。
 * @param inset 该侧系统栏（状态栏 / 导航栏）高度。
 */
@Composable
fun FloatingChromeCapsule(
    state: TabChromeState,
    fromTop: Boolean,
    inset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { state.updateTravel(fromTop, it.height.toFloat()) }
            .padding(
                start = ChromeCapsuleHorizontalPadding,
                end = ChromeCapsuleHorizontalPadding,
                top = if (fromTop) inset + ChromeCapsuleGap else 0.dp,
                bottom = if (fromTop) 0.dp else inset + ChromeCapsuleGap,
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    // 只改绘制位置：进度与行程都在绘制阶段读取，拖动时不触发重组和重测量。
                    // 行程多加一段间隙，收起后连阴影一起离开屏幕边缘。
                    val travel = (state.travelPx(fromTop) + ChromeCapsuleGap.toPx()) * state.progress
                    translationY = if (fromTop) -travel else travel
                },
            shape = ChromeCapsuleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
        ) {
            content()
        }
    }
}

/** 胶囊左右留白：栏位不再贴着屏幕边缘。 */
private val ChromeCapsuleHorizontalPadding = 12.dp

/** 胶囊与系统栏之间的间隙。 */
private val ChromeCapsuleGap = 8.dp

private val ChromeCapsuleShape = RoundedCornerShape(28.dp)

/** 栏位尚未测量时使用的拖动行程。 */
private val ChromeFallbackDragDistance = 88.dp

/** 松手时超过这个速度（按 dp/s 折算）就顺着甩动方向收起或展开。 */
private val ChromeSettleVelocity = 600.dp
