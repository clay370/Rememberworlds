package com.example.rememberworlds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.rememberworlds.BookModel
import com.example.rememberworlds.MainViewModel
import com.example.rememberworlds.Screen
import com.example.rememberworlds.data.db.WordEntity
import coil.compose.AsyncImage

/**
 * 个人资料屏幕
 * 根据用户登录状态显示不同的视图：
 * - 未登录：显示登录/注册视图
 * - 登录且非复习模式：显示用户信息视图
 * - 登录且复习模式：显示复习列表视图
 *
 * @param viewModel 主视图模型
 * @param navController 导航控制器，用于页面导航
 */
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    navController: androidx.navigation.NavController? = null
) {
    // 从ViewModel收集用户状态
    val currentUser by viewModel.currentUser.collectAsState()
    
    // 从ViewModel收集复习模式状态
    val isReviewing by viewModel.isReviewingMode.collectAsState()

    // 根据用户状态和复习模式显示不同的视图
    if (currentUser == null) {
        // 未登录：显示登录/注册视图
        LoginRegisterView(viewModel)
    } else {
        if (isReviewing) {
            // 登录且复习模式：显示复习列表视图
            ReviewListView(viewModel)
        } else {
            // 登录且非复习模式：显示用户信息视图
            UserProfileView(viewModel, navController)
        }
    }
}

// ================= 1. 登录/注册页 =================

/**
 * 登录/注册视图
 * 用于用户登录和注册的界面
 *
 * @param viewModel 主视图模型
 */
