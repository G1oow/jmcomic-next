package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabTopBar(title: String) {
    TopAppBar(
        // 背景与系统栏内边距都由外层悬浮胶囊提供，栏位自身保持透明。
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
 * 底层顶栏：常驻页面最上方、和内容同层，不随滚动隐藏。
 *
 * 没有标题的页面（如 AI 对话）不画胶囊，但仍保留状态栏空间，内容不会钻到系统栏下面。
 */
@Composable
fun TopBarComponent(
    inset: Dp,
) {
    val tabNavController = LocalTabNavController.current
    val backStackEntryState by tabNavController.currentBackStackEntryAsState()
    val title = when (backStackEntryState?.destination?.route) {
        "home" -> "禁漫天堂"
        "collect" -> "我的收藏"
        "user" -> "个人中心"
        else -> null
    }
    if (title == null) {
        Spacer(modifier = Modifier.height(inset))
        return
    }
    FloatingTopCapsule(inset = inset) {
        TabTopBar(title)
    }
}
