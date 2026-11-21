package com.padelpro.app.feature.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.padelpro.app.core.common.LockLandscape
import com.padelpro.app.core.designsystem.components.PpCourtBackdrop
import com.padelpro.app.core.designsystem.components.clickableNoRipple
import com.padelpro.app.core.designsystem.components.glassPanel
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles

data class LiveSideUi(
    val teamLabel: String,
    val players: String,
    val score: String,
    val games: Int,
    val setsWon: Int,
    val serving: Boolean,
    val serverName: String?,
    val setPoint: Boolean,
    val matchPoint: Boolean,
    val accent: Color
)

data class LiveRailUi(
    val timer: String,
    val paused: Boolean,
    val canUndo: Boolean,
    val busy: Boolean,
    val setLabel: String,
    val formatLabel: String,
    val deuceLabel: String,
    val banner: String?,
    val autoSwap: Boolean,
    val swapped: Boolean,
    val sets: List<String>
)

@Composable
fun LiveMatchLandscape(
    left: LiveSideUi,
    right: LiveSideUi,
    rail: LiveRailUi,
    onLeftTap: () -> Unit,
    onRightTap: () -> Unit,
    onUndo: () -> Unit,
    onPauseToggle: () -> Unit,
    onSwapSides: () -> Unit,
    onSettings: () -> Unit,
    onFinish: () -> Unit
) {
    LockLandscape()
    val haptics = LocalHapticFeedback.current

    Box(Modifier.fillMaxSize()) {
        PpCourtBackdrop()
        Row(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(PpSpacing.sm)
        ) {
            SideHalf(
                ui = left,
                enabled = !rail.busy && !rail.paused,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onTap = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onLeftTap() }
            )
            CenterRail(
                rail = rail,
                onUndo = onUndo,
                onPauseToggle = onPauseToggle,
                onSwapSides = onSwapSides,
                onSettings = onSettings,
                onFinish = onFinish,
                modifier = Modifier.width(118.dp).fillMaxHeight().padding(horizontal = PpSpacing.sm)
            )
            SideHalf(
                ui = right,
                enabled = !rail.busy && !rail.paused,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onTap = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onRightTap() }
            )
        }
    }
}

