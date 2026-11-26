package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
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
    // 每个 Tab 各自绑定固定标题的 header，避免切页退场期间因路由变化导致标题消失/重排跳动。
    val homeHeader: @Composable () -> Unit = remember(statusBarInset) {
        { TabHeader(inset = statusBarInset, title = null) }
    }
    val userHeader: @Composable () -> Unit = remember(statusBarInset) {
        { TabHeader(inset = statusBarInset, title = "个人中心") }
    }
    val pageBackground = MaterialTheme.colorScheme.background

    CompositionLocalProvider(
        LocalTabNavController provides tabNavController,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
        ) {
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
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(pageBackground),
                        navController = tabNavController,
                        // M3 Fade Through：旧页先完全淡出，新页再柔和显现，彻底避免双页重叠残影与位移跳动。
                        enterTransition = { motion.tabEnter() },
                        exitTransition = { motion.tabExit() },
                        popEnterTransition = { motion.tabEnter() },
                        popExitTransition = { motion.tabExit() },
                        startDestination = tabName
                    ) {
                        composable("home") {
                            CompositionLocalProvider(LocalTabHeader provides homeHeader) {
                                Box(modifier = Modifier.fillMaxSize().background(pageBackground)) {
                                    HomeScreen()
                                }
                            }
                        }
                        composable("user") {
                            CompositionLocalProvider(LocalTabHeader provides userHeader) {
                                Box(modifier = Modifier.fillMaxSize().background(pageBackground)) {
                                    UserScreen()
                                }
                            }
                        }
                        composable("ai") {
                            // 聊天页自己不管系统栏，内边距放在 destination 内：切页时外层布局不动。
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(pageBackground)
                                    .statusBarsPadding()
                            ) {
                                AiChatScreen()
                            }
                        }
                        composable("collect") {
                            Box(modifier = Modifier.fillMaxSize().background(pageBackground)) {
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
