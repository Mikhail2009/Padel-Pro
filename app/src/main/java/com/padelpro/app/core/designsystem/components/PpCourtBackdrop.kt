package com.padelpro.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpCourtDeep
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpLime

/**
 * Спокойный фон табло: едва заметная разметка корта и мягкий лаймовый отсвет
 * над подающей стороной. Линии не проходят через центр, где стоит колонка таймера.
 * [servingLeft] — какая половина подсвечена (подающая сторона).
 */
@Composable
fun PpCourtBackdrop(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    servingLeft: Boolean? = null,
    dimmed: Boolean = false
) {
    val motion = LocalPpMotion.current
    val spec = tween<Float>(durationMillis = if (motion.enabled) 450 else 1)
    val leftGlow by animateFloatAsState(
        targetValue = if (servingLeft == true) 1f else 0f,
        animationSpec = spec,
        label = "pp-glow-left"
    )
    val rightGlow by animateFloatAsState(
        targetValue = if (servingLeft == false) 1f else 0f,
        animationSpec = spec,
        label = "pp-glow-right"
    )

    Canvas(modifier.fillMaxSize()) {
        val u = 1.dp.toPx()
        drawRect(PpCourtDeep)

        if (glow && !dimmed) {
            drawRect(
                Brush.radialGradient(
                    listOf(PpLime.copy(alpha = 0.07f * leftGlow), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.5f),
                    radius = size.maxDimension * 0.45f
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(PpLime.copy(alpha = 0.07f * rightGlow), Color.Transparent),
                    center = Offset(size.width * 0.75f, size.height * 0.5f),
                    radius = size.maxDimension * 0.45f
                )
            )
        }

        val padX = size.width * 0.035f
        val padY = size.height * 0.09f
        val w = size.width - padX * 2
        val h = size.height - padY * 2
        val left = padX
        val top = padY
        val ink = if (dimmed) 0.10f else 0.20f
        val line = PpCourtLineStrong.copy(alpha = ink)

        drawRoundRect(
            color = line,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(14 * u, 14 * u),
            style = Stroke(width = 1f * u)
        )

        // Линии подачи — только по краям, центр (таймер) остаётся чистым.
        val sx1 = left + w * 0.1525f
        val sx2 = left + w * 0.8475f
        drawLine(line, Offset(sx1, top + 14 * u), Offset(sx1, top + h - 14 * u), 1f * u)
        drawLine(line, Offset(sx2, top + 14 * u), Offset(sx2, top + h - 14 * u), 1f * u)

        val midY = top + h / 2
        val gapHalf = 72 * u
        val netX = left + w / 2
        drawLine(line, Offset(sx1, midY), Offset(netX - gapHalf, midY), 1f * u)
        drawLine(line, Offset(netX + gapHalf, midY), Offset(sx2, midY), 1f * u)

        if (dimmed) drawRect(PpCourtDeep.copy(alpha = 0.45f))
    }
}
