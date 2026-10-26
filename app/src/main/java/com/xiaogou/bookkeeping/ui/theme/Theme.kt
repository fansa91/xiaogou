package com.xiaogou.bookkeeping.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Walk 主题体系（Compose 版）：
 * - 五种主题色，取自源码配色：黄 / 绿 / 红 / 蓝 / 青，每组 primary + dark + 30% 透明。
 *   当前重写版默认仍用黄色（与之前版本一致）；源码兜底是绿色，用户在设置里
 *   选绿色即可回到源码默认。
 * - 夜间三档：跟随系统 / 白天 / 夜间。存储沿用源码的两个键：
 *   key_night_mode（是否开启夜间）+ key_night_mode_by_system（是否跟随系统），
 *   夜间生效口径：开启夜间 且（不跟随系统 或 系统本身是夜间）。
 * - 字体：res/font/marvel.ttf，见 Type.kt；形状：直角/圆角/卡片，见 Shape.kt。
 */
enum class WalkThemeId(
    val key: String,
    val label: String,
    val primary: Color,
    val dark: Color,
) {
    YELLOW("theme_yellow", "黄色", Color(0xFFFEAD23), Color(0xFFCB8A1C)),
    GREEN("theme_green", "绿色", Color(0xFF0F9D58), Color(0xFF0D864B)),
    RED("theme_red", "红色", Color(0xFFFE2542), Color(0xFFCB1E35)),
    BLUE("theme_blue", "蓝色", Color(0xFF2581FE), Color(0xFF1C61BE)),
    CYAN("theme_cyan", "青色", Color(0xFF65C3AC), Color(0xFF57AA93));

    /** 30% 透明的主题色（源码 *_trans 口径，#4c…）。 */
    val trans: Color get() = primary.copy(alpha = 0.3f)

    /** 50% 透明的主题色（源码 theme_alpha 口径）。 */
    val alpha: Color get() = primary.copy(alpha = 0.5f)

    companion object {
        fun fromKey(key: String?): WalkThemeId =
            values().firstOrNull { it.key == key } ?: YELLOW
    }
}

/** 夜间模式选项：0 跟随系统 / 1 白天 / 2 夜间。 */
object NightMode {
    const val SYSTEM = 0
    const val DAY = 1
    const val NIGHT = 2
    val labels = listOf("跟随系统", "白天", "夜间")
}

private fun onPrimaryFor(theme: WalkThemeId, darkTheme: Boolean): Color =
    when (theme) {
        WalkThemeId.YELLOW, WalkThemeId.CYAN ->
            if (darkTheme) Color(0xFF202124) else Color(0xFF3C424A)
        else -> Color(0xFFFFFFFF)
    }

private fun walkLightColors(theme: WalkThemeId) = lightColorScheme(
    primary = theme.primary,
    onPrimary = onPrimaryFor(theme, false),
    primaryContainer = Color(0xFFF2F5F9),
    secondary = theme.dark,
    error = WalkThemeId.RED.primary,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F5F9),
    onBackground = Color(0xFF3C424A),
    onSurface = Color(0xFF3C424A),
    onSurfaceVariant = Color(0xFF60707A),
    outline = Color(0xFFBCC2CA),
)

private fun walkDarkColors(theme: WalkThemeId) = darkColorScheme(
    primary = theme.primary,
    onPrimary = onPrimaryFor(theme, true),
    primaryContainer = Color(0xFF131416),
    secondary = theme.dark,
    error = WalkThemeId.RED.primary,
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
    themeId: WalkThemeId = WalkThemeId.YELLOW,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) walkDarkColors(themeId) else walkLightColors(themeId)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WalkTypography,
        shapes = WalkShapes,
        content = content,
    )
}
