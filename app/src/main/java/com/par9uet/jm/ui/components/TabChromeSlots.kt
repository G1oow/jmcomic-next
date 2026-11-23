package com.par9uet.jm.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.par9uet.jm.ui.pagingSource.ComicPageState

/**
 * tab 壳是否接管页面底部的分页条。
 *
 * tab 内页面底部被悬浮胶囊盖住，分页条只有交给胶囊渲染才不会被遮；
 * 其他路由（搜索、历史、下载等）读到 false，保持各自原样。
 */
val LocalTabChromeActive = staticCompositionLocalOf { false }

/** 交给悬浮胶囊渲染的分页状态。 */
data class TabChromePager(
    val state: ComicPageState,
    val onPageChange: (Int) -> Unit,
)

/**
 * 页面把分页条注册给悬浮胶囊。
 *
 * @param owner 注册方标识：只有当前 owner 才能清空自己注册的内容，
 * 页面切换动画期间新旧页面重叠时不会互相覆盖。
 */
val LocalTabChromePager = staticCompositionLocalOf<(owner: Any, pager: TabChromePager?) -> Unit> {
    { _, _ -> }
}
