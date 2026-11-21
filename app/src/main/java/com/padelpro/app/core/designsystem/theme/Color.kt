package com.padelpro.app.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Основа: ночной корт (холодная тёмная семья, самые большие поля) ─────────────
val PpCourtDeep = Color(0xFF071016)   // фон приложения
val PpCourtNight = Color(0xFF0B1A22)  // поверхности
val PpCourtPanel = Color(0xFF10242E)  // карточки
val PpCourtGlass = Color(0xFF16323F)  // оверлеи, sheets
val PpCourtLine = Color(0xFF1E3A47)   // границы
val PpCourtLineStrong = Color(0xFF2C5566)

// ── Акценты: мяч, корт, стекло ────────────────────────────────────────────────
val PpLime = Color(0xFFC8F751)        // мяч — главный акцент
val PpLimeDeep = Color(0xFF9BD62B)
val PpLimeGlow = Color(0xFFE4FF8A)
val PpCourtBlue = Color(0xFF1E6FE8)   // синий корт
val PpGlassCyan = Color(0xFF5FE3FF)   // блик стекла / информация
val PpAmberLed = Color(0xFFFFB43D)    // LED-счёт, XP
val PpTrophyMagenta = Color(0xFFFF3D8F) // достижения
val PpViolet = Color(0xFF9B7BFF)

// ── Семантика ─────────────────────────────────────────────────────────────────
val PpSuccess = Color(0xFF2FD98A)
val PpWarning = PpAmberLed
val PpDanger = Color(0xFFFF4D5E)
val PpInfo = PpGlassCyan

// ── Нейтральные ───────────────────────────────────────────────────────────────
val PpTextHigh = Color(0xFFF4F8F7)
val PpTextMid = Color(0xFF9DB2BC)
val PpTextLow = Color(0xFF6B8391)
val PpOnAccent = Color(0xFF04120A)
val PpScrimColor = Color(0xE604090C)

// ── Совместимость с существующими именами токенов ─────────────────────────────
val PpBackground = PpCourtDeep
val PpSurface = PpCourtNight
val PpSurfaceVariant = PpCourtPanel
val PpCardSurface = PpCourtPanel
val PpCard = PpCourtPanel
val PpPrimary = PpLime
val PpPrimaryDim = PpLimeDeep
val PpPrimaryTint = Color(0x1FC8F751)
val PpOnPrimary = PpOnAccent
val PpTextPrimary = PpTextHigh
val PpTextSecondary = PpTextMid
val PpTextTertiary = PpTextLow
val PpOutline = PpCourtLine
val PpOutlineStrong = PpCourtLineStrong
val PpError = PpDanger
val PpErrorTint = Color(0x26FF4D5E)

// ── Градиенты через реально разные оттенки ────────────────────────────────────
val PpCourtSweep: Brush get() = Brush.linearGradient(listOf(PpCourtBlue, PpGlassCyan, PpLime))
val PpBallTrail: Brush get() = Brush.linearGradient(listOf(PpLimeGlow, PpLime, PpAmberLed))
val PpTrophySweep: Brush get() = Brush.linearGradient(listOf(PpTrophyMagenta, PpAmberLed))
val PpGlassFill: Brush get() = Brush.verticalGradient(
    listOf(Color(0x1A5FE3FF), Color(0x0AFFFFFF), Color(0x00000000))
)
val PpSpecularEdge: Brush get() = Brush.verticalGradient(
    listOf(Color(0x59FFFFFF), Color(0x265FE3FF), Color(0x0DFFFFFF), Color(0x00FFFFFF))
)
val PpPanelFill: Brush get() = Brush.verticalGradient(listOf(PpCourtPanel, PpCourtNight))
val PpScrimVertical: Brush get() = Brush.verticalGradient(
    listOf(Color(0x00071016), Color(0xB3071016), PpCourtDeep)
)

// ── Палитра графиков: 8 различимых оттенков (разная светлота + прямые подписи) ─
val PpChartPalette = listOf(
    PpLime, PpGlassCyan, PpCourtBlue, PpAmberLed,
    PpTrophyMagenta, PpSuccess, PpViolet, Color(0xFF7FA0B0)
)