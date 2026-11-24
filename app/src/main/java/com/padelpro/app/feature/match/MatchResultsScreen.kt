package com.padelpro.app.feature.match

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpScrollColumn
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.components.PpSetScoreRow
import com.padelpro.app.core.designsystem.components.formatDurationRu
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.core.designsystem.theme.PpTrophySweep
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.SetScore
import com.padelpro.app.domain.model.TeamSide

@Composable
fun MatchResultsScreen(
    matchId: String,
    viewModel: MatchResultsViewModel,
    onViewStatistics: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onReturnHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(matchId) {
        viewModel.loadMatch(matchId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PpLime)
                }
            }

            state.match == null -> {
                PpScrollColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PpSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = PpSpacing.xl)
                        ) {
                            Text(
                                text = stringResource(R.string.results_completed_label),
                                style = PpTextStyles.Micro,
                                color = PpTextMid
                            )
                            Spacer(Modifier.height(PpSpacing.lg))
                            PpEmptyState(
                                icon = Icons.Rounded.EmojiEvents,
                                title = stringResource(R.string.results_empty_title),
                                subtitle = stringResource(R.string.results_empty_body)
                            )
                        }
                        ResultsActionButtons(
                            matchId = matchId,
                            detailsEnabled = false,
                            onViewStatistics = onViewStatistics,
                            onOpenDetails = onOpenDetails,
                            onReturnHome = onReturnHome
                        )
                    }
                }
            }

            else -> {
                val match = state.match!!
                MatchResultsContent(
                    match = match,
                    xpEarned = state.xpEarned,
                    formattedDate = viewModel.formatDate(match.endedAt ?: match.startedAt),
                    onViewStatistics = onViewStatistics,
                    onOpenDetails = onOpenDetails,
                    onReturnHome = onReturnHome
                )
            }
        }
    }
}

