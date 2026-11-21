package com.padelpro.app.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpTrophyMagenta
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun PpWinDonut(fraction: Float, centerLabel: String, modifier: Modifier = Modifier) {
    val motion = LocalPpMotion.current
    val f by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        animationSpec = if (motion.enabled) motion.glide() else snap(),
        label = "pp-donut"
    )
    Box(modifier.size(148.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(148.dp)) {
            val st = 12.dp.toPx()
            val inset = st / 2
            val arc = Size(size.width - st, size.height - st)
            drawArc(PpCourtLine, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(st, cap = StrokeCap.Round))
            if (f > 0f) {
                drawArc(
                    PpLime,
                    -90f, 360f * f, false, Offset(inset, inset), arc, style = Stroke(st, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, style = PpTextStyles.DisplayNum, color = PpTextHigh)
            Text("Побед", style = PpTextStyles.Label, color = PpTextMid)
        }
    }
}

@Composable
fun PpActivityBars(labels: List<String>, values: List<Int>, modifier: Modifier = Modifier) {
    val motion = LocalPpMotion.current
    val grow = remember { Animatable(0f) }
    LaunchedEffect(values) {
        if (motion.enabled) grow.animateTo(1f, motion.glide()) else grow.snapTo(1f)
    }
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(132.dp)) {
            val n = values.size.coerceAtLeast(1)
            val gap = 10.dp.toPx()
            val bw = ((size.width - gap * (n - 1)) / n).coerceAtLeast(6.dp.toPx())
            val base = size.height - 18.dp.toPx()
            drawLine(PpCourtLine, Offset(0f, base), Offset(size.width, base), 1f)
            values.forEachIndexed { i, v ->
                val h = (base - 8.dp.toPx()) * (v.toFloat() / max) * grow.value
                // Один цвет: максимум — лайм, остальные — нейтральные. Радуга из 6 цветов мешает читать данные.
                val c = if (v == max && v > 0) PpLime else PpCourtLineStrong
                drawRoundRect(
                    c,
                    topLeft = Offset(i * (bw + gap), base - h),
                    size = Size(bw, h.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, l ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    val v = values.getOrNull(i) ?: 0
                    Text(
                        v.toString(), style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                        color = if (v == max && v > 0) PpTextHigh else PpTextMid
                    )
                    Text(l, style = PpTextStyles.Micro.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = PpTextLow, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun PpFormChips(results: List<Boolean>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
        results.forEach { win ->
            val c = if (win) PpLime else PpTrophyMagenta
            Box(
                Modifier.size(36.dp).glassPanel(
                    radius = 18.dp,
                    fill = SolidColor(c.copy(alpha = 0.14f)),
                    edge = SolidColor(c.copy(alpha = 0.35f))
                ),
                contentAlignment = Alignment.Center
            ) { Text(if (win) "В" else "П", style = PpTextStyles.Label, color = c) }
        }
    }
}