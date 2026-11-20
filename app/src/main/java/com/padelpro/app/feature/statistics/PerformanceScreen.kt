package com.padelpro.app.feature.statistics

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsTennis
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpChartBar
import com.padelpro.app.core.designsystem.components.PpChartDonut
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpProgressBar
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.components.PpStatCard
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun PerformanceScreen(
    viewModel: PerformanceViewModel,
    listState: LazyListState,
    onOpenMatchHistory: () -> Unit,
    onStartMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                Text(
                    text = stringResource(R.string.performance_title),
                    style = PpTextStyles.H1,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PpChartDonut(winRatePercent = state.winRatePercent)
                        Spacer(Modifier.width(PpSpacing.xl))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(PpSpacing.sm)
                        ) {
                            Text(
                                text = pluralStringResource(R.plurals.performance_wins_format, state.wins, state.wins),
                                style = PpTextStyles.Title,
                                color = if (state.wins > 0) PpPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = pluralStringResource(R.plurals.performance_losses_format, state.losses, state.losses),
                                style = PpTextStyles.Title,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    PpStatCard(
                        value = state.totalMatches.toString(),
                        label = stringResource(R.string.performance_kpi_matches),
                        onClick = onOpenMatchHistory,
                        modifier = Modifier.weight(1f)
                    )
                    val currentStreakValue = when (state.isCurrentStreakWin) {
                        true -> stringResource(R.string.streak_win_short_format, state.currentStreakCount)
                        false -> stringResource(R.string.streak_loss_short_format, state.currentStreakCount)
                        null -> stringResource(R.string.dash_placeholder)
                    }
                    PpStatCard(
                        value = currentStreakValue,
                        label = stringResource(R.string.performance_kpi_current_streak),
                        accentValue = state.isCurrentStreakWin == true,
                        modifier = Modifier.weight(1f)
                    )
                    PpStatCard(
                        value = if (state.bestWinStreak > 0) {
                            stringResource(R.string.streak_win_short_format, state.bestWinStreak)
                        } else {
                            stringResource(R.string.dash_placeholder)
                        },
                        label = stringResource(R.string.performance_kpi_best_streak),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.totalMatches == 0) {
                item {
                    PpEmptyState(
                        icon = Icons.Rounded.SportsTennis,
                        title = stringResource(R.string.performance_empty_cta_text),
                        subtitle = stringResource(R.string.home_recent_match_empty_sub),
                        ctaText = stringResource(R.string.action_start_a_match),
                        onCtaClick = onStartMatch,
                        modifier = Modifier.testTag("performance_empty_state")
                    )
                }
            }

            item {
                PpSectionHeader(
                    title = stringResource(R.string.performance_section_monthly),
                    actionText = stringResource(R.string.performance_last_6_months),
                    onActionClick = null
                )
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    PpChartBar(items = state.monthlyActivity)
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.performance_section_format_split))
                Spacer(Modifier.height(PpSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    FormatSplitCard(
                        title = stringResource(R.string.match_center_singles),
                        matches = state.singlesMatches,
                        winRatePercent = state.singlesWinRatePercent,
                        modifier = Modifier.weight(1f)
                    )
                    FormatSplitCard(
                        title = stringResource(R.string.match_center_doubles),
                        matches = state.doublesMatches,
                        winRatePercent = state.doublesWinRatePercent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                PpSectionHeader(
                    title = stringResource(R.string.performance_section_recent_form),
                    actionText = stringResource(R.string.performance_last_5_matches),
                    onActionClick = null
                )
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    if (state.recentForm.isEmpty()) {
                        Text(
                            text = stringResource(R.string.dash_placeholder),
                            style = PpTextStyles.Body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
                            state.recentForm.forEach { won ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (won) PpPrimaryTint else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (won) PpPrimary else PpOutline,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (won) stringResource(R.string.performance_form_w)
                                        else stringResource(R.string.performance_form_l),
                                        style = PpTextStyles.Micro,
                                        color = if (won) PpPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.performance_view_history),
                        style = PpTextStyles.Micro,
                        color = PpPrimary,
                        modifier = Modifier
                            .clip(PpRadius.pill)
                            .clickable(onClick = onOpenMatchHistory)
                            .testTag("performance_history_link")
                            .padding(horizontal = PpSpacing.lg, vertical = PpSpacing.md)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatSplitCard(
    title: String,
    matches: Int,
    winRatePercent: Int?,
    modifier: Modifier = Modifier
) {
    val rateStr = winRatePercent?.let { "$it%" } ?: stringResource(R.string.dash_placeholder)
    PpCard(modifier = modifier) {
        Text(
            text = title.uppercase(),
            style = PpTextStyles.Micro,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = pluralStringResource(R.plurals.performance_format_meta_format, matches, matches, rateStr),
            style = PpTextStyles.Label,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(PpSpacing.md))
        PpProgressBar(
            progress = (winRatePercent ?: 0) / 100f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
