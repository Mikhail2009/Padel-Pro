package com.padelpro.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextStyles

enum class BottomTab(val labelRes: Int, val icon: ImageVector, val testTag: String) {
    HOME(R.string.nav_home, Icons.Rounded.Home, "nav_tab_home"),
    ACADEMY(R.string.nav_academy, Icons.Rounded.School, "nav_tab_academy"),
    PLAY(R.string.nav_play, Icons.Rounded.SportsTennis, "nav_tab_play"),
    STATS(R.string.nav_stats, Icons.Rounded.BarChart, "nav_tab_stats")
}

@Composable
fun PpBottomBar(
    selectedTab: BottomTab,
    onSelectTab: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = LocalPpMotion.current
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = PpSpacing.md, vertical = PpSpacing.sm)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .glassPanel(radius = 26.dp)
                .padding(vertical = 6.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab.entries.forEach { tab ->
                val active = tab == selectedTab
                val glow by animateFloatAsState(
                    if (active) 1f else 0f,
                    animationSpec = if (motion.enabled) motion.glide() else androidx.compose.animation.core.snap(),
                    label = "pp-tab"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .height(58.dp)
                        .testTag(tab.testTag)
                        .drawBehind {
                            if (glow > 0.01f) {
                                drawRoundRect(
                                    Brush.verticalGradient(listOf(Color(0x33C8F751), Color(0x0FC8F751))),
                                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                                    size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                                    cornerRadius = CornerRadius(20.dp.toPx()),
                                    alpha = glow
                                )
                                drawRoundRect(
                                    Brush.horizontalGradient(listOf(PpGlassCyan, PpLime)),
                                    topLeft = Offset(size.width * 0.3f, 1.dp.toPx()),
                                    size = Size(size.width * 0.4f, 2.dp.toPx()),
                                    cornerRadius = CornerRadius(1.dp.toPx()),
                                    alpha = glow
                                )
                            }
                        }
                        .clickableNoRipple { onSelectTab(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        tab.icon, stringResource(tab.labelRes),
                        tint = if (active) PpLime else PpTextLow,
                        modifier = Modifier.size(if (active) 24.dp else 22.dp)
                    )
                    Text(
                        stringResource(tab.labelRes).uppercase(),
                        style = PpTextStyles.Overline,
                        color = if (active) PpTextHigh else PpTextLow,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}