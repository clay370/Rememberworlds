package com.example.rememberworlds.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


// =================================================================
// ============== 亮色配色方案 (Light Color Scheme) =================
// =================================================================
private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,

    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,

    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,

    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,

    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,

    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,

    error = md_theme_light_error,
    onError = md_theme_light_onError,

    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,

    outline = md_theme_light_outline,

    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,

    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,

    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
)


// =================================================================
// =============== 暗色配色方案 (Dark Color Scheme) ==================
// =================================================================
private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,

    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,

    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,

    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,

    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,

    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,

    error = md_theme_dark_error,
    onError = md_theme_dark_onError,

    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,

    outline = md_theme_dark_outline,

    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,

    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,

    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
)


// =================================================================
// ========================= 主题 Composable =========================
// =================================================================
@Composable
fun RememberWorldsTheme(
    // 默认值：跟随系统暗色主题设置
    darkTheme: Boolean = isSystemInDarkTheme(),

    // 默认值：手动关闭动态取色，因为它可能会破坏自定义配色的效果
    dynamicColor: Boolean = false,

    // 接收 Composable 内容
    content: @Composable () -> Unit
) {
    // --- 1. 确定实际使用的配色方案 ---
    val colorScheme = when {
        // 动态取色逻辑 (仅适用于 Android S 及更高版本)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        // 使用自定义的暗色方案
        darkTheme -> DarkColors

        // 使用自定义的亮色方案
        else -> LightColors
    }

    // --- 2. 状态栏和导航栏适应性设置 ---
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            // 获取当前 Activity 的 Window
            val window = (view.context as Activity).window

            // 设置状态栏颜色为主题背景色
            window.statusBarColor = colorScheme.background.toArgb()

            // 确保状态栏内容颜色与主题背景色形成对比
            // 暗色主题时，状态栏内容（图标、文字）应为亮色
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    // --- 3. 应用 Material 3 主题 ---
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
        typography = Typography
    )
}