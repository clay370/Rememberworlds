   package com.example.rememberworlds.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.example.rememberworlds.data.model.BookModel
import com.example.rememberworlds.ui.main.MainViewModel
import com.example.rememberworlds.ui.main.Question
import com.example.rememberworlds.ui.main.QuizType
import java.util.Locale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

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
private enum class QuizScreenState { Selection, Active, Result }

@Composable
fun QuizScreen(viewModel: MainViewModel) {
    val questions by viewModel.quizQuestions.collectAsState()
    val isFinished by viewModel.isQuizFinished.collectAsState()
    
    val currentState = remember(questions.isEmpty(), isFinished) {
        when {
            questions.isEmpty() -> QuizScreenState.Selection
            isFinished -> QuizScreenState.Result
            else -> QuizScreenState.Active
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                 fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "QuizScreenTransition"
        ) { state ->
            when (state) {
                QuizScreenState.Selection -> QuizSelectionView(viewModel)
                QuizScreenState.Active -> {
                    BackHandler {
                         viewModel.quitQuiz()
                    }
                    ActiveQuizView(viewModel)
                }
                QuizScreenState.Result -> {
                    BackHandler {
                        viewModel.quitQuiz()
                    }
                    QuizResultView(viewModel)
                }
            }
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
 */
@Composable
fun QuizSelectionView(
    viewModel: MainViewModel
) {
    // 收集书籍列表状态
    val books by viewModel.bookList.collectAsState()
    
    // 收集当前测验步骤状态
    val step by viewModel.quizStep.collectAsState()
    
    // 收集选中的书籍类型状态
    val selectedBookType by viewModel.quizSelectedBookType.collectAsState()
    
    // 获取选中书籍的名称
    val selectedBookName = books.find { it.bookId == selectedBookType }?.name ?: ""

    // 下拉菜单展开状态
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 标题栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "选择挑战模式",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 允许切换题库，显示为下拉菜单
        Row(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 32.dp)
                .clickable { isDropdownExpanded = true }, // 点击触发下拉
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "当前题库：",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            
            // 包含下拉菜单的 Box
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedBookName.ifEmpty { "四级词汇 (CET4)" }, // 防止初始空名字
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "切换",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 下拉菜单
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    // 筛选已下载的书籍
                    val downloadedBooks = books.filter { it.isDownloaded }
                    
                    if (downloadedBooks.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("无已下载词书") },
                            onClick = { isDropdownExpanded = false }
                        )
                    } else {
                        downloadedBooks.forEach { book ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = book.name,
                                        fontWeight = if (book.bookId == selectedBookType) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                onClick = {
                                    viewModel.selectQuizBook(book.bookId)
                                    isDropdownExpanded = false
                                },
                                trailingIcon = if (book.bookId == selectedBookType) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // 显示测验模式选择列表
        Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()) // 内部滚动
        ) {
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
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp) // 项间距16dp
            ) {
                // 遍历测验模式列表，为每个模式创建一个选择卡片
                modes.forEach { mode ->
                    QuizModeSelectionCard(mode) {
                        viewModel.startQuiz(mode.modeId)
                    }
                }
            }
            
            // 底部预留空间，防止贴底
            Spacer(modifier = Modifier.height(24.dp))
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
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp), // 圆角24dp
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp), // 固定高度80dp
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if(book.isDownloaded) 4.dp else 0.dp
        )
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
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
             containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp), // 圆角20dp
        modifier = Modifier
            .fillMaxWidth(), // 填充宽度
        elevation = CardDefaults.elevatedCardElevation(2.dp)
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
    if (questions.isEmpty() || currentIndex >= questions.size) return
    val currentQ = questions[currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // 键盘避让
            .padding(24.dp)
    ) {
        // --- 顶部固定区域 ---
        
        // 顶部操作栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(),
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
            
            // 进度计数
            Text(
                text = "${currentIndex + 1}/${questions.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 连击展示动画 (固定高度占位，防止画面跳动)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (comboState.count > 1) {
                // 使用 Box 和 scale 来缩小显示
                Box(modifier = Modifier.scale(0.7f)) {
                     ComboDisplay(
                        combo = comboState.count,
                        multiplier = comboState.multiplier
                    )
                }
            }
        }

        // 倒计时条
        LinearProgressIndicator(
            progress = {
                timeLeft / 15.0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (timeLeft < 5f) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 中间滚动区域 (题目内容) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 题目区域
            if (currentQ.type == QuizType.SPELLING) {
                SpellingQuizView(currentQ, viewModel)
            } else {
                MultipleChoiceView(currentQ, viewModel)
            }
            // 底部留白，防止内容贴底
            Spacer(modifier = Modifier.height(16.dp)) 
        }

        // --- 底部固定区域 (按钮) ---
        // --- 底部固定区域 (按钮) ---
        // 使用 Box 预留空间，防止按钮出现时布局跳动
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (answerState != 0) {
                val buttonText = if (currentIndex < questions.size - 1) "下一题" else "查看结果"
                val buttonColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
                
                Button(
                    onClick = {
                        viewModel.nextQuestion()
                    },
                    modifier = Modifier.fillMaxSize(),
                    colors = buttonColors,
                    shape = CircleShape
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
 * 拼写题视图
 * 显示拼写题界面，包含中文提示、输入框和操作按钮
 * 支持实时输入验证和提示功能
 *
 * @param question 当前题目数据
 * @param viewModel 主视图模型
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellingQuizView(question: Question, viewModel: MainViewModel) {
    // 收集拼写状态
    val state by viewModel.spellingState.collectAsState()
    
    // 收集答案状态
    val answerState by viewModel.answerState.collectAsState()

    // 焦点控制，自动弹出键盘
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        // 延迟一点点以确保UI绘制完成
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }

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
                .height(24.dp)
        )
        
        // 中文提示（需要拼写的单词的中文释义）
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .heightIn(max = 200.dp) // 限制最大高度，防止占据过多屏幕
                .verticalScroll(rememberScrollState()), // 允许长文本滚动
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = question.targetWord.cn,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 24.sp,
                lineHeight = 42.sp, // 增加行高，防止文字重叠
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        
        // 间距
        Spacer(
            modifier = Modifier
                .height(48.dp)
        )

        // 自定义输入框 (填空题样式)
        BasicTextField(
            value = state.input, // 当前输入内容
            onValueChange = { 
                if (answerState == 0) {
                    viewModel.updateSpellingInput(it)
                }
            },
            enabled = answerState == 0,
            textStyle = TextStyle(color = Color.Transparent), // 隐藏原生文本
            cursorBrush = SolidColor(Color.Transparent), // 隐藏光标
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            decorationBox = {
                // 自定义绘制逻辑
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val length = state.correctAnswer.length
                    for (i in 0 until length) {
                        val char = if (i < state.input.length) state.input[i] else null
                        // 状态：0默认，1正确(绿)，2错误(红)
                        val status = if (answerState == 1) 1 else if (state.isError || answerState == 2) 2 else 0
                        // 是否当前输入焦点
                        val isFocused = (i == state.input.length) && answerState == 0
                        
                        SpellingCharSlot(
                            char = char,
                            status = status,
                            isFocused = isFocused
                        )
                    }
                }
            }
        )
        
        // 错误提示文本
        if (state.isError) {
             Spacer(Modifier.height(16.dp))
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
        } else {
             Spacer(Modifier.height(24.dp))
        }

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
 * 单个拼写字符槽位
 *
 * @param char 当前字符
 * @param status 状态：0默认，1正确，2错误
 * @param isFocused 是否获得焦点
 */
@Composable
fun SpellingCharSlot(
    char: Char?, 
    status: Int, 
    isFocused: Boolean
) {
    val color = when (status) {
        1 -> Color(0xFF66BB6A) // 绿色
        2 -> MaterialTheme.colorScheme.error // 红色
        else -> MaterialTheme.colorScheme.primary // 默认色
    }
    
    val underlineColor = if (isFocused) color else color.copy(alpha = 0.5f)
    val underlineHeight = if (isFocused) 4.dp else 2.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(32.dp) // 固定宽度
    ) {
        // 字符
        Text(
            text = char?.toString() ?: "",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(4.dp))
        
        // 下划线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(underlineHeight)
                .background(underlineColor, RoundedCornerShape(2.dp))
        )
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
            // Removed vertical padding to prevent clipping in fixed height box
    ) {
        // 连击数文本
        Text(
            text = "$combo Combo!",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFFFD700), // 金色文本
            fontWeight = FontWeight.Black
        )
        
        // Removed Chinese multiplier text as requested
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        // 题目区域 (自动占据剩余空间)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp) // 限制最大高度
                        .verticalScroll(rememberScrollState()), // 允许滚动
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = question.targetWord.cn,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 42.sp // 确保行间距足够大，防止重叠
                    )
                }
            }
            // 听音选义：显示播放按钮，让用户听音频后选择正确释义
            QuizType.AUDIO_TO_CN -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：播放按钮 (居右对齐)
                    Box(
                        modifier = Modifier.weight(0.4f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                // 播放音频
                                viewModel.playAudio(
                                    question.targetWord.audio,
                                    question.targetWord.word
                                )
                            },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(80.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // 右侧：文本信息 (居左对齐)
                    Box(
                        modifier = Modifier.weight(0.6f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "点击重播",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
            
                            // 显示音标提示
                            val detail = question.targetWord.detailedContent
                            val phone = detail?.usphone ?: detail?.ukphone
                            if (!phone.isNullOrEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "/$phone/",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
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

    // 选项列表 (底部固定)
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
}

/**
 * 测验结果视图
 * 显示测验完成后的结果，包括得分和评语
 * 包含垂直渐变背景和居中布局，增强视觉效果
 */
@Composable
fun QuizResultView(viewModel: MainViewModel) {
    val score by viewModel.quizScore.collectAsState()
    val history by viewModel.quizHistory.collectAsState()

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
        .padding(24.dp)

    Column(
        modifier = containerModifier,
        horizontalAlignment = Alignment.CenterHorizontally // 水平居中
    ) {
        Spacer(
            modifier = Modifier
                .height(24.dp)
        )
        
        // 结果图标
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp), 
            tint = MaterialTheme.colorScheme.primary
        )

        // 分数数值
        Text(
            text = "$score",
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        // 根据得分生成评语
        val comment = if (score >= 80) {
            "太棒了！继续保持！"
        } else {
            "再接再厉，多背背哦"
        }
        
        Text(
            text = comment,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier
                .height(16.dp)
        )
        
        // 分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        
        // 历史记录标题
        Text(
            text = "本次回顾",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // 历史记录列表
        LazyColumn(
            modifier = Modifier
                .weight(1f) // 占据剩余空间
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(history) { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.question.targetWord.word,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.question.targetWord.cn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        
                        Icon(
                            imageVector = if(item.isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if(item.isCorrect) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 完成测试按钮
        Button(
            onClick = {
                viewModel.quitQuiz()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape 
        ) {
            Text(
                text = "完成测试",
                fontSize = 18.sp
            )
        }
    }
}
