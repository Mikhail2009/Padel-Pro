package com.padelpro.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpAvatar
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpMatchCard
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpProgressBar
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.components.PpStatCard
import com.padelpro.app.core.designsystem.components.PpStreakFlame
import com.padelpro.app.core.designsystem.components.PpXpIndicator
import com.padelpro.app.core.designsystem.components.levelTitleRes
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.MatchFormat

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    listState: LazyListState,
    onOpenProfile: () -> Unit,
    onStartMatch: () -> Unit,
    onContinueLearning: () -> Unit,
    onOpenMatchDetails: (String) -> Unit,
    onResumeMatch: () -> Unit,
    onOpenPerformance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val greetingText = when (state.greetingPeriod) {
        GreetingPeriod.MORNING -> stringResource(R.string.home_greeting_morning)
        GreetingPeriod.AFTERNOON -> stringResource(R.string.home_greeting_afternoon)
        GreetingPeriod.EVENING -> stringResource(R.string.home_greeting_evening)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(PpSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greetingText,
                            style = PpTextStyles.Micro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(PpSpacing.xs))
                        Text(
                            text = state.displayName.ifBlank { stringResource(R.string.profile_default_player_name) },
                            style = PpTextStyles.H1,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(PpSpacing.md))
                    PpAvatar(
                        avatarId = state.avatarId,
                        size = 48.dp,
                        onClick = onOpenProfile,
                        modifier = Modifier.testTag("home_profile_avatar")
                    )
                }
            }

            if (state.activeMatch != null) {
                item {
                    PpCard(
                        borderColor = PpPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.home_resume_match_title),
                                    style = PpTextStyles.Micro,
                                    color = PpPrimary
                                )
                                Spacer(Modifier.height(PpSpacing.xs))
                                val activeFormatText = when (state.activeMatch?.format) {
                                    MatchFormat.SINGLES -> stringResource(R.string.match_format_singles)
                                    MatchFormat.DOUBLES -> stringResource(R.string.match_format_doubles)
                                    null -> ""
                                }
                                Text(
                                    text = activeFormatText,
                                    style = PpTextStyles.Title,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            PpPrimaryButton(
                                text = stringResource(R.string.home_resume_cta),
                                onClick = onResumeMatch,
                                fullWidth = false
                            )
                        }
                    }
                }
            }

            item {
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    PpXpIndicator(
                        level = state.level,
                        levelTitle = stringResource(levelTitleRes(state.level)),
                        currentXp = state.totalXp,
                        nextLevelXp = state.nextLevelXp,
                        progress = state.levelProgress
                    )
                    Spacer(Modifier.height(PpSpacing.md))
                    PpStreakFlame(streakDays = state.streakDays)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    QuickActionCard(
                        title = stringResource(R.string.home_action_start_match),
                        subtitle = stringResource(R.string.home_action_start_match_sub),
                        icon = Icons.Rounded.SportsTennis,
                        accent = true,
                        onClick = onStartMatch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_start_match_card")
                    )
                    QuickActionCard(
                        title = stringResource(R.string.home_action_continue_learning),
                        subtitle = stringResource(R.string.home_action_continue_learning_sub),
                        icon = Icons.Rounded.School,
                        accent = false,
                        onClick = onContinueLearning,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_continue_learning_card")
                    )
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.home_section_academy_progress))
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onContinueLearning
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.home_academy_progress_format,
                                state.totalLessons,
                                state.academyPercent,
                                state.completedLessons,
                                state.totalLessons
                            ),
                            style = PpTextStyles.Title,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(Modifier.height(PpSpacing.md))
                    PpProgressBar(
                        progress = if (state.totalLessons == 0) 0f else state.completedLessons.toFloat() / state.totalLessons.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.home_section_recent_match))
                Spacer(Modifier.height(PpSpacing.sm))
                val recent = state.recentMatch
                if (recent != null) {
                    PpMatchCard(
                        match = recent,
                        dateText = viewModel.formatMatchDate(recent.endedAt ?: recent.startedAt),
                        onClick = { onOpenMatchDetails(recent.id) }
                    )
                } else {
                    PpEmptyState(
                        icon = Icons.Rounded.SportsTennis,
                        title = stringResource(R.string.home_recent_match_empty_title),
                        subtitle = stringResource(R.string.home_recent_match_empty_sub)
                    )
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.home_section_performance))
                Spacer(Modifier.height(PpSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    PpStatCard(
                        value = state.totalMatches.toString(),
                        label = stringResource(R.string.home_stat_matches_label),
                        onClick = onOpenPerformance,
                        modifier = Modifier.weight(1f)
                    )
                    PpStatCard(
                        value = state.winRatePercent?.let { "$it%" } ?: stringResource(R.string.dash_placeholder),
                        label = stringResource(R.string.home_stat_win_rate_label),
                        accentValue = state.winRatePercent != null,
                        onClick = onOpenPerformance,
                        modifier = Modifier.weight(1f)
                    )
                    val streakValue = when (state.isCurrentStreakWin) {
                        true -> stringResource(R.string.streak_win_short_format, state.currentStreakCount)
                        false -> stringResource(R.string.streak_loss_short_format, state.currentStreakCount)
                        null -> stringResource(R.string.dash_placeholder)
                    }
                    PpStatCard(
                        value = streakValue,
                        label = stringResource(R.string.home_stat_streak_label),
                        onClick = onOpenPerformance,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(PpRadius.card)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (accent) PpPrimaryTint else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = if (accent) PpPrimary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(PpSpacing.lg)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (accent) PpPrimary else MaterialTheme.colorScheme.surface)
                .border(1.dp, if (accent) PpPrimary else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (accent) MaterialTheme.colorScheme.onPrimary else PpPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(PpSpacing.md))
        Text(
            text = title,
            style = PpTextStyles.Micro,
            color = if (accent) PpPrimary else MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = subtitle,
            style = PpTextStyles.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
