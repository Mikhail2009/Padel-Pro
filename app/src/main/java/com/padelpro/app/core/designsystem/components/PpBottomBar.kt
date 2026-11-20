package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpSurface
import com.padelpro.app.core.designsystem.theme.PpTextStyles

enum class BottomTab(
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PpSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PpOutline)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = PpSpacing.sm, vertical = PpSpacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                val label = stringResource(tab.labelRes)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(PpRadius.pill)
                        .clickable { onSelectTab(tab) }
                        .testTag(tab.testTag)
                        .padding(vertical = PpSpacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(PpSpacing.xs))
                    Text(
                        text = label,
                        style = PpTextStyles.Micro.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = tint,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
