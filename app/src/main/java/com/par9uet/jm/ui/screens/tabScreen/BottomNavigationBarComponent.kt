package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.par9uet.jm.R
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.TabChromePager
import org.koin.compose.getKoin

/** 底栏胶囊里的一个入口。 */
@Immutable
private class CapsuleNavItem(
    val route: String,
    val label: String,
    val iconRes: Int? = null,
    val icon: ImageVector? = null,
)

/**
 * 上层悬浮胶囊底栏：图标挤成一组塞进 stadium 胶囊里，选中项用淡强调色 pill 铺满槽位。
 *
 * 只有屏幕够窄时才用胶囊；宽屏走左侧 [NavigationRailComponent]（由 TabScreen 决定）。
 */
@Composable
fun BottomNavigationBarComponent(
    chromeState: TabChromeState,
    inset: Dp,
    pager: TabChromePager? = null,
    modifier: Modifier = Modifier,
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val tabNavController = LocalTabNavController.current
    val backStackEntryState by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntryState?.destination?.route
    val localSetting by localSettingManager.localSettingState.collectAsState()
    if (currentRoute == "login") return

    // 稳定回调：每次重组给底栏塞新 lambda 会让每个入口白白重组一次。
    val onNavigate: (String) -> Unit = remember(tabNavController, currentRoute) {
        { name ->
            if (name != currentRoute) {
                tabNavController.navigate(name) {
                    popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val items = remember(localSetting.showAiEntry) {
        buildList {
            add(CapsuleNavItem("home", "首页", iconRes = R.drawable.home_icon))
            if (localSetting.showAiEntry) {
                add(CapsuleNavItem("ai", "AI", icon = Icons.Rounded.Psychology))
            }
            add(CapsuleNavItem("collect", "收藏", icon = Icons.Filled.Bookmark))
            add(CapsuleNavItem("user", "我的", iconRes = R.drawable.person_icon))
        }
    }

    FloatingBottomCapsule(state = chromeState, inset = inset, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (pager != null) {
                CapsulePagerRow(
                    pager = pager,
                    rowWidth = ChromeCapsuleSlotWidth * items.size + ChromeCapsuleInnerInset * 2,
                )
            }
            CapsuleNavRow(
                items = items,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun CapsuleNavRow(
    items: List<CapsuleNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val colorScheme = MaterialTheme.colorScheme
    // 颜色只算一次：copy() 每次重组都会生成新的 Color，让修饰符白白重建。
    val pillColor = remember(colorScheme.primary) { colorScheme.primary.copy(alpha = 0.11f) }
    // 选中 pill 在槽位之间弹簧滑动：pill 铺满整个槽位，胶囊胖瘦比例由槽宽统一决定。
    val pillOffset by animateDpAsState(
        targetValue = ChromeCapsuleSlotWidth * selectedIndex,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 1_200f),
        label = "capsuleSelectedPill",
    )
    Box(modifier = Modifier.padding(ChromeCapsuleInnerInset)) {
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .size(width = ChromeCapsuleSlotWidth, height = ChromeCapsuleItemHeight)
                .clip(ChromeCapsuleStadium)
                .background(pillColor)
        )
        Row {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                // 图标颜色跟着 pill 一起过渡，避免切换时颜色瞬跳。
                val tint by animateColorAsState(
                    targetValue = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 160),
                    label = "capsuleIconTint",
                )
                Box(
                    modifier = Modifier
                        .size(width = ChromeCapsuleSlotWidth, height = ChromeCapsuleItemHeight)
                        .clip(ChromeCapsuleStadium)
                        .clickable { onNavigate(item.route) }
                        .semantics { contentDescription = item.label },
                    contentAlignment = Alignment.Center,
                ) {
                    val vector = item.icon
                    val res = item.iconRes
                    when {
                        vector != null -> Icon(
                            imageVector = vector,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(CapsuleNavIconSize),
                        )
                        res != null -> Icon(
                            painter = painterResource(res),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(CapsuleNavIconSize),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 胶囊里的分页行：上一页 / 页码 / 下一页。
 *
 * 页面底部的分页条会被悬浮胶囊盖住，所以在 tab 内改由胶囊渲染这一行，
 * 高度压到和导航行同一段，整颗胶囊仍然是一个 stadium。
 */
@Composable
private fun CapsulePagerRow(pager: TabChromePager, rowWidth: Dp) {
    val state = pager.state
    Row(
        modifier = Modifier
            .width(rowWidth)
            .padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { pager.onPageChange(state.page - 1) },
            enabled = !state.isLoading && state.page > 1,
            modifier = Modifier.heightIn(min = 36.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text("上一页", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        Text(
            text = state.pageCount?.let { "${state.page} / $it" } ?: "第 ${state.page} 页",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { pager.onPageChange(state.page + 1) },
            enabled = !state.isLoading && state.hasNext,
            modifier = Modifier.heightIn(min = 36.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text("下一页", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun NavigationRailComponent(
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val tabNavController = LocalTabNavController.current
    val backStackEntryState by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntryState?.destination?.route
    val localSetting by localSettingManager.localSettingState.collectAsState()

    fun navigate(name: String) {
        if (name == currentRoute) return
        tabNavController.navigate(name) {
            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        NavigationRailItem(
            colors = itemColors,
            icon = {
                Icon(
                    painterResource(R.drawable.home_icon),
                    contentDescription = "Home"
                )
            },
            selected = currentRoute == "home",
            onClick = { navigate("home") }
        )
        if (localSetting.showAiEntry) {
            NavigationRailItem(
                colors = itemColors,
                icon = {
                    Icon(
                        Icons.Rounded.Psychology,
                        contentDescription = "AI"
                    )
                },
                selected = currentRoute == "ai",
                onClick = { navigate("ai") }
            )
        }
        NavigationRailItem(
            colors = itemColors,
            icon = {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Collect"
                )
            },
            selected = currentRoute == "collect",
            onClick = { navigate("collect") }
        )
        NavigationRailItem(
            colors = itemColors,
            icon = {
                Icon(
                    painterResource(R.drawable.person_icon),
                    contentDescription = "User"
                )
            },
            selected = currentRoute == "user",
            onClick = { navigate("user") }
        )
    }
}

/** 胶囊内图标尺寸，与 M3 导航图标同轴。 */
private val CapsuleNavIconSize = 24.dp
