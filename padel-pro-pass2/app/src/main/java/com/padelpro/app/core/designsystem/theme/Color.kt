package com.padelpro.app.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color

// ── Основа: «обсидиан» — почти чёрный с едва заметным зелёным подтоном ───────────
// Ступени поверхностей отличаются на 3–4% светлоты: глубина создаётся не градиентами,
// а чёткой лестницей слоёв и тонкой границей.
val PpCourtDeep = Color(0xFF090B0A)   // фон приложения
val PpCourtNight = Color(0xFF111512)  // поверхность уровня 1 (нижняя панель, поля ввода)
val PpCourtPanel = Color(0xFF171C18)  // карточки
val PpCourtGlass = Color(0xFF1E241F)  // приподнятые слои: sheets, диалоги, чипы
val PpCourtLine = Color(0xFF28302A)   // тонкие границы (hairline)
val PpCourtLineStrong = Color(0xFF3A453C)

// ── Акценты: один главный (лайм) + сдержанные второстепенные ───────────────────
val PpLime = Color(0xFFC2F54C)        // мяч — единственный «громкий» акцент
val PpLimeDeep = Color(0xFF8FCB38)
val PpLimeGlow = Color(0xFFDBFF86)
val PpCourtBlue = Color(0xFF3E7BE6)   // синий корт (только графики)
val PpGlassCyan = Color(0xFF72B7F0)   // небесный: команда B, «текущий» урок, информация
val PpAmberLed = Color(0xFFF2B84B)    // золото: XP, подача, серия
val PpTrophyMagenta = Color(0xFFE8638C) // поражение / редкие достижения (приглушённый розовый)
val PpViolet = Color(0xFFA793F2)

// ── Семантика ─────────────────────────────────────────────────────────────────
val PpSuccess = Color(0xFF46D68F)
val PpWarning = PpAmberLed
val PpDanger = Color(0xFFFF5A5F)
val PpInfo = PpGlassCyan

// ── Текст ─────────────────────────────────────────────────────────────────────
val PpTextHigh = Color(0xFFF5F7F2)
val PpTextMid = Color(0xFF9BA59B)
val PpTextLow = Color(0xFF737E74)
val PpOnAccent = Color(0xFF0A1006)
val PpScrimColor = Color(0xE6050706)

// ── Совместимость с существующими именами токенов ─────────────────────────────
val PpBackground = PpCourtDeep
val PpSurface = PpCourtNight
val PpSurfaceVariant = PpCourtPanel
val PpCardSurface = PpCourtPanel
val PpCard = PpCourtPanel
val PpPrimary = PpLime
val PpPrimaryDim = PpLimeDeep
val PpPrimaryTint = Color(0x1FC2F54C)
val PpOnPrimary = PpOnAccent
val PpTextPrimary = PpTextHigh
val PpTextSecondary = PpTextMid
val PpTextTertiary = PpTextLow
val PpOutline = PpCourtLine
val PpOutlineStrong = PpCourtLineStrong
val PpError = PpDanger
val PpErrorTint = Color(0x26FF5A5F)

// ── Заливки и кромки: максимально тихие ───────────────────────────────────────
val PpCourtSweep: Brush get() = Brush.linearGradient(listOf(PpLime, PpLimeDeep))
val PpBallTrail: Brush get() = Brush.horizontalGradient(listOf(PpLimeDeep, PpLime))
val PpTrophySweep: Brush get() = Brush.linearGradient(listOf(PpAmberLed, Color(0xFFFFDA8F)))
val PpGlassFill: Brush get() = Brush.verticalGradient(
    listOf(Color(0x14FFFFFF), Color(0x06FFFFFF), Color(0x00000000))
)

/** Кромка карточки: светлее сверху, почти исчезает снизу — как подсветка грани. */
val PpSpecularEdge: Brush get() = Brush.verticalGradient(
    listOf(Color(0x2EFFFFFF), Color(0x14FFFFFF), Color(0x0AFFFFFF))
)

/** Заливка карточки: на 2–3% светлее к верху — почти плоская. */
val PpPanelFill: Brush get() = SolidColor(PpCourtPanel)
val PpScrimVertical: Brush get() = Brush.verticalGradient(
    listOf(Color(0x00090B0A), Color(0xB3090B0A), PpCourtDeep)
)

// ── Палитра графиков: 8 различимых оттенков (разная светлота + прямые подписи) ─
val PpChartPalette = listOf(
    PpLime, PpGlassCyan, PpAmberLed, PpViolet,
    PpTrophyMagenta, PpSuccess, PpCourtBlue, Color(0xFF8A9A8C)
)
