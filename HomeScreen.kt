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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.padelpro.app.core.designsystem.components.accentRail
import com.padelpro.app.core.designsystem.components.glassPanel
import com.padelpro.app.core.designsystem.components.levelTitleRes
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpBallTrail
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtPanel
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpOnAccent
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.MatchFormat

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    listState: LazyListState,
    onOpenProfile: () -> Unit,
    onStartMatch: () -> Unit,
    onContinueLearning: (String?) -> Unit,
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
            contentPadding = PaddingValues(horizontal = PpSpacing.screen, vertical = PpSpacing.lg),
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
                            color = PpGlassCyan
                        )
                        Spacer(Modifier.height(PpSpacing.xs))
                        Text(
                            text = state.displayName.ifBlank { stringResource(R.string.profile_default_player_name) },
                            style = PpTextStyles.H1,
                            color = PpTextHigh,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(PpSpacing.md))
                    PpAvatar(
                        avatarId = state.avatarId,
                        size = 52.dp,
                        highlighted = true,
                        onClick = onOpenProfile,
                        modifier = Modifier.testTag("home_profile_avatar")
                    )
                }
            }

            if (state.activeMatch != null) {
                item {
                    PpCard(
                        accent = PpAmberLed,
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
                                    color = PpAmberLed
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
                                    color = PpTextHigh
                                )
                            }
                            Spacer(Modifier.width(PpSpacing.md))
                            PpPrimaryButton(
                                text = stringResource(R.string.home_resume_cta),
                                onClick = onResumeMatch
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
                val learningSub = when {
                    state.totalLessons > 0 && state.completedLessons >= state.totalLessons ->
                        stringResource(R.string.home_action_continue_learning_all_done)
                    state.nextUnlockedLessonId != null ->
                        stringResource(R.string.home_action_continue_learning_next)
                    else -> stringResource(R.string.home_action_continue_learning_sub)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    QuickActionCard(
                        title = stringResource(R.string.home_action_start_match),
                        subtitle = stringResource(R.string.home_action_start_match_sub),
                        icon = Icons.Rounded.SportsTennis,
                        accentColor = PpLime,
                        highlighted = true,
                        onClick = onStartMatch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_start_match_card")
                    )
                    QuickActionCard(
                        title = stringResource(R.string.home_action_continue_learning),
                        subtitle = learningSub,
                        icon = Icons.Rounded.School,
                        accentColor = PpGlassCyan,
                        highlighted = false,
                        onClick = { onContinueLearning(state.nextUnlockedLessonId) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_continue_learning_card")
                    )
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.home_section_academy_progress))
                Spacer(Modifier.height(PpSpacing.xs))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassPanel(radius = PpRadius.card)
                        .clickable { onContinueLearning(state.nextUnlockedLessonId) }
                        .padding(PpSpacing.lg)
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.home_academy_progress_format,
                            state.totalLessons,
                            state.academyPercent,
                            state.completedLessons,
                            state.totalLessons
                        ),
                        style = PpTextStyles.Title.copy(fontFeatureSettings = "tnum"),
                        color = PpTextHigh
                    )
                    Spacer(Modifier.height(PpSpacing.md))
                    PpProgressBar(
                        progress = if (state.totalLessons == 0) 0f else state.completedLessons.toFloat() / state.totalLessons.toFloat(),
                        brush = PpBallTrail,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.home_section_recent_match))
                Spacer(Modifier.height(PpSpacing.xs))
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
                Spacer(Modifier.height(PpSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    PpStatCard(
                        value = state.totalMatches.toString(),
                        label = stringResource(R.string.home_stat_matches_label),
                        accent = PpGlassCyan,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenPerformance)
                    )
                    PpStatCard(
                        value = state.winRatePercent?.let { "$it%" } ?: stringResource(R.string.dash_placeholder),
                        label = stringResource(R.string.home_stat_win_rate_label),
                        accent = if (state.winRatePercent != null) PpLime else PpTextMid,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenPerformance)
                    )
                    val streakValue = when (state.isCurrentStreakWin) {
                        true -> stringResource(R.string.streak_win_short_format, state.currentStreakCount)
                        false -> stringResource(R.string.streak_loss_short_format, state.currentStreakCount)
                        null -> stringResource(R.string.dash_placeholder)
                    }
                    PpStatCard(
                        value = streakValue,
                        label = stringResource(R.string.home_stat_streak_label),
                        accent = PpAmberLed,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenPerformance)
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
    icon: ImageVector,
    accentColor: Color,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .glassPanel(
                radius = PpRadius.card,
                fill = if (highlighted) {
                    Brush.verticalGradient(listOf(Color(0x1FC2F54C), PpCourtPanel))
                } else {
                    Brush.verticalGradient(listOf(PpCourtGlass, PpCourtPanel))
                }
            )
            .accentRail(accentColor)
            .clickable(onClick = onClick)
            .padding(PpSpacing.lg)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (highlighted) PpLime else accentColor.copy(alpha = 0.16f))
                .border(1.dp, accentColor.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) PpOnAccent else accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(PpSpacing.md))
        Text(
            text = title,
            style = PpTextStyles.Micro,
            color = accentColor
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = subtitle,
            style = PpTextStyles.Label,
            color = PpTextMid,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
