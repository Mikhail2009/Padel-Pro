package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
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
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchSummary

@Composable
fun PpCourseCard(
    title: String,
    meta: String,
    progressFraction: Float,
    progressText: String,
    statusLabel: String,
    icon: ImageVector,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val railColor: Color? = when {
        isHighlighted -> PpGlassCyan
        isCompleted -> PpLime
        else -> null
    }
    val statusAccent = when {
        !isUnlocked -> PpTextLow
        isHighlighted -> PpGlassCyan
        isCompleted -> PpLime
        else -> PpTextMid
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isUnlocked) 1f else 0.62f)
            .glassPanel(
                radius = PpRadius.card,
                fill = if (isUnlocked) {
                    Brush.verticalGradient(listOf(PpCourtGlass, PpCourtPanel))
                } else {
                    Brush.verticalGradient(listOf(PpCourtNight, PpCourtNight))
                }
            )
            .then(if (railColor != null) Modifier.accentRail(railColor) else Modifier)
            .clickable(onClick = onClick)
            .padding(PpSpacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isHighlighted -> PpGlassCyan.copy(alpha = 0.16f)
                            isCompleted -> PpLime.copy(alpha = 0.16f)
                            else -> PpCourtNight
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            isHighlighted -> PpGlassCyan.copy(alpha = 0.7f)
                            isCompleted -> PpLime.copy(alpha = 0.7f)
                            else -> PpCourtLine
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (!isUnlocked) Icons.Rounded.Lock else icon,
                    contentDescription = null,
                    tint = statusAccent,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(PpSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = PpTextStyles.Title,
                        color = if (isUnlocked) PpTextHigh else PpTextLow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(PpSpacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = PpLime,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(PpSpacing.xs))
                        }
                        Text(
                            text = statusLabel.uppercase(),
                            style = PpTextStyles.Overline,
                            color = statusAccent
                        )
                    }
                }
                Spacer(Modifier.height(PpSpacing.xs))
                Text(
                    text = meta,
                    style = PpTextStyles.Label,
                    color = if (isUnlocked) PpTextMid else PpTextLow
                )
                if (isUnlocked) {
                    Spacer(Modifier.height(PpSpacing.md))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PpProgressBar(
                            progress = progressFraction,
                            brush = PpBallTrail,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(PpSpacing.md))
                        Text(
                            text = progressText,
                            style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                            color = if (isCompleted) PpLime else PpTextMid
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PpLessonRow(
    index: Int,
    title: String,
    meta: String,
    xpLabel: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = isUnlocked
) {
    val railColor: Color? = when {
        isCurrent -> PpGlassCyan
        isCompleted -> PpLime
        else -> null
    }
    val accent = when {
        isCurrent -> PpGlassCyan
        isCompleted -> PpLime
        else -> PpTextLow
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .alpha(if (isUnlocked) 1f else 0.56f)
            .glassPanel(
                radius = PpRadius.card,
                fill = when {
                    isCurrent -> Brush.horizontalGradient(listOf(Color(0x265FE3FF), PpCourtPanel))
                    isUnlocked -> Brush.verticalGradient(listOf(PpCourtGlass, PpCourtPanel))
                    else -> Brush.verticalGradient(listOf(PpCourtNight, PpCourtNight))
                },
                edge = if (isCurrent) {
                    Brush.linearGradient(listOf(PpGlassCyan, PpLime.copy(alpha = 0.4f)))
                } else {
                    Brush.linearGradient(listOf(PpCourtLine, PpCourtLine))
                }
            )
            .then(if (railColor != null) Modifier.accentRail(railColor) else Modifier)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(PpSpacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> PpGlassCyan.copy(alpha = 0.18f)
                            isCompleted -> PpLime.copy(alpha = 0.16f)
                            else -> PpCourtNight
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            isCurrent -> PpGlassCyan
                            isCompleted -> PpLime
                            else -> PpCourtLine
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isCompleted -> Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = PpLime,
                        modifier = Modifier.size(18.dp)
                    )
                    isCurrent -> Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = PpGlassCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    !isUnlocked -> Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = PpTextLow,
                        modifier = Modifier.size(16.dp)
                    )
                    else -> Text(
                        text = index.toString(),
                        style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                        color = PpTextHigh
                    )
                }
            }
            Spacer(Modifier.width(PpSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PpTextStyles.Title,
                    color = if (isUnlocked) PpTextHigh else PpTextLow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = PpTextStyles.Label,
                    color = if (isUnlocked) PpTextMid else PpTextLow
                )
            }
            Spacer(Modifier.width(PpSpacing.sm))
            Text(
                text = xpLabel,
                style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
                color = if (isCurrent || isCompleted) accent else PpTextLow
            )
        }
    }
}

