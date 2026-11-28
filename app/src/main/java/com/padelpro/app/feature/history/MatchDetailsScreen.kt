package com.padelpro.app.feature.history

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
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.padelpro.app.core.designsystem.components.PpConfirmDialog
import com.padelpro.app.core.designsystem.components.PpDangerOutlineButton
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.components.PpSetScoreRow
import com.padelpro.app.core.designsystem.components.formatDurationRu
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpCourtLine
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
import com.padelpro.app.domain.model.MatchDetailsData
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchParticipantDetail
import com.padelpro.app.domain.model.TeamSide

@Composable
fun MatchDetailsScreen(
    matchId: String,
    viewModel: MatchDetailsViewModel,
    onBack: () -> Unit,
    onOpenInsights: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(matchId) {
        viewModel.loadMatch(matchId)
    }

    LaunchedEffect(Unit) {
        viewModel.deletedEvent.collect {
            onBack()
        }
    }

    BackHandler(enabled = !state.isDeleting, onBack = onBack)

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
                        DetailsTopBar(onBack = onBack)
                    }
                    item {
                        PpEmptyState(
                            icon = Icons.Rounded.SportsTennis,
                            title = stringResource(R.string.details_empty_title),
                            subtitle = stringResource(R.string.details_empty_body),
                            modifier = Modifier.testTag("details_empty_state")
                        )
                    }
                }
            }

            else -> {
                val details = state.details!!
                MatchDetailsBody(
                    details = details,
                    formattedDate = viewModel.formatDateTime(details.summary.endedAt ?: details.summary.startedAt),
                    isDeleting = state.isDeleting,
                    onBack = onBack,
                    onOpenInsights = { onOpenInsights(details.summary.id) },
                    onRequestDelete = { showDeleteDialog = true }
                )
            }
        }
    }

    if (showDeleteDialog) {
        PpConfirmDialog(
            title = stringResource(R.string.details_delete_confirm_title),
            body = stringResource(R.string.details_delete_confirm_body),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteMatch()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun DetailsTopBar(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PpIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack,
            modifier = Modifier.testTag("details_back_button")
        )
        Spacer(Modifier.width(PpSpacing.sm))
        Text(
            text = stringResource(R.string.details_title),
            style = PpTextStyles.H1,
            color = PpTextHigh
        )
    }
}

