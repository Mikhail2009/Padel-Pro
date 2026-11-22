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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpCourtDeep
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpCourtNight
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTrophyMagenta

/** Шесть приглушённых тонов: каждый аватар окрашен одним цветом, без радуги. */
private val AvatarTones = listOf(
    PpLime,
    PpGlassCyan,
    PpAmberLed,
    PpTrophyMagenta,
    Color(0xFF46D68F),
    Color(0xFFB69CFF)
)

/**
 * Аватар игрока: тёмный диск с мягким тоновым свечением у края и плотный знак по центру.
 * Двенадцать знаков из мира падела (ракетка, мяч, сетка, корт, молния, щит, шевроны,
 * мишень, волна, ромб, искра, корона). [avatarId] 0..11, остальное приводится к диапазону.
 */
@Composable
fun PpAvatar(
    avatarId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val index = avatarId.coerceIn(0, 11)
    val tone = AvatarTones[index % AvatarTones.size]
    val cd = stringResource(R.string.cd_user_avatar)
    // Активный профиль — лаймовое кольцо, остальные — тонкая нейтральная граница.
    val ringBrush: Brush = if (highlighted) SolidColor(PpLime) else SolidColor(PpCourtLineStrong)
    val borderWidth = if (highlighted) 2.dp else 1.dp

    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(
            Brush.linearGradient(
                listOf(tone.copy(alpha = 0.22f).compositeOver(PpCourtNight), PpCourtNight)
            )
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
            drawAvatarGlyph(index, tone)
        }
    }
}

