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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.rememberworlds.BookModel
import com.example.rememberworlds.MainViewModel
import com.example.rememberworlds.data.db.WordEntity

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isReviewing by viewModel.isReviewingMode.collectAsState()

    if (currentUser == null) {
        LoginRegisterView(viewModel)
    } else {
        if (isReviewing) {
            ReviewListView(viewModel)
        } else {
            UserProfileView(viewModel)
        }
    }
}

// ================= 1. 登录/注册页 =================
@Composable
fun LoginRegisterView(viewModel: MainViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMsg by viewModel.statusMsg.collectAsState()
    var isRegisterMode by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // 顶部背景装饰
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)
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
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(20.dp).fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isRegisterMode) "创建新账号" else "欢迎回来",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "开始你的单词记忆之旅",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 输入框
            OutlinedTextField(
                value = usernameInput, onValueChange = { usernameInput = it },
                label = { Text("用户名") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = passwordInput, onValueChange = { passwordInput = it },
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (statusMsg.isNotEmpty()) {
                Text(
                    text = statusMsg,
                    color = if (statusMsg.contains("成功") || statusMsg.contains("欢迎")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { if (isRegisterMode) viewModel.register(usernameInput, passwordInput) else viewModel.login(usernameInput, passwordInput) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = usernameInput.isNotBlank() && passwordInput.isNotBlank()
                ) {
                    Text(if (isRegisterMode) "立即注册" else "登 录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isRegisterMode = !isRegisterMode; usernameInput = ""; passwordInput = "" }) {
                Text(if (isRegisterMode) "已有账号？去登录" else "没有账号？去注册")
            }
        }
    }
}

// ================= 2. 用户信息页 (含设置和统计) =================
@Composable
fun UserProfileView(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val books by viewModel.bookList.collectAsState()

    // 统计数据
    val learnedCount by viewModel.learnedCount.collectAsState(initial = 0)
    val streakDays by viewModel.streakDays.collectAsState()
    val dailyCount by viewModel.dailyCount.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()

    val isDark by viewModel.isDarkTheme.collectAsState()

    // --- 【新增 1】状态监听 ---
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMsg by viewModel.statusMsg.collectAsState()
    val context = LocalContext.current

    // 监听 statusMsg 变化并弹出 Toast
    LaunchedEffect(statusMsg) {
        if (statusMsg.isNotEmpty()) {
            Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
        }
    }
    // -----------------------

    // 弹窗控制状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    // 折叠状态
    var isSettingsExpanded by remember { mutableStateOf(true) }
    var isReviewExpanded by remember { mutableStateOf(true) }

    // --- 每日目标设置弹窗 ---
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("设定每日背诵目标") },
            text = {
                Column {
                    listOf(10, 20, 50, 100).forEach { goal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDailyGoal(goal); showGoalDialog = false }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (dailyGoal == goal), onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$goal 个单词")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGoalDialog = false }) { Text("取消") } }
        )
    }

    // --- 退出登录确认弹窗 ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Close, null) },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？\n本地缓存将被清除。") },
            confirmButton = { Button(onClick = { showLogoutDialog = false; viewModel.logout() }) { Text("确定退出") } },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("取消") } }
        )
    }

    // --- 注销确认弹窗 ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("注销账户") },
            text = { Text("确定要注销吗？所有数据将被永久删除且无法恢复。") },
            confirmButton = { Button(onClick = { showDeleteDialog = false; viewModel.deleteAccount() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("确认注销") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    // 使用 Box 包裹整个 LazyColumn，以便在最上层覆盖 Loading 动画
    Box(modifier = Modifier.fillMaxSize()) {
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // --- 头部个人信息 ---
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(100.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = currentUser?.email?.firstOrNull()?.toString()?.uppercase() ?: "U", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = currentUser?.email ?: "User", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(text = "ID: ${currentUser?.uid?.take(6)}...", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                }
            }

            // --- 数据统计仪表盘 ---
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatItem(icon = Icons.Default.Star, value = "$streakDays", label = "坚持天数", tint = Color(0xFFFFD700))
                        val progressText = if (dailyCount >= dailyGoal) "✅" else "$dailyCount/$dailyGoal"
                        StatItem(icon = Icons.Default.PlayArrow, value = progressText, label = "今日打卡", tint = Color.White)
                        StatItem(icon = Icons.Default.Check, value = "$learnedCount", label = "累计已斩", tint = MaterialTheme.colorScheme.tertiaryContainer)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- 应用设置卡片 (可折叠) ---
            item {
                Row(modifier = Modifier.fillMaxWidth().clickable { isSettingsExpanded = !isSettingsExpanded }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "应用设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = if (isSettingsExpanded) "收起" else "展开", modifier = Modifier.rotate(if (isSettingsExpanded) 90f else 0f), tint = MaterialTheme.colorScheme.primary)
                }

                if (isSettingsExpanded) {
                    Card(modifier = Modifier.padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column {
                            // 深色模式
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(16.dp)); Text("深色模式", style = MaterialTheme.typography.bodyLarge) }
                                Switch(checked = isDark, onCheckedChange = { viewModel.toggleTheme(it) })
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            // 每日目标
                            Row(modifier = Modifier.fillMaxWidth().clickable { showGoalDialog = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(16.dp)); Text("每日目标", style = MaterialTheme.typography.bodyLarge) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Text("$dailyGoal 词", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline) }
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            // 版本信息
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(16.dp)); Text("版本信息", style = MaterialTheme.typography.bodyLarge) }
                                Text("v1.0.0", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- 我的词库 (可折叠标题栏) ---
            item {
                Row(modifier = Modifier.fillMaxWidth().clickable { isReviewExpanded = !isReviewExpanded }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "单词复习 (已斩)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = if (isReviewExpanded) "收起" else "展开", modifier = Modifier.rotate(if (isReviewExpanded) 90f else 0f), tint = MaterialTheme.colorScheme.primary)
                }
            }

            // --- 列表内容 (根据状态显示/隐藏) ---
            if (isReviewExpanded) {
                val downloadedBooks = books.filter { it.isDownloaded }
                if (downloadedBooks.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("暂无下载的词书，去首页看看吧", color = Color.Gray) } }
                } else {
                    items(downloadedBooks) { book -> ReviewBookItem(book) { viewModel.openReviewList(book.type) } }
                }
            }

            // --- 底部危险区 ---
            item {
                Spacer(modifier = Modifier.height(48.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Button(onClick = { showLogoutDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest, contentColor = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("退出登录")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error); Spacer(modifier = Modifier.width(4.dp)); Text("注销账户 (永久删除)", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // --- 【新增 2】全屏 Loading 遮罩 ---
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)) // 半透明黑色背景
                    .clickable(enabled = false) {}, // 拦截点击事件
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "处理中...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 独立的统计小组件
@Composable
fun StatItem(icon: ImageVector, value: String, label: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
fun ReviewBookItem(book: BookModel, onClick: () -> Unit) {
    Card(onClick = onClick, elevation = CardDefaults.cardElevation(0.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { Text(book.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("查看列表", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun ReviewListView(viewModel: MainViewModel) {
    val learnedWords by viewModel.reviewedWords.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(shadowElevation = 2.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 12.dp, horizontal = 8.dp)) {
                IconButton(onClick = { viewModel.closeReviewList() }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "已斩单词 (${learnedWords.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (learnedWords.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.List, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant); Spacer(modifier = Modifier.height(16.dp)); Text("空空如也，快去斩词吧！", color = MaterialTheme.colorScheme.outline) } } } else { LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(learnedWords, key = { it.id }) { word -> LearnedWordItem(word, viewModel) } } }
    }
}

@Composable
fun LearnedWordItem(word: WordEntity, viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Card(elevation = CardDefaults.cardElevation(0.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(word.word, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (expanded) { Spacer(modifier = Modifier.height(4.dp)); Text(word.cn, color = MaterialTheme.colorScheme.primary) }
            }
            if (expanded) {
                IconButton(onClick = { viewModel.playAudio(word.audio, word.word) }) { Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { viewModel.unlearnWord(word) }) { Icon(Icons.Default.Refresh, "复活", tint = MaterialTheme.colorScheme.error) }
            } else { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.outline) }
        }
    }
}