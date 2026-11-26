package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * tab 页列表头部的顶栏。
 *
 * 由 [TabScreen] 为每个 destination 单独提供，标题在 destination 创建时即固定，
 * 切页动画期间不会因为当前路由变化而突然出现或消失，避免页面高度跳变。
 */
val LocalTabHeader = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

/**
 * 页面标题：左上角一行文字，跟着列表滚走。首页没有标题（[title] == null），只占出状态栏内边距。
 *
 * 标题不额外加左右内边距：横向缩进交给页面的内容边距（网格 12dp / 卡片列表 16dp），
 * 标题才能和下面的卡片、以及悬浮胶囊的侧边距对齐。
 *
 * @param inset 状态栏高度。
 * @param title 当前 destination 固定的页面标题；切页退场期间保持不变。
 */
@Composable
fun TabHeader(
    inset: Dp,
    title: String? = null,
) {
    // 首页不放标题：这里只留状态栏内边距加一点呼吸，网格尽量往上。
    Box(
        modifier = Modifier
            .padding(top = inset + 12.dp, bottom = if (title == null) 0.dp else 12.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
