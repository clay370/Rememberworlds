package com.example.rememberworlds.ui.main

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rememberworlds.ui.home.HomeScreen
import com.example.rememberworlds.ui.profile.ProfileScreen
import com.example.rememberworlds.ui.quiz.QuizScreen
import com.example.rememberworlds.ui.theme.RememberWorldsTheme

// =================================================================
// ========================= 导航目标定义 ==========================
// =================================================================
/**
 * 导航屏幕密封类
 * 定义应用程序的所有导航目标
 *
 * @param route 路由路径
 * @param title 屏幕标题
 * @param icon 导航图标
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    /** 首页屏幕 - 学习功能 */
    object Home : Screen("home", "学习", Icons.Default.Home)
    /** 测验屏幕 - 测试功能 */
    object Quiz : Screen("quiz", "测试", Icons.Default.Star)
    /** 个人中心屏幕 - 我的功能 */
    object Profile : Screen("profile", "我的", Icons.Default.Person)
    /** 个人资料屏幕 - 个人信息编辑 */
    object PersonalInfo : Screen("personal_info", "个人资料", Icons.Default.Person)
}

// =================================================================
// ========================= Activity 入口 ===========================
// =================================================================
/**
 * 应用程序主入口 Activity
 * 设置内容为 RootApp 组合函数
 */
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
/**
 * 应用程序根视图
 * 管理应用程序的主题、登录状态和主内容显示
 */
@Composable
fun RootApp() {
    // 获取共享的 MainViewModel 实例
    val sharedViewModel: MainViewModel = viewModel()

    // 状态收集
    val currentUser by sharedViewModel.currentUser.collectAsState() // 当前登录用户
    val isOnline by sharedViewModel.isOnline.collectAsState() // 网络状态
    val isDark by sharedViewModel.isDarkTheme.collectAsState() // 深色主题状态

    // 应用主题
    RememberWorldsTheme(
        darkTheme = isDark // 根据状态设置深色主题
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

                // 根据登录状态决定显示内容
                if (currentUser != null) {
                    // 已登录：显示主应用内容
                    MainAppContent(sharedViewModel)
                } else {
                    // 未登录：显示个人中心屏幕（包含登录功能）
                    ProfileScreen(viewModel = sharedViewModel)
                }
            }
        }
    }
}

/**
 * 顶部断网提示条
 * 当网络断开时显示，包含平滑的显示/隐藏动画
 *
 * @param isOnline 当前网络状态
 */
@Composable
fun OfflineWarningBar(isOnline: Boolean) {
    // 动画可见性：仅当网络断开时显示
    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically() + fadeIn(), // 进入动画：垂直展开+淡入
        exit = shrinkVertically() + fadeOut() // 退出动画：垂直收缩+淡出
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error) // 错误色背景
                .padding(4.dp),
            contentAlignment = Alignment.Center // 内容居中
        ) {
            Text(
                "网络已断开，正在使用离线模式",
                color = MaterialTheme.colorScheme.onError, // 错误色文字
                style = MaterialTheme.typography.labelSmall // 小号字体
            )
        }
    }
}

// =================================================================
// ========================== 主应用内容 ============================
// =================================================================
/**
 * 主应用内容
 * 包含底部导航栏和导航主机
 *
 * @param viewModel 共享的 MainViewModel 实例
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    // 创建导航控制器
    val navController = rememberNavController()
    // 底部导航栏项目列表
    val items = listOf(Screen.Home, Screen.Quiz, Screen.Profile)

    // 使用 Scaffold 布局，设置底部导航栏
    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                items = items
            )
        }
    ) { innerPadding ->
        // 导航主机，管理应用程序的所有屏幕导航
        AppNavigationHost(
            navController = navController,
            viewModel = viewModel,
            innerPadding = innerPadding
        )
    }
}

/**
 * 底部导航栏
 * 显示应用程序的主要导航项
 *
 * @param navController 导航控制器
 * @param items 导航项列表
 */
@Composable
fun AppBottomNavigationBar(navController: androidx.navigation.NavHostController, items: List<Screen>) {
    NavigationBar {
        // 获取当前导航回栈条目
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        // 获取当前路由
        val currentRoute = navBackStackEntry?.destination?.route

        // 遍历所有导航项
        items.forEach { screen ->
            // 检查当前项是否被选中
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
                    // 只有未选中时才执行导航
                    if (!isSelected) {
                        navController.navigate(screen.route) {
                            // 弹出到起始目的地，避免导航栈过深
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true // 保存状态
                            }
                            // 避免重复创建相同路由的实例
                            launchSingleTop = true
                            // 恢复先前保存的状态
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * 导航主机
 * 定义应用程序的所有导航路由和动画
 *
 * @param navController 导航控制器
 * @param viewModel 共享的 MainViewModel 实例
 * @param innerPadding 内边距，来自 Scaffold
 */
@Composable
fun AppNavigationHost(
    navController: androidx.navigation.NavHostController,
    viewModel: MainViewModel,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
    // 导航切换动画持续时间（毫秒）
    val defaultDuration = 300

    // 导航主机，管理所有屏幕导航
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route, // 起始目的地为首页
        modifier = Modifier.padding(innerPadding),

        // 进入动画：从右侧滑入+淡入
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth }, // 从屏幕右侧进入
                animationSpec = tween(defaultDuration)
            ) + fadeIn(animationSpec = tween(defaultDuration))
        },

        // 退出动画：向左侧滑出+淡出
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth }, // 向屏幕左侧退出
                animationSpec = tween(defaultDuration)
            ) + fadeOut(animationSpec = tween(defaultDuration))
        },

        // 返回进入动画：从左侧滑入+淡入
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth }, // 从屏幕左侧进入
                animationSpec = tween(defaultDuration)
            ) + fadeIn(animationSpec = tween(defaultDuration))
        },

        // 返回退出动画：向右侧滑出+淡出
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth }, // 向屏幕右侧退出
                animationSpec = tween(defaultDuration)
            ) + fadeOut(animationSpec = tween(defaultDuration))
        }
    ) {
        // 首页路由
        composable(Screen.Home.route) {
            HomeScreen(viewModel)
        }
        // 测验路由
        composable(Screen.Quiz.route) {
            QuizScreen(viewModel)
        }
        // 个人中心路由
        composable(Screen.Profile.route) {
            ProfileScreen(viewModel, navController)
        }
        // 个人资料路由
        composable(Screen.PersonalInfo.route) {
            com.example.rememberworlds.ui.onboarding.PersonalInfoScreen(navController, viewModel)
        }
    }
}