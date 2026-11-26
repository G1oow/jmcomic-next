package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
 * 悬浮胶囊底栏的显隐状态。
 *
 * 收起进度跟着手指连续变化：上滑多少就收起多少，下滑多少就展开多少，松手后再按位置和
 * 甩动方向补一段短动画收尾。只有手指拖动（[NestedScrollSource.UserInput]）改变进度，
 * 惯性滑动不参与，避免快速甩动列表时胶囊抖动。
 */
@Stable
class TabChromeState internal constructor(
    private val scope: CoroutineScope,
    private val motion: AppMotionPolicy,
    private val dragDistancePx: Float,
    private val settleVelocityPx: Float,
    /** 分页胶囊在场时多出来的收起行程，拖动距离按同样的量加长，跟手比例才不漂移。 */
    private val pagerBlockPx: Float = 0f,
) {
    /** 0f = 完全展开，1f = 完全收起。 */
    private var progressState by mutableFloatStateOf(0f)

    private var settleJob: Job? = null

    /** 最近一次拖动的方向：1f 收起，-1f 展开，0f 本次手势尚未拖动。 */
    private var dragDirection = 0f

    /** 底栏胶囊上方是否还叠着一层分页胶囊，收起行程要连它一起算。 */
    private var pagerPresentState by mutableStateOf(false)

    /** 绘制阶段读取，进度变化只重绘，不重组。 */
    val progress: Float
        get() = progressState

    /** 底栏胶囊上方是否叠着分页胶囊，同样在绘制阶段读取。 */
    val pagerPresent: Boolean
        get() = pagerPresentState

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // available.y < 0 表示手指上滑、内容向前滚动，此时收起。
            if (source == NestedScrollSource.UserInput) dragBy(-available.y)
            // 只观察，不消费：滚动量全部留给列表，胶囊只做绘制位移。
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

    /** 由页面里的悬浮分页胶囊登记自己在场，好让整叠胶囊一起滑出。 */
    fun setPagerPresent(present: Boolean) {
        if (pagerPresentState != present) pagerPresentState = present
    }

    /** [deltaPx] 为正表示收起方向：手指上滑，内容向前翻。 */
    private fun dragBy(deltaPx: Float) {
        if (deltaPx == 0f) return
        settleJob?.cancel()
        settleJob = null
        dragDirection = if (deltaPx > 0f) 1f else -1f
        val distancePx = dragDistancePx + if (pagerPresentState) pagerBlockPx else 0f
        if (distancePx <= 0f) return
        val next = (progressState + deltaPx / distancePx).coerceIn(0f, 1f)
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
            pagerBlockPx = with(density) { ChromeCapsulePagerBlock.toPx() },
        )
    }
}

/**
 * 悬浮在内容之上的胶囊底栏：尺寸按入口数量自适应并居中，滚动手势下跟手收起。
 *
 * 它不占布局：收起时只用 [graphicsLayer] 平移出屏幕，所以隐藏过程既不会改变页面高度，
 * 也不会牵动内容测量；内容直接铺到屏幕底部，胶囊只是盖在上面。
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
    val density = LocalDensity.current
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
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val borderColor = remember(outlineVariant) {
            outlineVariant.copy(alpha = 0.45f)
        }
        Surface(
            modifier = Modifier
                .widthIn(max = ChromeCapsuleMaxWidth)
                .graphicsLayer {
                    // 行程在绘制阶段算，进度变化不触发重组和重测量。
                    translationY = state.progress * chromeStackTravelPx(density, inset, state.pagerPresent)
                },
            shape = ChromeCapsuleStadium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, borderColor),
        ) {
            content()
        }
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

/** 手指滑过这段距离胶囊正好收起到底。 */
private val ChromeCapsuleDragDistance = 72.dp

/** 松手时超过这个速度（按 dp/s 折算）就顺着甩动方向收起或展开。 */
private val ChromeCapsuleSettleVelocity = 600.dp

/** 底栏胶囊：全高一半的圆角（stadium）。 */
val ChromeCapsuleStadium = RoundedCornerShape(percent = 50)

/**
 * 分页胶囊在收起行程里占的高度：胶囊自身高度加上它与底栏胶囊之间的间距。
 */
val ChromeCapsulePagerBlock = ChromeCapsuleHeight + ChromeCapsuleBottomMargin

/**
 * 收起行程：整叠悬浮胶囊（分页胶囊 + 底栏胶囊）一起滑出屏幕所需的下移距离。
 *
 * 两层胶囊用同一个行程做绘制位移，相对间距全程不变，收起时才不会互相穿插。
 */
fun chromeStackTravelPx(density: Density, inset: Dp, pagerPresent: Boolean): Float =
    with(density) {
        (ChromeCapsuleHeight + inset + ChromeCapsuleBottomMargin * 2 +
            if (pagerPresent) ChromeCapsulePagerBlock else 0.dp).toPx()
    }

/**
 * tab 壳里的悬浮层上下文：底栏胶囊与叠在它上面的分页胶囊共用同一份收起状态与导航栏内边距。
 *
 * 为 null 表示当前页面不在 tab 壳里（没有悬浮底栏），页面上的分页控件就该保持普通样式。
 */
@Immutable
class TabFloatingChrome(
    val state: TabChromeState,
    val navigationBarInset: Dp,
)

val LocalTabFloatingChrome = staticCompositionLocalOf<TabFloatingChrome?> { null }
