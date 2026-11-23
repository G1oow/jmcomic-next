package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * tab 页列表头部的顶栏。
 *
 * 由 [TabScreen] 提供、页面塞进列表首位：它和漫画列表处在同一条滚动流里，
 * 滑动时跟着一起滚出屏幕，不是浮层，也不占固定高度。
 */
val LocalTabHeader = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabTopBar(title: String) {
    TopAppBar(
        // 背景与系统栏内边距都由外层胶囊提供，栏位自身保持透明。
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {}
    )
}

/**
 * 列表头部的悬浮胶囊顶栏：状态栏内边距与左右留白都在这一项里，跟着列表滚走。
 *
 * @param inset 状态栏高度。
 */
@Composable
fun TabHeader(
    inset: Dp,
) {
    val tabNavController = LocalTabNavController.current
    val backStackEntryState by tabNavController.currentBackStackEntryAsState()
    val title = when (backStackEntryState?.destination?.route) {
        "home" -> "禁漫天堂"
        "collect" -> "我的收藏"
        "user" -> "个人中心"
        else -> null
    } ?: return
    Box(
        modifier = Modifier
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
            shape = ChromeHeaderShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            TabTopBar(title)
        }
    }
}

/** 顶栏胶囊的圆角：比底栏 stadium 收敛一些，容纳标题文字更稳。 */
private val ChromeHeaderShape = RoundedCornerShape(28.dp)