@Composable
fun LoginRegisterView(viewModel: MainViewModel) {
    // 从ViewModel收集加载状态
    val isLoading by viewModel.isLoading.collectAsState()
    
    // 从ViewModel收集状态消息
    val statusMsg by viewModel.statusMsg.collectAsState()
    
    // 注册模式状态
    var isRegisterMode by remember {
        mutableStateOf(false)
    }
    
    // 用户名输入
    var usernameInput by remember {
        mutableStateOf("")
    }
    
    // 密码输入
    var passwordInput by remember {
        mutableStateOf("")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 顶部背景装饰
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(100.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                )
            }

            // 间距
            Spacer(
                modifier = Modifier
                    .height(24.dp)
            )

            // 标题
            Text(
                text = if (isRegisterMode) "创建新账号" else "欢迎回来",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // 副标题
            Text(
                text = "开始你的单词记忆之旅",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            // 间距
            Spacer(
                modifier = Modifier
                    .height(32.dp)
            )

            // 用户名输入框
            OutlinedTextField(
                value = usernameInput,
                onValueChange = {
                    usernameInput = it
                },
                label = {
                    Text("用户名")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )

            // 间距
            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            // 密码输入框
            OutlinedTextField(
                value = passwordInput,
                onValueChange = {
                    passwordInput = it
                },
                label = {
                    Text("密码")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        null
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            // 间距
            Spacer(
                modifier = Modifier
                    .height(24.dp)
            )

            // 状态消息
            if (statusMsg.isNotEmpty()) {
                Text(
                    text = statusMsg,
                    color = if (
                        statusMsg.contains("成功") || 
                        statusMsg.contains("欢迎")
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // 间距
                Spacer(
                    modifier = Modifier
                        .height(16.dp)
                )
            }

            // 加载指示器或登录/注册按钮
            if (isLoading) {
                // 加载中：显示圆形进度指示器
                CircularProgressIndicator()
            } else {
                // 未加载：显示登录/注册按钮
                Button(
                    onClick = {
                        if (isRegisterMode) {
                            // 注册模式：调用注册方法
                            viewModel.register(usernameInput, passwordInput)
                        } else {
                            // 登录模式：调用登录方法
                            viewModel.login(usernameInput, passwordInput)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = usernameInput.isNotBlank() && passwordInput.isNotBlank()
                ) {
                    Text(
                        text = if (isRegisterMode) "立即注册" else "登 录",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 间距
            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            // 切换登录/注册模式按钮
            TextButton(
                onClick = {
                    // 切换模式
                    isRegisterMode = !isRegisterMode
                    // 清空输入
                    usernameInput = ""
                    passwordInput = ""
                }
            ) {
                Text(
                    text = if (isRegisterMode) "已有账号？去登录" else "没有账号？去注册"
                )
            }
        }
    }
}

// ================= 2. 用户信息页 (含设置和统计) =================

/**
 * 用户信息视图
 * 显示用户的个人信息、统计数据、应用设置和单词复习选项
 *
 * @param viewModel 主视图模型
 * @param navController 导航控制器，用于页面导航
 */
@Composable
fun UserProfileView(
    viewModel: MainViewModel,
    navController: androidx.navigation.NavController?
) {
    // 获取详细资料
    val userProfile by viewModel.userProfile.collectAsState()
    
    // 获取当前用户
    val currentUser by viewModel.currentUser.collectAsState()
    
    // 获取书籍列表
    val books by viewModel.bookList.collectAsState()

    // 统计数据
    val learnedCount by viewModel.learnedCount.collectAsState(initial = 0)
    val streakDays by viewModel.streakDays.collectAsState()
    val dailyCount by viewModel.dailyCount.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()

    // 深色模式状态
    val isDark by viewModel.isDarkTheme.collectAsState()

    // 状态监听
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMsg by viewModel.statusMsg.collectAsState()
    val context = LocalContext.current

    // 监听 statusMsg 变化并弹出 Toast
    LaunchedEffect(statusMsg) {
        if (statusMsg.isNotEmpty()) {
            Toast.makeText(
                context,
                statusMsg,
                Toast.LENGTH_SHORT
            ).show()
            
            // 显示完立即清空，防止下次进来再弹
            viewModel.clearStatusMsg()
        }
    }

    // 弹窗控制状态
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    
    var showLogoutDialog by remember {
        mutableStateOf(false)
    }
    
    var showGoalDialog by remember {
        mutableStateOf(false)
    }

    // 折叠状态
    var isSettingsExpanded by remember {
        mutableStateOf(true)
    }
    
    var isReviewExpanded by remember {
        mutableStateOf(true)
    }

    // --- 每日目标设置弹窗 ---
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = {
                showGoalDialog = false
            },
            title = {
                Text("设定每日背诵目标")
            },
            text = {
                Column {
                    // 每日目标选项列表
                    val goalOptions = listOf(10, 20, 50, 100)
                    
                    // 遍历目标选项
                    goalOptions.forEach {
                        goal ->
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // 设置每日目标
                                    viewModel.setDailyGoal(goal)
                                    // 关闭弹窗
                                    showGoalDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 单选按钮
                            RadioButton(
                                selected = (dailyGoal == goal),
                                onClick = null
                            )
                            
                            // 间距
                            Spacer(
                                modifier = Modifier
                                    .width(8.dp)
                            )
                            
                            // 目标文本
                            Text(
                                text = "$goal 个单词"
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 关闭弹窗
                        showGoalDialog = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // --- 退出登录确认弹窗 ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                showLogoutDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.Close,
                    null
                )
            },
            title = {
                Text("退出登录")
            },
            text = {
                Text("确定要退出当前账号吗？\n本地缓存将被清除。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 关闭弹窗
                        showLogoutDialog = false
                        // 调用退出登录方法
                        viewModel.logout()
                    }
                ) {
                    Text("确定退出")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // 关闭弹窗
                        showLogoutDialog = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // --- 注销确认弹窗 ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("注销账户")
            },
            text = {
                Text("确定要注销吗？所有数据将被永久删除且无法恢复。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 关闭弹窗
                        showDeleteDialog = false
                        // 调用注销账号方法
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认注销")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // 关闭弹窗
                        showDeleteDialog = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 使用 Box 包裹整个 LazyColumn，以便在最上层覆盖 Loading 动画
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // --- 头部个人信息 ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 40.dp,
                            bottom = 24.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 头像
                    if (userProfile.avatarUrl.isNotEmpty()) {
                        // 显示网络头像
                        AsyncImage(
                            model = userProfile.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .clickable {
                                    navController?.navigate(Screen.PersonalInfo.route)
                                },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 显示默认字母头像
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(100.dp)
                                .clickable {
                                    navController?.navigate(Screen.PersonalInfo.route)
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.nickname.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // 间距
                    Spacer(
                        modifier = Modifier
                            .height(16.dp)
                    )
                    
                    // 显示用户昵称
                    Text(
                        text = userProfile.nickname,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 显示用户ID
                    Text(
                        text = "ID: ${currentUser?.uid?.take(6)}...",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // --- 数据统计仪表盘 ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 坚持天数统计项
                        StatItem(
                            icon = Icons.Default.Star,
                            value = "$streakDays",
                            label = "坚持天数",
                            tint = Color(0xFFFFD700)
                        )
                        
                        // 今日打卡统计项
                        val progressText = if (dailyCount >= dailyGoal) "✅" else "$dailyCount/$dailyGoal"
                        StatItem(
                            icon = Icons.Default.PlayArrow,
                            value = progressText,
                            label = "今日打卡",
                            tint = Color.White
                        )
                        
                        // 累计已斩统计项
                        StatItem(
                            icon = Icons.Default.Check,
                            value = "$learnedCount",
                            label = "累计已斩",
                            tint = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    }
                }
                
                // 间距
                Spacer(
                    modifier = Modifier
                        .height(24.dp)
                )
            }

            // --- 应用设置卡片 (可折叠) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isSettingsExpanded = !isSettingsExpanded
                        }
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "应用设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = if (isSettingsExpanded) "收起" else "展开",
                        modifier = Modifier
                            .rotate(if (isSettingsExpanded) 90f else 0f),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (isSettingsExpanded) {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column {
                            // 深色模式设置
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    // 间距
                                    Spacer(
                                        modifier = Modifier
                                            .width(16.dp)
                                    )
                                    
                                    Text(
                                        "深色模式",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                
                                Switch(
                                    checked = isDark,
                                    onCheckedChange = {
                                        viewModel.toggleTheme(it)
                                    }
                                )
                            }
                            
                            // 分隔线
                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                            )
                            
                            // 每日目标设置
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showGoalDialog = true
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    // 间距
                                    Spacer(
                                        modifier = Modifier
                                            .width(16.dp)
                                    )
                                    
                                    Text(
                                        "每日目标",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "$dailyGoal 词",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    // 间距
                                    Spacer(
                                        modifier = Modifier
                                            .width(8.dp)
                                    )
                                    
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        null,
                                        modifier = Modifier
                                            .size(16.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            
                            // 分隔线
                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                            )
                            
                            // 版本信息
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    // 间距
                                    Spacer(
                                        modifier = Modifier
                                            .width(16.dp)
                                    )
                                    
                                    Text(
                                        "版本信息",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                
                                Text(
                                    "v1.0.0",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                // 间距
                Spacer(
                    modifier = Modifier
                        .height(24.dp)
                )
            }

            // --- 我的词库 (可折叠标题栏) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isReviewExpanded = !isReviewExpanded
                        }
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "单词复习 (已斩)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = if (isReviewExpanded) "收起" else "展开",
                        modifier = Modifier
                            .rotate(if (isReviewExpanded) 90f else 0f),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // --- 列表内容 (根据状态显示/隐藏) ---
            if (isReviewExpanded) {
                val downloadedBooks = books.filter { it.isDownloaded }
                
                if (downloadedBooks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无下载的词书，去首页看看吧",
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    items(downloadedBooks) {
                        book ->
                        
                        ReviewBookItem(book) {
                            viewModel.openReviewList(book.type)
                        }
                    }
                }
            }

            // --- 底部危险区 ---
            item {
                Spacer(
                    modifier = Modifier
                        .height(48.dp)
                )
                
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                ) {
                    // 退出登录按钮
                    Button(
                        onClick = {
                            showLogoutDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            modifier = Modifier
                                .size(18.dp)
                        )
                        
                        // 间距
                        Spacer(
                            modifier = Modifier
                                .width(8.dp)
                        )
                        
                        Text("退出登录")
                    }
                    
                    // 间距
                    Spacer(
                        modifier = Modifier
                            .height(16.dp)
                    )
                    
                    // 注销账户按钮
                    TextButton(
                        onClick = {
                            showDeleteDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            null,
                            modifier = Modifier
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        // 间距
                        Spacer(
                            modifier = Modifier
                                .width(4.dp)
                        )
                        
                        Text(
                            "注销账户 (永久删除)",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 全屏 Loading 遮罩
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        enabled = false
                    ) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color.White
                    )
                    
                    // 间距
                    Spacer(
                        modifier = Modifier
                            .height(16.dp)
                    )
                    
                    Text(
                        text = "处理中...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 统计项组件
 * 显示单个统计数据项，包含图标、数值和标签
 *
 * @param icon 统计项图标
 * @param value 统计项数值
 * @param label 统计项标签
 * @param tint 图标和文本的颜色
 */
@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            null,
            tint = tint,
            modifier = Modifier
                .size(28.dp)
        )
        
        // 间距
        Spacer(
            modifier = Modifier
                .height(4.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/**
 * 复习书籍项组件
 * 显示一本可复习的书籍，点击后进入复习列表
 *
 * @param book 书籍模型
 * @param onClick 点击事件回调
 */
@Composable
fun ReviewBookItem(
    book: BookModel,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 4.dp
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // 间距
            Spacer(
                modifier = Modifier
                    .width(16.dp)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    book.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "查看列表",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Icon(
                Icons.Default.ArrowForward,
                null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

// ================= 3. 单词复习列表页 =================

/**
 * 复习列表视图
 * 显示用户已学习的单词列表，支持查看和复活单词
 *
 * @param viewModel 主视图模型
 */
@Composable
fun ReviewListView(viewModel: MainViewModel) {
    // 从ViewModel收集已学习的单词
    val learnedWords by viewModel.reviewedWords.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Surface(
            shadowElevation = 2.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        vertical = 12.dp,
                        horizontal = 8.dp
                    )
            ) {
                IconButton(
                    onClick = {
                        viewModel.closeReviewList()
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
                
                // 间距
                Spacer(
                    modifier = Modifier
                        .width(8.dp)
                )
                
                Text(
                    text = "已斩单词 (${learnedWords.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        if (learnedWords.isEmpty()) {
            // 空列表状态
            Box(
                Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.List,
                        null,
                        modifier = Modifier
                            .size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    // 间距
                    Spacer(
                        modifier = Modifier
                            .height(16.dp)
                    )
                    
                    Text(
                        "空空如也，快去斩词吧！",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // 单词列表
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    learnedWords,
                    key = { it.id }
                ) {
                    word ->
                    
                    LearnedWordItem(word, viewModel)
                }
            }
        }
    }
}

/**
 * 已学习单词项组件
 * 显示一个已学习的单词，支持展开查看详细信息和复活单词
 *
 * @param word 单词实体
 * @param viewModel 主视图模型
 */
@Composable
fun LearnedWordItem(
    word: WordEntity,
    viewModel: MainViewModel
) {
    // 展开状态
    var expanded by remember {
        mutableStateOf(false)
    }
    
    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
            }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    word.word,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (expanded) {
                    // 展开时显示中文释义
                    Spacer(
                        modifier = Modifier
                            .height(4.dp)
                    )
                    
                    Text(
                        word.cn,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (expanded) {
                // 展开时显示播放和复活按钮
                IconButton(
                    onClick = {
                        viewModel.playAudio(word.audio, word.word)
                    }
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = {
                        viewModel.unlearnWord(word)
                    }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        "复活",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // 收起时显示添加图标
                Icon(
                    Icons.Default.Add,
                    null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
