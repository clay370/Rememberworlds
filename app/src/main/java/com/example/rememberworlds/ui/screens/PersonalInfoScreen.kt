package com.example.rememberworlds.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.rememberworlds.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
/**
 * 个人信息屏幕
 * 用于显示和编辑用户的个人资料信息
 *
 * @param navController 导航控制器，用于页面导航
 * @param viewModel 主视图模型，用于获取和更新用户资料
 */
@Composable
fun PersonalInfoScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    // 从ViewModel收集用户资料状态
    val userProfile by viewModel.userProfile.collectAsState()
    
    // 创建滚动状态，用于垂直滚动
    val scrollState = rememberScrollState()

    // 状态控制变量
    // 用于控制普通文本编辑弹窗的显示和隐藏
    var showEditDialog by remember {
        mutableStateOf(false)
    }
    
    // 用于控制性别选择弹窗的显示和隐藏
    var showGenderDialog by remember {
        mutableStateOf(false)
    }
    
    // 用于控制日期选择器的显示和隐藏
    var showDatePicker by remember {
        mutableStateOf(false)
    }

    // 编辑中间变量
    // 用于存储当前正在编辑的字段名
    var editField by remember {
        mutableStateOf("")
    }
    
    // 用于存储编辑弹窗的标题
    var editTitle by remember {
        mutableStateOf("")
    }
    
    // 用于存储当前编辑的值
    var editValue by remember {
        mutableStateOf("")
    }

    // 头像选择器配置
    val photoPickerLauncher = rememberLauncherForActivityResult(
        // 使用ActivityResultContracts.PickVisualMedia作为合约
        contract = ActivityResultContracts.PickVisualMedia(),
        // 处理选择结果的回调函数
        onResult = { uri ->
            // 检查是否选择了图片
            if (uri != null) {
                // 调用ViewModel的上传头像方法
                viewModel.uploadAvatar(uri)
            }
        }
    )

    // 使用Scaffold作为基础布局
    Scaffold(
        // 配置顶部应用栏
        topBar = {
            CenterAlignedTopAppBar(
                // 设置标题
                title = {
                    Text(
                        text = "个人资料",
                        fontWeight = FontWeight.Bold
                    )
                },
                // 设置导航图标
                navigationIcon = {
                    IconButton(
                        // 点击返回按钮，弹出当前页面
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                // 设置应用栏颜色
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) {
        // 解构Scaffold的paddingValues参数
        paddingValues ->
        
        // 主内容区域
        Column(
            modifier = Modifier
                // 填充整个屏幕
                .fillMaxSize()
                // 应用Scaffold的内边距
                .padding(paddingValues)
                // 启用垂直滚动
                .verticalScroll(scrollState)
                // 设置背景颜色
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. 头像栏
            AvatarRowItem(
                label = "头像",
                avatarUrl = userProfile.avatarUrl,
                fallbackChar = userProfile.nickname.firstOrNull()?.toString() ?: "U",
                onClick = {
                    // 启动图片选择器
                    photoPickerLauncher.launch(
                        // 创建图片选择请求，只允许选择图片
                        PickVisualMediaRequest(
                            mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            )

            // 添加分隔线
            MyDivider()

            // 2. 昵称 (文本输入)
            InfoRowItem(
                label = "昵称",
                value = userProfile.nickname,
                onClick = {
                    // 设置编辑字段为昵称
                    editField = "nickname"
                    // 设置编辑弹窗标题
                    editTitle = "修改昵称"
                    // 设置初始编辑值
                    editValue = userProfile.nickname
                    // 显示编辑弹窗
                    showEditDialog = true
                }
            )

            // 3. 性别 (单选弹窗)
            InfoRowItem(
                label = "性别",
                value = userProfile.gender,
                onClick = {
                    // 显示性别选择弹窗
                    showGenderDialog = true
                }
            )

            // 4. 出生日期 (日期选择器)
            InfoRowItem(
                label = "出生",
                value = userProfile.birthDate,
                onClick = {
                    // 显示日期选择器
                    showDatePicker = true
                }
            )

            // 5. 位置 (文本输入)
            InfoRowItem(
                label = "位置",
                value = userProfile.location,
                onClick = {
                    // 设置编辑字段为位置
                    editField = "location"
                    // 设置编辑弹窗标题
                    editTitle = "修改位置"
                    // 设置初始编辑值
                    editValue = userProfile.location
                    // 显示编辑弹窗
                    showEditDialog = true
                }
            )
            
            // 6. 学校 (文本输入)
            InfoRowItem(
                label = "学校",
                value = userProfile.school,
                onClick = {
                    // 设置编辑字段为学校
                    editField = "school"
                    // 设置编辑弹窗标题
                    editTitle = "修改学校"
                    // 设置初始编辑值
                    editValue = userProfile.school
                    // 显示编辑弹窗
                    showEditDialog = true
                }
            )
            
            // 7. 年级 (文本输入)
            InfoRowItem(
                label = "年级",
                value = userProfile.grade,
                onClick = {
                    // 设置编辑字段为年级
                    editField = "grade"
                    // 设置编辑弹窗标题
                    editTitle = "修改年级"
                    // 设置初始编辑值
                    editValue = userProfile.grade
                    // 显示编辑弹窗
                    showEditDialog = true
                }
            )
            
            // 添加分隔线
            MyDivider()
            
            // 8. ID (只读项)
            InfoRowItem(
                label = "ID",
                value = userProfile.uid.take(6),
                showArrow = false,
                onClick = {}
            )
        }
    }

    // --- 弹窗逻辑 ---

    // A. 普通文本修改弹窗
    if (showEditDialog) {
        AlertDialog(
            // 设置点击外部区域的关闭逻辑
            onDismissRequest = {
                showEditDialog = false
            },
            // 设置弹窗标题
            title = {
                Text(
                    text = editTitle
                )
            },
            // 设置弹窗内容
            text = {
                OutlinedTextField(
                    // 当前输入值
                    value = editValue,
                    // 值变化的回调
                    onValueChange = {
                        editValue = it
                    },
                    // 只允许单行输入
                    singleLine = true,
                    // 填充宽度
                    modifier = Modifier
                        .fillMaxWidth()
                )
            },
            // 设置确认按钮
            confirmButton = {
                TextButton(
                    onClick = {
                        // 调用ViewModel更新字段
                        viewModel.updateProfileField(
                            field = editField,
                            value = editValue
                        )
                        // 关闭弹窗
                        showEditDialog = false
                    }
                ) {
                    Text(
                        text = "保存"
                    )
                }
            },
            // 设置取消按钮
            dismissButton = {
                TextButton(
                    onClick = {
                        // 关闭弹窗
                        showEditDialog = false
                    }
                ) {
                    Text(
                        text = "取消"
                    )
                }
            }
        )
    }

    // B. 性别选择弹窗
    if (showGenderDialog) {
        AlertDialog(
            // 设置点击外部区域的关闭逻辑
            onDismissRequest = {
                showGenderDialog = false
            },
            // 设置弹窗标题
            title = {
                Text(
                    text = "选择性别"
                )
            },
            // 设置弹窗内容
            text = {
                Column {
                    // 定义性别选项列表
                    val genderOptions = listOf(
                        "男",
                        "女",
                        "保密"
                    )
                    
                    // 遍历性别选项
                    genderOptions.forEach {
                        gender ->
                        
                        Row(
                            modifier = Modifier
                                // 填充宽度
                                .fillMaxWidth()
                                // 设置点击事件
                                .clickable {
                                    // 调用ViewModel更新性别
                                    viewModel.updateProfileField(
                                        field = "gender",
                                        value = gender
                                    )
                                    // 关闭弹窗
                                    showGenderDialog = false
                                }
                                // 设置内边距
                                .padding(12.dp),
                            // 垂直居中对齐
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 单选按钮
                            RadioButton(
                                // 检查当前性别是否被选中
                                selected = (
                                    userProfile.gender == gender
                                ),
                                // 点击事件，这里设为null，因为点击Row触发
                                onClick = null
                            )
                            // 性别文本
                            Text(
                                text = gender,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            // 设置确认按钮（这里留空，因为点击选项直接触发）
            confirmButton = {
                // 空实现，因为性别选择是通过点击Row实现的
            }
        )
    }

    // C. 日期选择器 (Material 3)
    if (showDatePicker) {
        // 创建日期选择器状态
        val datePickerState = rememberDatePickerState()
        
        // 显示日期选择器对话框
        DatePickerDialog(
            // 设置点击外部区域的关闭逻辑
            onDismissRequest = {
                showDatePicker = false
            },
            // 设置确认按钮
            confirmButton = {
                TextButton(
                    onClick = {
                        // 获取选中的日期毫秒数
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        
                        // 检查是否选择了日期
                        if (selectedDateMillis != null) {
                            // 创建日期格式化器
                            val sdf = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            )
                            
                            // 格式化日期
                            val dateStr = sdf.format(
                                Date(selectedDateMillis)
                            )
                            
                            // 调用ViewModel更新出生日期
                            viewModel.updateProfileField(
                                field = "birthDate",
                                value = dateStr
                            )
                        }
                        
                        // 关闭日期选择器
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "确定"
                    )
                }
            },
            // 设置取消按钮
            dismissButton = {
                TextButton(
                    onClick = {
                        // 关闭日期选择器
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "取消"
                    )
                }
            }
        ) {
            // 日期选择器组件
            DatePicker(
                state = datePickerState
            )
        }
    }
}

// --- 辅助组件 ---

/**
 * 自定义分隔线组件
 * 用于在信息项之间添加分隔线
 */
@Composable
fun MyDivider() {
    Divider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .padding(horizontal = 20.dp)
    )
}

/**
 * 信息行组件
 * 用于显示一条信息项，包含标签、值和可选的箭头图标
 *
 * @param label 信息标签
 * @param value 信息值
 * @param showArrow 是否显示箭头图标
 * @param onClick 点击事件回调
 */
@Composable
fun InfoRowItem(
    label: String,
    value: String,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            // 填充宽度
            .fillMaxWidth()
            // 设置点击事件，仅当showArrow为true时启用
            .clickable(
                enabled = showArrow,
                onClick = onClick
            )
            // 设置内边距
            .padding(
                horizontal = 24.dp,
                vertical = 18.dp
            ),
        // 水平两端对齐
        horizontalArrangement = Arrangement.SpaceBetween,
        // 垂直居中对齐
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 标签文本
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        
        // 值和箭头区域
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 值文本
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            // 如果需要显示箭头，则添加箭头图标
            if (showArrow) {
                // 添加间距
                Spacer(
                    modifier = Modifier
                        .width(8.dp)
                )
                // 箭头图标
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.LightGray
                )
            }
        }
    }
    
    // 添加分隔线
    MyDivider()
}

/**
 * 头像行组件
 * 用于显示头像信息项，包含头像图片或默认字符
 *
 * @param label 信息标签
 * @param avatarUrl 头像图片URL
 * @param fallbackChar 当没有头像时显示的字符
 * @param onClick 点击事件回调
 */
@Composable
fun AvatarRowItem(
    label: String,
    avatarUrl: String,
    fallbackChar: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            // 填充宽度
            .fillMaxWidth()
            // 设置点击事件
            .clickable(
                onClick = onClick
            )
            // 设置内边距
            .padding(
                horizontal = 24.dp,
                vertical = 12.dp
            ),
        // 水平两端对齐
        horizontalArrangement = Arrangement.SpaceBetween,
        // 垂直居中对齐
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 标签文本
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        
        // 头像和箭头区域
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            if (avatarUrl.isNotEmpty()) {
                // 使用Coil加载网络图片
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 默认头像，显示字符
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fallbackChar.uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // 箭头图标
            Spacer(
                modifier = Modifier
                    .width(8.dp)
            )
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}
