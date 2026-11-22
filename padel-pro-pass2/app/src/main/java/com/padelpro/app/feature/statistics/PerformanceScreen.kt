package com.padelpro.app.feature.statistics

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpActivityBars
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpFormChips
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpProgressBar
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.components.PpStatCard
import com.padelpro.app.core.designsystem.components.PpWinDonut
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpBallTrail
import com.padelpro.app.core.designsystem.theme.PpCourtSweep
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.core.designsystem.theme.PpTrophyMagenta

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
            contentPadding = PaddingValues(horizontal = PpSpacing.screen, vertical = PpSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
        ) {
            item {
                Text(
                    text = stringResource(R.string.performance_title),
                    style = PpTextStyles.H1,
                    color = PpTextHigh
                )
            }

            item {
                PpCard(
                    accent = PpLime,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PpWinDonut(
                            fraction = (state.winRatePercent ?: 0) / 100f,
                            centerLabel = state.winRatePercent?.let { "$it%" }
                                ?: stringResource(R.string.dash_placeholder)
                        )
                        Spacer(Modifier.width(PpSpacing.xl))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(PpSpacing.sm)
                        ) {
                            Text(
                                text = pluralStringResource(R.plurals.performance_wins_format, state.wins, state.wins),
                                style = PpTextStyles.Title.copy(fontFeatureSettings = "tnum"),
                                color = if (state.wins > 0) PpLime else PpTextMid
                            )
                            Text(
                                text = pluralStringResource(R.plurals.performance_losses_format, state.losses, state.losses),
                                style = PpTextStyles.Title.copy(fontFeatureSettings = "tnum"),
                                color = if (state.losses > 0) PpTrophyMagenta else PpTextMid
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
                        accent = PpGlassCyan,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenMatchHistory)
                    )
                    val currentStreakValue = when (state.isCurrentStreakWin) {
                        true -> stringResource(R.string.streak_win_short_format, state.currentStreakCount)
                        false -> stringResource(R.string.streak_loss_short_format, state.currentStreakCount)
                        null -> stringResource(R.string.dash_placeholder)
                    }
                    PpStatCard(
                        value = currentStreakValue,
                        label = stringResource(R.string.performance_kpi_current_streak),
                        accent = if (state.isCurrentStreakWin == true) PpLime else PpAmberLed,
                        modifier = Modifier.weight(1f)
                    )
                    PpStatCard(
                        value = if (state.bestWinStreak > 0) {
                            stringResource(R.string.streak_win_short_format, state.bestWinStreak)
                        } else {
                            stringResource(R.string.dash_placeholder)
                        },
                        label = stringResource(R.string.performance_kpi_best_streak),
                        accent = PpAmberLed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.totalMatches == 0) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PpEmptyState(
                            icon = Icons.Rounded.SportsTennis,
                            title = stringResource(R.string.performance_empty_cta_text),
                            subtitle = stringResource(R.string.performance_empty_sub),
                            modifier = Modifier.testTag("performance_empty_state")
                        )
                        Spacer(Modifier.height(PpSpacing.md))
                        PpPrimaryButton(
                            text = stringResource(R.string.action_start_a_match),
                            onClick = onStartMatch,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                SectionHeaderWithCaption(
                    title = stringResource(R.string.performance_section_monthly),
                    caption = stringResource(R.string.performance_last_6_months)
                )
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    PpActivityBars(
                        labels = state.monthlyActivity.map { it.label },
                        values = state.monthlyActivity.map { it.count }
                    )
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
                        progressBrush = PpCourtSweep,
                        modifier = Modifier.weight(1f)
                    )
                    FormatSplitCard(
                        title = stringResource(R.string.match_center_doubles),
                        matches = state.doublesMatches,
                        winRatePercent = state.doublesWinRatePercent,
                        progressBrush = PpBallTrail,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SectionHeaderWithCaption(
                    title = stringResource(R.string.performance_section_recent_form),
                    caption = stringResource(R.string.performance_last_5_matches)
                )
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    if (state.recentForm.isEmpty()) {
                        Text(
                            text = stringResource(R.string.dash_placeholder),
                            style = PpTextStyles.Body,
                            color = PpTextMid
                        )
                    } else {
                        PpFormChips(results = state.recentForm)
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
                        color = PpGlassCyan,
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
private fun SectionHeaderWithCaption(
    title: String,
    caption: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PpSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(6.dp)) { drawCircle(PpLime) }
            Spacer(Modifier.size(PpSpacing.sm))
            Text(
                text = title,
                style = PpTextStyles.Micro,
                color = PpTextMid
            )
        }
        Text(
            text = caption,
            style = PpTextStyles.Overline,
            color = PpTextLow
        )
    }
}

@Composable
private fun FormatSplitCard(
    title: String,
    matches: Int,
    winRatePercent: Int?,
    progressBrush: Brush,
    modifier: Modifier = Modifier
) {
    val rateStr = winRatePercent?.let { "$it%" } ?: stringResource(R.string.dash_placeholder)
    PpCard(modifier = modifier) {
        Text(
            text = title,
            style = PpTextStyles.Micro,
            color = PpTextMid
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = pluralStringResource(R.plurals.performance_format_meta_format, matches, matches, rateStr),
            style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
            color = PpTextHigh
        )
        Spacer(Modifier.height(PpSpacing.md))
        PpProgressBar(
            progress = (winRatePercent ?: 0) / 100f,
            brush = progressBrush,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
