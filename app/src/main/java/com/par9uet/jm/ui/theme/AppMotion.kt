package com.par9uet.jm.ui.theme

import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
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

internal val motionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

data class AppMotionPolicy(val enabled: Boolean, val enterMs: Int, val exitMs: Int) {
    /**
     * 只让进入页做位移，退出页纯淡出：两个页面同时朝不同方向滑动会让切换看着发飘，
     * 也等于每次切换多画一层位移。位移距离取屏宽的 1/12、封顶 72dp，
     * 只用来交代方向，不喧宾夺主。
     */
    fun enter(back: Boolean = false): EnterTransition = if (!enabled) EnterTransition.None else
        fadeIn(tween(enterMs, easing = motionEasing)) +
            slideInHorizontally(tween(enterMs, easing = motionEasing)) { width ->
                (width / 12).coerceAtMost(72) * if (back) -1 else 1
            }

    fun exit(back: Boolean = false): ExitTransition = if (!enabled) ExitTransition.None else
        fadeOut(tween(exitMs, easing = motionEasing))
}

internal fun appMotionPolicy(api: Int, systemScale: Float): AppMotionPolicy = when {
    systemScale <= 0f -> AppMotionPolicy(false, 0, 0)
    api <= 23 -> AppMotionPolicy(true, 160, 110)
    else -> AppMotionPolicy(true, 220, 140)
}

val LocalAppMotion = staticCompositionLocalOf { AppMotionPolicy(true, 220, 140) }

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

/** Animate one layer when a new page arrives; keep the grid and scroll state mounted. */
@Composable
fun Modifier.revealComicPage(contentKey: Any?, ready: Boolean): Modifier {
    val motion = LocalAppMotion.current
    val alpha = remember(contentKey) { Animatable(if (ready && motion.enabled) 0f else 1f) }
    LaunchedEffect(contentKey, ready, motion.enabled) {
        if (!motion.enabled) alpha.snapTo(1f)
        else if (ready) {
            alpha.snapTo(0f)
            alpha.animateTo(1f, tween(motion.exitMs, easing = motionEasing))
        }
    }
    return graphicsLayer {
        this.alpha = if (motion.enabled) alpha.value else 1f
        translationY = if (motion.enabled) (1f - alpha.value) * 6f * density else 0f
    }
}
