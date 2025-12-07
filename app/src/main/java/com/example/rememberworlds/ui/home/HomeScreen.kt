package com.example.rememberworlds.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.example.rememberworlds.data.model.BookModel
import com.example.rememberworlds.ui.main.MainViewModel
import com.example.rememberworlds.data.db.WordEntity
import com.example.rememberworlds.data.network.SearchResponseItem
import com.example.rememberworlds.data.model.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
/**
 * 应用程序主页
 * 根据学习模式显示不同的视图：学习模式显示LearningView，非学习模式显示BookshelfView
 * 包含搜索结果弹窗
 *
 * @param viewModel 主视图模型
 */
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    // 状态收集 - 从ViewModel中获取应用状态
    val isLearningMode by viewModel.isLearningMode.collectAsState()
    val showSearchDialogState by viewModel.showSearchDialog.collectAsState()
    val searchResultState by viewModel.searchResult.collectAsState()

    // 配置底部弹窗状态
    val modalBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    // 搜索结果弹窗 - 当需要显示且有搜索结果时展示
    if (showSearchDialogState && searchResultState != null) {
        ModalBottomSheet(
            onDismissRequest = {
                // 关闭搜索对话框
                viewModel.closeSearchDialog()
            },
            sheetState = modalBottomSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.large
        ) {
            // 显示单词详情视图
            WordDetailView(
                wordItem = searchResultState!!,
                viewModel = viewModel
            )
        }
    }

    // 根据当前模式显示不同的主视图 (使用动画切换)
    AnimatedContent(
        targetState = isLearningMode,
        transitionSpec = {
            if (targetState) {
                // 进入学习模式：从右向左
                slideInHorizontally { it } + fadeIn() togetherWith 
                    slideOutHorizontally { -it } + fadeOut()
            } else {
                // 返回书架：从左向右
                slideInHorizontally { -it } + fadeIn() togetherWith 
                    slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "HomeScreenTransition"
    ) { learningMode ->
        if (learningMode) {
            // 学习模式
            BackHandler {
                viewModel.quitLearning()
            }
            LearningView(viewModel = viewModel)
        } else {
            // 书架模式
            BookshelfView(viewModel = viewModel)
        }
    }
}

/**
 * 书架视图
 * 显示用户的书籍列表，包含搜索功能和书籍卡片
 *
 * @param viewModel 主视图模型，用于获取书籍列表和处理搜索逻辑
 */
@Composable
fun BookshelfView(viewModel: MainViewModel) {
    // 从ViewModel收集状态
    val booksList by viewModel.bookList.collectAsState() // 书籍列表
    val myBooksList by viewModel.myBooksList.collectAsState() // 我的单词本列表
    val currentDownloadingBookType by viewModel.downloadingBookType.collectAsState() // 当前正在下载的书籍类型
    val isSearchingState by viewModel.isSearching.collectAsState() // 是否正在搜索

    // 局部状态管理
    var searchInputText by remember {
        mutableStateOf("") // 搜索输入文本
    }
    val localFocusManager = LocalFocusManager.current // 焦点管理器，用于清除焦点

    // 容器修饰符 - 定义整体布局样式
    val containerModifier = Modifier
        .fillMaxSize()
        .padding(
            horizontal = 16.dp,
            vertical = 16.dp
        )

    Column(
        modifier = containerModifier
    ) {
        // 搜索输入框配置
        val searchTextFieldColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )

        val searchKeyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
        )

        val searchKeyboardActions = KeyboardActions(
            onSearch = {
                // 执行搜索操作
                viewModel.searchWord(searchInputText)
                // 清除焦点
                localFocusManager.clearFocus()
            }
        )

        // 搜索输入框组件
        OutlinedTextField(
            value = searchInputText,
            onValueChange = {
                searchInputText = it
            },
            placeholder = {
                Text(
                    text = "查词...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            shape = CircleShape,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索图标",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (isSearchingState) {
                    // 搜索中显示加载指示器
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (searchInputText.isNotEmpty()) {
                    // 搜索完成且有输入时显示搜索按钮
                    IconButton(
                        onClick = {
                            viewModel.searchWord(searchInputText)
                            localFocusManager.clearFocus()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "搜索",
                            modifier = Modifier.rotate(180f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            keyboardOptions = searchKeyboardOptions,
            keyboardActions = searchKeyboardActions,
            colors = searchTextFieldColors
        )

        // 间距
        Spacer(modifier = Modifier.height(16.dp))

        // 书籍网格列表
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // --- 我的单词本 Section ---
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "我的单词本",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "我的单词本",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(myBooksList) { book ->
                 BookItemCard(
                    book = book,
                    isDownloading = false, // 本地生成，无需下载
                    viewModel = viewModel
                )
            }

            // --- 官方词书 Section ---
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "官方词书",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "官方词书",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(booksList) { book ->
                // 检查当前书籍是否正在下载
                val isBookDownloading = (currentDownloadingBookType == book.bookId)
                // 渲染单个书籍卡片
                BookItemCard(
                    book = book,
                    isDownloading = isBookDownloading,
                    viewModel = viewModel
                )
            }
        }
    }
}


/**
 * 书籍卡片
 * 显示单本书籍的信息，包括名称、下载状态和操作按钮
 *
 * @param book 书籍模型，包含书籍的基本信息
 * @param isDownloading 是否正在下载当前书籍
 * @param viewModel 主视图模型，用于处理书籍相关操作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookItemCard(book: BookModel, isDownloading: Boolean, viewModel: MainViewModel) {
    // 检查当前是否为深色主题
    val isDarkTheme = isSystemInDarkTheme()

    // 根据书籍下载状态设置容器颜色
    val cardContainerColor = if (book.isDownloaded) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    // 根据书籍下载状态设置内容颜色
    val cardContentColor = if (book.isDownloaded) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // 根据书籍下载状态设置书脊颜色
    val cardSpineColor = if (book.isDownloaded) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    // 根据主题和下载状态设置卡片边框
    val cardBorder = if (isDarkTheme && !book.isDownloaded) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

    // 卡片样式配置
    val cardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    val cardShape = RoundedCornerShape(12.dp)
    val cardColors = CardDefaults.cardColors(containerColor = cardContainerColor)

    // 卡片组件
    ElevatedCard(
        onClick = {
            // 根据书籍状态执行不同操作
            if (book.isDownloaded) {
                // 已下载：开始学习
                viewModel.startLearning(book.bookId)
            }
            else if (!isDownloading) {
                // 未下载且未在下载中：开始下载
                viewModel.downloadBook(book.bookId)
            }
        },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = cardContainerColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 书脊 - 左侧彩色条
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(
                        cardSpineColor.copy(alpha = 0.8f)
                    )
            )

            // 内容区域 - 右侧主要内容
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
                    // 书名
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardContentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // 删除按钮 - 仅已下载书籍显示
                    if (book.isDownloaded) {
                        IconButton(
                            onClick = {
                                // 执行删除书籍操作
                                viewModel.deleteBook(book.bookId)
                            },
                            modifier = Modifier
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除书籍",
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
                    // 根据下载状态显示不同背景图标
                    Icon(
                        imageVector = if (book.isDownloaded) Icons.Default.List else Icons.Default.Add,
                        contentDescription = "背景图标",
                        tint = cardContentColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(60.dp)
                    )
                }

                // 底部：操作按钮
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when {
                        // 正在下载状态
                        isDownloading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = cardContentColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "下载中...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cardContentColor
                                )
                            }
                        }
                        // 已下载状态
                        book.isDownloaded -> {
                            // 开始学习按钮配置
                            val startLearningButtonColors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                            val startLearningButtonShape = RoundedCornerShape(8.dp)

                            // 开始学习按钮
                            Button(
                                onClick = {
                                    viewModel.startLearning(book.bookId)
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
                                    contentDescription = "开始学习",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "开始背诵",
                                    fontSize = 13.sp
                                )
                            }
                        }
                        // 未下载状态
                        else -> {
                            // 下载按钮配置
                            val downloadButtonColors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                contentColor = cardContentColor
                            )
                            val downloadButtonShape = RoundedCornerShape(8.dp)

                            // 下载按钮
                            OutlinedButton(
                                onClick = {
                                    viewModel.downloadBook(book.bookId)
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
                                    contentDescription = "下载书籍",
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
}

/**
 * 学习视图
 * 显示单词卡片，支持翻转查看释义，包含操作按钮
 *
 * @param viewModel 主视图模型，用于获取单词数据和处理学习操作
 */
@Composable
fun LearningView(viewModel: MainViewModel) {
    // 状态收集
    val currentWord by viewModel.currentWord.collectAsState() // 当前要学习的单词
    val isLoading by viewModel.isLoading.collectAsState() // 加载状态
    val currentBookProgress by viewModel.currentBookProgress.collectAsState() // 学习进度

    // 局部状态管理
    var rotationState by remember {
        mutableStateOf(0f) // 卡片旋转角度，0f为正面，180f为背面
    }
    var autoPlayAudio by remember {
        mutableStateOf(true) // 是否自动播放发音
    }

    // 单词切换时的副作用
    LaunchedEffect(currentWord) {
        // 重置卡片旋转状态
        rotationState = 0f
        // 如果有单词且开启了自动发音，则延迟播放音频
        if (currentWord != null && autoPlayAudio) {
            delay(300) // 延迟300ms播放，给用户准备时间
            viewModel.playAudio(
                currentWord!!.audio,
                currentWord!!.word
            )
        }
    }

    // 旋转动画配置
    val rotation by animateFloatAsState(
        targetValue = rotationState,
        animationSpec = tween(
            durationMillis = 400, // 动画持续时间400ms
            easing = androidx.compose.animation.core.LinearOutSlowInEasing
        ),
        label = "cardFlip"
    )

    // 主布局
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
            // 左侧：返回按钮和标题
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        // 退出学习模式，返回书架
                        viewModel.quitLearning()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回书架",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "背单词",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 右侧：自动发音开关
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 收藏按钮
                val isFavorite = currentWord?.isFavorite == true
                IconButton(
                    onClick = { viewModel.toggleFavorite() }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "自动发音",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(8.dp))

                // 开关颜色配置
                val switchColors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )

                // 自动发音开关组件
                Switch(
                    checked = autoPlayAudio,
                    onCheckedChange = {
                        autoPlayAudio = it
                    },
                    modifier = Modifier.graphicsLayer {
                        // 缩小开关尺寸
                        scaleX = 0.8f
                        scaleY = 0.8f
                    },
                    colors = switchColors
                )
            }
        }

        // --- 学习进度条 ---
        val (learned, total) = currentBookProgress
        val progress = if (total > 0) learned.toFloat() / total else 0f
        
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
           Row(
               modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
               horizontalArrangement = Arrangement.SpaceBetween
           ) {
               Text(
                   text = "学习进度", 
                   style = MaterialTheme.typography.labelMedium,
                   color = MaterialTheme.colorScheme.outline
               )
               Text(
                   text = "$learned / $total", 
                   style = MaterialTheme.typography.labelMedium, 
                   fontWeight = FontWeight.Bold,
                   color = MaterialTheme.colorScheme.primary
               )
           }
           LinearProgressIndicator(
               progress = { progress },
               modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
               color = MaterialTheme.colorScheme.primary,
               trackColor = MaterialTheme.colorScheme.surfaceVariant,
           )
        }

        // 内容区域：根据状态显示不同内容
        if (isLoading) {
            // 加载中
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (currentWord == null) {
            // 单词背完或列表为空
            EmptyStateView(viewModel)
        } else {
            val word = currentWord!!

            // 单词卡片区域
            Box(
                modifier = Modifier
                    .weight(1f) // 占据剩余空间
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center // 居中对齐
            ) {
                // 卡片样式配置
                val cardShape = RoundedCornerShape(24.dp)
                val cardElevation = CardDefaults.cardElevation(8.dp)
                val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

                // 可翻转的卡片组件
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = rotation // 应用旋转动画
                            cameraDistance = 12f * density // 设置3D效果
                        }
                        .clickable {
                            // 点击翻转卡片
                            rotationState = if (rotationState == 0f) 180f else 0f
                        },
                    elevation = CardDefaults.elevatedCardElevation(8.dp),
                    shape = cardShape,
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (rotation <= 90f) {
                            // 正面内容：显示单词
                            FrontCardContent(word.word)
                        } else {
                            // 背面内容：显示释义，需要反向旋转才能看到正向文字
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
                // 左侧：不认识按钮
                val unknownButtonColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
                val buttonShape = RoundedCornerShape(24.dp)

                Button(
                    onClick = {
                        // 标记为不认识
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
                        Icon(Icons.Default.Close, contentDescription = "不认识")
                        Text(
                            text = "不认识",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 右侧：认识按钮
                val knownButtonColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )

                Button(
                    onClick = {
                        // 标记为认识（斩）
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
                        Icon(Icons.Default.Check, contentDescription = "认识")
                        Text(
                            text = "认识 (斩)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单词卡片正面内容
 * 显示单词和提示文字
 *
 * @param wordText 单词文本
 */
@Composable
fun FrontCardContent(wordText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        // 左上角装饰图标
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "装饰图标",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .size(40.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )

        // 单词本身
        Text(
            text = wordText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 48.sp,
                letterSpacing = 1.sp
            ),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // 提示文字
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "点击查看详情",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Icon(
                imageVector = Icons.Default.Create, // Using a generic icon as indicator, or could be a hand gesture
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp).size(20.dp)
            )
        }
    }
}

@Composable
fun BackCardContent(word: WordEntity, viewModel: MainViewModel) {
    val details = word.detailedContent
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start
    ) {
        // --- 头部区域 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = word.word,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 播放按钮 (主要)
            IconButton(
                onClick = { viewModel.playAudio(word.audio, word.word) },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 音标区域 ---
        if (details != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!details.ukphone.isNullOrEmpty()) {
                    PhoneticChip(text = "英 /${details.ukphone}/", onClick = { /* Play UK if available */ })
                }
                if (!details.usphone.isNullOrEmpty()) {
                    PhoneticChip(text = "美 /${details.usphone}/", onClick = { /* Play US if available */ })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // --- 释义区域 (核心) ---
        if (details?.translations != null && details.translations.isNotEmpty()) {
            details.translations.forEach { trans ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 词性标签
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = trans.pos,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // 中文释义
                    Text(
                        text = trans.tranCn,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                }
            }
        } else {
            // Fallback for simple data
            Text(
                text = word.cn,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // --- 富文本板块 ---
        if (details != null) {
            // 1. 例句
            if (details.sentence?.sentences?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(24.dp))
                WordSectionHeader("例句", Icons.Default.Info)
                details.sentence.sentences.take(3).forEach { sent ->
                    SentenceItem(sent)
                }
            }

            // 2. 短语
            if (details.phrase?.phrases?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(24.dp))
                WordSectionHeader("常用短语", Icons.Default.List)
                details.phrase.phrases.take(3).forEach { phrase ->
                    PhraseItem(phrase)
                }
            }

            // 3. 同义词/辨析
            if (details.syno?.synos?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(24.dp))
                WordSectionHeader("同义词", Icons.Default.Star)
                details.syno.synos.take(2).forEach { syn ->
                    SynonymItem(syn)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom actions
    }
}

// --- 辅助组件 ---

@Composable
fun WordSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun PhoneticChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SentenceItem(sentence: SentencePair) {
    Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
        Text(
            text = sentence.sContent,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = sentence.sCn,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun PhraseItem(phrase: PhraseItem) {
    Column(
        modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()
    ) {
        Text(
            text = phrase.pContent,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = phrase.pCn,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun SynonymItem(syn: SynonymGroup) {
    Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${syn.pos}. ${syn.tran}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = syn.hwds.joinToString(", ") { it.w },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


/**
 * 单词详情视图
 * 显示搜索结果的详细信息，包括单词、音标、发音和释义
 *
 * @param wordItem 搜索响应项，包含单词的详细信息
 * @param viewModel 主视图模型，用于播放音频
 */
@Composable
fun WordDetailView(wordItem: SearchResponseItem, viewModel: MainViewModel) {
    // 列布局修饰符
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
            // 左侧：单词和音标
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 单词
                Text(
                    text = wordItem.word ?: "Unknown",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // 音标
                if (!wordItem.phonetic.isNullOrEmpty()) {
                    Text(
                        text = wordItem.phonetic!!,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // 右侧：发音按钮
            val audioUrl = wordItem.phonetics?.find { !it.audio.isNullOrEmpty() }?.audio
            if (!audioUrl.isNullOrEmpty()) {
                // 发音按钮样式
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
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放发音",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 分隔线
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
                                text = meaning.partOfSpeech ?: "其他",
                                fontStyle = FontStyle.Italic
                            )
                        },
                        border = null
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // 具体的定义和例句，最多显示3个
                    meaning.definitions?.take(3)?.forEachIndexed { index, definition ->
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // 定义
                            Text(
                                text = "${index + 1}. ${definition.definition}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // 例句
                            if (!definition.example.isNullOrEmpty()) {
                                Text(
                                    text = "e.g. \"${definition.example}\"",
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
            // 底部间距
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * 空状态视图
 * 当没有单词可学时显示
 *
 * @param viewModel 主视图模型，用于返回书架
 */
@Composable
fun EmptyStateView(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 庆祝表情
        Text(
            text = "🎉",
            fontSize = 80.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        // 庆祝文字
        Text(
            text = "太棒了！",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(48.dp))
        // 返回书架按钮
        Button(
            onClick = {
                viewModel.quitLearning()
            },
            modifier = Modifier
                .width(200.dp)
                .height(50.dp)
        ) {
            Text(text = "返回书架")
        }
    }
}

// 扩展函数：为Modifier添加scale方法
/**
 * 扩展Modifier，添加缩放功能
 *
 * @param scale 缩放比例，1.0f为原始大小
 * @return 修改后的Modifier
 */
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
)