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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 底栏的显隐状态。
 *
 * 收起进度跟着手指连续变化：上滑多少就收起多少，下滑多少就展开多少，松手后再按位置和
 * 甩动方向补一段短动画收尾。只有手指拖动（[NestedScrollSource.UserInput]）改变进度，
 * 惯性滑动不参与，避免快速甩动列表时栏位抖动。
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

    /** 收起需要滑过的距离，由 [CollapsingBottomCapsule] 测量后回报，和手指一一对应。 */
    private var dragDistancePx by mutableFloatStateOf(0f)

    private var settleJob: Job? = null

    /** 最近一次拖动的方向：1f 收起，-1f 展开，0f 本次手势尚未拖动。 */
    private var dragDirection = 0f

    /** 0f 完全展开，1f 完全收起。布局阶段读取，进度变化不触发重组。 */
    val progress: Float
        get() = progressState

    /** 栏位测量完成后回报「展开高度减去系统导航栏内边距」。 */
    internal fun updateDragDistance(distancePx: Float) {
        if (dragDistancePx != distancePx) dragDistancePx = distancePx
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // available.y < 0 表示手指上滑、内容向前滚动，此时收起。
            if (source == NestedScrollSource.UserInput) dragBy(-available.y)
            // 只观察，不消费：滚动量全部留给列表，栏位只跟着位移和收缩。
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

    private val resolvedDragDistancePx: Float
        get() = dragDistancePx.takeIf { it > 0f } ?: fallbackDragDistancePx

    /** [deltaPx] 为正表示收起方向：手指上滑，内容向前翻。 */
    private fun dragBy(deltaPx: Float) {
        if (deltaPx == 0f) return
        settleJob?.cancel()
        settleJob = null
        dragDirection = if (deltaPx > 0f) 1f else -1f
        val next = (progressState + deltaPx / resolvedDragDistancePx).coerceIn(0f, 1f)
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
 * 固定悬浮胶囊：顶栏常驻页面最上方，不随滚动隐藏。
 *
 * @param inset 状态栏高度。
 */
@Composable
fun FloatingTopCapsule(
    inset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ChromeCapsuleHorizontalPadding,
                end = ChromeCapsuleHorizontalPadding,
                top = inset + ChromeCapsuleGap,
                bottom = ChromeCapsuleGap,
            )
    ) {
        CapsuleSurface {
            content()
        }
    }
}

/**
 * 悬浮胶囊底栏：上滑跟手收起、下滑展开。
 *
 * 栏位高度跟着进度一起收缩，所以页面可用高度实时变化：收起过程中只改内容区的**底部**
 * 边界，顶部边界不动，内容不会整体位移；收起完成后内容一直补到系统导航栏上方，不留空白。
 * 高度在布局阶段算，进度变化只重新测量栏位与内容高度，不触发重组。
 *
 * @param inset 导航栏高度，收起后仍为系统手势区预留。
 */
@Composable
fun CollapsingBottomCapsule(
    state: TabChromeState,
    inset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val insetPx = with(density) { inset.roundToPx() }
    val gapPx = with(density) { ChromeCapsuleGap.roundToPx() }
    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val placeables = subcompose(BottomCapsuleContent) {
            CapsuleSurface { content() }
        }.map { it.measure(Constraints(maxWidth = constraints.maxWidth)) }
        val capsuleHeight = placeables.maxOfOrNull { it.height } ?: 0
        // 展开时：系统栏 + 下间隙 + 胶囊 + 上间隙。
        val expandedHeight = insetPx + gapPx + capsuleHeight + gapPx
        state.updateDragDistance((expandedHeight - insetPx).toFloat())
        val progress = state.progress
        val slotHeight = (expandedHeight - (expandedHeight - insetPx) * progress).roundToInt()
        // 收起行程：连阴影一起滑出屏幕底部。
        val travel = ((insetPx + gapPx + capsuleHeight) * progress).roundToInt()
        val capsuleTop = slotHeight - insetPx - gapPx - capsuleHeight + travel
        layout(constraints.maxWidth, slotHeight) {
            // 允许画到栏位之外：超出屏幕底部的部分由窗口自然裁掉。
            placeables.forEach { it.placeRelative(0, capsuleTop) }
        }
    }
}

/** 胶囊本体：圆角 + 阴影，背景色与系统栏内边距由外层决定。 */
@Composable
private fun CapsuleSurface(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ChromeCapsuleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
    ) {
        content()
    }
}

/** 胶囊左右留白：栏位不贴着屏幕边缘。 */
private val ChromeCapsuleHorizontalPadding = 12.dp

/** 胶囊与系统栏、页面内容之间的间隙。 */
private val ChromeCapsuleGap = 8.dp

private val ChromeCapsuleShape = RoundedCornerShape(28.dp)

/** 栏位尚未测量时使用的拖动行程。 */
private val ChromeFallbackDragDistance = 88.dp

/** 松手时超过这个速度（按 dp/s 折算）就顺着甩动方向收起或展开。 */
private val ChromeSettleVelocity = 600.dp

private const val BottomCapsuleContent = "bottomCapsuleContent"
