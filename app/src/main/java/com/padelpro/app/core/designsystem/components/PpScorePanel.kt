package com.padelpro.app.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun PpScoreHalf(
    teamLabel: String,
    score: String,
    serving: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = PpLime,
    banner: String? = null
) {
    val motion = LocalPpMotion.current
    val pop = remember { Animatable(1f) }
    LaunchedEffect(score) {
        if (!motion.enabled) { pop.snapTo(1f); return@LaunchedEffect }
        pop.snapTo(1.14f)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(196.dp)
            .glassPanel(radius = 24.dp)
            .drawBehind {
                if (serving) {
                    val cr = CornerRadius(24.dp.toPx())
                    drawRoundRect(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)), cornerRadius = cr)
                    drawRoundRect(accent.copy(alpha = 0.55f), cornerRadius = cr, style = Stroke(1.6f))
                }
            }
            .then(if (enabled) Modifier.clickableNoRipple(onTap) else Modifier)
            .padding(PpSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (serving) {
                    Box(Modifier.size(8.dp).drawBehind { drawCircle(accent) })
                    Spacer(Modifier.size(6.dp))
                }
                Text(teamLabel.uppercase(), style = PpTextStyles.Micro, color = PpTextMid, maxLines = 1)
            }
            Text(
                score,
                style = PpTextStyles.ScoreDisplay,
                color = if (serving) PpAmberLed else PpTextHigh,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            )
            if (banner != null) {
                Text(
                    banner.uppercase(), style = PpTextStyles.Overline, color = PpGlassCyan,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Text(
            "+", style = PpTextStyles.Numeral, color = accent.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun PpSetStrip(sets: List<String>, modifier: Modifier = Modifier, currentSet: String? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
        sets.forEach { s ->
            Box(Modifier.glassPanel(radius = 12.dp).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(s, style = PpTextStyles.Label, color = PpTextMid)
            }
        }
        if (currentSet != null) {
            Box(
                Modifier
                    .glassPanel(radius = 12.dp, fill = Brush.horizontalGradient(listOf(Color(0x33C8F751), Color(0x1A5FE3FF))))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text(currentSet, style = PpTextStyles.Label, color = PpLime) }
        }
    }
}