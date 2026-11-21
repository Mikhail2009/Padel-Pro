package com.padelpro.app.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpCourtDeep
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime

/**
 * Разметка корта сверху: длина по горизонтали, сетка вертикально по центру.
 * [servingLeft] — какая половина принимает свет софитов (подающая сторона).
 */
@Composable
fun PpCourtBackdrop(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    servingLeft: Boolean? = null,
    dimmed: Boolean = false
) {
    val motion = LocalPpMotion.current
    val pulse by rememberInfiniteTransition(label = "pp-flood").animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (motion.enabled) 5200 else 1),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pp-flood-pulse"
    )

    Canvas(modifier.fillMaxSize()) {
        val u = 1.dp.toPx()
        drawRect(PpCourtDeep)

        if (glow) {
            val base = if (dimmed) 0.10f else 0.20f
            val leftBoost = when (servingLeft) { true -> 1.5f; false -> 0.55f; null -> 1f }
            val rightBoost = when (servingLeft) { true -> 0.55f; false -> 1.5f; null -> 1f }
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF1E6FE8).copy(alpha = base * leftBoost * pulse), Color.Transparent),
                    center = Offset(size.width * 0.22f, -size.height * 0.2f),
                    radius = size.maxDimension * 0.7f
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF1E6FE8).copy(alpha = base * rightBoost * pulse), Color.Transparent),
                    center = Offset(size.width * 0.78f, -size.height * 0.2f),
                    radius = size.maxDimension * 0.7f
                )
            )
        }

        val padX = size.width * 0.035f
        val padY = size.height * 0.09f
        val w = size.width - padX * 2
        val h = size.height - padY * 2
        val left = padX
        val top = padY
        val ink = if (dimmed) 0.20f else 0.45f
        val line = PpCourtLineStrong.copy(alpha = ink)
        val faint = PpCourtLineStrong.copy(alpha = ink * 0.5f)

        drawRect(
            color = line,
            topLeft = Offset(left, top),
            size = Size(w, h),
            style = Stroke(width = 1.6f * u)
        )

        val sx1 = left + w * 0.1525f
        val sx2 = left + w * 0.8475f
        drawLine(line, Offset(sx1, top), Offset(sx1, top + h), 1.4f * u)
        drawLine(line, Offset(sx2, top), Offset(sx2, top + h), 1.4f * u)

        val midY = top + h / 2
        drawLine(faint, Offset(sx1, midY), Offset(left + w / 2, midY), 1.2f * u)
        drawLine(faint, Offset(left + w / 2, midY), Offset(sx2, midY), 1.2f * u)

        val netX = left + w / 2
        drawLine(
            PpGlassCyan.copy(alpha = if (dimmed) 0.18f else 0.38f),
            Offset(netX, top - 4 * u), Offset(netX, top + h + 4 * u), 2.4f * u
        )
        var y = top
        val step = 7 * u
        while (y < top + h) {
            drawLine(
                PpGlassCyan.copy(alpha = 0.12f),
                Offset(netX - 5 * u, y), Offset(netX + 5 * u, y), 1f * u
            )
            y += step
        }

        drawCircle(
            PpLime.copy(alpha = if (dimmed) 0.07f else 0.16f),
            26 * u,
            Offset(netX, midY)
        )

        if (dimmed) drawRect(PpCourtDeep.copy(alpha = 0.55f))
    }
}