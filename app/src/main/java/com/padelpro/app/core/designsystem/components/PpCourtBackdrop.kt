package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.padelpro.app.core.designsystem.theme.PpCourtDeep
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime

/** Разметка корта сверху: длина по горизонтали, сетка вертикально по центру. */
@Composable
fun PpCourtBackdrop(modifier: Modifier = Modifier, glow: Boolean = true) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(PpCourtDeep)
        if (glow) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0x261E6FE8), Color.Transparent),
                    center = Offset(size.width * 0.5f, -size.height * 0.15f),
                    radius = size.maxDimension * 0.8f
                )
            )
        }
        val padX = size.width * 0.035f
        val padY = size.height * 0.09f
        val w = size.width - padX * 2
        val h = size.height - padY * 2
        val left = padX
        val top = padY
        val line = PpCourtLineStrong.copy(alpha = 0.45f)
        val faint = PpCourtLineStrong.copy(alpha = 0.22f)

        // стеклянный периметр
        drawRect(line, Offset(left, top), Size(w, h), style = Stroke(1.6f))
        // линии подачи: 6.95 м от сетки при длине 20 м
        val sx1 = left + w * 0.1525f
        val sx2 = left + w * 0.8475f
        drawLine(line, Offset(sx1, top), Offset(sx1, top + h), 1.4f)
        drawLine(line, Offset(sx2, top), Offset(sx2, top + h), 1.4f)
        // центральная линия подачи (только между сеткой и линиями подачи)
        val midY = top + h / 2
        drawLine(faint, Offset(sx1, midY), Offset(left + w / 2, midY), 1.2f)
        drawLine(faint, Offset(left + w / 2, midY), Offset(sx2, midY), 1.2f)
        // сетка
        val netX = left + w / 2
        drawLine(PpGlassCyan.copy(alpha = 0.35f), Offset(netX, top - 4.dp.toPx()), Offset(netX, top + h + 4.dp.toPx()), 2.4f)
        var y = top
        while (y < top + h) {
            drawLine(PpGlassCyan.copy(alpha = 0.12f), Offset(netX - 5.dp.toPx(), y), Offset(netX + 5.dp.toPx(), y), 1f)
            y += 7.dp.toPx()
        }
        // мяч-акцент у сетки
        drawCircle(PpLime.copy(alpha = 0.18f), 26.dp.toPx(), Offset(netX, top + h * 0.5f))
    }
}

private val Int.dp get() = androidx.compose.ui.unit.Dp(this.toFloat())