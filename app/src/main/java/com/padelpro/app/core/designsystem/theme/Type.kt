package com.padelpro.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.padelpro.app.R

// В проекте один статический Inter Regular: регистрируем только Normal,
// жирность синтезируется платформой (FontSynthesis.All по умолчанию).
val InterFontFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))

private const val TABULAR = "tnum"

object PpTextStyles {
    // LED-цифры табло: табличные цифры, плотный трекинг
    val ScoreDisplay = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 84.sp, lineHeight = 86.sp, letterSpacing = (-1).sp,
        fontFeatureSettings = TABULAR, textAlign = TextAlign.Center
    )
    val ScoreLed = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = 0.sp,
        fontFeatureSettings = TABULAR, textAlign = TextAlign.Center
    )
    val DisplayNum = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp,
        fontFeatureSettings = TABULAR
    )
    val Numeral = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 26.sp, fontFeatureSettings = TABULAR
    )
    val H1 = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp
    )
    val H2 = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp
    )
    val Title = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 22.sp
    )
    val Body = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 23.sp
    )
    val Label = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp
    )
    val Micro = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.1.sp
    )
    val Overline = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 1.6.sp
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