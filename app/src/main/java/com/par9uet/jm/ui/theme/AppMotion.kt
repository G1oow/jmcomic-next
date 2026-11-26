package com.par9uet.jm.ui.theme

import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

/** M3 Emphasized Decelerate：用于页面进入与落位。 */
internal val motionEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** M3 Emphasized Accelerate：用于页面快速离场，避免与新页重叠残影。 */
internal val motionExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** M3 Standard：用于控件状态变化（图标着色、页码切换、局部显隐）。 */
internal val motionStandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

data class AppMotionPolicy(
    val enabled: Boolean,
    val enterMs: Int,
    val exitMs: Int,
    val pageTurnMs: Int = if (enabled) 180 else 0,
) {
    /**
     * M3 序贯切换时间拆分：前段（~35%）退出页完全淡出，后段（~65%）进入页再显现。
     * 两页在时间轴上严格错开，杜绝透明叠加导致的「上一页残影」。
     */
    internal val fadeThroughOutMs: Int
        get() = if (!enabled) 0 else (exitMs * 0.6f).roundToInt().coerceAtLeast(50)

    internal val fadeThroughInMs: Int
        get() = if (!enabled) 0 else (enterMs * 0.78f).roundToInt().coerceAtLeast(90)

    /**
     * M3 Shared Axis X（前进 / Push）：
     * 旧页在前段快速向左微退并完全淡出；新页延迟接力从右侧平滑滑入。
     */
    fun pushEnter(): EnterTransition = if (!enabled) EnterTransition.None else {
        val delay = fadeThroughOutMs
        fadeIn(tween(durationMillis = fadeThroughInMs, delayMillis = delay, easing = motionEasing)) +
            slideInHorizontally(
                animationSpec = tween(durationMillis = enterMs, delayMillis = delay / 2, easing = motionEasing)
            ) { width ->
                sharedAxisOffsetPx(width, fraction = 10, maxPx = 96)
            }
    }

    fun pushExit(): ExitTransition = if (!enabled) ExitTransition.None else
        fadeOut(tween(durationMillis = fadeThroughOutMs, easing = motionExitEasing)) +
            slideOutHorizontally(
                animationSpec = tween(durationMillis = fadeThroughOutMs, easing = motionExitEasing)
            ) { width ->
                -sharedAxisOffsetPx(width, fraction = 16, maxPx = 48)
            }

    /**
     * M3 Shared Axis X（返回 / Pop）：
     * 当前页先快速向右滑出并淡出；上一级页再从左侧平滑滑回并显现。
     */
    fun popEnter(): EnterTransition = if (!enabled) EnterTransition.None else {
        val delay = fadeThroughOutMs
        fadeIn(tween(durationMillis = fadeThroughInMs, delayMillis = delay, easing = motionEasing)) +
            slideInHorizontally(
                animationSpec = tween(durationMillis = enterMs, delayMillis = delay / 2, easing = motionEasing)
            ) { width ->
                -sharedAxisOffsetPx(width, fraction = 16, maxPx = 48)
            }
    }

    fun popExit(): ExitTransition = if (!enabled) ExitTransition.None else
        fadeOut(tween(durationMillis = fadeThroughOutMs, easing = motionExitEasing)) +
            slideOutHorizontally(
                animationSpec = tween(durationMillis = fadeThroughOutMs, easing = motionExitEasing)
            ) { width ->
                sharedAxisOffsetPx(width, fraction = 10, maxPx = 96)
            }

    /**
     * M3 Fade Through（顶层底栏 Tab 切换）：
     * 同级页面没有层级先后，采用官方推荐的 Fade Through——旧页先完全淡出，
     * 新页再以轻微缩放（0.97 -> 1.0）淡入。无横向漂移、无双页重叠残影。
     */
    fun tabEnter(): EnterTransition = if (!enabled) EnterTransition.None else {
        val delay = fadeThroughOutMs
        fadeIn(tween(durationMillis = fadeThroughInMs, delayMillis = delay, easing = motionEasing)) +
            scaleIn(
                initialScale = 0.97f,
                animationSpec = tween(durationMillis = fadeThroughInMs, delayMillis = delay, easing = motionEasing),
            )
    }

    fun tabExit(): ExitTransition = if (!enabled) ExitTransition.None else
        fadeOut(tween(durationMillis = fadeThroughOutMs, easing = motionExitEasing))

    /**
     * 沉浸式全屏页面（阅读页）进入/退出：
     * 序贯 Fade Through + 轻微缩放，与阅读内翻页手势正交、不产生重影。
     */
    fun readerEnter(): EnterTransition = if (!enabled) EnterTransition.None else {
        val delay = fadeThroughOutMs
        fadeIn(tween(durationMillis = fadeThroughInMs, delayMillis = delay, easing = motionEasing)) +
            scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(durationMillis = fadeThroughInMs, delayMillis = delay, easing = motionEasing),
            )
    }

    fun readerExit(): ExitTransition = if (!enabled) ExitTransition.None else
        fadeOut(tween(durationMillis = fadeThroughOutMs, easing = motionExitEasing)) +
            scaleOut(
                targetScale = 0.97f,
                animationSpec = tween(durationMillis = fadeThroughOutMs, easing = motionExitEasing),
            )

    /** 页码/分段文字切换：带方向感的轻快上下微位移淡入淡出。 */
    fun pageLabelTransform(forward: Boolean = true): ContentTransform = if (!enabled) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val direction = if (forward) 1 else -1
        (fadeIn(tween(exitMs, easing = motionEasing)) +
            slideInVertically(tween(exitMs, easing = motionEasing)) { height ->
                (height / 3).coerceAtMost(16) * direction
            }) togetherWith
            (fadeOut(tween((exitMs * 0.7f).roundToInt().coerceAtLeast(1), easing = motionExitEasing)) +
                slideOutVertically(tween(exitMs, easing = motionExitEasing)) { height ->
                    -(height / 3).coerceAtMost(16) * direction
                })
    }
}

