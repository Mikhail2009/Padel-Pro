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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import com.padelpro.app.core.designsystem.theme.PpCourtDeep
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpPanelFill
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpecularEdge
import com.padelpro.app.core.designsystem.theme.PpTextLow

/**
 * Фон приложения. Один мягкий источник света сверху и больше ничего:
 * без зерна, без разметки корта, без «софитов» — тишина фона делает контент дороже.
 * Параметры [floodlight] и [courtLines] оставлены для совместимости вызовов.
 */
@Composable
fun PpNightCourt(
    modifier: Modifier = Modifier,
    floodlight: Boolean = true,
    @Suppress("UNUSED_PARAMETER") courtLines: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(PpCourtDeep)
            if (floodlight) {
                // Едва заметное лаймовое свечение в верхнем левом углу (≈5%).
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color(0x14C2F54C), Color.Transparent),
                        center = Offset(size.width * 0.05f, -size.height * 0.04f),
                        radius = size.maxDimension * 0.62f
                    )
                )
            }
        }
        content()
    }
}

/** Оставлено для совместимости: зерно убрано как визуальный шум. */
@Composable
fun PpGrain(modifier: Modifier = Modifier, @Suppress("UNUSED_PARAMETER") alpha: Float = 0f) {
    Box(modifier)
}

/**
 * Базовая поверхность: плотная заливка + тонкая кромка. Имя сохранено ради существующих вызовов.
 * Без внутренних бликов и стеклянных градиентов.
 */
fun Modifier.glassPanel(
    radius: Dp = PpRadius.card,
    fill: Brush = PpPanelFill,
    edge: Brush = PpSpecularEdge,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(RoundedCornerShape(radius))
    .drawWithCache {
        val r = radius.toPx()
        val bw = borderWidth.toPx()
        onDrawBehind {
            drawRoundRect(fill, cornerRadius = CornerRadius(r, r))
            // Кромка рисуется внутрь, чтобы не обрезаться клипом.
            drawRoundRect(
                edge,
                topLeft = Offset(bw / 2, bw / 2),
                size = Size(size.width - bw, size.height - bw),
                cornerRadius = CornerRadius(r - bw / 2, r - bw / 2),
                style = Stroke(width = bw)
            )
        }
    }

/**
 * Раньше рисовал цветную метку слева на карточке — шаблонный приём, от него отказались.
 * Имя и сигнатура оставлены, чтобы не трогать вызовы; метод ничего не рисует.
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.accentRail(color: Color = PpLime, width: Dp = 3.dp): Modifier = this

/**
 * Едва заметный рисунок корта в правом нижнем углу карточки — фирменный штрих для «героев»
 * экрана (уровень, новая игра, статистика). Рисуется тонкой линией, без заливок.
 */
fun Modifier.courtMotif(color: Color = PpLime, alpha: Float = 0.09f): Modifier = this.drawBehind {
    val u = 1.dp.toPx()
    val w = size.width * 0.46f
    val h = w * 0.62f
    val left = size.width - w - 14 * u
    val top = size.height - h - 14 * u
    if (left < 0f || top < 0f) return@drawBehind
    val c = color.copy(alpha = alpha)
    drawRoundRect(c, Offset(left, top), Size(w, h), CornerRadius(6 * u, 6 * u), Stroke(1.2f * u))
    drawLine(c, Offset(left + w / 2, top - 4 * u), Offset(left + w / 2, top + h + 4 * u), 1.2f * u)
    drawLine(c, Offset(left + w * 0.2f, top), Offset(left + w * 0.2f, top + h), 1f * u)
    drawLine(c, Offset(left + w * 0.8f, top), Offset(left + w * 0.8f, top + h), 1f * u)
    drawLine(c, Offset(left + w * 0.2f, top + h / 2), Offset(left + w * 0.8f, top + h / 2), 1f * u)
}

/** Вертикальный скролл с тонким индикатором. */
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
                drawRoundRect(PpCourtLine.copy(alpha = 0.5f), cornerRadius = CornerRadius(2.dp.toPx()))
                drawRoundRect(
                    PpTextLow.copy(alpha = 0.7f),
                    topLeft = Offset(0f, (track - thumbH) * ratio),
                    size = Size(size.width, thumbH),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}

/** Траектория мяча — декоративный разделитель секций: тонкая линия, гаснущая слева. */
@Composable
fun PpBallTrailDivider(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        val p = Path().apply {
            moveTo(0f, size.height)
            quadraticBezierTo(size.width * 0.45f, -size.height * 0.8f, size.width, size.height * 0.4f)
        }
        drawPath(p, Brush.horizontalGradient(listOf(Color.Transparent, PpLime.copy(alpha = 0.7f))), style = Stroke(1.4f))
        drawCircle(PpLime, 3f, Offset(size.width, size.height * 0.4f))
    }
}
