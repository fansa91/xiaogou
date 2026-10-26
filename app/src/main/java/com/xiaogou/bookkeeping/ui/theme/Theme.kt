package com.xiaogou.bookkeeping.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * WalkTheme：把旧版 walk 主题体系翻译成 Compose Material3 主题。
 *
 * 旧版核心口径（见 docs/DATA_DICTIONARY.md 与方案第 2 节）：
 * - 颜色模式 color_mode（0–11，映射到主题色），默认主题色取黄色 #fead23，
 *   深色变体 #cb8a1c；Phase 0 先固定这一组，后续再接设置页的换色。
 * - 夜间跟随系统：KEY_NIGHT_MODE_BY_SYSTEM，Phase 0 用 isSystemInDarkTheme()。
 * - 字体：默认 assets/fonts/marvel.ttf，现放进 res/font/marvel.ttf，见 Type.kt。
 * - 形状：直角/圆角/卡片，对应旧版 ThemeRect / ThemeCircle / ThemeCardLayout。
 */
private val WalkYellow = Color(0xFFFEAD23)
private val WalkYellowDark = Color(0xFFCB8A1C)
private val WalkGreen = Color(0xFF0F9D58)
private val WalkRed = Color(0xFFFE2542)
private val WalkBlue = Color(0xFF2581FE)
private val WalkCyan = Color(0xFF65C3AC)

private val WalkLightColors = lightColorScheme(
    primary = WalkYellow,
    onPrimary = Color(0xFF3C424A),
    primaryContainer = Color(0xFFF2F5F9),
    secondary = WalkGreen,
    error = WalkRed,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F5F9),
    onBackground = Color(0xFF3C424A),
    onSurface = Color(0xFF3C424A),
    onSurfaceVariant = Color(0xFF60707A),
    outline = Color(0xFFBCC2CA),
)

private val WalkDarkColors = darkColorScheme(
    primary = WalkYellow,
    onPrimary = Color(0xFF202124),
    primaryContainer = Color(0xFF131416),
    secondary = WalkGreen,
    error = WalkRed,
    background = Color(0xFF202124),
    surface = Color(0xFF202124),
    surfaceVariant = Color(0xFF131416),
    onBackground = Color(0xFF999999),
    onSurface = Color(0xFF999999),
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFF444444),
)

@Composable
fun WalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) WalkDarkColors else WalkLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WalkTypography,
        shapes = WalkShapes,
        content = content,
    )
}
