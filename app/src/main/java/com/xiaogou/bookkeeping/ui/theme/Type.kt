package com.xiaogou.bookkeeping.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xiaogou.bookkeeping.R

/** 旧版默认字体：marvel.ttf。旧版行间距口径：默认 1.3 倍，见 Type.kt 的 lineHeight。 */
private val MarvelFontFamily = FontFamily(Font(R.font.marvel))

val WalkTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = MarvelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.8.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = MarvelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.6.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = MarvelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.8.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = MarvelFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.2.sp,
    ),
)