internal fun sharedAxisOffsetPx(width: Int, fraction: Int, maxPx: Int): Int =
    if (width <= 0) 0 else (width / fraction).coerceIn(1, maxPx)

internal fun appMotionPolicy(api: Int, systemScale: Float): AppMotionPolicy {
    if (systemScale <= 0f) return AppMotionPolicy(enabled = false, enterMs = 0, exitMs = 0, pageTurnMs = 0)
    val clampedScale = systemScale.coerceIn(0.25f, 2f)
    val baseEnter = if (api <= 23) 180 else 240
    val baseExit = if (api <= 23) 110 else 140
    val basePageTurn = if (api <= 23) 120 else 180
    return AppMotionPolicy(
        enabled = true,
        enterMs = (baseEnter * clampedScale).roundToInt().coerceAtLeast(80),
        exitMs = (baseExit * clampedScale).roundToInt().coerceAtLeast(60),
        pageTurnMs = (basePageTurn * clampedScale).roundToInt().coerceAtLeast(60),
    )
}

val LocalAppMotion = staticCompositionLocalOf { AppMotionPolicy(enabled = true, enterMs = 240, exitMs = 140, pageTurnMs = 180) }

/** One observer at the app root; cards never register per-item system observers. */
@Composable
fun rememberAppMotionPolicy(): AppMotionPolicy {
    val resolver = LocalContext.current.contentResolver
    fun readScale() = runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    var scale by remember(resolver) { mutableStateOf(readScale()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { scale = readScale() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return appMotionPolicy(Build.VERSION.SDK_INT, scale)
}

/**
 * 列表翻页/切换分类时的轻量淡入，不做位移以免出现「页面重新定位/抖动」观感。
 *
 * 首次数据就绪（[hasRevealed] == false）时只记录已展示的 key 而不播动画，
 * 避免切回 Tab 或 Paging 重连时整张列表闪一下。
 */
@Composable
fun Modifier.revealComicPage(
    contentKey: Any?,
    ready: Boolean,
): Modifier {
    val motion = LocalAppMotion.current
    val alpha = remember { Animatable(1f) }
    var hasRevealed by remember { mutableStateOf(false) }
    var shownKey by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(contentKey, ready, motion.enabled) {
        when {
            !motion.enabled -> {
                if (ready) {
                    hasRevealed = true
                    shownKey = contentKey
                }
                alpha.snapTo(1f)
            }
            !ready -> Unit
            !hasRevealed -> {
                hasRevealed = true
                shownKey = contentKey
                alpha.snapTo(1f)
            }
            shownKey == contentKey -> Unit
            else -> {
                shownKey = contentKey
                alpha.snapTo(0.35f)
                alpha.animateTo(1f, tween(motion.exitMs, easing = motionEasing))
            }
        }
    }
    return graphicsLayer {
        this.alpha = if (motion.enabled) alpha.value else 1f
    }
}