@Composable
private fun SideHalf(
    ui: LiveSideUi,
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .glassPanel(
                radius = 24.dp,
                fill = Brush.verticalGradient(
                    listOf(ui.accent.copy(alpha = if (ui.serving) 0.14f else 0.06f), Color(0x0A071016))
                ),
                edge = Brush.linearGradient(
                    listOf(ui.accent.copy(alpha = if (ui.serving) 0.75f else 0.25f), Color(0x142C5566))
                )
            )
            .then(if (enabled) Modifier.clickableNoRipple(onTap) else Modifier)
            .padding(PpSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ui.serving) {
                    Box(Modifier.size(9.dp).drawBehind { drawCircle(ui.accent) })
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    ui.teamLabel.uppercase(), style = PpTextStyles.Title, color = PpTextHigh,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (ui.players.isNotBlank()) {
                Text(
                    ui.players, style = PpTextStyles.Label, color = PpTextMid,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(PpSpacing.sm))
            Text(
                ui.score,
                style = PpTextStyles.ScoreDisplay.copy(fontSize = 92.sp, lineHeight = 94.sp),
                color = if (ui.serving) PpAmberLed else PpTextHigh,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(PpSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
                MiniChip("ГЕЙМ ${ui.games}", ui.accent)
                MiniChip("СЕТЫ ${ui.setsWon}", PpGlassCyan)
            }
            if (ui.serving && !ui.serverName.isNullOrBlank()) {
                Text(
                    "ПОДАЁТ · ${ui.serverName.uppercase()}",
                    style = PpTextStyles.Overline, color = PpAmberLed,
                    modifier = Modifier.padding(top = 6.dp), maxLines = 1
                )
            }
        }
        val flag = when {
            ui.matchPoint -> "МАТЧ-БОЛ"
            ui.setPoint -> "СЕТ-БОЛ"
            else -> null
        }
        if (flag != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).glassPanel(
                    radius = 14.dp,
                    fill = Brush.horizontalGradient(listOf(ui.accent.copy(alpha = 0.28f), Color(0x1A5FE3FF)))
                ).padding(horizontal = 14.dp, vertical = 6.dp)
            ) { Text(flag, style = PpTextStyles.Micro, color = PpTextHigh) }
        }
        Text(
            "+ ОЧКО", style = PpTextStyles.Overline,
            color = if (enabled) ui.accent.copy(alpha = 0.7f) else PpTextLow,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun MiniChip(text: String, accent: Color) {
    Box(
        Modifier.glassPanel(radius = 12.dp, fill = Brush.verticalGradient(listOf(PpCourtGlass, Color(0x0A071016))))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) { Text(text, style = PpTextStyles.Overline, color = accent) }
}

@Composable
private fun CenterRail(
    rail: LiveRailUi,
    onUndo: () -> Unit,
    onPauseToggle: () -> Unit,
    onSwapSides: () -> Unit,
    onSettings: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(rail.timer, style = PpTextStyles.Numeral, color = if (rail.paused) PpAmberLed else PpTextHigh)
            Text(rail.setLabel.uppercase(), style = PpTextStyles.Overline, color = PpTextMid, maxLines = 1)
            Text(rail.formatLabel.uppercase(), style = PpTextStyles.Overline, color = PpTextLow, maxLines = 1)
            if (rail.sets.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                rail.sets.takeLast(3).forEach { s ->
                    Text(s, style = PpTextStyles.Overline, color = PpGlassCyan, maxLines = 1)
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (rail.banner != null) {
                Box(
                    Modifier.fillMaxWidth().glassPanel(
                        radius = 12.dp,
                        fill = Brush.horizontalGradient(listOf(Color(0x33C8F751), Color(0x1A5FE3FF)))
                    ).padding(vertical = 6.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        rail.banner.uppercase(), style = PpTextStyles.Overline, color = PpTextHigh,
                        textAlign = TextAlign.Center, maxLines = 2
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            RailButton(
                icon = if (rail.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                label = if (rail.paused) "ИГРАТЬ" else "ПАУЗА",
                accent = if (rail.paused) PpAmberLed else PpGlassCyan,
                enabled = !rail.busy,
                onClick = onPauseToggle
            )
            RailButton(
                icon = Icons.Rounded.Undo, label = "ОТМЕНИТЬ", accent = PpTextMid,
                enabled = rail.canUndo && !rail.busy && !rail.paused, onClick = onUndo
            )
            RailButton(
                icon = Icons.Rounded.SwapHoriz,
                label = if (rail.autoSwap) "АВТО" else "СТОРОНЫ",
                accent = if (rail.swapped) PpLime else PpGlassCyan,
                enabled = !rail.busy,
                onClick = onSwapSides
            )
            RailButton(
                icon = Icons.Rounded.Settings, label = "НАСТРОЙКИ", accent = PpTextMid,
                enabled = !rail.busy, onClick = onSettings
            )
            RailButton(
                icon = Icons.Rounded.Flag, label = "ЗАВЕРШИТЬ", accent = PpLime,
                enabled = !rail.busy, onClick = onFinish
            )
        }

        Text(rail.deuceLabel.uppercase(), style = PpTextStyles.Overline, color = PpTextLow, maxLines = 2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RailButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .glassPanel(radius = 14.dp)
            .then(if (enabled) Modifier.clickableNoRipple(onClick) else Modifier)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (enabled) accent else PpTextLow, modifier = Modifier.size(20.dp))
        Text(
            label, style = PpTextStyles.Overline,
            color = if (enabled) PpTextMid else PpTextLow, maxLines = 1
        )
    }
}