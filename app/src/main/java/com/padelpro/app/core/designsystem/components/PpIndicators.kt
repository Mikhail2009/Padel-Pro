package com.padelpro.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.annotation.StringRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

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
fun PpProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250),
        label = "PpProgressBar"
    )
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(PpRadius.pill)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, PpRadius.pill)
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(PpRadius.pill)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.level_badge_format, level, levelTitle),
                style = PpTextStyles.Micro,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.xp_progress_format, currentXp, nextLevelXp),
                style = PpTextStyles.Label,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(PpSpacing.sm))
        PpProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun PpStreakFlame(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(PpRadius.pill)
            .background(if (streakDays > 0) PpPrimaryTint else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (streakDays > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outline,
                shape = PpRadius.pill
            )
            .padding(horizontal = PpSpacing.md, vertical = PpSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = if (streakDays > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(PpSpacing.xs))
        Text(
            text = pluralStringResource(R.plurals.streak_days_format, streakDays, streakDays),
            style = PpTextStyles.Micro,
            color = if (streakDays > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PpFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(PpRadius.pill)
            .background(if (selected) PpPrimaryTint else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = PpRadius.pill
            )
            .clickable(onClick = onClick)
            .padding(horizontal = PpSpacing.lg, vertical = PpSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(Modifier.width(PpSpacing.xs))
        }
        Text(
            text = label.uppercase(),
            style = PpTextStyles.Micro,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
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
    PpCard(
        modifier = modifier
            .width(140.dp)
            .alpha(if (unlocked) 1f else 0.55f),
        borderColor = if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (unlocked) PpPrimaryTint else MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(PpSpacing.md))
        Text(
            text = title,
            style = PpTextStyles.Micro,
            color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = description,
            style = PpTextStyles.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            tint = MaterialTheme.colorScheme.primary,
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
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        setScores.forEach { score ->
            Text(
                text = score,
                style = PpTextStyles.Title,
                color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(44.dp)
            )
        }
    }
}