@Composable
private fun MatchDetailsBody(
    details: MatchDetailsData,
    formattedDate: String,
    isDeleting: Boolean,
    onBack: () -> Unit,
    onOpenInsights: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val summary = details.summary
    val isManualDraw = summary.isManuallyCompleted
    val bannerAccent = when {
        summary.userWon == true -> PpLime
        summary.userWon == false -> PpTrophyMagenta
        else -> PpGlassCyan
    }
    val teamAText = summary.teamANames.joinToString(" & ")
        .ifBlank { stringResource(R.string.live_match_team_a_placeholder) }
    val teamBText = summary.teamBNames.joinToString(" & ")
        .ifBlank { stringResource(R.string.live_match_team_b_placeholder) }
    val winnerLabel = when {
        isManualDraw -> stringResource(R.string.results_manual_finish_badge)
        summary.winnerTeam == TeamSide.A -> stringResource(R.string.results_winner_team_a)
        summary.winnerTeam == TeamSide.B -> stringResource(R.string.results_winner_team_b)
        else -> ""
    }
    val winnerNames = when {
        isManualDraw -> stringResource(R.string.match_card_teams_vs_format, teamAText, teamBText)
        summary.winnerTeam == TeamSide.A -> teamAText
        summary.winnerTeam == TeamSide.B -> teamBText
        else -> stringResource(R.string.dash_placeholder)
    }
    val bannerTitle = when {
        summary.userWon == true -> stringResource(R.string.details_winner_banner_win)
        summary.userWon == false -> stringResource(R.string.details_winner_banner_loss)
        isManualDraw -> stringResource(R.string.details_winner_banner_draw)
        else -> stringResource(R.string.details_winner_banner_neutral)
    }
    val setsSummaryText = summary.sets.joinToString(" · ") { set ->
        val tb = if (set.tbA != null && set.tbB != null) {
            "(${minOf(set.tbA, set.tbB)})"
        } else ""
        "${set.gamesA}-${set.gamesB}$tb"
    }.ifBlank { stringResource(R.string.dash_placeholder) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .testTag("details_list"),
        contentPadding = PaddingValues(PpSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
    ) {
        item {
            DetailsTopBar(onBack = onBack)
        }

        // 1. Winner banner card
        item {
            PpCard(
                accent = bannerAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("details_winner_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (summary.userWon == true) {
                            Canvas(Modifier.size(50.dp)) {
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
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(bannerAccent.copy(alpha = 0.14f))
                                    .border(1.dp, bannerAccent.copy(alpha = 0.55f), CircleShape)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = if (summary.userWon == true) PpAmberLed else bannerAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(PpSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bannerTitle,
                            style = PpTextStyles.Micro,
                            color = bannerAccent
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = winnerNames,
                            style = PpTextStyles.Title,
                            color = PpTextHigh,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (winnerLabel.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$winnerLabel · $setsSummaryText",
                                style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                                color = if (summary.userWon == true) PpLime else PpTextMid
                            )
                        }
                    }
                }
            }
        }

        // 2. Set-by-set score table
        item {
            PpSectionHeader(title = stringResource(R.string.results_sets_section))
            Spacer(Modifier.height(PpSpacing.sm))
            PpCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("details_sets_table")
            ) {
                val sets = summary.sets
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
                Spacer(Modifier.height(PpSpacing.sm))
                PpSetScoreRow(
                    teamLabel = teamAText,
                    setScores = if (sets.isEmpty()) listOf(stringResource(R.string.dash_placeholder))
                    else sets.map { formatSetCell(it.gamesA, it.tbA, it.tbB) },
                    isWinner = summary.winnerTeam == TeamSide.A
                )
                Spacer(Modifier.height(PpSpacing.xs))
                PpSetScoreRow(
                    teamLabel = teamBText,
                    setScores = if (sets.isEmpty()) listOf(stringResource(R.string.dash_placeholder))
                    else sets.map { formatSetCell(it.gamesB, it.tbB, it.tbA) },
                    isWinner = summary.winnerTeam == TeamSide.B
                )
                summary.incompleteSet?.let { inc ->
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
                            .testTag("details_incomplete_set_box")
                    ) {
                        Text(
                            text = incText,
                            style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                            color = PpGlassCyan
                        )
                    }
                }
            }
        }

        // 3. Team rosters
        item {
            PpSectionHeader(title = stringResource(R.string.details_section_teams))
            Spacer(Modifier.height(PpSpacing.sm))
            PpCard(modifier = Modifier.fillMaxWidth()) {
                RosterTeamBlock(
                    teamTitle = stringResource(R.string.match_setup_team_a),
                    accent = PpLime,
                    isWinner = summary.winnerTeam == TeamSide.A,
                    players = details.teamAPlayers
                )
                Spacer(Modifier.height(PpSpacing.md))
                RosterTeamBlock(
                    teamTitle = stringResource(R.string.match_setup_team_b),
                    accent = PpGlassCyan,
                    isWinner = summary.winnerTeam == TeamSide.B,
                    players = details.teamBPlayers
                )
            }
        }

        // 4. Match metadata (format, scoring mode, duration, date)
        item {
            PpSectionHeader(title = stringResource(R.string.details_section_info))
            Spacer(Modifier.height(PpSpacing.sm))
            PpCard(modifier = Modifier.fillMaxWidth()) {
                val formatText = when (summary.format) {
                    MatchFormat.SINGLES -> stringResource(R.string.match_format_singles)
                    MatchFormat.DOUBLES -> stringResource(R.string.match_format_doubles)
                }
                val scoringText = when (summary.deuceMode) {
                    DeuceMode.ADVANTAGE -> stringResource(R.string.match_setup_deuce_advantage)
                    DeuceMode.GOLDEN_POINT -> stringResource(R.string.match_setup_deuce_golden)
                    DeuceMode.STAR_POINT -> stringResource(R.string.match_setup_deuce_star)
                }

                DetailMetaRow(
                    label = stringResource(R.string.results_format_label),
                    value = formatText
                )
                Spacer(Modifier.height(PpSpacing.sm))
                DetailMetaRow(
                    label = stringResource(R.string.results_scoring_label),
                    value = scoringText
                )
                Spacer(Modifier.height(PpSpacing.sm))
                DetailMetaRow(
                    label = stringResource(R.string.results_duration_label),
                    value = summary.durationMs?.let { formatDurationRu(it) }
                        ?: stringResource(R.string.dash_placeholder)
                )
                Spacer(Modifier.height(PpSpacing.sm))
                DetailMetaRow(
                    label = stringResource(R.string.results_date_label),
                    value = formattedDate
                )
                if (isManualDraw) {
                    Spacer(Modifier.height(PpSpacing.sm))
                    DetailMetaRow(
                        label = stringResource(R.string.details_completion_mode_label),
                        value = stringResource(R.string.details_completion_mode_manual)
                    )
                }
            }
        }

        // 5. Event-derived statistics (hidden completely if no POINT_WON events exist)
        val stats = details.stats
        if (stats != null) {
            item {
                PpSectionHeader(title = stringResource(R.string.details_section_stats))
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(
                    accent = PpGlassCyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("details_stats_card")
                ) {
                    DetailMetaRow(
                        label = stringResource(R.string.details_total_points),
                        value = stats.totalPoints.toString()
                    )
                    Spacer(Modifier.height(PpSpacing.sm))
                    DetailMetaRow(
                        label = stringResource(R.string.details_points_won),
                        value = stringResource(
                            R.string.details_points_team_ratio_format,
                            stats.teamAPoints,
                            stats.teamBPoints
                        )
                    )
                    if (stats.totalPoints > 0) {
                        Spacer(Modifier.height(PpSpacing.sm))
                        PointsComparisonBar(
                            teamAPoints = stats.teamAPoints,
                            teamBPoints = stats.teamBPoints
                        )
                    }
                    if (stats.longestSetIndex != null && stats.longestSetDurationMs != null) {
                        Spacer(Modifier.height(PpSpacing.sm))
                        DetailMetaRow(
                            label = stringResource(R.string.details_longest_set),
                            value = stringResource(
                                R.string.details_longest_set_format,
                                stats.longestSetIndex,
                                formatDurationRu(stats.longestSetDurationMs)
                            )
                        )
                    }
                }
            }
        }

        // 6. Match Insights & Delete match button
        item {
            Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.md)) {
                PpSecondaryButton(
                    text = stringResource(R.string.insights_entry_btn),
                    leadingIcon = Icons.Rounded.Leaderboard,
                    onClick = onOpenInsights,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("details_insights_button")
                )
                PpDangerOutlineButton(
                    text = stringResource(R.string.details_delete_btn),
                    onClick = onRequestDelete,
                    enabled = !isDeleting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("details_delete_button")
                )
            }
        }
    }
}

