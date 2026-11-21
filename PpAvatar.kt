package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpCourtNight
import com.padelpro.app.core.designsystem.theme.PpCourtPanel
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid

@Composable
fun PpAvatar(
    avatarId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val index = avatarId.coerceIn(0, 11)
    val cd = stringResource(R.string.cd_user_avatar)
    // Один цвет вместо радужного кольца: активный профиль — лайм, остальные — нейтральная граница.
    val ringBrush: Brush = if (highlighted) SolidColor(PpLime) else SolidColor(PpCourtLineStrong)
    val borderWidth = if (highlighted) 2.dp else 1.dp

    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(
            Brush.radialGradient(listOf(PpCourtGlass, PpCourtPanel))
        )
        .border(borderWidth, ringBrush, CircleShape)
        .semantics { contentDescription = cd }

    val clickableModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }

    Box(modifier = clickableModifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val c = Offset(w / 2f, h / 2f)
            val stroke = w * 0.055f

            val accent = when (index % 3) {
                0 -> PpLime
                1 -> PpGlassCyan
                else -> PpTextHigh
            }
            val muted = PpTextMid.copy(alpha = 0.48f)

            when (index) {
                0 -> {
                    drawCircle(color = muted, radius = w * 0.34f, center = c, style = Stroke(stroke))
                    drawOval(
                        color = accent,
                        topLeft = Offset(w * 0.32f, h * 0.22f),
                        size = Size(w * 0.36f, h * 0.42f),
                        style = Stroke(stroke)
                    )
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.5f, h * 0.64f),
                        end = Offset(w * 0.5f, h * 0.82f),
                        strokeWidth = stroke * 1.2f,
                        cap = StrokeCap.Round
                    )
                }
                1 -> {
                    drawCircle(color = accent, radius = w * 0.22f, center = c, style = Stroke(stroke))
                    drawArc(
                        color = muted,
                        startAngle = -35f,
                        sweepAngle = 210f,
                        useCenter = false,
                        topLeft = Offset(w * 0.16f, h * 0.16f),
                        size = Size(w * 0.68f, h * 0.68f),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    drawCircle(color = PpLime, radius = w * 0.07f, center = Offset(w * 0.72f, h * 0.28f))
                }
                2 -> {
                    val p = Path().apply {
                        moveTo(w * 0.28f, h * 0.58f)
                        lineTo(w * 0.5f, h * 0.32f)
                        lineTo(w * 0.72f, h * 0.58f)
                    }
                    drawPath(p, color = accent, style = Stroke(stroke * 1.15f, cap = StrokeCap.Round))
                    val p2 = Path().apply {
                        moveTo(w * 0.34f, h * 0.72f)
                        lineTo(w * 0.5f, h * 0.52f)
                        lineTo(w * 0.66f, h * 0.72f)
                    }
                    drawPath(p2, color = muted, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                3 -> {
                    val p = Path().apply {
                        moveTo(w * 0.5f, h * 0.20f)
                        lineTo(w * 0.78f, h * 0.5f)
                        lineTo(w * 0.5f, h * 0.80f)
                        lineTo(w * 0.22f, h * 0.5f)
                        close()
                    }
                    drawPath(p, color = accent, style = Stroke(stroke))
                    drawCircle(color = PpGlassCyan, radius = w * 0.09f, center = c)
                }
                4 -> {
                    drawCircle(color = accent, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.34f))
                    drawArc(
                        color = accent,
                        startAngle = 195f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(w * 0.22f, h * 0.50f),
                        size = Size(w * 0.56f, h * 0.48f),
                        style = Stroke(stroke * 1.3f, cap = StrokeCap.Round)
                    )
                }
                5 -> {
                    drawLine(
                        color = muted,
                        start = Offset(w * 0.22f, h * 0.5f),
                        end = Offset(w * 0.78f, h * 0.5f),
                        strokeWidth = stroke
                    )
                    drawArc(
                        color = accent,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.20f, h * 0.24f),
                        size = Size(w * 0.60f, h * 0.52f),
                        style = Stroke(stroke * 1.1f, cap = StrokeCap.Round)
                    )
                    drawCircle(color = PpLime, radius = w * 0.08f, center = Offset(w * 0.74f, h * 0.36f))
                }
                6 -> {
                    val p = Path()
                    for (i in 0 until 6) {
                        val angle = Math.toRadians((60 * i - 30).toDouble())
                        val x = c.x + (w * 0.30f * kotlin.math.cos(angle)).toFloat()
                        val y = c.y + (h * 0.30f * kotlin.math.sin(angle)).toFloat()
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    p.close()
                    drawPath(p, color = accent, style = Stroke(stroke))
                    drawCircle(color = muted, radius = w * 0.12f, center = c, style = Stroke(stroke))
                }
                7 -> {
                    drawLine(
                        color = muted,
                        start = Offset(w * 0.2f, h * 0.55f),
                        end = Offset(w * 0.8f, h * 0.55f),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.3f, h * 0.74f),
                        end = Offset(w * 0.7f, h * 0.26f),
                        strokeWidth = stroke * 1.3f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = PpLime, radius = w * 0.07f, center = Offset(w * 0.7f, h * 0.26f))
                }
                8 -> {
                    drawCircle(color = muted, radius = w * 0.34f, center = c, style = Stroke(stroke * 0.8f))
                    drawCircle(color = accent, radius = w * 0.21f, center = c, style = Stroke(stroke))
                    drawCircle(color = PpGlassCyan, radius = w * 0.08f, center = c)
                }
                9 -> {
                    val topTri = Path().apply {
                        moveTo(w * 0.5f, h * 0.22f)
                        lineTo(w * 0.74f, h * 0.54f)
                        lineTo(w * 0.26f, h * 0.54f)
                        close()
                    }
                    drawPath(topTri, color = accent, style = Stroke(stroke))
                    drawLine(
                        color = muted,
                        start = Offset(w * 0.30f, h * 0.68f),
                        end = Offset(w * 0.70f, h * 0.68f),
                        strokeWidth = stroke * 1.2f,
                        cap = StrokeCap.Round
                    )
                }
                10 -> {
                    drawRect(
                        color = muted,
                        topLeft = Offset(w * 0.28f, h * 0.22f),
                        size = Size(w * 0.44f, h * 0.56f),
                        style = Stroke(stroke * 0.85f)
                    )
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.28f, h * 0.5f),
                        end = Offset(w * 0.72f, h * 0.5f),
                        strokeWidth = stroke * 1.1f
                    )
                    drawLine(
                        color = muted,
                        start = Offset(w * 0.5f, h * 0.34f),
                        end = Offset(w * 0.5f, h * 0.66f),
                        strokeWidth = stroke * 0.85f
                    )
                }
                else -> {
                    val p = Path().apply {
                        moveTo(w * 0.24f, h * 0.66f)
                        lineTo(w * 0.32f, h * 0.34f)
                        lineTo(w * 0.5f, h * 0.52f)
                        lineTo(w * 0.68f, h * 0.34f)
                        lineTo(w * 0.76f, h * 0.66f)
                        close()
                    }
                    drawPath(p, color = accent, style = Stroke(stroke))
                }
            }
        }
    }
}

@Composable
fun PpAvatarPickerGrid(
    selectedAvatarId: Int,
    onSelectAvatar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
    ) {
        for (row in 0 until 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0 until 6) {
                    val id = row * 6 + col
                    val selected = id == selectedAvatarId
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) PpLime.copy(alpha = 0.14f) else PpCourtNight
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PpAvatar(
                            avatarId = id,
                            size = 44.dp,
                            highlighted = selected,
                            onClick = { onSelectAvatar(id) }
                        )
                    }
                }
            }
        }
    }
}
