package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.par9uet.jm.ui.screens.AiChatScreen
import com.par9uet.jm.ui.screens.HomeScreen
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.screens.UserCollectComicScreen
import com.par9uet.jm.ui.screens.UserScreen
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.LoginRequiredPlaceholder
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
    // 切页时恢复栏位，避免新页面一进来就是收起状态。
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
    CompositionLocalProvider(
        LocalTabNavController provides tabNavController,
    ) {
        BoxWithConstraints {
            val useNavigationRail = maxWidth >= 700.dp
            val hideBottomNavigation = currentRoute == "ai" && imeVisible
            Scaffold(
                bottomBar = {
                    if (!useNavigationRail && !hideBottomNavigation) {
                        // 上滑收起、下滑展开的底栏。
                        CollapsingChromeSlot(
                            state = chromeState,
                            fromTop = false,
                            minVisible = navigationBarInset,
                        ) {
                            BottomNavigationBarComponent()
                        }
                    }
                },
                topBar = {
                    // 上滑收起、下滑展开的顶栏。
                    CollapsingChromeSlot(
                        state = chromeState,
                        fromTop = true,
                        minVisible = statusBarInset,
                    ) {
                        TopBarComponent()
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .nestedScroll(chromeState.nestedScrollConnection)
                ) {
                    if (useNavigationRail) {
                        NavigationRailComponent()
                    }
                    NavHost(
                        modifier = Modifier.weight(1f),
                        navController = tabNavController,
                        enterTransition = { motion.enter() },
                        exitTransition = { motion.exit() },
                        popEnterTransition = { motion.enter(back = true) },
                        popExitTransition = { motion.exit(back = true) },
                        startDestination = tabName
                    ) {
                        composable("home") {
                            HomeScreen()
                        }
                        composable("user") {
                            UserScreen()
                        }
                        composable("ai") {
                            AiChatScreen()
                        }
                        composable("collect") {
                            if (isLogin) {
                                UserCollectComicScreen(
                                    useScaffold = false,
                                )
                            } else {
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
    }
}

val LocalTabNavController = staticCompositionLocalOf<NavHostController> {
    error("none")
}
