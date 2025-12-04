   package com.example.rememberworlds.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import com.example.rememberworlds.ChatMessage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rememberworlds.BookModel
import com.example.rememberworlds.MainViewModel
import com.example.rememberworlds.Question
import com.example.rememberworlds.QuizType
import java.util.Locale

/**
 * 测验模式项数据类
 * 用于展示测验模式选择卡片的信息
 *
 * @property title 模式标题
 * @property desc 模式描述
 * @property icon 模式图标
 * @property modeId 模式ID
 * @property color 模式颜色
 */
data class QuizModeItem(
    /**
     * 模式标题
     */
    val title: String,
    
    /**
     * 模式描述
     */
    val desc: String,
    
    /**
     * 模式图标
     */
    val icon: ImageVector,
    
    /**
     * 模式ID
     */
    val modeId: Int,
    
    /**
     * 模式颜色
     */
    val color: Color
)

/**
 * 测验主屏幕
 * 根据当前测验状态显示不同的视图：
 * - 初始状态：显示测验选择视图
 * - 测验进行中：显示活跃测验视图
 * - 测验结束：显示结果视图
 *
 * @param viewModel 主视图模型
 */
@Composable
fun QuizScreen(viewModel: MainViewModel) {
    val questions by viewModel.quizQuestions.collectAsState()
    val isFinished by viewModel.isQuizFinished.collectAsState()
    
    // [修改] 新增一个状态来控制是否显示 AI 聊天界面
    var showAiChat by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // [修改] 路由逻辑：优先判断是否显示聊天界面
        if (showAiChat) {
            // 显示我们将在第 5 步创建的聊天界面
            AIChatView(viewModel = viewModel) {
                showAiChat = false // 点击返回时关闭聊天
            }
        } else if (questions.isEmpty()) {
            // 初始状态：显示选择视图
            // 我们需要传递一个回调来处理 AI 模式的点击
            QuizSelectionView(viewModel) { isAiMode ->
                if (isAiMode) {
                    viewModel.clearChatHistory() // 进入前清空历史
                    showAiChat = true
                }
            }
        } else if (isFinished) {
            QuizResultView(viewModel)
        } else {
            ActiveQuizView(viewModel)
        }
    }
}

/**
 * 测验选择视图
 * 提供题库选择和测验模式选择的界面
 * 分为两个步骤：
 * 1. 选择题库
 * 2. 选择挑战模式
 *
 * @param viewModel 主视图模型
 * @param onAiModeSelected 点击AI模式时的回调
 */
