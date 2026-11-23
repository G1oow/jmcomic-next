package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.theme.LocalAppMotion
import com.par9uet.jm.ui.theme.motionEasing
import kotlin.math.roundToInt

/**
 * 顶栏与底栏的显隐状态。
 *
 * 只有用户手指拖动（[NestedScrollSource.UserInput]）才改变状态，惯性滑动不参与判定，
 * 避免快速甩动列表时栏位来回抖动。上滑越过阈值后收起，下滑越过阈值后展开。
 */
@Stable
class TabChromeState internal constructor(
    private val hideThresholdPx: Float,
    private val showThresholdPx: Float,
) {
    var hidden by mutableStateOf(false)
        private set

    private var draggedForward = 0f
    private var draggedBackward = 0f

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput) onDrag(available.y)
            // 只观察，不消费，滚动行为完全交给列表。
            return Offset.Zero
        }
    }

    /** 切页、回到前台等场景重新展开栏位。 */
    fun show() {
        draggedForward = 0f
        draggedBackward = 0f
        hidden = false
    }

    private fun onDrag(deltaY: Float) {
        if (deltaY < 0f) {
            // 手指上滑：内容向前翻，收起栏位。
            draggedBackward = 0f
            if (hidden) return
            draggedForward += -deltaY
            if (draggedForward >= hideThresholdPx) {
                draggedForward = 0f
                hidden = true
            }
        } else if (deltaY > 0f) {
            // 手指下滑：回头看内容，展开栏位。
            draggedForward = 0f
            if (!hidden) return
            draggedBackward += deltaY
            if (draggedBackward >= showThresholdPx) {
                draggedBackward = 0f
                hidden = false
            }
        }
    }
}

@Composable
fun rememberTabChromeState(): TabChromeState {
    val density = LocalDensity.current
    return remember(density) {
        TabChromeState(
            hideThresholdPx = with(density) { 12.dp.toPx() },
            showThresholdPx = with(density) { 4.dp.toPx() },
        )
    }
}

/** 0f = 完全展开，1f = 完全收起。 */
@Composable
fun TabChromeState.rememberChromeProgress(): Float {
    val motion = LocalAppMotion.current
    val hiddenNow = hidden
    val progress by animateFloatAsState(
        targetValue = if (hiddenNow) 1f else 0f,
        animationSpec = if (!motion.enabled) {
            snap()
        } else {
            tween(
                durationMillis = if (hiddenNow) motion.exitMs else motion.enterMs,
                easing = motionEasing,
            )
        },
        label = "tabChromeProgress",
    )
    return progress
}

/**
 * 栏位容器：栏位高度与内容位移同步收缩，[Scaffold] 的内边距随之变化，
 * 因此内容不会在动画结束后突然跳动。
 *
 * @param fromTop true 表示顶栏（向上收起），false 表示底栏（向下收起）。
 * @param minVisible 收起后仍为系统栏预留的高度，避免内容滑到状态栏/导航栏手势区下面。
 */
@Composable
fun CollapsingChromeSlot(
    progress: Float,
    fromTop: Boolean,
    modifier: Modifier = Modifier,
    minVisible: Dp = 0.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    val minVisiblePx = with(LocalDensity.current) { minVisible.roundToPx() }
    val fraction = progress.coerceIn(0f, 1f)
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
            val shift = (fullHeight * fraction).roundToInt()
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

private const val ChromeSlotContent = "tabChromeSlot"