@Composable
private fun MatchResultsContent(
    match: MatchSummary,
    xpEarned: Int,
    formattedDate: String,
    onViewStatistics: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onReturnHome: () -> Unit
) {
    val teamAText = match.teamANames.joinToString(" и ")
        .ifBlank { stringResource(R.string.live_match_team_a_placeholder) }
    val teamBText = match.teamBNames.joinToString(" и ")
        .ifBlank { stringResource(R.string.live_match_team_b_placeholder) }

    val isManualDraw = match.isManuallyCompleted
    val isWin = match.userWon == true
    val winnerNames = when {
        isManualDraw -> stringResource(R.string.match_card_teams_vs_format, teamAText, teamBText)
        match.winnerTeam == TeamSide.A -> teamAText
        match.winnerTeam == TeamSide.B -> teamBText
        else -> stringResource(R.string.dash_placeholder)
    }
    val winnerSubtitle = when {
        isManualDraw -> stringResource(R.string.results_draw_subtitle)
        match.winnerTeam == TeamSide.A -> stringResource(R.string.results_winner_team_a)
        match.winnerTeam == TeamSide.B -> stringResource(R.string.results_winner_team_b)
        else -> ""
    }
    val headline = when {
        isWin -> stringResource(R.string.results_win_title)
        isManualDraw -> stringResource(R.string.results_draw_title)
        else -> stringResource(R.string.results_loss_title)
    }

    PpScrollColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PpSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PpSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isWin) {
                        Canvas(Modifier.size(80.dp)) {
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0x33E8638C),
                                        Color(0x1AF2B84B),
                                        Color.Transparent
                                    )
                                )
                            )
                            drawCircle(
                                brush = PpTrophySweep,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(PpCourtGlass.copy(alpha = 0.6f))
                                .border(1.5.dp, PpCourtLineStrong, CircleShape)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = if (isWin) PpAmberLed else PpTextMid,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(PpSpacing.md))
                Text(
                    text = headline,
                    style = PpTextStyles.H1,
                    color = when {
                        isWin -> PpLime
                        isManualDraw -> PpGlassCyan
                        else -> PpTextHigh
                    },
                    textAlign = TextAlign.Center
                )
                if (isManualDraw) {
                    Spacer(Modifier.height(PpSpacing.xs))
                    Box(
                        modifier = Modifier
                            .clip(PpRadius.pill)
                            .background(PpAmberLed.copy(alpha = 0.14f))
                            .border(1.dp, PpAmberLed.copy(alpha = 0.45f), PpRadius.pill)
                            .padding(horizontal = PpSpacing.md, vertical = 4.dp)
                            .testTag("results_manual_badge")
                    ) {
                        Text(
                            text = stringResource(R.string.results_manual_finish_badge),
                            style = PpTextStyles.Micro,
                            color = PpAmberLed
                        )
                    }
                }
                Spacer(Modifier.height(PpSpacing.xs))
                Text(
                    text = winnerNames,
                    style = PpTextStyles.Title,
                    color = PpTextHigh,
                    textAlign = TextAlign.Center
                )
                if (winnerSubtitle.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = winnerSubtitle,
                        style = PpTextStyles.Label,
                        color = PpTextMid,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (xpEarned > 0) {
                PpCard(
                    accent = PpAmberLed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("results_xp_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = PpAmberLed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(PpSpacing.sm))
                            Text(
                                text = when {
                                    isManualDraw -> stringResource(R.string.results_xp_breakdown_manual)
                                    isWin -> stringResource(R.string.results_xp_breakdown_win)
                                    else -> stringResource(R.string.results_xp_breakdown_participation)
                                },
                                style = PpTextStyles.Label,
                                color = PpTextHigh
                            )
                        }
                        Spacer(Modifier.width(PpSpacing.md))
                        Text(
                            text = stringResource(R.string.results_xp_earned_format, xpEarned),
                            style = PpTextStyles.Numeral,
                            color = PpAmberLed
                        )
                    }
                }
            }

            PpSectionHeader(title = stringResource(R.string.results_sets_section))
            PpCard(modifier = Modifier.fillMaxWidth()) {
                val formattedSetsSummary = formatSetsLine(match.sets)
                Text(
                    text = formattedSetsSummary,
                    style = PpTextStyles.H2.copy(fontFeatureSettings = "tnum"),
                    color = if (isManualDraw) PpTextHigh else PpLime,
                    modifier = Modifier.testTag("results_sets_line")
                )
                if (match.sets.isNotEmpty()) {
                    Spacer(Modifier.height(PpSpacing.md))
                    PpSetScoreRow(
                        teamLabel = teamAText,
                        setScores = match.sets.map { formatSetCell(it.gamesA, it.tbA, it.tbB) },
                        isWinner = match.winnerTeam == TeamSide.A
                    )
                    Spacer(Modifier.height(PpSpacing.sm))
                    PpSetScoreRow(
                        teamLabel = teamBText,
                        setScores = match.sets.map { formatSetCell(it.gamesB, it.tbB, it.tbA) },
                        isWinner = match.winnerTeam == TeamSide.B
                    )
                }
                match.incompleteSet?.let { inc ->
                    Spacer(Modifier.height(PpSpacing.md))
                    val scoreA = if (inc.scoreA == "AD") stringResource(R.string.live_score_advantage_short) else inc.scoreA
                    val scoreB = if (inc.scoreB == "AD") stringResource(R.string.live_score_advantage_short) else inc.scoreB
                    val incText = if (inc.isTieBreak) {
                        stringResource(
                            R.string.incomplete_set_tiebreak_score_format,
                            inc.setIndex,
                            inc.gamesA,
                            inc.gamesB,
                            scoreA,
                            scoreB
                        )
                    } else {
                        stringResource(
                            R.string.incomplete_set_score_format,
                            inc.setIndex,
                            inc.gamesA,
                            inc.gamesB,
                            scoreA,
                            scoreB
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PpRadius.inputShape)
                            .background(PpGlassCyan.copy(alpha = 0.10f))
                            .border(1.dp, PpGlassCyan.copy(alpha = 0.35f), PpRadius.inputShape)
                            .padding(horizontal = PpSpacing.md, vertical = PpSpacing.sm)
                            .testTag("results_incomplete_set_box")
                    ) {
                        Text(
                            text = incText,
                            style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                            color = PpGlassCyan
                        )
                    }
                }
            }

            PpCard(modifier = Modifier.fillMaxWidth()) {
                val formatText = when (match.format) {
                    MatchFormat.SINGLES -> stringResource(R.string.match_format_singles)
                    MatchFormat.DOUBLES -> stringResource(R.string.match_format_doubles)
                }
                val scoringText = when (match.deuceMode) {
                    DeuceMode.ADVANTAGE -> stringResource(R.string.match_setup_deuce_advantage)
                    DeuceMode.GOLDEN_POINT -> stringResource(R.string.match_setup_deuce_golden)
                    DeuceMode.STAR_POINT -> stringResource(R.string.match_setup_deuce_star)
                }

                ResultMetaRow(
                    label = stringResource(R.string.results_duration_label),
                    value = formatDurationRu(match.durationMs ?: 0L)
                )
                Spacer(Modifier.height(PpSpacing.sm))
                ResultMetaRow(
                    label = stringResource(R.string.results_date_label),
                    value = formattedDate
                )
                Spacer(Modifier.height(PpSpacing.sm))
                ResultMetaRow(
                    label = stringResource(R.string.results_format_label),
                    value = formatText
                )
                Spacer(Modifier.height(PpSpacing.sm))
                ResultMetaRow(
                    label = stringResource(R.string.results_scoring_label),
                    value = scoringText
                )
                Spacer(Modifier.height(PpSpacing.sm))
                ResultMetaRow(
                    label = stringResource(R.string.results_players_section),
                    value = stringResource(R.string.match_card_teams_vs_format, teamAText, teamBText)
                )
            }

            ResultsActionButtons(
                matchId = match.id,
                detailsEnabled = true,
                onViewStatistics = onViewStatistics,
                onOpenDetails = onOpenDetails,
                onReturnHome = onReturnHome
            )
        }
    }
}

