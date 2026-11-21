package com.padelpro.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object PpSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object PpRadius {
    val small = 10.dp
    val input = 14.dp
    val card = 22.dp
    val panel = 28.dp
    val sheet = 30.dp
    val pill = RoundedCornerShape(50)
    val cardShape = RoundedCornerShape(card)
    val panelShape = RoundedCornerShape(panel)
    val inputShape = RoundedCornerShape(input)
    val sheetShape = RoundedCornerShape(topStart = sheet, topEnd = sheet)
}

object PpStroke { val hair = 1.dp; val accent = 2.dp; val rail = 3.dp }
object PpTouch { val min = 48.dp; val score = 96.dp }