// [修改] 增加 onAiModeSelected 回调参数
@Composable
fun QuizSelectionView(
    viewModel: MainViewModel,
    onAiModeSelected: (Boolean) -> Unit = {} // 默认为空以兼容旧代码
) {
    // 收集书籍列表状态
    val books by viewModel.bookList.collectAsState()
    
    // 收集当前测验步骤状态
    val step by viewModel.quizStep.collectAsState()
    
    // 收集选中的书籍类型状态
    val selectedBookType by viewModel.quizSelectedBookType.collectAsState()
    
    // 获取选中书籍的名称
    val selectedBookName = books.find { it.type == selectedBookType }?.name ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 根据当前步骤显示不同的标题
        val titleText = if (step == 1) "选择题库" else "选择挑战模式"
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 根据当前步骤显示不同的描述文本
        val descText = if (step == 1) "准备好开始挑战了吗？" else "当前题库：$selectedBookName"
        Text(
            text = descText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 32.dp)
        )

        // 根据当前步骤显示不同的内容
        if (step == 1) {
            // 步骤1：显示题库选择列表
            LazyVerticalGrid(
                columns = GridCells.Fixed(1), // 单列布局
                verticalArrangement = Arrangement.spacedBy(16.dp) // 项间距16dp
            ) {
                // 遍历书籍列表，为每本书创建一个选择卡片
                items(books) { book ->
                    QuizBookSelectionCard(book) {
                        // 只有已下载的书籍才能被选中
                        if (book.isDownloaded) {
                            viewModel.selectQuizBook(book.type)
                        }
                    }
                }
            }
        } else {
            // 步骤2：显示测验模式选择列表
            val modes = listOf(
                // 综合测试模式
                QuizModeItem(
                    title = "综合测试",
                    desc = "全方位考察听说读写",
                    icon = Icons.Default.List,
                    modeId = 0,
                    color = Color(0xFF5C6BC0)
                ),
                // 英转中模式
                QuizModeItem(
                    title = "英 ➡ 中",
                    desc = "快速回忆中文释义",
                    icon = Icons.Default.Create,
                    modeId = 1,
                    color = Color(0xFF42A5F5)
                ),
                // 中转英模式
                QuizModeItem(
                    title = "中 ➡ 英",
                    desc = "逆向思维拼写记忆",
                    icon = Icons.Default.Face,
                    modeId = 2,
                    color = Color(0xFF66BB6A)
                ),
                // 听音选义模式
                QuizModeItem(
                    title = "听音选义",
                    desc = "磨耳朵专项训练",
                    icon = Icons.Default.PlayArrow,
                    modeId = 3,
                    color = Color(0xFFFFA726)
                ),
                // 拼写训练模式
                QuizModeItem(
                    title = "拼写训练",
                    desc = "强化拼写能力",
                    icon = Icons.Default.Star,
                    modeId = 4,
                    color = Color(0xFFEC407A)
                ),
                // [新增] AI 模式
                QuizModeItem(
                    title = "AI 口语对练",
                    desc = "与 AI 英语私教实时对话",
                    icon = Icons.Default.Face,
                    modeId = 99,
                    color = Color(0xFF00BCD4)
                )
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp) // 项间距16dp
            ) {
                // 遍历测验模式列表，为每个模式创建一个选择卡片
                modes.forEach { mode ->
                    QuizModeSelectionCard(mode) {
                        // [修改] 点击处理逻辑
                        if (mode.modeId == 99) {
                            onAiModeSelected(true) // 触发 AI 模式
                        } else {
                            viewModel.startQuiz(mode.modeId)
                        }
                    }
                }
            }
            
            // 占位符，将底部按钮推到底部
            Spacer(
                modifier = Modifier
                    .weight(1f)
            )

            // 切换题库按钮，允许用户返回重新选择题库
            TextButton(
                onClick = {
                    viewModel.backToBookSelection()
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally) // 居中对齐
            ) {
                Text(
                    text = "切换题库",
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * 测验书籍选择卡片
 * 显示单个书籍的选择卡片，根据书籍是否已下载显示不同样式
 *
 * @param book 书籍模型
 * @param onClick 点击事件回调
 */
@Composable
fun QuizBookSelectionCard(book: BookModel, onClick: () -> Unit) {
    // 根据书籍是否已下载设置不同的容器颜色
    val containerColor = if (book.isDownloaded) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    // 根据书籍是否已下载设置不同的内容颜色
    val contentColor = if (book.isDownloaded) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) // 未下载时显示半透明
    }
    
    // 根据书籍是否已下载设置不同的卡片阴影
    val cardElevation = CardDefaults.cardElevation(
        defaultElevation = if(book.isDownloaded) 4.dp else 0.dp
    )
    
    // 卡片组件
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp), // 圆角24dp
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp), // 固定高度80dp
        elevation = cardElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp), // 水平内边距24dp
            verticalAlignment = Alignment.CenterVertically, // 垂直居中
            horizontalArrangement = Arrangement.SpaceBetween // 水平两端对齐
        ) {
            // 书籍信息列
            Column {
                // 书籍名称
                Text(
                    text = book.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                // 未下载提示
                if (!book.isDownloaded) {
                    Text(
                        text = "未下载",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor
                    )
                }
            }
            // 已下载时显示播放箭头图标
            if (book.isDownloaded) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}

/**
 * 测验模式选择卡片
 * 显示单个测验模式的选择卡片，包含图标、标题和描述
 *
 * @param mode 测验模式项
 * @param onClick 点击事件回调
 */
@Composable
fun QuizModeSelectionCard(mode: QuizModeItem, onClick: () -> Unit) {
    // 卡片颜色配置
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    
    // 卡片边框配置
    val cardBorder = androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
    
    // 卡片组件
    Card(
        onClick = onClick,
        colors = cardColors,
        shape = RoundedCornerShape(20.dp), // 圆角20dp
        border = cardBorder, // 边框样式
        modifier = Modifier
            .fillMaxWidth() // 填充宽度
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp), // 内边距20dp
            verticalAlignment = Alignment.CenterVertically // 垂直居中
        ) {
            // 左侧图标区域
            Box(
                modifier = Modifier
                    .size(48.dp) // 48dp大小的正方形
                    .background(
                        mode.color.copy(alpha = 0.1f),
                        CircleShape
                    ), // 圆形背景，使用模式颜色的10%透明度
                contentAlignment = Alignment.Center // 图标居中
            ) {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = null,
                    tint = mode.color // 使用模式颜色
                )
            }
            
            // 图标和文本之间的间距
            Spacer(
                modifier = Modifier
                    .width(16.dp)
            )
            
            // 右侧文本内容
            Column {
                // 模式标题
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // 模式描述
                Text(
                    text = mode.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * 活跃测验视图
 * 显示当前正在进行的测验题目
 * 根据题目类型显示不同的题目视图
 *
 * @param viewModel 主视图模型
 */
@Composable
fun ActiveQuizView(viewModel: MainViewModel) {
    // 状态收集
    val questions by viewModel.quizQuestions.collectAsState() // 测验题目列表
    val currentIndex by viewModel.currentQuizIndex.collectAsState() // 当前题目索引
    val answerState by viewModel.answerState.collectAsState() // 答案状态：0未回答, 1正确, 2错误
    val userSelectedOption by viewModel.userSelectedOption.collectAsState() // 用户选择的选项
    
    // [新增] 状态
    val timeLeft by viewModel.timeLeft.collectAsState() // 剩余时间
    val comboState by viewModel.comboState.collectAsState() // 连击状态

    // 获取当前题目数据
    val currentQ = questions[currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 顶部操作栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 退出按钮
            IconButton(
                onClick = {
                    viewModel.quitQuiz()
                },
                modifier = Modifier
                    .size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "退出",
                    tint = Color.Gray
                )
            }
            
            // 占位符，将进度计数推到右侧
            Spacer(
                modifier = Modifier
                    .weight(1f)
            )
            
            // 进度计数：当前题目/总题目数
            Text(
                text = "${currentIndex + 1}/${questions.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 间距
        Spacer(
            modifier = Modifier
                .height(16.dp)
        )

        // 倒计时条
        LinearProgressIndicator(
            progress = {
                timeLeft / 15.0f
            }, // 15秒倒计时
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (timeLeft < 5f) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }, // 剩余5秒时变红
        )

        // 连击展示动画，只有连击数大于1时显示
        if (comboState.count > 1) {
            ComboDisplay(
                combo = comboState.count,
                multiplier = comboState.multiplier
            )
        }

        // 占位符，将题目区域垂直居中
        Spacer(
            modifier = Modifier
                .weight(1f)
        )

        // 题目区域：根据题目类型切换不同的视图
        if (currentQ.type == QuizType.SPELLING) {
            // 拼写题视图
            SpellingQuizView(currentQ, viewModel)
        } else {
            // 选择题视图（包括英转中、中转英、听音选义）
            MultipleChoiceView(currentQ, viewModel)
        }

        // 占位符，将下一题按钮推到底部
        Spacer(
            modifier = Modifier
                .weight(1f)
        )

        // 下一题按钮/占位符
        if (answerState != 0) { // 已回答
            // 根据是否是最后一题显示不同的按钮文本
            val buttonText = if (currentIndex < questions.size - 1) "下一题" else "查看结果"
            
            // 按钮颜色配置
            val buttonColors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
            
            Button(
                onClick = {
                    viewModel.nextQuestion()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = buttonColors,
                shape = CircleShape
            ) {
                Text(
                    text = buttonText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // 未回答时显示占位符，保持布局稳定
            Spacer(
                modifier = Modifier
                    .height(56.dp)
            )
        }
    }
}

/**
 * 测验选项按钮
 * 显示单个测验选项，根据答案状态显示不同的样式
 * 包含动画效果，当答案状态改变时平滑过渡颜色
 *
 * @param text 选项文本
 * @param answerState 答案状态：0未回答, 1正确, 2错误
 * @param isCorrectAnswer 该选项是否为正确答案
 * @param isSelected 该选项是否被用户选中
 * @param onClick 点击事件回调
 */
@Composable
fun QuizOptionButton(
    text: String,
    answerState: Int,
    isCorrectAnswer: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 动画目标颜色计算
    val targetContainerColor = when {
        answerState != 0 && isCorrectAnswer -> Color(0xFF66BB6A) // 正确答案：绿色
        answerState != 0 && isSelected && !isCorrectAnswer -> MaterialTheme.colorScheme.error // 选中错误答案：红色
        else -> MaterialTheme.colorScheme.surfaceContainerHigh // 默认：浅灰色背景
    }

    // 颜色动画，实现平滑过渡
    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(
            durationMillis = 300
        ),
        label = "btnColor"
    )

    // 内容颜色计算
    val contentColor = if (answerState != 0 && (isCorrectAnswer || isSelected)) {
        Color.White // 已回答时，正确或选中的选项文本为白色
    } else {
        MaterialTheme.colorScheme.onSurface // 未回答或未选中时，文本为默认颜色
    }

    // 按钮颜色配置
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = containerColor, // 禁用状态下保持相同的背景色
        disabledContentColor = contentColor // 禁用状态下保持相同的文本色
    )

    // 按钮启用状态：只有未回答时才能点击
    val buttonEnabled = answerState == 0

    // 按钮组件
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp), // 固定高度60dp
        shape = RoundedCornerShape(16.dp), // 圆角16dp
        colors = buttonColors,
        enabled = buttonEnabled,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp
        ) // 默认阴影2dp
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1, // 最多显示一行
            overflow = TextOverflow.Ellipsis // 超出部分显示省略号
        )
    }
}