@Composable
private fun ResultsActionButtons(
    matchId: String,
    detailsEnabled: Boolean,
    onViewStatistics: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onReturnHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PpSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
    ) {
        PpPrimaryButton(
            text = stringResource(R.string.results_btn_view_stats),
            onClick = onViewStatistics,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("results_view_stats_button")
        )
        if (detailsEnabled) {
            PpSecondaryButton(
                text = stringResource(R.string.results_btn_match_details),
                onClick = { onOpenDetails(matchId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("results_match_details_button")
            )
        }
        TextButton(
            onClick = onReturnHome,
            modifier = Modifier.testTag("results_return_home_button")
        ) {
            Text(
                text = stringResource(R.string.results_btn_return_home),
                style = PpTextStyles.Label,
                color = PpGlassCyan
            )
        }
    }
}

@Composable
private fun ResultMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = PpTextStyles.Micro,
            color = PpTextMid
        )
        Spacer(Modifier.width(PpSpacing.md))
        Text(
            text = value,
            style = PpTextStyles.Label,
            color = PpTextHigh,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatSetCell(games: Int, teamTb: Int?, oppTb: Int?): String {
    return if (teamTb != null && oppTb != null && teamTb < oppTb) {
        "$games($teamTb)"
    } else {
        games.toString()
    }
}

private fun formatSetsLine(sets: List<SetScore>): String {
    if (sets.isEmpty()) return "—"
    return sets.joinToString(" · ") { set ->
        val tb = if (set.tbA != null && set.tbB != null) {
            "(${minOf(set.tbA, set.tbB)})"
        } else ""
        "${set.gamesA}-${set.gamesB}$tb"
    }
}
