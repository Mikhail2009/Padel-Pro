package com.padelpro.app.feature.match

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.padelpro.app.core.common.LockLandscape
import com.padelpro.app.core.designsystem.components.PpCourtBackdrop
import com.padelpro.app.core.designsystem.components.clickableNoRipple
import com.padelpro.app.core.designsystem.components.glassPanel
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
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
    val servingLeft = when { left.serving -> true; right.serving -> false; else -> null }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 700.dp
        val railWidth = if (compact) 96.dp else 118.dp
        val gap = if (compact) 4.dp else PpSpacing.sm

        PpCourtBackdrop(servingLeft = servingLeft, dimmed = rail.paused)

        Row(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(gap)
        ) {
            SideHalf(
                ui = left,
                enabled = !rail.busy && !rail.paused,
                paused = rail.paused,
                compact = compact,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onLeftTap()
                }
            )
            CenterRail(
                rail = rail,
                compact = compact,
                onUndo = onUndo,
                onPauseToggle = onPauseToggle,
                onSwapSides = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSwapSides()
                },
                onSettings = onSettings,
                onFinish = onFinish,
                modifier = Modifier.width(railWidth).fillMaxHeight().padding(horizontal = gap)
            )
            SideHalf(
                ui = right,
                enabled = !rail.busy && !rail.paused,
                paused = rail.paused,
                compact = compact,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onRightTap()
                }
            )
        }
    }
}