/**
 * [新增] AI 聊天界面
 */
@Composable
fun AIChatView(viewModel: MainViewModel, onBack: () -> Unit) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    
    // 语音识别启动器：将语音转换为文字
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            // 获取识别结果
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                // 发送给 AI
                viewModel.sendMessageToAI(spokenText)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 1. 顶部标题栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "退出")
            }
            Text(
                "AI 英语私教",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // 2. 聊天消息列表
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = false
        ) {
            items(chatHistory) { msg ->
                ChatBubble(msg)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (isThinking) {
                item {
                    Text(
                        "AI 正在思考...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                    )
                }
            }
        }

        // 3. 底部录音按钮
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    // 启动系统语音识别
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString()) // 强制识别英语
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something in English...")
                    }
                    try {
                        speechRecognizerLauncher.launch(intent)
                    } catch (e: Exception) {
                        // 如果设备不支持，这里可以弹个Toast
                    }
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Icon(
                    Icons.Default.Face,
                    contentDescription = "按住说话",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            "点击麦克风练习口语",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * [新增] 聊天气泡组件
 */
@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val align = if (isUser) Alignment.End else Alignment.Start
    
    // 气泡颜色
    val bubbleColor = if (isUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
        
    val textColor = if (isUser)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    // 气泡形状：让尖角指向对应的方向
    val bubbleShape = if (isUser)
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    else
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        // 显示角色名
        Text(
            text = if (isUser) "Me" else "AI Teacher",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

/**
 * 拼写题视图
 * 显示拼写题界面，包含中文提示、输入框和操作按钮
 * 支持实时输入验证和提示功能
 *
 * @param question 当前题目数据
 * @param viewModel 主视图模型
 */
@Composable
fun SpellingQuizView(question: Question, viewModel: MainViewModel) {
    // 收集拼写状态
    val state by viewModel.spellingState.collectAsState()
    
    // 收集答案状态
    val answerState by viewModel.answerState.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 题目提示文本
        Text(
            text = "请拼写出中文对应的单词",
            color = Color.Gray
        )
        
        // 间距
        Spacer(
            modifier = Modifier
                .height(16.dp)
        )
        
        // 中文提示（需要拼写的单词的中文释义）
        Text(
            text = question.targetWord.cn,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // 间距
        Spacer(
            modifier = Modifier
                .height(32.dp)
        )

        // 单词输入框
        OutlinedTextField(
            value = state.input, // 当前输入内容
            onValueChange = { 
                if (answerState == 0) {
                    viewModel.updateSpellingInput(it)
                }
            }, // 只有未回答时才能输入
            label = {
                Text("输入单词")
            },
            isError = state.isError, // 是否显示错误状态
            singleLine = true, // 单行输入
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth(),
            enabled = answerState == 0 // 只有未回答时才能输入
        )
        
        // 错误提示文本
        if (state.isError) {
            Text(
                text = "拼写错误，请重试",
                color = MaterialTheme.colorScheme.error
            )
        }
        
        // 显示正确答案（当回答错误时）
        if (answerState == 2) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "正确答案是:",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                )
                Text(
                    text = state.correctAnswer,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 间距
        Spacer(
            modifier = Modifier
                .height(24.dp)
        )

        // 操作按钮行
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 提示按钮
            OutlinedButton(
                onClick = {
                    viewModel.useHint()
                },
                enabled = answerState == 0 // 只有未回答时才能使用提示
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null
                )
                
                // 间距
                Spacer(
                    modifier = Modifier
                        .width(4.dp)
                )
                
                Text(
                    text = "提示"
                )
            }
            
            // 提交按钮
            Button(
                onClick = {
                    viewModel.submitSpelling()
                },
                enabled = answerState == 0 && state.input.isNotEmpty() // 只有未回答且输入不为空时才能提交
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null
                )
                
                // 间距
                Spacer(
                    modifier = Modifier
                        .width(4.dp)
                )
                
                Text(
                    text = "提交"
                )
            }
        }
    }
}

