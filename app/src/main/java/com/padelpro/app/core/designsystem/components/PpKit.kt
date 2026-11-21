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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpBallTrail
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun PpCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .glassPanel()
            .then(if (accent != null) Modifier.accentRail(accent) else Modifier)
            .padding(PpSpacing.lg),
        content = content
    )
}

@Composable
fun PpStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = PpLime
) {
    Column(
        modifier
            .glassPanel(radius = 16.dp)
            .padding(PpSpacing.md)
    ) {
        Canvas(Modifier.fillMaxWidth().height(2.dp)) {
            drawRect(Brush.horizontalGradient(listOf(accent, Color.Transparent)))
        }
        Spacer(Modifier.height(PpSpacing.sm))
        Text(value, style = PpTextStyles.Numeral, color = accent, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            label.uppercase(), style = PpTextStyles.Overline, color = PpTextMid,
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
        modifier.fillMaxWidth().padding(vertical = PpSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(6.dp)) { drawCircle(PpLime) }
            Spacer(Modifier.size(PpSpacing.sm))
            Text(title.uppercase(), style = PpTextStyles.Micro, color = PpTextMid)
        }
        if (actionLabel != null && onActionClick != null) {
            Text(
                actionLabel.uppercase(), style = PpTextStyles.Micro, color = PpGlassCyan,
                modifier = Modifier.clickableNoRipple(onActionClick).padding(PpSpacing.xs)
            )
        }
    }
}

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
                fill = if (selected) Brush.horizontalGradient(listOf(Color(0x33C8F751), Color(0x1A5FE3FF)))
                else Brush.verticalGradient(listOf(PpCourtGlass, PpCourtGlass)),
                edge = if (selected) Brush.linearGradient(listOf(PpLime, PpGlassCyan))
                else Brush.linearGradient(listOf(PpCourtLine, PpCourtLine))
            )
            .clickableNoRipple(onClick)
            .padding(horizontal = PpSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) { leadingContent(); Spacer(Modifier.size(6.dp)) }
        Text(
            label.uppercase(), style = PpTextStyles.Micro,
            color = if (selected) PpTextHigh else PpTextMid, maxLines = 1
        )
    }
}

@Composable
fun PpProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    brush: Brush = PpBallTrail,
    height: Dp = 8.dp
) {
    val motion = LocalPpMotion.current
    val p by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        animationSpec = if (motion.enabled) motion.glide() else snap(),
        label = "pp-progress"
    )
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = size.height / 2
        drawRoundRect(Color(0x1F2C5566), cornerRadius = CornerRadius(r))
        if (p > 0f) {
            drawRoundRect(brush, size = Size(size.width * p, size.height), cornerRadius = CornerRadius(r))
            drawCircle(Color(0xFFE4FF8A), r * 0.9f, Offset(size.width * p, r))
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
    Box(modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(132.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color(0x1F2C5566), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(PpGlassCyan, PpLime, PpAmberLed, PpGlassCyan)),
                -90f, 360f * p, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(center, style = PpTextStyles.Numeral, color = PpTextHigh)
            if (caption.isNotEmpty()) Text(caption.uppercase(), style = PpTextStyles.Overline, color = PpTextMid)
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
        modifier.fillMaxWidth().glassPanel().padding(PpSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(64.dp)) {
                drawCircle(Brush.radialGradient(listOf(Color(0x335FE3FF), Color.Transparent)))
                drawCircle(Color(0x332C5566), style = Stroke(1f))
            }
            Icon(icon, null, tint = PpGlassCyan, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(PpSpacing.md))
        Text(title, style = PpTextStyles.Title, color = PpTextHigh)
        Spacer(Modifier.height(PpSpacing.xs))
        Text(subtitle, style = PpTextStyles.Body, color = PpTextLow)
    }
}