package com.padelpro.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.padelpro.app.R

// Раньше в проекте лежал один урезанный Inter Regular БЕЗ кириллицы: русский текст
// рисовался системным Roboto, а жирность синтезировалась платформой («фейковый» bold).
// Теперь — настоящие начертания Inter (текст) и Inter Display (крупные цифры и заголовки)
// с латиницей и кириллицей, по одному файлу на каждый вес.
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val InterDisplayFamily = FontFamily(
    Font(R.font.inter_display_semibold, FontWeight.SemiBold),
    Font(R.font.inter_display_bold, FontWeight.Bold),
    Font(R.font.inter_display_extrabold, FontWeight.ExtraBold)
)

private const val TABULAR = "tnum"

// Строки выровнены по центру глифов: убирает «пустоту» сверху у крупных цифр.
private val TrimLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

object PpTextStyles {
    // Цифры табло: табличные, очень плотный трекинг — выглядят как отлитые, а не набранные.
    val ScoreDisplay = TextStyle(
        fontFamily = InterDisplayFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 88.sp, lineHeight = 88.sp, letterSpacing = (-3).sp,
        fontFeatureSettings = TABULAR, textAlign = TextAlign.Center,
        lineHeightStyle = TrimLines
    )
    val ScoreLed = TextStyle(
        fontFamily = InterDisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = (-1.6).sp,
        fontFeatureSettings = TABULAR, textAlign = TextAlign.Center,
        lineHeightStyle = TrimLines
    )
    val DisplayNum = TextStyle(
        fontFamily = InterDisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1).sp,
        fontFeatureSettings = TABULAR
    )
    val Numeral = TextStyle(
        fontFamily = InterDisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp,
        fontFeatureSettings = TABULAR
    )
    val H1 = TextStyle(
        fontFamily = InterDisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp
    )
    val H2 = TextStyle(
        fontFamily = InterDisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.3).sp
    )
    val Title = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp
    )
    val Body = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    )
    val Label = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp
    )
    // Подпись-«эйбрау» и чипы: обычный регистр, средний вес, лёгкий трекинг.
    val Micro = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp
    )
    // Единственный стиль для КАПСА: крошечные метки метрик. Трекинг умеренный:
    // на кириллице широкий разряд читается плохо.
    val Overline = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 0.7.sp
    )
}

val PpTypography = Typography(
    displayLarge = PpTextStyles.ScoreDisplay,
    displayMedium = PpTextStyles.DisplayNum,
    displaySmall = PpTextStyles.Numeral,
    headlineLarge = PpTextStyles.H1,
    headlineMedium = PpTextStyles.H2,
    headlineSmall = PpTextStyles.Title,
    titleLarge = PpTextStyles.Title,
    titleMedium = PpTextStyles.Title,
    bodyLarge = PpTextStyles.Body,
    bodyMedium = PpTextStyles.Body,
    bodySmall = PpTextStyles.Label,
    labelLarge = PpTextStyles.Label,
    labelMedium = PpTextStyles.Label,
    labelSmall = PpTextStyles.Micro
)
