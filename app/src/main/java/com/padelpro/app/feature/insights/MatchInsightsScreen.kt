package com.padelpro.app.feature.insights

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpAvatar
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.components.PpSetScoreRow
import com.padelpro.app.core.designsystem.components.formatDurationRu
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.core.designsystem.theme.PpTrophyMagenta
import com.padelpro.app.core.designsystem.theme.PpTrophySweep
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchAnalytics
import com.padelpro.app.domain.model.MatchDetailsData
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchOverview
import com.padelpro.app.domain.model.MatchParticipantDetail
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.TeamSide

@Composable
fun MatchInsightsScreen(
    matchId: String,
    viewModel: MatchInsightsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(matchId) {
        viewModel.loadMatch(matchId)
    }

    BackHandler(onBack = onBack)

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

            state.details == null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp),
                    contentPadding = PaddingValues(PpSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
                ) {
                    item {
                        InsightsTopBar(onBack = onBack)
                    }
                    item {
                        PpEmptyState(
                            icon = Icons.Rounded.SportsTennis,
                            title = stringResource(R.string.insights_empty_title),
                            subtitle = stringResource(R.string.insights_empty_body),
                            modifier = Modifier.testTag("insights_empty_state")
                        )
                    }
                }
            }

            else -> {
                val details = state.details!!
                MatchInsightsContent(
                    details = details,
                    formattedDate = viewModel.formatDateTime(
                        details.summary.endedAt ?: details.summary.startedAt
                    ),
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
fun MatchInsightsContent(
    details: MatchDetailsData,
    formattedDate: String,
    onBack: () -> Unit
) {
    val analytics = details.analytics
    val summary = details.summary
    val teamAText = summary.teamANames.joinToString(" & ")
        .ifBlank { stringResource(R.string.live_match_team_a_placeholder) }
    val teamBText = summary.teamBNames.joinToString(" & ")
        .ifBlank { stringResource(R.string.live_match_team_b_placeholder) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .testTag("insights_list"),
        contentPadding = PaddingValues(PpSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
    ) {
        item {
            InsightsTopBar(
                isLive = summary.status == MatchStatus.ACTIVE,
                onBack = onBack
            )
        }

        if (analytics == null) {
            item {
                PpEmptyState(
                    icon = Icons.Rounded.SportsTennis,
                    title = stringResource(R.string.insights_no_analytics_title),
                    subtitle = stringResource(R.string.insights_no_analytics_body),
                    modifier = Modifier.testTag("insights_no_analytics_state")
                )
            }
        } else {
            // A. Match Overview
            item {
                MatchOverviewSection(
                    details = details,
                    analytics = analytics,
                    teamAText = teamAText,
                    teamBText = teamBText,
                    formattedDate = formattedDate
                )
            }

            // B. Point Statistics
            item {
                PointStatsSection(
                    pointStats = analytics.pointStats,
                    teamALabel = teamAText,
                    teamBLabel = teamBText
                )
            }

            // C. Game Statistics
            item {
                GameStatsSection(
                    gameStats = analytics.gameStats,
                    teamALabel = teamAText,
                    teamBLabel = teamBText
                )
            }

            // D. Set Statistics
            item {
                SetStatsSection(
                    overview = analytics.overview,
                    gameStats = analytics.gameStats,
                    teamALabel = teamAText,
                    teamBLabel = teamBText
                )
            }

            // E. Momentum
            item {
                MomentumSection(
                    momentum = analytics.momentum,
                    teamALabel = teamAText,
                    teamBLabel = teamBText
                )
            }

            // F. Highlights
            item {
                HighlightsSection(
                    highlights = analytics.highlights,
                    teamALabel = teamAText,
                    teamBLabel = teamBText
                )
            }
        }
    }
}

@Composable
private fun InsightsTopBar(
    isLive: Boolean = false,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PpIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack,
            modifier = Modifier.testTag("insights_back_button")
        )
        Spacer(Modifier.width(PpSpacing.sm))
        Text(
            text = stringResource(R.string.insights_title),
            style = PpTextStyles.H1,
            color = PpTextHigh,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isLive) {
            Spacer(Modifier.width(PpSpacing.sm))
            Row(
                modifier = Modifier
                    .clip(PpRadius.pill)
                    .background(PpLime.copy(alpha = 0.16f))
                    .border(1.dp, PpLime.copy(alpha = 0.55f), PpRadius.pill)
                    .padding(horizontal = PpSpacing.md, vertical = 5.dp)
                    .testTag("insights_live_badge"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(PpLime)
                )
                Spacer(Modifier.width(PpSpacing.xs + 2.dp))
                Text(
                    text = stringResource(R.string.insights_live_badge),
                    style = PpTextStyles.Micro,
                    color = PpLime
                )
            }
        }
    }
}

@Composable
private fun MatchOverviewSection(
    details: MatchDetailsData,
    analytics: MatchAnalytics,
    teamAText: String,
    teamBText: String,
    formattedDate: String
) {
    val overview = analytics.overview
    val summary = details.summary
    val isLive = !overview.isCompleted
    val isManualDraw = overview.isManuallyCompleted

    val bannerAccent = when {
        isLive -> PpLime
        summary.userWon == true -> PpLime
        summary.userWon == false -> PpTrophyMagenta
        else -> PpGlassCyan
    }

    val bannerTitle = when {
        isLive -> stringResource(R.string.home_resume_match_title)
        summary.userWon == true -> stringResource(R.string.details_winner_banner_win)
        summary.userWon == false -> stringResource(R.string.details_winner_banner_loss)
        isManualDraw -> stringResource(R.string.details_winner_banner_draw)
        else -> stringResource(R.string.details_winner_banner_neutral)
    }

    val headlineNames = when {
        isLive || isManualDraw -> stringResource(R.string.match_card_teams_vs_format, teamAText, teamBText)
        overview.winnerTeam == TeamSide.A -> teamAText
        overview.winnerTeam == TeamSide.B -> teamBText
        else -> stringResource(R.string.match_card_teams_vs_format, teamAText, teamBText)
    }

    val setsSummaryText = overview.sets.joinToString(" · ") { set ->
        val tb = if (set.tbA != null && set.tbB != null) {
            "(${minOf(set.tbA, set.tbB)})"
        } else ""
        "${set.gamesA}-${set.gamesB}$tb"
    }

    val formatText = when (overview.format) {
        MatchFormat.SINGLES -> stringResource(R.string.match_format_singles)
        MatchFormat.DOUBLES -> stringResource(R.string.match_format_doubles)
    }
    val scoringText = when (overview.deuceMode) {
        DeuceMode.ADVANTAGE -> stringResource(R.string.match_setup_deuce_advantage)
        DeuceMode.GOLDEN_POINT -> stringResource(R.string.match_setup_deuce_golden)
        DeuceMode.STAR_POINT -> stringResource(R.string.match_setup_deuce_star)
    }

    Column(modifier = Modifier.testTag("insights_overview_section")) {
        PpSectionHeader(title = stringResource(R.string.insights_section_overview))
        PpCard(
            accent = bannerAccent,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("insights_overview_card")
        ) {
            // Шапка статуса / результата
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isLive && summary.userWon == true) {
                        Canvas(Modifier.size(48.dp)) {
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(Color(0x33E8638C), Color(0x1AF2B84B), Color.Transparent)
                                )
                            )
                            drawCircle(
                                brush = PpTrophySweep,
                                style = Stroke(width = 1.6.dp.toPx())
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(bannerAccent.copy(alpha = 0.14f))
                                .border(1.dp, bannerAccent.copy(alpha = 0.55f), CircleShape)
                        )
                    }
                    Icon(
                        imageVector = if (isLive) Icons.Rounded.SportsTennis else Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = if (!isLive && summary.userWon == true) PpAmberLed else bannerAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(PpSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PpSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bannerTitle,
                            style = PpTextStyles.Micro,
                            color = bannerAccent
                        )
                        if (isManualDraw) {
                            Box(
                                modifier = Modifier
                                    .clip(PpRadius.pill)
                                    .background(PpAmberLed.copy(alpha = 0.14f))
                                    .padding(horizontal = PpSpacing.sm, vertical = 2.dp)
                                    .testTag("insights_manual_badge")
                            ) {
                                Text(
                                    text = stringResource(R.string.results_manual_finish_badge),
                                    style = PpTextStyles.Overline,
                                    color = PpAmberLed
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = headlineNames,
                        style = PpTextStyles.Title,
                        color = PpTextHigh,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = when {
                        isLive && setsSummaryText.isNotEmpty() ->
                            "${stringResource(R.string.insights_live_subtitle)} · $setsSummaryText"
                        isLive -> stringResource(R.string.insights_live_subtitle)
                        setsSummaryText.isNotEmpty() -> setsSummaryText
                        else -> ""
                    }
                    if (subtitle.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                            color = if (summary.userWon == true) PpLime else PpTextMid
                        )
                    }
                }
            }

            Spacer(Modifier.height(PpSpacing.md))

            // Таблица завершённых сетов
            OverviewSetsTable(
                overview = overview,
                teamAText = teamAText,
                teamBText = teamBText
            )

            Spacer(Modifier.height(PpSpacing.md))

            // Составы с аватарами
            OverviewRosterRow(
                teamTitle = stringResource(R.string.match_setup_team_a),
                accent = PpLime,
                isWinner = !isLive && !isManualDraw && overview.winnerTeam == TeamSide.A,
                players = details.teamAPlayers
            )
            Spacer(Modifier.height(PpSpacing.sm))
            OverviewRosterRow(
                teamTitle = stringResource(R.string.match_setup_team_b),
                accent = PpGlassCyan,
                isWinner = !isLive && !isManualDraw && overview.winnerTeam == TeamSide.B,
                players = details.teamBPlayers
            )

            Spacer(Modifier.height(PpSpacing.md))

            // Метаданные матча
            InsightMetaRow(
                label = stringResource(R.string.results_format_label),
                value = formatText
            )
            Spacer(Modifier.height(PpSpacing.sm))
            InsightMetaRow(
                label = stringResource(R.string.results_scoring_label),
                value = scoringText
            )
            Spacer(Modifier.height(PpSpacing.sm))
            InsightMetaRow(
                label = stringResource(R.string.results_duration_label),
                value = overview.durationMs?.let { formatDurationRu(it) }
                    ?: stringResource(R.string.dash_placeholder)
            )
            Spacer(Modifier.height(PpSpacing.sm))
            InsightMetaRow(
                label = stringResource(R.string.results_date_label),
                value = formattedDate
            )
            if (isManualDraw) {
                Spacer(Modifier.height(PpSpacing.sm))
                InsightMetaRow(
                    label = stringResource(R.string.details_completion_mode_label),
                    value = stringResource(R.string.details_completion_mode_manual)
                )
            }
        }
    }
}

