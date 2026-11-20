package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchSummary

@Composable
fun PpCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(PpRadius.card)
    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = cardModifier
    ) {
        Column(
            modifier = Modifier.padding(PpSpacing.lg),
            content = content
        )
    }
}

@Composable
fun PpSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = PpTextStyles.Micro,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionText != null) {
            if (onActionClick != null) {
                Text(
                    text = actionText.uppercase(),
                    style = PpTextStyles.Micro,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(PpRadius.pill)
                        .clickable(onClick = onActionClick)
                        .padding(horizontal = PpSpacing.sm, vertical = PpSpacing.xs)
                )
            } else {
                Text(
                    text = actionText.uppercase(),
                    style = PpTextStyles.Micro,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = PpSpacing.sm, vertical = PpSpacing.xs)
                )
            }
        }
    }
}

@Composable
fun PpStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentValue: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    PpCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = value,
            style = PpTextStyles.H2,
            color = if (accentValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = label.uppercase(),
            style = PpTextStyles.Micro.copy(letterSpacing = 0.6.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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
    val borderColor = when {
        isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.outline
    }
    PpCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isUnlocked) 1f else 0.6f),
        borderColor = borderColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted || isHighlighted) PpPrimaryTint else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        1.dp,
                        if (isCompleted || isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (!isUnlocked) Icons.Rounded.Lock else icon,
                    contentDescription = null,
                    tint = if (isCompleted || isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
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
                        color = MaterialTheme.colorScheme.onBackground,
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(PpSpacing.xs))
                        }
                        Text(
                            text = statusLabel.uppercase(),
                            style = PpTextStyles.Micro,
                            color = if (isCompleted || isHighlighted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(PpSpacing.xs))
                Text(
                    text = meta,
                    style = PpTextStyles.Label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isUnlocked) {
                    Spacer(Modifier.height(PpSpacing.md))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PpProgressBar(
                            progress = progressFraction,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(PpSpacing.md))
                        Text(
                            text = progressText,
                            style = PpTextStyles.Label,
                            color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
    PpCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .alpha(if (isUnlocked) 1f else 0.55f),
        borderColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
        onClick = if (enabled) onClick else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted || isCurrent -> PpPrimaryTint
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isCompleted || isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isCompleted -> Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    isCurrent -> Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    !isUnlocked -> Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    else -> Text(
                        text = index.toString(),
                        style = PpTextStyles.Label,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(Modifier.width(PpSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PpTextStyles.Title,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = PpTextStyles.Label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(PpSpacing.sm))
            Text(
                text = xpLabel,
                style = PpTextStyles.Micro,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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

    PpCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(PpRadius.pill)
                    .background(if (isWin) PpPrimaryTint else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (isWin) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
                        PpRadius.pill
                    )
                    .padding(horizontal = PpSpacing.md, vertical = PpSpacing.xs)
            ) {
                Text(
                    text = chipText,
                    style = PpTextStyles.Micro,
                    color = if (isWin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(PpSpacing.xs))
                Text(
                    text = scoreLine,
                    style = PpTextStyles.Body,
                    color = if (isWin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
