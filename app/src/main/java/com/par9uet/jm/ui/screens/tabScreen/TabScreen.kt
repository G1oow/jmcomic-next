package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.LoginRequiredPlaceholder
import com.par9uet.jm.ui.screens.AiChatScreen
import com.par9uet.jm.ui.screens.HomeScreen
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.screens.UserCollectComicScreen
import com.par9uet.jm.ui.screens.UserScreen
import com.par9uet.jm.ui.theme.LocalAppMotion
import org.koin.compose.getKoin

@Composable
fun TabScreen(
    tabName: String,
    userManager: UserManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val tabNavController = rememberNavController()
    val motion = LocalAppMotion.current
    val mainNavController = LocalMainNavController.current
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isLogin by userManager.isLoginState.collectAsState(false)
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val density = LocalDensity.current
    val chromeState = rememberTabChromeState()
    val statusBarInset = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val navigationBarInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    // 切页时恢复胶囊，避免新页面一进来就是收起状态。
    LaunchedEffect(currentRoute) { chromeState.show() }
    LaunchedEffect(localSetting.showAiEntry, currentRoute) {
        if (!localSetting.showAiEntry && currentRoute == "ai") {
            tabNavController.navigate("home") {
                popUpTo("ai") {
                    inclusive = true
                }
            }
        }
    }
    // 稳定 header lambda：否则每次 TabScreen 重组都会让三个列表的头部重新组合。
    val tabHeader: @Composable () -> Unit = remember(statusBarInset) {
        { TabHeader(inset = statusBarInset) }
    }
    CompositionLocalProvider(
        LocalTabNavController provides tabNavController,
        LocalTabHeader provides tabHeader,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 700.dp
            val hideBottomCapsule = currentRoute == "ai" && imeVisible
            val showBottomCapsule = !useNavigationRail && !hideBottomCapsule
            // 悬浮层上下文：页面里的分页胶囊靠它跟底栏胶囊共用同一份收起状态与导航栏内边距。
            // 宽屏走侧栏、没有悬浮底栏，页面里的分页控件保持普通样式。
            val floatingChrome = remember(chromeState, navigationBarInset, useNavigationRail) {
                if (useNavigationRail) null else TabFloatingChrome(chromeState, navigationBarInset)
            }
            CompositionLocalProvider(LocalTabFloatingChrome provides floatingChrome) {
                // 底层：页面内容。顶栏由各页列表自己在首位渲染，这里不再有固定栏位。
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(chromeState.nestedScrollConnection)
                ) {
                    if (useNavigationRail) {
                        NavigationRailComponent()
                    }
                    NavHost(
                        modifier = Modifier.weight(1f),
                        navController = tabNavController,
                        enterTransition = {
                            motion.enter(
                                fromLeft = entersFromLeft(
                                    initialState.destination.route,
                                    targetState.destination.route,
                                )
                            )
                        },
                        exitTransition = { motion.exit() },
                        // pop 也按位置判方向：回到左边的首页时是从右边进场，不是固定从左边。
                        popEnterTransition = {
                            motion.enter(
                                fromLeft = entersFromLeft(
                                    initialState.destination.route,
                                    targetState.destination.route,
                                )
                            )
                        },
                        popExitTransition = { motion.exit() },
                        startDestination = tabName
                    ) {
                        composable("home") {
                            HomeScreen()
                        }
                        composable("user") {
                            UserScreen()
                        }
                        composable("ai") {
                            // 聊天页自己不管系统栏，内边距放在 destination 内：切页时外层布局不动。
                            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                                AiChatScreen()
                            }
                        }
                        composable("collect") {
                            if (isLogin) {
                                UserCollectComicScreen(
                                    useScaffold = false,
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                                    // 不在这里跳登录页：登录页返回后本页会重新组合，跳转会被再次触发。
                                    LoginRequiredPlaceholder(
                                        message = "登录后可查看收藏、历史与签到",
                                        onLogin = { mainNavController.navigate("login") },
                                    )
                                }
                            }
                        }
                    }
                }
                // 上层：悬浮胶囊底栏。内容直接铺到屏幕底部，胶囊只是盖在上面。
                if (showBottomCapsule) {
                    BottomNavigationBarComponent(
                        chromeState = chromeState,
                        inset = navigationBarInset,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

val LocalTabNavController = staticCompositionLocalOf<NavHostController> {
    error("none")
}

/** 底栏顺序，只用来判断切页时页面该从哪边滑进来。 */
private val TabRoutes = listOf("home", "ai", "collect", "user")

/**
 * 目标页在来源页右边，说明是从左边那一项点过来的，页面就从左边滑进来；
 * 反过来从右边滑进来。最右的「个人中心」因此只会从左边入场。
 */
internal fun entersFromLeft(initialRoute: String?, targetRoute: String?): Boolean {
    val initial = TabRoutes.indexOf(initialRoute)
    val target = TabRoutes.indexOf(targetRoute)
    return initial >= 0 && target >= 0 && target > initial
}