@Composable
private fun OverviewSetsTable(
    overview: MatchOverview,
    teamAText: String,
    teamBText: String
) {
    val sets = overview.sets
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.details_table_team),
            style = PpTextStyles.Micro,
            color = PpTextMid,
            modifier = Modifier.weight(1f)
        )
        if (sets.isEmpty()) {
            Text(
                text = stringResource(R.string.dash_placeholder),
                style = PpTextStyles.Micro,
                color = PpTextMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(44.dp)
            )
        } else {
            sets.forEach { set ->
                Text(
                    text = stringResource(R.string.details_table_set_format, set.setIndex),
                    style = PpTextStyles.Micro,
                    color = PpTextMid,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(44.dp)
                )
            }
        }
    }
    Spacer(Modifier.height(PpSpacing.xs))
    PpSetScoreRow(
        teamLabel = teamAText,
        setScores = if (sets.isEmpty()) listOf(stringResource(R.string.dash_placeholder))
        else sets.map { formatSetCell(it.gamesA, it.tbA, it.tbB) },
        isWinner = overview.winnerTeam == TeamSide.A
    )
    Spacer(Modifier.height(PpSpacing.xs))
    PpSetScoreRow(
        teamLabel = teamBText,
        setScores = if (sets.isEmpty()) listOf(stringResource(R.string.dash_placeholder))
        else sets.map { formatSetCell(it.gamesB, it.tbB, it.tbA) },
        isWinner = overview.winnerTeam == TeamSide.B
    )

    overview.incompleteSet?.let { inc ->
        Spacer(Modifier.height(PpSpacing.sm))
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
                .testTag("insights_overview_incomplete_set")
        ) {
            Text(
                text = incText,
                style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                color = PpGlassCyan
            )
        }
    }
}