@Composable
fun formatDurationRu(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> stringResource(R.string.results_duration_hour_min_format, hours, minutes)
        minutes > 0L -> stringResource(R.string.results_duration_min_sec_format, minutes, seconds)
        else -> stringResource(R.string.results_duration_sec_format, seconds)
    }
}

@Composable
fun PpMatchCard(
    match: MatchSummary,
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val teamAText = match.teamANames.joinToString(" & ").ifBlank { stringResource(R.string.live_match_team_a_placeholder) }
    val teamBText = match.teamBNames.joinToString(" & ").ifBlank { stringResource(R.string.live_match_team_b_placeholder) }
    val resultColor = when (match.userWon) {
        true -> PpLime
        false -> PpTrophyMagenta
        null -> PpTextLow
    }
    val isWin = match.userWon == true
    val chipText = when (match.userWon) {
        true -> stringResource(R.string.history_chip_win)
        false -> stringResource(R.string.history_chip_loss)
        null -> stringResource(R.string.history_chip_completed)
    }
    val scoreLine = match.sets.joinToString(" ") { set ->
        val tb = if (set.tbA != null && set.tbB != null) {
            "(${minOf(set.tbA, set.tbB)})"
        } else ""
        "${set.gamesA}-${set.gamesB}$tb"
    }.ifBlank { stringResource(R.string.dash_placeholder) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(radius = PpRadius.card)
            .accentRail(resultColor)
            .clickable(onClick = onClick)
            .padding(PpSpacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(PpRadius.pill)
                    .background(resultColor.copy(alpha = 0.14f))
                    .border(
                        width = 1.dp,
                        color = resultColor.copy(alpha = 0.55f),
                        shape = PpRadius.pill
                    )
                    .padding(horizontal = PpSpacing.md, vertical = PpSpacing.xs)
            ) {
                Text(
                    text = chipText,
                    style = PpTextStyles.Overline,
                    color = resultColor
                )
            }
            Spacer(Modifier.width(PpSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                val formatLabel = when (match.format) {
                    MatchFormat.SINGLES -> stringResource(R.string.match_format_singles_short)
                    MatchFormat.DOUBLES -> stringResource(R.string.match_format_doubles_short)
                }
                Text(
                    text = stringResource(R.string.match_card_teams_vs_format, teamAText, teamBText),
                    style = PpTextStyles.Title,
                    color = PpTextHigh,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(PpSpacing.xs))
                Text(
                    text = scoreLine,
                    style = PpTextStyles.Title.copy(fontFeatureSettings = "tnum"),
                    color = if (isWin) PpLime else PpTextMid
                )
                Spacer(Modifier.height(2.dp))
                val metaText = if (match.durationMs != null) {
                    stringResource(
                        R.string.match_card_meta_duration_format,
                        formatLabel,
                        dateText,
                        formatDurationRu(match.durationMs)
                    )
                } else {
                    stringResource(R.string.match_card_meta_format, formatLabel, dateText)
                }
                Text(
                    text = metaText,
                    style = PpTextStyles.Label,
                    color = PpTextMid,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = PpTextLow
            )
        }
    }
}