@Composable
private fun PointsComparisonBar(
    teamAPoints: Int,
    teamBPoints: Int
) {
    val total = (teamAPoints + teamBPoints).coerceAtLeast(1)
    val ratioA = (teamAPoints.toFloat() / total).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val r = size.height / 2f
            drawRoundRect(
                color = PpCourtLine,
                cornerRadius = CornerRadius(r, r)
            )
            val widthA = size.width * ratioA
            if (widthA > 0f) {
                drawRoundRect(
                    color = PpLime,
                    topLeft = Offset.Zero,
                    size = Size(widthA, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            }
            val widthB = size.width - widthA
            if (widthB > 0f) {
                drawRoundRect(
                    color = PpGlassCyan,
                    topLeft = Offset(widthA, 0f),
                    size = Size(widthB, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            }
        }
        Spacer(Modifier.height(PpSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(R.string.match_setup_team_a)}: $teamAPoints",
                style = PpTextStyles.Overline.copy(fontFeatureSettings = "tnum"),
                color = PpLime
            )
            Text(
                text = "${stringResource(R.string.match_setup_team_b)}: $teamBPoints",
                style = PpTextStyles.Overline.copy(fontFeatureSettings = "tnum"),
                color = PpGlassCyan
            )
        }
    }
}

@Composable
private fun RosterTeamBlock(
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
        Spacer(Modifier.height(PpSpacing.sm))
        if (players.isEmpty()) {
            Text(
                text = stringResource(R.string.dash_placeholder),
                style = PpTextStyles.Label,
                color = PpTextMid
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
                players.forEach { participant ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PpAvatar(
                            avatarId = participant.player.avatarId,
                            size = 36.dp,
                            highlighted = participant.isUser
                        )
                        Spacer(Modifier.width(PpSpacing.md))
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

@Composable
private fun DetailMetaRow(label: String, value: String) {
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
            style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
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
