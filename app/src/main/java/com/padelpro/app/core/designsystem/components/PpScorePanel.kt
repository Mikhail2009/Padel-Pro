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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLine
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
        pop.snapTo(0.94f)
        pop.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(196.dp)
            .glassPanel(
                radius = 24.dp,
                edge = if (serving) SolidColor(accent.copy(alpha = 0.6f)) else com.padelpro.app.core.designsystem.theme.PpSpecularEdge
            )
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
                Text(teamLabel, style = PpTextStyles.Label, color = PpTextMid, maxLines = 1)
            }
            Text(
                score,
                style = PpTextStyles.ScoreDisplay,
                color = PpTextHigh,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            )
            if (banner != null) {
                Text(
                    banner, style = PpTextStyles.Label, color = accent,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PpSetStrip(sets: List<String>, modifier: Modifier = Modifier, currentSet: String? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
        sets.forEach { s ->
            Box(
                Modifier
                    .glassPanel(radius = 12.dp, fill = SolidColor(PpCourtGlass), edge = SolidColor(PpCourtLine))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(s, style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"), color = PpTextMid)
            }
        }
        if (currentSet != null) {
            Box(
                Modifier
                    .glassPanel(radius = 12.dp, fill = SolidColor(PpLime.copy(alpha = 0.14f)), edge = SolidColor(PpLime.copy(alpha = 0.4f)))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text(currentSet, style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"), color = PpLime) }
        }
    }
}
