package com.padelpro.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Сетка 4 dp. Поля экрана — `screen` (20 dp): больше «воздуха» — главный признак дорогого UI.
object PpSpacing {
    val screen = 20.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 32.dp
}

// Радиусы: ступенчатые и вложенные (внутренний = внешний − отступ), без «пузырей».
object PpRadius {
    val small = 10.dp
    val input = 14.dp
    val card = 20.dp
    val panel = 24.dp
    val sheet = 28.dp
    val sheetTop = 28.dp
    val pill = RoundedCornerShape(50)
    val cardShape = RoundedCornerShape(card)
    val panelShape = RoundedCornerShape(panel)
    val inputShape = RoundedCornerShape(input)
    val sheetShape = RoundedCornerShape(topStart = sheet, topEnd = sheet)
}

object PpStroke { val hair = 1.dp; val accent = 2.dp; val rail = 3.dp }
object PpTouch { val min = 48.dp; val score = 96.dp }
