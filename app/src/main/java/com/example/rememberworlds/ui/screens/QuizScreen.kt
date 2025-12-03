package com.example.rememberworlds.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rememberworlds.BookModel
import com.example.rememberworlds.MainViewModel
import com.example.rememberworlds.QuizType

data class QuizModeItem(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val modeId: Int,
    val color: Color
)

@Composable
fun QuizScreen(viewModel: MainViewModel) {
    val questions by viewModel.quizQuestions.collectAsState()
    val isFinished by viewModel.isQuizFinished.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (questions.isEmpty()) {
            QuizSelectionView(viewModel)
        } else if (isFinished) {
            QuizResultView(viewModel)
        } else {
            ActiveQuizView(viewModel)
        }
    }
}

// ... QuizSelectionView (保持展开后的样子) ...
@Composable
fun QuizSelectionView(viewModel: MainViewModel) {
    val books by viewModel.bookList.collectAsState()
    val step by viewModel.quizStep.collectAsState()
    val selectedBookType by viewModel.quizSelectedBookType.collectAsState()
    val selectedBookName = books.find { it.type == selectedBookType }?.name ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val titleText = if (step == 1) "选择题库" else "选择挑战模式"
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        val descText = if (step == 1) "准备好开始挑战了吗？" else "当前题库：$selectedBookName"
        Text(
            text = descText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 32.dp)
        )

        if (step == 1) {
            // 题库选择列表
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(books) { book ->
                    QuizBookSelectionCard(book) {
                        if (book.isDownloaded) {
                            viewModel.selectQuizBook(book.type)
                        }
                    }
                }
            }
        } else {
            // 模式选择列表
            val modes = listOf(
                QuizModeItem("综合测试", "全方位考察听说读写", Icons.Default.List, 0, Color(0xFF5C6BC0)),
                QuizModeItem("英 ➡ 中", "快速回忆中文释义", Icons.Default.Create, 1, Color(0xFF42A5F5)),
                QuizModeItem("中 ➡ 英", "逆向思维拼写记忆", Icons.Default.Face, 2, Color(0xFF66BB6A)),
                QuizModeItem("听音选义", "磨耳朵专项训练", Icons.Default.PlayArrow, 3, Color(0xFFFFA726))
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                modes.forEach { mode ->
                    QuizModeSelectionCard(mode) {
                        viewModel.startQuiz(mode.modeId)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            // 切换题库按钮
            TextButton(
                onClick = {
                    viewModel.backToBookSelection()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "切换题库",
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun QuizBookSelectionCard(book: BookModel, onClick: () -> Unit) {
    val containerColor = if (book.isDownloaded) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (book.isDownloaded) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    val cardElevation = CardDefaults.cardElevation(
        if(book.isDownloaded) 4.dp else 0.dp
    )
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        elevation = cardElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = book.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                if (!book.isDownloaded) {
                    Text(
                        "未下载",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor
                    )
                }
            }
            if (book.isDownloaded) {
                Icon(
                    Icons.Default.PlayArrow,
                    null,
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
fun QuizModeSelectionCard(mode: QuizModeItem, onClick: () -> Unit) {
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    val cardBorder = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant
    )
    Card(
        onClick = onClick,
        colors = cardColors,
        shape = RoundedCornerShape(20.dp),
        border = cardBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(mode.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    mode.icon,
                    null,
                    tint = mode.color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // 文本内容
            Column {
                Text(
                    mode.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    mode.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}


// ================= 核心测试视图 (展开) =================
@Composable
fun ActiveQuizView(viewModel: MainViewModel) {
    // 状态收集
    val questions by viewModel.quizQuestions.collectAsState()
    val currentIndex by viewModel.currentQuizIndex.collectAsState()
    val answerState by viewModel.answerState.collectAsState() // 0: 未回答, 1: 正确, 2: 错误
    val userSelectedOption by viewModel.userSelectedOption.collectAsState()

    // 当前题目数据
    val currentQ = questions[currentIndex]

    // 进度条动画
    val targetProgress = (currentIndex + 1) / questions.size.toFloat()
    val progressAnim by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(500),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 顶部栏：退出按钮和进度计数
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    viewModel.quitQuiz()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    "退出",
                    tint = Color.Gray
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${currentIndex + 1}/${questions.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 进度条
        LinearProgressIndicator(
            progress = {
                progressAnim
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            strokeCap = StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // 题干区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentQ.type) {
                    QuizType.EN_TO_CN -> {
                        Text(
                            text = currentQ.targetWord.word,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 50.sp
                        )
                    }
                    QuizType.CN_TO_EN -> {
                        Text(
                            text = currentQ.targetWord.cn,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    QuizType.AUDIO_TO_CN -> {
                        // 发音按钮
                        FilledTonalIconButton(
                            onClick = {
                                viewModel.playAudio(currentQ.targetWord.audio, currentQ.targetWord.word)
                            },
                            modifier = Modifier.size(100.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "点击重播",
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 选项区域
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            currentQ.options.forEach { option ->
                // 判断当前选项是否是正确答案
                val isCorrectAnswer = when (currentQ.type) {
                    QuizType.CN_TO_EN -> option == currentQ.targetWord.word
                    else -> option == currentQ.targetWord.cn
                }

                QuizOptionButton(
                    text = option,
                    answerState = answerState,
                    isCorrectAnswer = isCorrectAnswer,
                    isSelected = (option == userSelectedOption),
                    onClick = {
                        viewModel.answerQuestion(option)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // 下一题按钮/占位符
        if (answerState != 0) {
            val buttonText = if (currentIndex < questions.size - 1) "下一题" else "查看结果"
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
                    buttonText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}

// ================= 选项按钮 (展开) =================
@Composable
fun QuizOptionButton(text: String, answerState: Int, isCorrectAnswer: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    // 动画颜色计算
    val targetContainerColor = when {
        answerState != 0 && isCorrectAnswer -> Color(0xFF66BB6A) // 绿色
        answerState != 0 && isSelected && !isCorrectAnswer -> MaterialTheme.colorScheme.error // 红色
        else -> MaterialTheme.colorScheme.surfaceContainerHigh // 默认色
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        label = "btnColor"
    )

    // 内容颜色计算
    val contentColor = if (answerState != 0 && (isCorrectAnswer || isSelected)) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    // 按钮颜色配置
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = containerColor,
        disabledContentColor = contentColor
    )

    // 按钮的启用状态
    val buttonEnabled = answerState == 0

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(16.dp),
        colors = buttonColors,
        enabled = buttonEnabled,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ================= 结果视图 (展开) =================
@Composable
fun QuizResultView(viewModel: MainViewModel) {
    // 状态收集
    val score by viewModel.quizScore.collectAsState()

    // 背景和修饰
    val backgroundBrush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surface
        )
    )
    val containerModifier = Modifier
        .fillMaxSize()
        .background(backgroundBrush)
        .padding(32.dp)

    Column(
        modifier = containerModifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 图标
        Icon(
            Icons.Default.Star,
            null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 分数标题
        Text(
            "本次得分",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Gray
        )

        // 分数数值
        Text(
            "$score",
            fontSize = 100.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 评语
        val comment = if (score >= 80) {
            "太棒了！继续保持！"
        } else {
            "再接再厉，多背背哦"
        }
        Text(
            comment,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(64.dp))

        // 完成按钮
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
                "完成测试",
                fontSize = 18.sp
            )
        }
    }
}