/**
 * 连击特效组件
 * 显示当前连击数和得分倍数
 * 包含无限循环的缩放动画，增强视觉效果
 *
 * @param combo 当前连击数
 * @param multiplier 当前得分倍数
 */
@Composable
fun ComboDisplay(combo: Int, multiplier: Float) {
    // 创建无限过渡动画
    val infiniteTransition = rememberInfiniteTransition()
    
    // 缩放动画：在1.0到1.2之间循环，周期500ms，反向重复
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500
            ),
            repeatMode = RepeatMode.Reverse
        ), 
        label = "combo"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 连击数文本
        Text(
            text = "$combo Combo!",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFFFD700), // 金色文本
            fontWeight = FontWeight.Black
        )
        
        // 得分倍数文本
        Text(
            text = "得分 x${String.format("%.1f", multiplier)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 选择题视图
 * 封装原有的选择题逻辑，根据不同的题目类型显示不同的题干
 * 支持三种选择题类型：英转中、中转英、听音选义
 *
 * @param question 当前题目数据
 * @param viewModel 主视图模型
 */
@Composable
fun MultipleChoiceView(question: Question, viewModel: MainViewModel) {
    // 收集答案状态
    val answerState by viewModel.answerState.collectAsState()
    
    // 收集用户选择的选项
    val userSelectedOption by viewModel.userSelectedOption.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 根据题目类型显示不同的题干
        when (question.type) {
            // 英转中：显示英文单词，让用户选择中文释义
            QuizType.EN_TO_CN -> {
                Text(
                    text = question.targetWord.word,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 50.sp
                )
            }
            // 中转英：显示中文释义，让用户选择英文单词
            QuizType.CN_TO_EN -> {
                Text(
                    text = question.targetWord.cn,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            // 听音选义：显示播放按钮，让用户听音频后选择正确释义
            QuizType.AUDIO_TO_CN -> {
                // 发音按钮
                FilledTonalIconButton(
                    onClick = {
                        // 播放音频
                        viewModel.playAudio(
                            question.targetWord.audio,
                            question.targetWord.word
                        )
                    },
                    modifier = Modifier
                        .size(100.dp) // 大尺寸按钮
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp), // 大尺寸图标
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 间距
                Spacer(
                    modifier = Modifier
                        .height(16.dp)
                )
                
                Text(
                    text = "点击重播",
                    color = Color.Gray
                )
            }
            else -> {
                // 其他类型，空实现
            }
        }
    }
    
    // 间距
    Spacer(
        modifier = Modifier
            .height(24.dp)
    )

    // 选项列表
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        question.options.forEach { option ->
            // 根据题目类型判断选项是否正确
            val isCorrect = if (question.type == QuizType.CN_TO_EN) {
                // 中转英：选项与目标单词的英文匹配
                option == question.targetWord.word
            } else {
                // 英转中或听音选义：选项与目标单词的中文匹配
                option == question.targetWord.cn
            }
            
            // 创建选项按钮
            QuizOptionButton(
                text = option,
                answerState = answerState,
                isCorrectAnswer = isCorrect,
                isSelected = (option == userSelectedOption),
                onClick = {
                    viewModel.answerQuestion(option)
                }
            )
        }
    }
}

/**
 * 测验结果视图
 * 显示测验完成后的结果，包括得分和评语
 * 包含垂直渐变背景和居中布局，增强视觉效果
 *
 * @param viewModel 主视图模型
 */
@Composable
fun QuizResultView(viewModel: MainViewModel) {
    // 收集测验得分状态
    val score by viewModel.quizScore.collectAsState()

    // 创建垂直渐变背景
    val backgroundBrush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), // 顶部浅色
            MaterialTheme.colorScheme.surface // 底部默认背景色
        )
    )
    
    // 容器修饰符
    val containerModifier = Modifier
        .fillMaxSize()
        .background(backgroundBrush) // 应用渐变背景
        .padding(32.dp) // 内边距32dp

    Column(
        modifier = containerModifier,
        verticalArrangement = Arrangement.Center, // 垂直居中
        horizontalAlignment = Alignment.CenterHorizontally // 水平居中
    ) {
        // 结果图标
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp), // 大尺寸图标
            tint = MaterialTheme.colorScheme.primary // 使用主题主色调
        )

        // 间距
        Spacer(
            modifier = Modifier
                .height(32.dp)
        )

        // 分数标题
        Text(
            text = "本次得分",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Gray
        )

        // 分数数值，使用超大字体
        Text(
            text = "$score",
            fontSize = 100.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        // 间距
        Spacer(
            modifier = Modifier
                .height(16.dp)
        )

        // 根据得分生成评语
        val comment = if (score >= 80) {
            "太棒了！继续保持！"
        } else {
            "再接再厉，多背背哦"
        }
        
        Text(
            text = comment,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )

        // 间距
        Spacer(
            modifier = Modifier
                .height(64.dp)
        )

        // 完成测试按钮，返回主界面
        Button(
            onClick = {
                viewModel.quitQuiz()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape // 圆形按钮
        ) {
            Text(
                text = "完成测试",
                fontSize = 18.sp
            )
        }
    }
}
