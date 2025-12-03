package com.example.rememberworlds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp // 引入 dp 单位
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rememberworlds.ui.screens.HomeScreen
import com.example.rememberworlds.ui.screens.ProfileScreen
import com.example.rememberworlds.ui.screens.QuizScreen
import com.example.rememberworlds.ui.theme.RememberWorldsTheme

// =================================================================
// ========================= 导航目标定义 ==========================
// =================================================================
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "学习", Icons.Default.Home)
    object Quiz : Screen("quiz", "测试", Icons.Default.Star)
    object Profile : Screen("profile", "我的", Icons.Default.Person)
}

// =================================================================
// ========================= Activity 入口 ===========================
// =================================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RootApp()
        }
    }
}

// =================================================================
// =========================== 根视图 ===============================
// =================================================================
@Composable
fun RootApp() {
    // 获取共享的 ViewModel
    val sharedViewModel: MainViewModel = viewModel()

    // 状态收集
    val currentUser by sharedViewModel.currentUser.collectAsState()
    val isOnline by sharedViewModel.isOnline.collectAsState()
    val isDark by sharedViewModel.isDarkTheme.collectAsState()

    // 应用主题
    RememberWorldsTheme(
        darkTheme = isDark
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部断网提示条
                OfflineWarningBar(isOnline = isOnline)

                // 根据登录状态决定显示主内容还是登录页
                if (currentUser != null) {
                    MainAppContent(sharedViewModel)
                } else {
                    ProfileScreen(viewModel = sharedViewModel)
                }
            }
        }
    }
}

// 顶部断网提示条 Composable
@Composable
fun OfflineWarningBar(isOnline: Boolean) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "网络已断开，正在使用离线模式",
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// =================================================================
// ========================== 主应用内容 ============================
// =================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Quiz, Screen.Profile)

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                items = items
            )
        }
    ) { innerPadding ->
        AppNavigationHost(
            navController = navController,
            viewModel = viewModel,
            innerPadding = innerPadding
        )
    }
}

// 底部导航栏 Composable
@Composable
fun AppBottomNavigationBar(navController: androidx.navigation.NavHostController, items: List<Screen>) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                icon = {
                    Icon(
                        screen.icon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(screen.title)
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(screen.route) {
                            // 避免在目标路由重复创建实例
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // 避免目标页位于栈顶时，再次创建新的实例
                            launchSingleTop = true
                            // 恢复先前已保存的状态
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

// 导航主机 Composable (已修复动画 API 错误)
@Composable
fun AppNavigationHost(
    navController: androidx.navigation.NavHostController,
    viewModel: MainViewModel,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
    // 导航切换动画定义
    val defaultDuration = 300

    // 将动画逻辑直接放在 NavHost 中，避免类型推断错误
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(innerPadding),

        // --- 修复后的动画定义 ---
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth }, // fullWidth 隐式地作为参数
                animationSpec = tween(defaultDuration)
            ) + fadeIn(animationSpec = tween(defaultDuration))
        },

        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth }, // targetOffsetX 隐式地作为参数
                animationSpec = tween(defaultDuration)
            ) + fadeOut(animationSpec = tween(defaultDuration))
        },

        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth }, // initialOffsetX 隐式地作为参数
                animationSpec = tween(defaultDuration)
            ) + fadeIn(animationSpec = tween(defaultDuration))
        },

        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth }, // targetOffsetX 隐式地作为参数
                animationSpec = tween(defaultDuration)
            ) + fadeOut(animationSpec = tween(defaultDuration))
        }
        // --- 动画定义结束 ---

    ) {
        composable(Screen.Home.route) {
            HomeScreen(viewModel)
        }
        composable(Screen.Quiz.route) {
            QuizScreen(viewModel)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(viewModel)
        }
    }
}