@Composable
private fun SideHalf(
    ui: LiveSideUi,
    enabled: Boolean,
    paused: Boolean,
    compact: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = LocalPpMotion.current
    val pop = remember { Animatable(1f) }
    LaunchedEffect(ui.score) {
        if (!motion.enabled) { pop.snapTo(1f); return@LaunchedEffect }
        pop.snapTo(1.13f)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    BoxWithConstraints(
        modifier
            .glassPanel(
                radius = 22.dp,
                fill = Brush.verticalGradient(
                    listOf(ui.accent.copy(alpha = if (ui.serving) 0.15f else 0.06f), Color(0x0A071016))
                ),
                edge = Brush.linearGradient(
                    listOf(ui.accent.copy(alpha = if (ui.serving) 0.8f else 0.22f), Color(0x142C5566))
                )
            )
            .then(if (enabled) Modifier.clickableNoRipple(onTap) else Modifier)
            .padding(horizontal = if (compact) PpSpacing.md else PpSpacing.lg, vertical = PpSpacing.md)
    ) {
        val density = LocalDensity.current
        val scoreSize = with(density) { (maxHeight * 0.36f).toSp().value.coerceIn(40f, 96f).sp }
        val scoreLine = with(density) { (maxHeight * 0.37f).toSp().value.coerceIn(42f, 98f).sp }

        AnimatedContent(
            targetState = ui.teamLabel,
            transitionSpec = {
                if (motion.enabled) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "pp-side"
        ) { _ ->
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ui.serving) {
                        Box(Modifier.size(9.dp).drawBehind { drawCircle(ui.accent) })
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        ui.teamLabel.uppercase(),
                        style = if (compact) PpTextStyles.Label else PpTextStyles.Title,
                        color = PpTextHigh, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (ui.players.isNotBlank() && !compact) {
                    Text(
                        ui.players, style = PpTextStyles.Label, color = PpTextMid,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    ui.score,
                    style = PpTextStyles.ScoreDisplay.copy(fontSize = scoreSize, lineHeight = scoreLine),
                    color = if (ui.serving) PpAmberLed else PpTextHigh,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniChip("ГЕЙМ ${ui.games}", ui.accent)
                    MiniChip("СЕТЫ ${ui.setsWon}", PpGlassCyan)
                }
                if (ui.serving && !ui.serverName.isNullOrBlank()) {
                    Text(
                        "ПОДАЁТ · ${ui.serverName.uppercase()}",
                        style = PpTextStyles.Overline, color = PpAmberLed,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
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
                    radius = 13.dp,
                    fill = Brush.horizontalGradient(listOf(ui.accent.copy(alpha = 0.3f), Color(0x1A5FE3FF)))
                ).padding(horizontal = 14.dp, vertical = 5.dp)
            ) { Text(flag, style = PpTextStyles.Micro, color = PpTextHigh, maxLines = 1) }
        }

        Text(
            if (paused) "ПАУЗА" else "+ ОЧКО",
            style = PpTextStyles.Overline,
            color = if (enabled) ui.accent.copy(alpha = 0.75f) else PpAmberLed.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun MiniChip(text: String, accent: Color) {
    Box(
        Modifier.glassPanel(radius = 11.dp, fill = Brush.verticalGradient(listOf(PpCourtGlass, Color(0x0A071016))))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) { Text(text, style = PpTextStyles.Overline, color = accent, maxLines = 1) }
}

@Composable
private fun CenterRail(
    rail: LiveRailUi,
    compact: Boolean,
    onUndo: () -> Unit,
    onPauseToggle: () -> Unit,
    onSwapSides: () -> Unit,
    onSettings: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Box(modifier) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                rail.timer,
                style = if (compact) PpTextStyles.Title else PpTextStyles.Numeral,
                color = if (rail.paused) PpAmberLed else PpTextHigh,
                maxLines = 1
            )
            Text(rail.setLabel.uppercase(), style = PpTextStyles.Overline, color = PpTextMid, maxLines = 1)
            Text(rail.formatLabel.uppercase(), style = PpTextStyles.Overline, color = PpTextLow, maxLines = 1)
            rail.sets.takeLast(3).forEach { s ->
                Text(s, style = PpTextStyles.Overline, color = PpGlassCyan, maxLines = 1)
            }

            Spacer(Modifier.height(6.dp))

            if (rail.banner != null) {
                Box(
                    Modifier.fillMaxWidth().glassPanel(
                        radius = 11.dp,
                        fill = Brush.horizontalGradient(listOf(Color(0x33C8F751), Color(0x1A5FE3FF)))
                    ).padding(vertical = 5.dp, horizontal = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        rail.banner.uppercase(), style = PpTextStyles.Overline, color = PpTextHigh,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(5.dp))
            }

            RailButton(
                icon = if (rail.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                label = if (rail.paused) "ИГРАТЬ" else "ПАУЗА",
                accent = if (rail.paused) PpAmberLed else PpGlassCyan,
                enabled = !rail.busy, compact = compact, onClick = onPauseToggle
            )
            RailButton(
                icon = Icons.Rounded.Undo, label = "ОТМЕНИТЬ", accent = PpTextMid,
                enabled = rail.canUndo && !rail.busy && !rail.paused,
                compact = compact, onClick = onUndo
            )
            RailButton(
                icon = Icons.Rounded.SwapHoriz,
                label = if (rail.autoSwap) "АВТО" else "СТОРОНЫ",
                accent = if (rail.swapped) PpLime else PpGlassCyan,
                enabled = !rail.busy, compact = compact, onClick = onSwapSides
            )
            RailButton(
                icon = Icons.Rounded.Settings, label = "НАСТРОЙКИ", accent = PpTextMid,
                enabled = !rail.busy, compact = compact, onClick = onSettings
            )
            RailButton(
                icon = Icons.Rounded.Flag, label = "ЗАВЕРШИТЬ", accent = PpLime,
                enabled = !rail.busy, compact = compact, onClick = onFinish
            )

            Spacer(Modifier.height(4.dp))
            Text(
                rail.deuceLabel.uppercase(), style = PpTextStyles.Overline, color = PpTextLow,
                maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis
            )
        }

        if (scroll.maxValue > 0) {
            Canvas(
                Modifier.align(Alignment.CenterEnd).width(3.dp).fillMaxHeight().padding(vertical = 4.dp)
            ) {
                val track = size.height
                val ratio = scroll.value.toFloat() / scroll.maxValue.coerceAtLeast(1)
                val thumb = (track * (track / (track + scroll.maxValue))).coerceAtLeast(24.dp.toPx())
                drawRoundRect(Color(0x1F2C5566), cornerRadius = CornerRadius(2.dp.toPx()))
                drawRoundRect(
                    Brush.verticalGradient(listOf(PpGlassCyan, PpLime)),
                    topLeft = Offset(0f, (track - thumb) * ratio),
                    size = Size(size.width, thumb),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun RailButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .glassPanel(radius = 13.dp)
            .then(if (enabled) Modifier.clickableNoRipple(onClick) else Modifier)
            .padding(vertical = if (compact) 5.dp else 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (enabled) accent else PpTextLow, modifier = Modifier.size(if (compact) 18.dp else 20.dp))
        Text(
            label, style = PpTextStyles.Overline,
            color = if (enabled) PpTextMid else PpTextLow,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}