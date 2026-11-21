package com.padelpro.app.core.designsystem.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SportsBaseball
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpBallTrail
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpCourtNight
import com.padelpro.app.core.designsystem.theme.PpCourtPanel
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.core.designsystem.theme.PpTrophyMagenta
import com.padelpro.app.core.designsystem.theme.PpTrophySweep

@StringRes
fun levelTitleRes(level: Int): Int = when (level) {
    1 -> R.string.level_title_rookie
    2 -> R.string.level_title_challenger
    3 -> R.string.level_title_competitor
    4 -> R.string.level_title_playmaker
    5 -> R.string.level_title_strategist
    6 -> R.string.level_title_ace
    7 -> R.string.level_title_pro
    8 -> R.string.level_title_elite
    9 -> R.string.level_title_master
    else -> R.string.level_title_legend
}

@Composable
fun PpXpIndicator(
    level: Int,
    levelTitle: String,
    currentXp: Int,
    nextLevelXp: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PpXpRing(
            progress = progress,
            center = level.toString(),
            caption = levelTitle
        )
        Spacer(Modifier.width(PpSpacing.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.level_badge_format, level, levelTitle),
                style = PpTextStyles.Micro,
                color = PpGlassCyan
            )
            Spacer(Modifier.height(PpSpacing.xs))
            Text(
                text = stringResource(R.string.xp_progress_format, currentXp, nextLevelXp),
                style = PpTextStyles.Numeral,
                color = PpAmberLed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(PpSpacing.md))
            PpProgressBar(
                progress = progress,
                brush = PpBallTrail,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PpStreakFlame(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val active = streakDays > 0
    Row(
        modifier = modifier
            .clip(PpRadius.pill)
            .background(if (active) Color(0x26FFB43D) else PpCourtNight)
            .border(
                width = 1.dp,
                color = if (active) PpAmberLed.copy(alpha = 0.65f) else PpCourtLine,
                shape = PpRadius.pill
            )
            .padding(horizontal = PpSpacing.md, vertical = PpSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = if (active) PpAmberLed else PpTextLow,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(PpSpacing.xs))
        Text(
            text = pluralStringResource(R.plurals.streak_days_format, streakDays, streakDays),
            style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
            color = if (active) PpAmberLed else PpTextLow
        )
    }
}

@Composable
fun PpAchievementBadge(
    title: String,
    description: String,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(148.dp)
            .alpha(if (unlocked) 1f else 0.56f)
            .glassPanel(
                radius = PpRadius.card,
                fill = if (unlocked) {
                    Brush.verticalGradient(listOf(Color(0x26FF3D8F), PpCourtPanel))
                } else {
                    Brush.verticalGradient(listOf(PpCourtNight, PpCourtNight))
                },
                edge = if (unlocked) {
                    PpTrophySweep
                } else {
                    Brush.linearGradient(listOf(PpCourtLine, PpCourtLine))
                }
            )
            .padding(PpSpacing.md)
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            if (unlocked) {
                Canvas(Modifier.size(44.dp)) {
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0x4DFF3D8F), Color(0x1AFFB43D), Color.Transparent))
                    )
                    drawCircle(
                        brush = PpTrophySweep,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            } else {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PpCourtGlass.copy(alpha = 0.4f))
                        .border(1.dp, PpCourtLine, CircleShape)
                )
            }
            Icon(
                imageVector = if (unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (unlocked) PpAmberLed else PpTextLow,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(PpSpacing.md))
        Text(
            text = title,
            style = PpTextStyles.Micro,
            color = if (unlocked) PpTrophyMagenta else PpTextLow,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = description,
            style = PpTextStyles.Label,
            color = if (unlocked) PpTextMid else PpTextLow,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
fun PpServeIndicator(
    isServing: Boolean,
    modifier: Modifier = Modifier
) {
    if (isServing) {
        Icon(
            imageVector = Icons.Rounded.SportsBaseball,
            contentDescription = stringResource(R.string.cd_serving_indicator),
            tint = PpAmberLed,
            modifier = modifier.size(16.dp)
        )
    }
}

@Composable
fun PpSetScoreRow(
    teamLabel: String,
    setScores: List<String>,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = teamLabel,
            style = PpTextStyles.Title,
            color = PpTextHigh,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        setScores.forEach { score ->
            Text(
                text = score,
                style = PpTextStyles.Title.copy(fontFeatureSettings = "tnum"),
                color = if (isWinner) PpLime else PpTextMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(44.dp)
            )
        }
    }
}
