package com.padelpro.app.feature.match

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
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
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpCourtPanel
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpOnAccent
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
    val accent: Color,
    val pointsWon: Int = 0
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
    val sets: List<String>,
    val recentPoints: List<Boolean> = emptyList()
)

private const val TAP_GUARD_MS = 220L

/** Подписи рейла: обычный регистр, без широкого трекинга. */
private val RailLabel: TextStyle = PpTextStyles.Micro.copy(fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.sp)

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
    var lastTapAt by remember { mutableLongStateOf(0L) }

    fun guarded(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastTapAt < TAP_GUARD_MS) return
        lastTapAt = now
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 700.dp
        val railWidth = if (compact) 100.dp else 122.dp
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
                onTap = { guarded(onLeftTap) }
            )
            CenterRail(
                rail = rail,
                compact = compact,
                onUndo = { guarded(onUndo) },
                onPauseToggle = { guarded(onPauseToggle) },
                onSwapSides = { guarded(onSwapSides) },
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
                onTap = { guarded(onRightTap) }
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
        pop.snapTo(1.06f)
        pop.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium))
    }

    val burst = remember { Animatable(1f) }
    var seenPoints by remember { mutableLongStateOf(ui.pointsWon.toLong()) }
    LaunchedEffect(ui.pointsWon) {
        if (ui.pointsWon.toLong() == seenPoints) return@LaunchedEffect
        seenPoints = ui.pointsWon.toLong()
        if (!motion.enabled) { burst.snapTo(1f); return@LaunchedEffect }
        burst.snapTo(0f)
        burst.animateTo(1f, tween(durationMillis = 520, easing = LinearOutSlowInEasing))
    }

    BoxWithConstraints(
        modifier
            .glassPanel(
                radius = 22.dp,
                fill = SolidColor(if (ui.serving) ui.accent.copy(alpha = 0.10f).compositeOver(PpCourtPanel) else PpCourtPanel),
                edge = SolidColor(if (ui.serving) ui.accent.copy(alpha = 0.55f) else PpCourtLine)
            )
            .drawWithContent {
                drawContent()
                val t = burst.value
                if (t < 1f) {
                    val maxR = size.maxDimension * 0.62f
                    drawCircle(
                        color = ui.accent.copy(alpha = 0.30f * (1f - t)),
                        radius = maxR * t,
                        center = center
                    )
                    drawCircle(
                        color = ui.accent.copy(alpha = 0.55f * (1f - t)),
                        radius = maxR * t,
                        center = center,
                        style = Stroke(width = 2.dp.toPx() * (1f - t) + 0.5f)
                    )
                }
            }
            .then(if (enabled) Modifier.clickableNoRipple(onTap) else Modifier)
            .padding(horizontal = if (compact) PpSpacing.md else PpSpacing.lg, vertical = PpSpacing.sm)
    ) {
        val density = LocalDensity.current
        val tight = maxHeight < 230.dp || density.fontScale > 1.2f
        val scoreSize = with(density) { (maxHeight * (if (tight) 0.32f else 0.38f)).toSp() }
            .value.coerceIn(40f, 116f).sp
        val scoreLine = with(density) { (maxHeight * (if (tight) 0.33f else 0.39f)).toSp() }
            .value.coerceIn(42f, 118f).sp

        AnimatedContent(
            targetState = ui.teamLabel,
            transitionSpec = {
                if (motion.enabled) {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
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
                        ui.teamLabel,
                        style = if (compact || tight) PpTextStyles.Label else PpTextStyles.Title,
                        color = PpTextHigh, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (ui.players.isNotBlank() && !compact && !tight) {
                    Text(
                        ui.players, style = PpTextStyles.Label, color = PpTextMid,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    ui.score,
                    style = PpTextStyles.ScoreDisplay.copy(fontSize = scoreSize, lineHeight = scoreLine),
                    color = PpTextHigh,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniChip("Гейм ${ui.games}", PpTextHigh)
                    if (!tight) MiniChip("Сеты ${ui.setsWon}", PpTextMid)
                }
                if (ui.serving && !ui.serverName.isNullOrBlank() && !tight) {
                    Text(
                        "Подаёт · ${ui.serverName}",
                        style = RailLabel, color = PpAmberLed,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        val flag = when {
            ui.matchPoint -> "Матчбол"
            ui.setPoint -> "Сетбол"
            else -> null
        }
        if (flag != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).glassPanel(
                    radius = 13.dp,
                    fill = SolidColor(PpLime),
                    edge = SolidColor(PpLime)
                ).padding(horizontal = 14.dp, vertical = 5.dp)
            ) { Text(flag, style = PpTextStyles.Micro, color = PpOnAccent, maxLines = 1) }
        }

        Text(
            if (paused) "Пауза" else "+ очко",
            style = RailLabel,
            color = if (enabled) PpTextLow else PpAmberLed,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun MiniChip(text: String, accent: Color) {
    Box(
        Modifier.glassPanel(radius = 11.dp, fill = SolidColor(PpCourtGlass), edge = SolidColor(PpCourtLine))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(text, style = RailLabel, color = accent, maxLines = 1) }
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
    BoxWithConstraints(modifier) {
        // Колонка без прокрутки: шапка и подвал фиксированы, кнопки делят остаток поровну.
        val tight = maxHeight < 330.dp
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                rail.timer,
                style = if (compact) PpTextStyles.Title else PpTextStyles.Numeral,
                color = if (rail.paused) PpAmberLed else PpTextHigh,
                maxLines = 1
            )
            Text(rail.setLabel, style = RailLabel, color = PpTextMid, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!tight) {
                Text(rail.formatLabel, style = RailLabel, color = PpTextLow, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (rail.sets.isNotEmpty()) {
                Text(
                    rail.sets.takeLast(3).joinToString("  "),
                    style = RailLabel, color = PpTextMid, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }

            if (rail.recentPoints.isNotEmpty() && !tight) {
                Spacer(Modifier.height(4.dp))
                PointTape(rail.recentPoints)
            }

            if (rail.banner != null) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().glassPanel(
                        radius = 11.dp,
                        fill = SolidColor(PpLime.copy(alpha = 0.12f).compositeOver(PpCourtPanel)),
                        edge = SolidColor(PpLime.copy(alpha = 0.35f))
                    ).padding(vertical = 4.dp, horizontal = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        rail.banner, style = RailLabel, color = PpTextHigh,
                        textAlign = TextAlign.Center, maxLines = if (tight) 1 else 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RailButton(
                    icon = if (rail.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    label = if (rail.paused) "Играть" else "Пауза",
                    accent = if (rail.paused) PpAmberLed else PpTextHigh,
                    enabled = !rail.busy, compact = compact, onClick = onPauseToggle,
                    modifier = Modifier.weight(1f)
                )
                RailButton(
                    icon = Icons.Rounded.Undo, label = "Отменить", accent = PpTextMid,
                    enabled = rail.canUndo && !rail.busy && !rail.paused,
                    compact = compact, onClick = onUndo,
                    modifier = Modifier.weight(1f)
                )
                RailButton(
                    icon = Icons.Rounded.SwapHoriz,
                    label = if (rail.autoSwap) "Авто" else "Стороны",
                    accent = if (rail.swapped) PpLime else PpTextMid,
                    enabled = !rail.busy, compact = compact, onClick = onSwapSides,
                    modifier = Modifier.weight(1f)
                )
                RailButton(
                    icon = Icons.Rounded.Settings, label = "Настройки", accent = PpTextMid,
                    enabled = !rail.busy, compact = compact, onClick = onSettings,
                    modifier = Modifier.weight(1f)
                )
                RailButton(
                    icon = Icons.Rounded.Flag, label = "Завершить", accent = PpLime,
                    enabled = !rail.busy, compact = compact, onClick = onFinish,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!tight) {
                Spacer(Modifier.height(4.dp))
                Text(
                    rail.deuceLabel, style = RailLabel, color = PpTextLow,
                    maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Лента последних очков: лайм — команда A, небесный — команда B; последняя крупнее. */
@Composable
private fun PointTape(points: List<Boolean>) {
    val tail = points.takeLast(8)
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        val n = tail.size.coerceAtLeast(1)
        val step = size.width / n
        tail.forEachIndexed { i, teamA ->
            val c = if (teamA) PpLime else PpGlassCyan
            val last = i == tail.lastIndex
            drawCircle(
                color = c.copy(alpha = if (last) 1f else 0.42f + 0.06f * i),
                radius = if (last) 4.2.dp.toPx() else 2.6.dp.toPx(),
                center = Offset(step * (i + 0.5f), size.height / 2)
            )
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .heightIn(max = 56.dp)
            .glassPanel(radius = 13.dp)
            .then(if (enabled) Modifier.clickableNoRipple(onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Подпись показываем, только если кнопке хватает высоты; иначе остаётся иконка.
        val showLabel = maxHeight >= 40.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon, label,
                tint = if (enabled) accent else PpTextLow,
                modifier = Modifier.size(if (compact) 18.dp else 20.dp)
            )
            if (showLabel) {
                Text(
                    label, style = RailLabel,
                    color = if (enabled) PpTextMid else PpTextLow,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