@Composable
private fun OverviewRosterRow(
    teamTitle: String,
    accent: Color,
    isWinner: Boolean,
    players: List<MatchParticipantDetail>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = teamTitle,
            style = PpTextStyles.Micro,
            color = if (isWinner) accent else PpTextMid
        )
        Spacer(Modifier.height(PpSpacing.xs))
        if (players.isEmpty()) {
            Text(
                text = stringResource(R.string.dash_placeholder),
                style = PpTextStyles.Label,
                color = PpTextMid
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.xs + 2.dp)) {
                players.forEach { participant ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PpAvatar(
                            avatarId = participant.player.avatarId,
                            size = 32.dp,
                            highlighted = participant.isUser
                        )
                        Spacer(Modifier.width(PpSpacing.sm + 2.dp))
                        Text(
                            text = participant.player.displayName,
                            style = PpTextStyles.Body,
                            color = PpTextHigh,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (participant.isUser) {
                            Spacer(Modifier.width(PpSpacing.sm))
                            Box(
                                modifier = Modifier
                                    .clip(PpRadius.pill)
                                    .background(PpLime.copy(alpha = 0.16f))
                                    .border(1.dp, PpLime.copy(alpha = 0.55f), PpRadius.pill)
                                    .padding(horizontal = PpSpacing.sm, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.label_you),
                                    style = PpTextStyles.Micro,
                                    color = PpLime
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSetCell(games: Int, teamTb: Int?, oppTb: Int?): String {
    return if (teamTb != null && oppTb != null && teamTb < oppTb) {
        "$games($teamTb)"
    } else {
        games.toString()
    }
}
