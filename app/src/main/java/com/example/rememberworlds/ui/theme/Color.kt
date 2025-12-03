package com.example.rememberworlds.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// ========================== 亮色模式 (Light Theme) ============================
// ==============================================================================

// 主色 (Primary) 系列：用于强调UI的主要元素，如按钮、选中状态等。
val md_theme_light_primary = Color(0xFF6750A4)      // 主色：深紫
val md_theme_light_onPrimary = Color(0xFFFFFFFF)    // 主色上的文字颜色
val md_theme_light_primaryContainer = Color(0xFFEADDFF) // 主色容器：浅紫
val md_theme_light_onPrimaryContainer = Color(0xFF21005D) // 主色容器上的文字颜色


// 次色 (Secondary) 系列：用于UI的次要元素或区别于Primary颜色的部分。
val md_theme_light_secondary = Color(0xFF625B71)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFE8DEF8)
val md_theme_light_onSecondaryContainer = Color(0xFF1D192B)


// 第三色 (Tertiary) 系列：用于强调与Primary和Secondary不同的元素，增加视觉丰富性。
val md_theme_light_tertiary = Color(0xFF7D5260)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFFD8E4)
val md_theme_light_onTertiaryContainer = Color(0xFF31111D)


// 错误色 (Error) 系列：用于指示错误或需要用户注意的状态。
val md_theme_light_error = Color(0xFFB3261E)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFF9DEDC)
val md_theme_light_onErrorContainer = Color(0xFF410E0B)


// 背景/表面 (Background/Surface) 系列：用于界面的背景和卡片、弹窗等元素的背景。
val md_theme_light_background = Color(0xFFFFFBFE) // 背景：米白
val md_theme_light_onBackground = Color(0xFF1C1B1F) // 背景上的主要文字颜色

val md_theme_light_surface = Color(0xFFFFFBFE)      // 表面色，通常与背景色相同
val md_theme_light_onSurface = Color(0xFF1C1B1F)    // 表面上的主要文字颜色

val md_theme_light_surfaceVariant = Color(0xFFE7E0EC) // 表面变体色，用于卡片灰底或不那么突出的元素
val md_theme_light_onSurfaceVariant = Color(0xFF49454F) // 表面变体上的次要文字颜色

val md_theme_light_outline = Color(0xFF79747E) // 轮廓色，用于边框或分割线


// ==============================================================================
// =========================== 暗色模式 (Dark Theme) ============================
// ==============================================================================
// 核心优化：使用柔和的粉紫色，背景不是纯黑，而是带有一点点紫色的深灰，以达到护眼效果。

// 主色 (Primary) 系列
val md_theme_dark_primary = Color(0xFFD0BCFF)      // 主色：柔和亮紫
val md_theme_dark_onPrimary = Color(0xFF381E72)    // 主色上的字：深紫
val md_theme_dark_primaryContainer = Color(0xFF4F378B) // 主色容器
val md_theme_dark_onPrimaryContainer = Color(0xFFEADDFF) // 主色容器上的文字颜色


// 次色 (Secondary) 系列
val md_theme_dark_secondary = Color(0xFFCCC2DC)    // 次色：灰紫
val md_theme_dark_onSecondary = Color(0xFF332D41)
val md_theme_dark_secondaryContainer = Color(0xFF4A4458)
val md_theme_dark_onSecondaryContainer = Color(0xFFE8DEF8)


// 第三色 (Tertiary) 系列
val md_theme_dark_tertiary = Color(0xFFEFB8C8)     // 第三色：粉色
val md_theme_dark_onTertiary = Color(0xFF492532)
val md_theme_dark_tertiaryContainer = Color(0xFF633B48)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFD8E4)


// 错误色 (Error) 系列
val md_theme_dark_error = Color(0xFFF2B8B5)        // 错误色：柔和红
val md_theme_dark_onError = Color(0xFF601410)
val md_theme_dark_errorContainer = Color(0xFF8C1D18)
val md_theme_dark_onErrorContainer = Color(0xFFF9DEDC)


// 背景/表面 (Background/Surface) 系列
val md_theme_dark_background = Color(0xFF141218)   // 背景：极深灰紫 (护眼)
val md_theme_dark_onBackground = Color(0xFFE6E1E5) // 背景上的主要文字：柔和白

val md_theme_dark_surface = Color(0xFF141218)      // 表面色，通常与背景色相同
val md_theme_dark_onSurface = Color(0xFFE6E1E5)    // 表面上的主要文字颜色

val md_theme_dark_surfaceVariant = Color(0xFF49454F) // 表面变体色，用于卡片背景
val md_theme_dark_onSurfaceVariant = Color(0xFFCAC4D0) // 表面变体上的次要文字颜色

val md_theme_dark_outline = Color(0xFF938F99) // 轮廓色