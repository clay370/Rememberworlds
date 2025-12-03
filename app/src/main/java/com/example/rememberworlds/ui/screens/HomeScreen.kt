package com.example.rememberworlds.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rememberworlds.BookModel
import com.example.rememberworlds.MainViewModel
import com.example.rememberworlds.data.db.WordEntity
import com.example.rememberworlds.data.network.SearchResponseItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    // 状态收集
    val isLearning by viewModel.isLearningMode.collectAsState()
    val showSearchDialog by viewModel.showSearchDialog.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()

    // Sheet 状态
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // 搜索结果弹窗
    if (showSearchDialog && searchResult != null) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.closeSearchDialog()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer, // 优化：使用 Container 色
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            WordDetailView(
                wordItem = searchResult!!,
                viewModel = viewModel
            )
        }
    }

    // 根据模式显示不同视图
    if (isLearning) {
        LearningView(viewModel)
    } else {
        BookshelfView(viewModel)
    }
}

@Composable
fun BookshelfView(viewModel: MainViewModel) {
    // 状态收集
    val books by viewModel.bookList.collectAsState()
    val downloadingType by viewModel.downloadingBookType.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    // 局部状态
    var searchText by remember {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current

    val containerModifier = Modifier
        .fillMaxSize()
        .padding(
            horizontal = 16.dp,
            vertical = 16.dp
        )

    Column(
        modifier = containerModifier
    ) {
        // 查词输入框
        val searchColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )

        val searchKeyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        )

        val searchKeyboardActions = KeyboardActions(
            onSearch = {
                viewModel.searchWord(searchText)
                focusManager.clearFocus()
            }
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            placeholder = {
                Text(
                    "查词...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            shape = CircleShape,
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else if (searchText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            viewModel.searchWord(searchText)
                            focusManager.clearFocus()
                        }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "搜索",
                            modifier = Modifier.rotate(180f)
                        )
                    }
                }
            },
            keyboardOptions = searchKeyboardOptions,
            keyboardActions = searchKeyboardActions,
            colors = searchColors
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 标题行
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.List,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "我的书架",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 书籍网格列表
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(books) { book ->
                val isThisBookDownloading = (downloadingType == book.type)
                BookItemCard(
                    book = book,
                    isDownloading = isThisBookDownloading,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookItemCard(book: BookModel, isDownloading: Boolean, viewModel: MainViewModel) {
    val isDark = isSystemInDarkTheme()

    val containerColor = if (book.isDownloaded) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }


    val contentColor = if (book.isDownloaded) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }


    val spineColor = if (book.isDownloaded) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    val cardBorder = if (isDark && !book.isDownloaded) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

    val cardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    val cardShape = RoundedCornerShape(12.dp)
    val cardColors = CardDefaults.cardColors(containerColor = containerColor)


    Card(
        onClick = {
            if (book.isDownloaded) {
                viewModel.startLearning(book.type)
            }
            else if (!isDownloading) {
                viewModel.downloadBook(book)
            }
        },
        elevation = cardElevation,
        shape = cardShape,
        colors = cardColors,
        border = cardBorder, // 关键优化：深色模式下添加微弱边框，防止卡片和背景融为一体
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {

            // 书脊
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(
                        spineColor.copy(alpha = 0.8f)
                    )
            )

            // 内容区域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 顶部：书名和删除按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (book.isDownloaded) {
                        IconButton(
                            onClick = {
                                viewModel.deleteBook(book)
                            },
                            modifier = Modifier
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 中间：背景图标
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = if (book.isDownloaded) Icons.Default.List else Icons.Default.Add,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(60.dp)
                    )
                }

                // 底部：操作按钮
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDownloading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = contentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "下载中...",
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor
                            )
                        }
                    } else if (book.isDownloaded) {
                        val startLearningButtonColors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        val startLearningButtonShape = RoundedCornerShape(8.dp)

                        Button(
                            onClick = {
                                viewModel.startLearning(book.type)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = startLearningButtonColors,
                            shape = startLearningButtonShape
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "开始背诵",
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        val downloadButtonColors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            contentColor = contentColor
                        )
                        val downloadButtonShape = RoundedCornerShape(8.dp)

                        OutlinedButton(
                            onClick = {
                                viewModel.downloadBook(book)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = downloadButtonShape,
                            border = null,
                            colors = downloadButtonColors
                        ) {
                            Icon(
                                Icons.Default.Add,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "点击下载",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LearningView(viewModel: MainViewModel) {
    // 状态收集
    val currentWord by viewModel.currentWord.collectAsState()

    // 局部状态
    var rotationState by remember {
        mutableStateOf(0f)
    }
    var autoPlay by remember {
        mutableStateOf(true)
    }

    // 单词切换时的 Side Effect
    LaunchedEffect(currentWord) {
        rotationState = 0f
        if (currentWord != null && autoPlay) {
            delay(300)
            viewModel.playAudio(
                currentWord!!.audio,
                currentWord!!.word
            )
        }
    }

    // 旋转动画
    val rotation by animateFloatAsState(
        targetValue = rotationState,
        animationSpec = tween(
            durationMillis = 400
        ),
        label = "cardFlip"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部操作栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 返回和标题
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.quitLearning()
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "背单词",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 自动发音开关
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "自动发音",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(8.dp))

                val switchColors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )

                Switch(
                    checked = autoPlay,
                    onCheckedChange = {
                        autoPlay = it
                    },
                    modifier = Modifier.graphicsLayer {
                        scaleX = 0.8f
                        scaleY = 0.8f
                    },
                    colors = switchColors
                )
            }
        }

        if (currentWord == null) {
            // 单词背完或列表为空
            EmptyStateView(viewModel)
        } else {
            val word = currentWord!!

            // 单词卡片区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                val cardShape = RoundedCornerShape(24.dp)
                val cardElevation = CardDefaults.cardElevation(8.dp)
                val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable {
                            rotationState = if (rotationState == 0f) 180f else 0f
                        },
                    elevation = cardElevation,
                    shape = cardShape,
                    colors = cardColors
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (rotation <= 90f) {
                            // 正面内容
                            FrontCardContent(word.word)
                        } else {
                            // 背面内容，需要反向旋转才能看到正向文字
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        rotationY = 180f
                                    }
                                    .fillMaxSize()
                            ) {
                                BackCardContent(word, viewModel)
                            }
                        }
                    }
                }
            }

            // 操作按钮区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 不认识按钮
                val unknownButtonColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
                val buttonShape = RoundedCornerShape(24.dp)

                Button(
                    onClick = {
                        viewModel.markUnknown()
                    },
                    colors = unknownButtonColors,
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Close, null)
                        Text(
                            "不认识",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 认识按钮
                val knownButtonColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )

                Button(
                    onClick = {
                        viewModel.markKnown()
                    },
                    colors = knownButtonColors,
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Check, null)
                        Text(
                            "认识 (斩)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FrontCardContent(wordText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 优化：深色模式下去掉过于明显的渐变，改用纯色背景，显得更干净
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        // 左上角装饰图标
        Icon(
            Icons.Default.Search,
            null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .size(40.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )

        // 单词本身
        Text(
            text = wordText,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        // 提示文字
        Text(
            text = "点击翻看释义",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
fun BackCardContent(word: WordEntity, viewModel: MainViewModel) {
    val cardBackgroundModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)

    Box(
        modifier = cardBackgroundModifier,
        contentAlignment = Alignment.Center
    ) {
        val columnModifier = Modifier
            .fillMaxSize()
            .padding(24.dp)

        Column(
            modifier = columnModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 单词
            Text(
                text = word.word,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 发音按钮
            val iconButtonModifier = Modifier.size(64.dp)
            FilledTonalIconButton(
                onClick = {
                    viewModel.playAudio(word.audio, word.word)
                },
                modifier = iconButtonModifier
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    null,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // 释义卡片
            val meaningCardColors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
            Card(
                colors = meaningCardColors,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = word.cn,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

// 4. 查词详情页
@Composable
fun WordDetailView(wordItem: SearchResponseItem, viewModel: MainViewModel) {
    val columnModifier = Modifier
        .fillMaxWidth()
        .padding(
            horizontal = 24.dp
        )
        .padding(
            bottom = 48.dp
        )

    Column(
        modifier = columnModifier
    ) {
        // 单词和发音按钮行
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 单词
                Text(
                    wordItem.word ?: "Unknown",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // 音标
                if (!wordItem.phonetic.isNullOrEmpty()) {
                    Text(
                        wordItem.phonetic!!,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // 发音按钮
            val audioUrl = wordItem.phonetics?.find { !it.audio.isNullOrEmpty() }?.audio
            if (!audioUrl.isNullOrEmpty()) {
                val iconButtonColors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
                FilledIconButton(
                    onClick = {
                        viewModel.playAudio(audioUrl!!, wordItem.word)
                    },
                    colors = iconButtonColors
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // 释义列表
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val meaningsList = wordItem.meanings ?: emptyList()
            items(meaningsList) { meaning ->
                Column {
                    // 词性
                    SuggestionChip(
                        onClick = { /* Do nothing */ },
                        label = {
                            Text(
                                meaning.partOfSpeech ?: "其他",
                                fontStyle = FontStyle.Italic
                            )
                        },
                        border = null
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // 具体的定义和例句
                    meaning.definitions?.take(3)?.forEachIndexed { index, def ->
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // 定义
                            Text(
                                "${index + 1}. ${def.definition}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // 例句
                            if (!def.example.isNullOrEmpty()) {
                                Text(
                                    "e.g. \"${def.example}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier
                                        .padding(start = 12.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun EmptyStateView(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "🎉",
            fontSize = 80.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "太棒了！",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = {
                viewModel.quitLearning()
            },
            modifier = Modifier
                .width(200.dp)
                .height(50.dp)
        ) {
            Text("返回书架")
        }
    }
}

// 扩展函数
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
)