private fun DrawScope.drawAvatarGlyph(index: Int, tone: Color) {
    val w = size.width
    val h = size.height
    val s = w * 0.06f
    val ink = PpCourtDeep
    fun p(x: Float, y: Float) = Offset(w * x, h * y)

    when (index) {
        0 -> rotate(degrees = -35f, pivot = p(0.5f, 0.5f)) { // ракетка
            drawOval(tone.copy(alpha = 0.22f), p(0.33f, 0.18f), Size(w * 0.34f, h * 0.44f))
            drawOval(tone, p(0.33f, 0.18f), Size(w * 0.34f, h * 0.44f), style = Stroke(s * 1.1f))
            drawLine(tone, p(0.5f, 0.62f), p(0.5f, 0.82f), s * 1.6f, StrokeCap.Round)
            drawCircle(tone.copy(alpha = 0.7f), w * 0.028f, p(0.44f, 0.34f))
            drawCircle(tone.copy(alpha = 0.7f), w * 0.028f, p(0.56f, 0.34f))
            drawCircle(tone.copy(alpha = 0.7f), w * 0.028f, p(0.5f, 0.45f))
        }
        1 -> { // мяч
            drawCircle(tone, w * 0.27f, p(0.5f, 0.5f))
            drawArc(ink, -30f, 60f, false, Offset(-w * 0.22f, h * 0.5f - w * 0.30f), Size(w * 0.60f, w * 0.60f), style = Stroke(s * 0.7f))
            drawArc(ink, 150f, 60f, false, Offset(w * 0.62f, h * 0.5f - w * 0.30f), Size(w * 0.60f, w * 0.60f), style = Stroke(s * 0.7f))
        }
        2 -> { // сетка
            val line = tone.copy(alpha = 0.55f)
            drawLine(line, p(0.36f, 0.32f), p(0.36f, 0.70f), s * 0.7f)
            drawLine(line, p(0.50f, 0.32f), p(0.50f, 0.70f), s * 0.7f)
            drawLine(line, p(0.64f, 0.32f), p(0.64f, 0.70f), s * 0.7f)
            drawLine(line, p(0.24f, 0.46f), p(0.76f, 0.46f), s * 0.7f)
            drawLine(line, p(0.24f, 0.60f), p(0.76f, 0.60f), s * 0.7f)
            drawLine(tone, p(0.22f, 0.30f), p(0.78f, 0.30f), s * 1.5f, StrokeCap.Round)
            drawLine(tone, p(0.26f, 0.30f), p(0.26f, 0.76f), s * 1.3f, StrokeCap.Round)
            drawLine(tone, p(0.74f, 0.30f), p(0.74f, 0.76f), s * 1.3f, StrokeCap.Round)
        }
        3 -> { // корт
            drawRoundRect(tone, p(0.26f, 0.22f), Size(w * 0.48f, h * 0.56f), cornerRadius = CornerRadius(w * 0.04f), style = Stroke(s))
            drawLine(tone, p(0.26f, 0.50f), p(0.74f, 0.50f), s)
            drawLine(tone.copy(alpha = 0.55f), p(0.50f, 0.50f), p(0.50f, 0.78f), s * 0.8f)
            drawCircle(tone, w * 0.05f, p(0.62f, 0.34f))
        }
        4 -> { // молния
            val path = Path().apply {
                moveTo(w * 0.57f, h * 0.16f)
                lineTo(w * 0.30f, h * 0.55f)
                lineTo(w * 0.47f, h * 0.55f)
                lineTo(w * 0.42f, h * 0.84f)
                lineTo(w * 0.70f, h * 0.43f)
                lineTo(w * 0.52f, h * 0.43f)
                close()
            }
            drawPath(path, tone)
        }
        5 -> { // щит
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.17f)
                lineTo(w * 0.74f, h * 0.27f)
                lineTo(w * 0.74f, h * 0.50f)
                cubicTo(w * 0.74f, h * 0.66f, w * 0.62f, h * 0.76f, w * 0.5f, h * 0.83f)
                cubicTo(w * 0.38f, h * 0.76f, w * 0.26f, h * 0.66f, w * 0.26f, h * 0.50f)
                lineTo(w * 0.26f, h * 0.27f)
                close()
            }
            drawPath(path, tone)
            val check = Path().apply {
                moveTo(w * 0.40f, h * 0.50f)
                lineTo(w * 0.48f, h * 0.58f)
                lineTo(w * 0.62f, h * 0.40f)
            }
            drawPath(check, ink, style = Stroke(s * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        6 -> { // шевроны
            val top = Path().apply { moveTo(w * 0.28f, h * 0.50f); lineTo(w * 0.5f, h * 0.30f); lineTo(w * 0.72f, h * 0.50f) }
            val low = Path().apply { moveTo(w * 0.28f, h * 0.70f); lineTo(w * 0.5f, h * 0.50f); lineTo(w * 0.72f, h * 0.70f) }
            drawPath(top, tone, style = Stroke(s * 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(low, tone.copy(alpha = 0.5f), style = Stroke(s * 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        7 -> { // мишень
            drawCircle(tone.copy(alpha = 0.45f), w * 0.32f, p(0.5f, 0.5f), style = Stroke(s * 0.9f))
            drawCircle(tone, w * 0.20f, p(0.5f, 0.5f), style = Stroke(s))
            drawCircle(tone, w * 0.07f, p(0.5f, 0.5f))
        }
        8 -> { // волна
            val wave = Path().apply {
                moveTo(w * 0.20f, h * 0.42f)
                cubicTo(w * 0.35f, h * 0.26f, w * 0.45f, h * 0.26f, w * 0.50f, h * 0.42f)
                cubicTo(w * 0.55f, h * 0.58f, w * 0.65f, h * 0.58f, w * 0.80f, h * 0.42f)
            }
            drawPath(wave, tone, style = Stroke(s * 1.3f, cap = StrokeCap.Round))
            val wave2 = Path().apply {
                moveTo(w * 0.20f, h * 0.62f)
                cubicTo(w * 0.35f, h * 0.46f, w * 0.45f, h * 0.46f, w * 0.50f, h * 0.62f)
                cubicTo(w * 0.55f, h * 0.78f, w * 0.65f, h * 0.78f, w * 0.80f, h * 0.62f)
            }
            drawPath(wave2, tone.copy(alpha = 0.5f), style = Stroke(s * 1.3f, cap = StrokeCap.Round))
        }
        9 -> { // ромб
            val outer = Path().apply {
                moveTo(w * 0.5f, h * 0.18f); lineTo(w * 0.78f, h * 0.5f); lineTo(w * 0.5f, h * 0.82f); lineTo(w * 0.22f, h * 0.5f); close()
            }
            val inner = Path().apply {
                moveTo(w * 0.5f, h * 0.33f); lineTo(w * 0.64f, h * 0.5f); lineTo(w * 0.5f, h * 0.67f); lineTo(w * 0.36f, h * 0.5f); close()
            }
            drawPath(outer, tone)
            drawPath(inner, ink)
            drawCircle(tone, w * 0.04f, p(0.5f, 0.5f))
        }
        10 -> { // искра
            val star = Path().apply {
                moveTo(w * 0.5f, h * 0.15f)
                quadraticTo(w * 0.5f, h * 0.5f, w * 0.85f, h * 0.5f)
                quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h * 0.85f)
                quadraticTo(w * 0.5f, h * 0.5f, w * 0.15f, h * 0.5f)
                quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h * 0.15f)
                close()
            }
            drawPath(star, tone)
        }
        else -> { // корона
            val crown = Path().apply {
                moveTo(w * 0.24f, h * 0.66f)
                lineTo(w * 0.28f, h * 0.36f)
                lineTo(w * 0.42f, h * 0.52f)
                lineTo(w * 0.5f, h * 0.30f)
                lineTo(w * 0.58f, h * 0.52f)
                lineTo(w * 0.72f, h * 0.36f)
                lineTo(w * 0.76f, h * 0.66f)
                close()
            }
            drawPath(crown, tone)
            drawLine(tone, p(0.26f, 0.75f), p(0.74f, 0.75f), s * 1.4f, StrokeCap.Round)
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
