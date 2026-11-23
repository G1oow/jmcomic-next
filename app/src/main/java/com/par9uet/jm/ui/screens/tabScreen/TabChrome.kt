package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
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
    private val dragDistancePx: Float,
    private val settleVelocityPx: Float,
) {
    /** 0f = 完全展开，1f = 完全收起。 */
    private var progressState by mutableFloatStateOf(0f)

    private var settleJob: Job? = null

    /** 最近一次拖动的方向：1f 收起，-1f 展开，0f 本次手势尚未拖动。 */
    private var dragDirection = 0f

    /** 0f 完全展开，1f 完全收起。绘制/布局阶段读取，进度变化不触发重组。 */
    val progress: Float
        get() = progressState

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // available.y < 0 表示手指上滑、内容向前滚动，此时收起。
            if (source == NestedScrollSource.UserInput) dragBy(-available.y)
            // 只观察，不消费：滚动量全部留给列表，胶囊只做视觉位移。
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            // 手指离开时按当前位置和甩动方向补齐剩余位移。
            settle(available.y)
            return Velocity.Zero
        }
    }

    /** 切页、回到前台等场景重新展开胶囊。 */
    fun show() = animateTo(0f)

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
            dragDistancePx = with(density) { ChromeCapsuleDragDistance.toPx() },
            settleVelocityPx = with(density) { ChromeCapsuleSettleVelocity.toPx() },
        )
    }
}

/**
 * 底层固定顶栏：常驻页面最上方，和页面内容同层、同属最底层，不随滚动隐藏。
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
                start = ChromeCapsuleSideMargin,
                end = ChromeCapsuleSideMargin,
                top = inset + ChromeCapsuleBottomMargin,
                bottom = ChromeCapsuleBottomMargin,
            )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ChromeTopSurfaceShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
        ) {
            content()
        }
    }
}

/**
 * 上层悬浮胶囊底栏：浮在页面内容之上，尺寸按内容自适应并居中。
 *
 * 它不占布局：收起时只用 [graphicsLayer] 平移出屏幕，所以隐藏过程既不会改变页面高度，
 * 也不会牵动内容的测量——内容底部为它让出的净空由 [bottomChromeClearance] 单独负责。
 *
 * @param inset 导航栏高度，胶囊默认避开系统手势区。
 */
@Composable
fun FloatingBottomCapsule(
    state: TabChromeState,
    inset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ChromeCapsuleSideMargin,
                end = ChromeCapsuleSideMargin,
                bottom = inset + ChromeCapsuleBottomMargin,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = ChromeCapsuleMaxWidth)
                .graphicsLayer {
                    // 只改绘制位置：行程在绘制阶段算，进度变化不触发重组和重测量。
                    translationY = state.progress * (
                        ChromeCapsuleHeight +
                            inset +
                            ChromeCapsuleBottomMargin * 2
                        ).toPx()
                },
            shape = ChromeCapsuleStadium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
        ) {
            content()
        }
    }
}

/**
 * 为悬浮胶囊让出的底部净空：进度在布局阶段读取，收起过程中净空连续收敛到 0，
 * 页面底部跟着填满，不需要重组页面。
 *
 * @param clearancePx 胶囊完全展开时需要的净空（含系统导航栏内边距）。
 */
fun Modifier.bottomChromeClearance(
    state: TabChromeState,
    clearancePx: Int,
): Modifier = layout { measurable, constraints ->
    val keep = (clearancePx * (1f - state.progress)).roundToInt().coerceIn(0, clearancePx)
    val placeable = measurable.measure(
        Constraints(
            minWidth = constraints.minWidth,
            maxWidth = constraints.maxWidth,
            minHeight = (constraints.minHeight - keep).coerceAtLeast(0),
            maxHeight = (constraints.maxHeight - keep).coerceAtLeast(0),
        )
    )
    layout(placeable.width, placeable.height + keep) {
        placeable.placeRelative(0, 0)
    }
}

/** 胶囊内单个入口的高度。 */
val ChromeCapsuleItemHeight = 40.dp

/** 胶囊内容四周留白。 */
val ChromeCapsuleInnerInset = 4.dp

/** 单个入口的槽宽：由高度派生，改高时选中 pill 的胖瘦比例不漂移。 */
val ChromeCapsuleSlotWidth = ChromeCapsuleItemHeight * 1.6f

/** 胶囊高度 = 入口高 + 上下留白。 */
val ChromeCapsuleHeight = ChromeCapsuleItemHeight + ChromeCapsuleInnerInset * 2

/** 胶囊距屏幕左右与底缘的外边距。 */
val ChromeCapsuleSideMargin = 12.dp
val ChromeCapsuleBottomMargin = 8.dp

/** 宽屏下胶囊不铺满，居中悬浮。 */
val ChromeCapsuleMaxWidth = 420.dp

/** 内容为展开态胶囊让出的净空：胶囊高 + 底边距 + 一点呼吸位。 */
val ChromeCapsuleClearance = ChromeCapsuleHeight + ChromeCapsuleBottomMargin + 8.dp

/** 手指滑过这段距离胶囊正好收起到底。 */
private val ChromeCapsuleDragDistance = 72.dp

/** 松手时超过这个速度（按 dp/s 折算）就顺着甩动方向收起或展开。 */
private val ChromeCapsuleSettleVelocity = 600.dp

/** 底栏胶囊：全高一半的圆角（stadium）。 */
val ChromeCapsuleStadium = RoundedCornerShape(percent = 50)

/** 顶栏胶囊的圆角：比 stadium 收敛一些，容纳标题文字更稳。 */
private val ChromeTopSurfaceShape = RoundedCornerShape(28.dp)
