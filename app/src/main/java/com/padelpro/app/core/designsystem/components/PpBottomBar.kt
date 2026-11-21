package com.padelpro.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpCourtDeep
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextStyles

enum class BottomTab(val labelRes: Int, val icon: ImageVector, val testTag: String) {
    HOME(R.string.nav_home, Icons.Rounded.Home, "nav_tab_home"),
    ACADEMY(R.string.nav_academy, Icons.Rounded.School, "nav_tab_academy"),
    PLAY(R.string.nav_play, Icons.Rounded.SportsTennis, "nav_tab_play"),
    STATS(R.string.nav_stats, Icons.Rounded.BarChart, "nav_tab_stats")
}

/**
 * Нижняя навигация, «пристыкованная» к краю экрана: сплошной фон, тонкая линия сверху.
 * Раньше это была плавающая стеклянная таблетка, под которой просвечивал контент.
 * Активная вкладка — мягкая лаймовая «капсула» вокруг иконки, подпись без капса.
 */
@Composable
fun PpBottomBar(
    selectedTab: BottomTab,
    onSelectTab: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = LocalPpMotion.current
    Column(
        modifier
            .fillMaxWidth()
            .background(PpCourtDeep)
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(PpCourtLine))
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab.entries.forEach { tab ->
                val active = tab == selectedTab
                val glow by animateFloatAsState(
                    if (active) 1f else 0f,
                    animationSpec = if (motion.enabled) motion.glide() else snap(),
                    label = "pp-tab"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .height(60.dp)
                        .testTag(tab.testTag)
                        .clickableNoRipple { onSelectTab(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier
                            .width(58.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .drawBehind { drawRect(PpLime.copy(alpha = 0.16f * glow)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            tab.icon, stringResource(tab.labelRes),
                            tint = if (active) PpLime else PpTextLow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        stringResource(tab.labelRes),
                        style = PpTextStyles.Micro.copy(fontSize = 11.sp, lineHeight = 14.sp),
                        color = if (active) PpTextHigh else PpTextLow,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
