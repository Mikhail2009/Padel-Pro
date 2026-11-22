package com.padelpro.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpBallTrail
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpOnAccent
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun PpCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    motif: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .glassPanel()
            .then(if (motif) Modifier.courtMotif() else Modifier)
            .then(if (accent != null) Modifier.accentRail(accent) else Modifier)
            .padding(PpSpacing.lg + 2.dp),
        content = content
    )
}

/**
 * Метрика: крупное число и спокойная подпись. Цвет-смысл передаётся маленькой точкой,
 * а не заливкой числа — три разноцветных числа подряд выглядят как «радуга».
 */
@Composable
fun PpStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = PpLime
) {
    Column(
        modifier
            .glassPanel(radius = 18.dp)
            .padding(horizontal = PpSpacing.md + 2.dp, vertical = PpSpacing.md + 2.dp)
    ) {
        Canvas(Modifier.size(6.dp)) { drawCircle(accent) }
        Spacer(Modifier.height(PpSpacing.md))
        Text(value, style = PpTextStyles.Numeral, color = PpTextHigh, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            label, style = PpTextStyles.Label, color = PpTextMid,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PpSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().padding(top = PpSpacing.xs, bottom = PpSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title, style = PpTextStyles.Title, color = PpTextHigh,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                actionLabel, style = PpTextStyles.Label, color = PpLime,
                modifier = Modifier.clickableNoRipple(onActionClick).padding(PpSpacing.xs)
            )
        }
    }
}

/** Фильтр-чип: выбранный — светлая заливка с тёмным текстом (лайм остаётся для главных действий). */
@Composable
fun PpFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .height(40.dp)
            .glassPanel(
                radius = 20.dp,
                fill = SolidColor(if (selected) PpTextHigh else PpCourtGlass),
                edge = SolidColor(if (selected) PpTextHigh else PpCourtLine)
            )
            .clickableNoRipple(onClick)
            .padding(horizontal = PpSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) { leadingContent(); Spacer(Modifier.size(6.dp)) }
        Text(
            label, style = PpTextStyles.Label,
            color = if (selected) PpOnAccent else PpTextMid, maxLines = 1
        )
    }
}

@Composable
fun PpProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    brush: Brush = PpBallTrail,
    height: Dp = 6.dp
) {
    val motion = LocalPpMotion.current
    val p by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        animationSpec = if (motion.enabled) motion.glide() else snap(),
        label = "pp-progress"
    )
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = size.height / 2
        drawRoundRect(PpCourtLine, cornerRadius = CornerRadius(r))
        if (p > 0f) {
            // Минимальная ширина = высота, чтобы малый прогресс не превращался в точку-артефакт.
            val w = (size.width * p).coerceAtLeast(size.height)
            drawRoundRect(brush, size = Size(w, size.height), cornerRadius = CornerRadius(r))
        }
    }
}

@Composable
fun PpXpRing(
    progress: Float,
    modifier: Modifier = Modifier,
    center: String = "",
    caption: String = ""
) {
    val motion = LocalPpMotion.current
    val p by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        animationSpec = if (motion.enabled) motion.glide() else snap(),
        label = "pp-ring"
    )
    Box(modifier.size(124.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(124.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(PpCourtLine, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (p > 0f) {
                drawArc(
                    PpLime,
                    -90f, 360f * p, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                center,
                style = PpTextStyles.DisplayNum.copy(fontSize = 32.sp, lineHeight = 36.sp),
                color = PpTextHigh,
                maxLines = 1
            )
            if (caption.isNotEmpty()) {
                Text(caption, style = PpTextStyles.Label, color = PpTextMid, maxLines = 1)
            }
        }
    }
}

@Composable
fun PpEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().glassPanel().padding(horizontal = PpSpacing.xl, vertical = PpSpacing.xl + 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(56.dp)
                .glassPanel(radius = 28.dp, fill = SolidColor(PpCourtGlass), edge = SolidColor(PpCourtLineStrong)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = PpLime, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(PpSpacing.lg))
        Text(title, style = PpTextStyles.Title, color = PpTextHigh, textAlign = TextAlign.Center)
        Spacer(Modifier.height(PpSpacing.xs))
        Text(subtitle, style = PpTextStyles.Body, color = PpTextMid, textAlign = TextAlign.Center)
    }
}
