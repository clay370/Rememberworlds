package com.example.rememberworlds.ui.quiz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rememberworlds.ui.main.MainViewModel

@Composable
fun QuizBookSelectionStep1(
    viewModel: MainViewModel,
    onBookSelected: (String) -> Unit
) {
    val books by viewModel.bookList.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "选择挑战题库",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(books) { book ->
                QuizBookSelectionCard(
                    book = book,
                    onClick = {
                        viewModel.selectQuizBook(book.bookId)
                        onBookSelected(book.bookId)
                    }
                )
            }
        }
    }
}

/**
 * 测验模式选择视图 (Step 2)
 * 显示所有可用模式供用户选择
 */
@Composable
fun QuizModeSelectionStep2(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val books by viewModel.bookList.collectAsState()
    val selectedBookType by viewModel.quizSelectedBookType.collectAsState()
    val selectedBookName = books.find { it.bookId == selectedBookType }?.name ?: "未知题库"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 标题栏带返回按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            
            Spacer(Modifier.width(8.dp))
            
            Column {
                Text(
                    text = "选择模式",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedBookName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        val modes = listOf(
            QuizModeItem("综合测试", "全方位考察听说读写", Icons.Default.List, 0, Color(0xFF5C6BC0)),
            QuizModeItem("英 ➡ 中", "快速回忆中文释义", Icons.Default.Create, 1, Color(0xFF42A5F5)),
            QuizModeItem("中 ➡ 英", "逆向思维拼写记忆", Icons.Default.Face, 2, Color(0xFF66BB6A)),
            QuizModeItem("听音选义", "磨耳朵专项训练", Icons.Default.PlayArrow, 3, Color(0xFFFFA726)),
            QuizModeItem("拼写训练", "强化拼写能力", Icons.Default.Star, 4, Color(0xFFEC407A)),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            modes.forEach { mode ->
                QuizModeSelectionCard(mode) {
                    viewModel.startQuiz(mode.modeId)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
