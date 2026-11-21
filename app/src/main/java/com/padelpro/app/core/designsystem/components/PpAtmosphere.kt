package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.padelpro.app.core.designsystem.theme.PpCourtBlue
import com.padelpro.app.core.designsystem.theme.PpCourtDeep
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpecularEdge
import kotlin.random.Random

/** Фон «ночной корт»: софиты, разметка, зерно. Ставится один раз на экран. */
@Composable
fun PpNightCourt(
    modifier: Modifier = Modifier,
    floodlight: Boolean = true,
    courtLines: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(PpCourtDeep)
            if (floodlight) {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color(0x335FE3FF), Color(0x141E6FE8), Color.Transparent),
                        center = Offset(size.width * 0.18f, -size.height * 0.06f),
                        radius = size.maxDimension * 0.72f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color(0x26C8F751), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.04f),
                        radius = size.maxDimension * 0.5f
                    )
                )
            }
            if (courtLines) {
                val w = size.width * 0.86f
                val h = w * 0.5f
                val left = (size.width - w) / 2f
                val top = size.height - h * 0.62f
                val line = Color(0x142C5566)
                val s = Stroke(width = 1.4f)
                drawRect(line, Offset(left, top), Size(w, h), s)
                drawLine(line, Offset(left, top + h / 2), Offset(left + w, top + h / 2), 1.4f)
                drawLine(line, Offset(left + w * 0.30f, top), Offset(left + w * 0.30f, top + h), 1.4f)
                drawLine(line, Offset(left + w * 0.70f, top), Offset(left + w * 0.70f, top + h), 1.4f)
                drawLine(Color(0x1FC8F751), Offset(left + w / 2, top), Offset(left + w / 2, top + h), 2f)
            }
        }
        PpGrain(Modifier.fillMaxSize())
        content()
    }
}

/** Детерминированное зерно — кэшируется, не пересчитывается на кадр. */
@Composable
fun PpGrain(modifier: Modifier = Modifier, alpha: Float = 0.035f) {
    Canvas(modifier.drawWithCache {
        val rnd = Random(20260101)
        val pts = List(700) { Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height) }
        val r = 0.9f
        onDrawBehind { pts.forEach { drawCircle(Color.White, r, it, alpha = alpha) } }
    }) {}
}

/** Стеклянная панель: слой блика + спекулярная кромка. */
fun Modifier.glassPanel(
    radius: Dp = PpRadius.card,
    fill: Brush = Brush.verticalGradient(listOf(PpCourtGlass, PpCourtDeep)),
    edge: Brush = PpSpecularEdge,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(androidx.compose.foundation.shape.RoundedCornerShape(radius))
    .drawWithCache {
        val r = radius.toPx()
        val bw = borderWidth.toPx()
        onDrawBehind {
            drawRoundRect(fill, cornerRadius = CornerRadius(r, r))
            drawRoundRect(
                edge, cornerRadius = CornerRadius(r, r),
                style = Stroke(width = bw)
            )
        }
    }

/** Акцентная кромка слева — для «главного» блока экрана. */
fun Modifier.accentRail(color: Color = PpLime, width: Dp = 3.dp): Modifier = this.drawWithCache {
    val w = width.toPx()
    onDrawBehind { drawRect(color, Offset.Zero, Size(w, size.height)) }
}

/** Вертикальный скролл с индикатором в палитре приложения. */
@Composable
fun PpScrollColumn(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier) {
        Column(Modifier.fillMaxWidth().verticalScroll(state), content = content)
        if (state.maxValue > 0) {
            Canvas(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(3.dp)
                    .fillMaxSize()
                    .padding(vertical = 6.dp)
            ) {
                val track = size.height
                val ratio = state.value.toFloat() / state.maxValue.coerceAtLeast(1)
                val thumbH = (track * (track / (track + state.maxValue))).coerceAtLeast(28.dp.toPx())
                drawRoundRect(Color(0x1F2C5566), cornerRadius = CornerRadius(2.dp.toPx()))
                drawRoundRect(
                    Brush.verticalGradient(listOf(PpGlassCyan, PpLime)),
                    topLeft = Offset(0f, (track - thumbH) * ratio),
                    size = Size(size.width, thumbH),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}

/** Траектория мяча — декоративный разделитель секций. */
@Composable
fun PpBallTrailDivider(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        val p = Path().apply {
            moveTo(0f, size.height)
            quadraticBezierTo(size.width * 0.45f, -size.height * 0.8f, size.width, size.height * 0.4f)
        }
        drawPath(p, Brush.horizontalGradient(listOf(Color.Transparent, PpCourtBlue, PpGlassCyan, PpLime)), style = Stroke(1.6f))
        drawCircle(PpLime, 3.2f, Offset(size.width, size.height * 0.4f))
    }
}