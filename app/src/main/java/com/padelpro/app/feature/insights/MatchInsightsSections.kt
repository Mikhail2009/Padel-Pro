package com.padelpro.app.feature.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.components.PpStatCard
import com.padelpro.app.core.designsystem.components.formatDurationRu
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.GameStats
import com.padelpro.app.domain.model.IncompleteSetScore
import com.padelpro.app.domain.model.MatchHighlight
import com.padelpro.app.domain.model.MatchOverview
import com.padelpro.app.domain.model.MomentumPoint
import com.padelpro.app.domain.model.PointStats
import com.padelpro.app.domain.model.SetPerformance
import com.padelpro.app.domain.model.SetScore
import com.padelpro.app.domain.model.TeamSide
import kotlin.math.roundToInt

@Composable
fun PointStatsSection(
    pointStats: PointStats,
    teamALabel: String,
    teamBLabel: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.testTag("insights_points_section")) {
        PpSectionHeader(title = stringResource(R.string.insights_section_points))
        PpCard(
            accent = PpLime,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (pointStats.totalPoints == 0) {
                Text(
                    text = stringResource(R.string.insights_points_empty),
                    style = PpTextStyles.Body,
                    color = PpTextMid
                )
            } else {
                val pctA = pointStats.teamAPointPercent?.roundToInt()
                val pctB = pointStats.teamBPointPercent?.roundToInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    PpStatCard(
                        value = pointStats.totalPoints.toString(),
                        label = stringResource(R.string.insights_points_total),
                        accent = PpAmberLed,
                        modifier = Modifier.weight(1f)
                    )
                    val diffPrefix = if (pointStats.pointDifference > 0) "+" else ""
                    PpStatCard(
                        value = "$diffPrefix${pointStats.pointDifference}",
                        label = stringResource(R.string.insights_points_diff),
                        accent = when {
                            pointStats.pointDifference > 0 -> PpLime
                            pointStats.pointDifference < 0 -> PpGlassCyan
                            else -> PpTextMid
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(PpSpacing.md))

                DualTeamBar(
                    teamALabel = teamALabel,
                    teamBLabel = teamBLabel,
                    valueA = pointStats.teamAPoints,
                    valueB = pointStats.teamBPoints,
                    suffixA = pctA?.let { " (${stringResource(R.string.insights_points_percent_format, it)})" }.orEmpty(),
                    suffixB = pctB?.let { " (${stringResource(R.string.insights_points_percent_format, it)})" }.orEmpty()
                )

                Spacer(Modifier.height(PpSpacing.md))

                InsightMetaRow(
                    label = stringResource(R.string.insights_points_max_streak),
                    value = stringResource(
                        R.string.insights_team_pair_format,
                        pointStats.maxPointStreakA.toString(),
                        pointStats.maxPointStreakB.toString()
                    )
                )
                Spacer(Modifier.height(PpSpacing.sm))
                InsightMetaRow(
                    label = stringResource(R.string.insights_points_lead_span),
                    value = stringResource(
                        R.string.insights_team_pair_format,
                        pluralStringResource(R.plurals.rallies_count_format, pointStats.longestLeadSpanA, pointStats.longestLeadSpanA),
                        pluralStringResource(R.plurals.rallies_count_format, pointStats.longestLeadSpanB, pointStats.longestLeadSpanB)
                    )
                )
                Spacer(Modifier.height(PpSpacing.sm))
                InsightMetaRow(
                    label = stringResource(R.string.insights_points_lead_changes),
                    value = pointStats.pointLeadChanges.toString()
                )
            }
        }
    }
}

@Composable
fun GameStatsSection(
    gameStats: GameStats,
    teamALabel: String,
    teamBLabel: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.testTag("insights_games_section")) {
        PpSectionHeader(title = stringResource(R.string.insights_section_games))
        PpCard(
            accent = PpGlassCyan,
            modifier = Modifier.fillMaxWidth()
        ) {
            val totalGames = gameStats.gamesWonA + gameStats.gamesWonB
            if (totalGames == 0) {
                Text(
                    text = stringResource(R.string.insights_games_empty),
                    style = PpTextStyles.Body,
                    color = PpTextMid
                )
            } else {
                DualTeamBar(
                    teamALabel = teamALabel,
                    teamBLabel = teamBLabel,
                    valueA = gameStats.gamesWonA,
                    valueB = gameStats.gamesWonB,
                    title = stringResource(R.string.insights_games_won)
                )

                Spacer(Modifier.height(PpSpacing.md))

                InsightMetaRow(
                    label = stringResource(R.string.insights_games_max_streak),
                    value = stringResource(
                        R.string.insights_team_pair_format,
                        gameStats.maxGameStreakA.toString(),
                        gameStats.maxGameStreakB.toString()
                    )
                )
                Spacer(Modifier.height(PpSpacing.sm))
                InsightMetaRow(
                    label = stringResource(R.string.insights_games_lead_changes),
                    value = gameStats.gamesLeadChanges.toString()
                )
                gameStats.biggestGamesLead?.let { lead ->
                    Spacer(Modifier.height(PpSpacing.sm))
                    val leadTeam = if (lead.team == TeamSide.A) teamALabel else teamBLabel
                    InsightMetaRow(
                        label = stringResource(R.string.insights_games_biggest_lead),
                        value = stringResource(
                            R.string.insights_games_biggest_lead_format,
                            leadTeam,
                            lead.lead,
                            lead.gameIndex
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SetStatsSection(
    overview: MatchOverview,
    gameStats: GameStats,
    teamALabel: String,
    teamBLabel: String,
    modifier: Modifier = Modifier
) {
    val perfByIndex = gameStats.setPerformances.associateBy { it.setIndex }

    Column(modifier = modifier.testTag("insights_sets_section")) {
        PpSectionHeader(title = stringResource(R.string.insights_section_sets))

        if (overview.sets.isEmpty() && overview.incompleteSet == null) {
            PpCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.insights_sets_empty),
                    style = PpTextStyles.Body,
                    color = PpTextMid
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.md)) {
                overview.sets.forEach { setScore ->
                    CompletedSetCard(
                        setScore = setScore,
                        performance = perfByIndex[setScore.setIndex],
                        teamALabel = teamALabel,
                        teamBLabel = teamBLabel
                    )
                }

                if (overview.sets.isNotEmpty() && gameStats.setPerformances.isEmpty()) {
                    Text(
                        text = stringResource(R.string.insights_sets_no_event_timing),
                        style = PpTextStyles.Label,
                        color = PpTextLow
                    )
                }

                overview.incompleteSet?.let { inc ->
                    IncompleteSetCard(
                        incompleteSet = inc,
                        pointsPlayed = overview.incompleteSetPointsPlayed,
                        durationMs = overview.incompleteSetDurationMs
                    )
                }

                if (overview.sets.size >= 2) {
                    SetComparisonSummaries(
                        gameStats = gameStats
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedSetCard(
    setScore: SetScore,
    performance: SetPerformance?,
    teamALabel: String,
    teamBLabel: String
) {
    val winnerSide = when {
        setScore.gamesA > setScore.gamesB -> TeamSide.A
        setScore.gamesB > setScore.gamesA -> TeamSide.B
        else -> null
    }
    val winnerColor = when (winnerSide) {
        TeamSide.A -> PpLime
        TeamSide.B -> PpGlassCyan
        null -> PpTextMid
    }
    val winnerLabel = when (winnerSide) {
        TeamSide.A -> teamALabel
        TeamSide.B -> teamBLabel
        null -> stringResource(R.string.dash_placeholder)
    }

    PpCard(
        accent = winnerColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_set_card_${setScore.setIndex}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.insights_set_card_title_format, setScore.setIndex),
                    style = PpTextStyles.Micro,
                    color = PpTextMid
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.insights_set_winner_format, winnerLabel),
                    style = PpTextStyles.Label,
                    color = winnerColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(PpSpacing.md))
            Text(
                text = "${setScore.gamesA}–${setScore.gamesB}",
                style = PpTextStyles.Numeral,
                color = winnerColor
            )
        }

        val hasTb = setScore.tbA != null && setScore.tbB != null
        if (hasTb || performance != null) {
            Spacer(Modifier.height(PpSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasTb) {
                    SetMetricPill(
                        text = stringResource(
                            R.string.insights_set_tiebreak_format,
                            setScore.tbA!!,
                            setScore.tbB!!
                        ),
                        tint = PpAmberLed
                    )
                }
                if (performance != null) {
                    SetMetricPill(
                        text = formatDurationRu(performance.durationMs),
                        tint = PpTextHigh
                    )
                    SetMetricPill(
                        text = pluralStringResource(
                            R.plurals.points_count_format,
                            performance.pointsPlayed,
                            performance.pointsPlayed
                        ),
                        tint = PpTextMid
                    )
                }
            }
        }
    }
}

@Composable
private fun IncompleteSetCard(
    incompleteSet: IncompleteSetScore,
    pointsPlayed: Int?,
    durationMs: Long?
) {
    val scoreA = if (incompleteSet.scoreA == "AD") {
        stringResource(R.string.live_score_advantage_short)
    } else {
        incompleteSet.scoreA
    }
    val scoreB = if (incompleteSet.scoreB == "AD") {
        stringResource(R.string.live_score_advantage_short)
    } else {
        incompleteSet.scoreB
    }
    val incText = if (incompleteSet.isTieBreak) {
        stringResource(
            R.string.incomplete_set_tiebreak_score_format,
            incompleteSet.setIndex,
            incompleteSet.gamesA,
            incompleteSet.gamesB,
            scoreA,
            scoreB
        )
    } else {
        stringResource(
            R.string.incomplete_set_score_format,
            incompleteSet.setIndex,
            incompleteSet.gamesA,
            incompleteSet.gamesB,
            scoreA,
            scoreB
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PpRadius.cardShape)
            .background(PpGlassCyan.copy(alpha = 0.10f))
            .border(1.dp, PpGlassCyan.copy(alpha = 0.4f), PpRadius.cardShape)
            .padding(PpSpacing.lg)
            .testTag("insights_incomplete_set_card")
    ) {
        Text(
            text = stringResource(R.string.incomplete_set_label_format, incompleteSet.setIndex),
            style = PpTextStyles.Micro,
            color = PpGlassCyan
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = incText,
            style = PpTextStyles.Title.copy(fontFeatureSettings = "tnum"),
            color = PpTextHigh
        )
        if (pointsPlayed != null || durationMs != null) {
            Spacer(Modifier.height(PpSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
                if (pointsPlayed != null) {
                    SetMetricPill(
                        text = pluralStringResource(R.plurals.points_count_format, pointsPlayed, pointsPlayed),
                        tint = PpGlassCyan
                    )
                }
                if (durationMs != null) {
                    SetMetricPill(
                        text = formatDurationRu(durationMs),
                        tint = PpTextHigh
                    )
                }
            }
        }
    }
}

@Composable
private fun SetComparisonSummaries(gameStats: GameStats) {
    PpCard(modifier = Modifier.fillMaxWidth()) {
        val entries = buildList {
            gameStats.longestSet?.let {
                add(
                    stringResource(R.string.insights_set_summary_longest) to
                        stringResource(
                            R.string.details_longest_set_format,
                            it.setIndex,
                            formatDurationRu(it.durationMs)
                        )
                )
            }
            gameStats.shortestSet?.takeIf { it.setIndex != gameStats.longestSet?.setIndex }?.let {
                add(
                    stringResource(R.string.insights_set_summary_shortest) to
                        stringResource(
                            R.string.details_longest_set_format,
                            it.setIndex,
                            formatDurationRu(it.durationMs)
                        )
                )
            }
            gameStats.mostDominantSet?.let {
                add(
                    stringResource(R.string.insights_set_summary_dominant) to
                        stringResource(
                            R.string.insights_set_summary_score_format,
                            it.setIndex,
                            it.gamesA,
                            it.gamesB
                        )
                )
            }
            gameStats.closestSet?.takeIf { it.setIndex != gameStats.mostDominantSet?.setIndex }?.let {
                add(
                    stringResource(R.string.insights_set_summary_closest) to
                        stringResource(
                            R.string.insights_set_summary_score_format,
                            it.setIndex,
                            it.gamesA,
                            it.gamesB
                        )
                )
            }
        }
        entries.forEachIndexed { idx, (label, value) ->
            if (idx > 0) Spacer(Modifier.height(PpSpacing.sm))
            InsightMetaRow(label = label, value = value)
        }
    }
}

@Composable
fun MomentumSection(
    momentum: List<MomentumPoint>,
    teamALabel: String,
    teamBLabel: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.testTag("insights_momentum_section")) {
        PpSectionHeader(title = stringResource(R.string.insights_section_momentum))
        PpCard(modifier = Modifier.fillMaxWidth()) {
            if (momentum.isEmpty()) {
                Text(
                    text = stringResource(R.string.insights_momentum_empty),
                    style = PpTextStyles.Body,
                    color = PpTextMid
                )
            } else {
                MomentumChartCardContent(
                    momentum = momentum,
                    teamALabel = teamALabel,
                    teamBLabel = teamBLabel
                )
            }
        }
    }
}

@Composable
fun HighlightsSection(
    highlights: List<MatchHighlight>,
    teamALabel: String,
    teamBLabel: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.testTag("insights_highlights_section")) {
        PpSectionHeader(title = stringResource(R.string.insights_section_highlights))
        if (highlights.isEmpty()) {
            PpCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.insights_highlights_empty),
                    style = PpTextStyles.Body,
                    color = PpTextMid
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.sm + 2.dp)) {
                highlights.forEach { highlight ->
                    HighlightCard(
                        highlight = highlight,
                        teamALabel = teamALabel,
                        teamBLabel = teamBLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightCard(
    highlight: MatchHighlight,
    teamALabel: String,
    teamBLabel: String
) {
    fun teamName(team: TeamSide) = if (team == TeamSide.A) teamALabel else teamBLabel
    fun teamAccent(team: TeamSide) = if (team == TeamSide.A) PpLime else PpGlassCyan

    val icon: ImageVector
    val accent: Color
    val title: String
    val desc: String

    when (highlight) {
        is MatchHighlight.LongestPointStreak -> {
            icon = Icons.Rounded.LocalFireDepartment
            accent = teamAccent(highlight.team)
            title = stringResource(R.string.insights_hl_point_streak_title)
            val pointsStr = pluralStringResource(R.plurals.points_count_format, highlight.length, highlight.length)
            desc = stringResource(
                R.string.insights_hl_point_streak_desc,
                teamName(highlight.team),
                pointsStr,
                highlight.startRallyIndex,
                highlight.endRallyIndex
            )
        }

        is MatchHighlight.BiggestPointLead -> {
            icon = Icons.Rounded.TrendingUp
            accent = teamAccent(highlight.team)
            title = stringResource(R.string.insights_hl_biggest_lead_title)
            desc = stringResource(
                R.string.insights_hl_biggest_lead_desc,
                teamName(highlight.team),
                highlight.lead,
                highlight.rallyIndex
            )
        }

        is MatchHighlight.MostDominantSet -> {
            icon = Icons.Rounded.EmojiEvents
            accent = teamAccent(highlight.winnerTeam)
            title = stringResource(R.string.insights_hl_dominant_set_title)
            desc = stringResource(
                R.string.insights_hl_dominant_set_desc,
                highlight.setIndex,
                teamName(highlight.winnerTeam),
                highlight.gamesWinner,
                highlight.gamesLoser
            )
        }

        is MatchHighlight.ClosestSet -> {
            icon = Icons.Rounded.Leaderboard
            accent = PpAmberLed
            title = stringResource(R.string.insights_hl_closest_set_title)
            desc = if (highlight.hadTieBreak) {
                stringResource(
                    R.string.insights_hl_closest_set_tb_desc,
                    highlight.setIndex,
                    highlight.gamesA,
                    highlight.gamesB
                )
            } else {
                stringResource(
                    R.string.insights_hl_closest_set_desc,
                    highlight.setIndex,
                    highlight.gamesA,
                    highlight.gamesB
                )
            }
        }

        is MatchHighlight.LongestSet -> {
            icon = Icons.Rounded.Schedule
            accent = PpAmberLed
            title = stringResource(R.string.insights_hl_longest_set_title)
            desc = stringResource(
                R.string.insights_hl_longest_set_desc,
                highlight.setIndex,
                formatDurationRu(highlight.durationMs)
            )
        }

        is MatchHighlight.BestGameStreak -> {
            icon = Icons.Rounded.Bolt
            accent = teamAccent(highlight.team)
            title = stringResource(R.string.insights_hl_game_streak_title)
            val gamesStr = pluralStringResource(R.plurals.games_count_format, highlight.length, highlight.length)
            desc = stringResource(
                R.string.insights_hl_game_streak_desc,
                teamName(highlight.team),
                gamesStr,
                highlight.startGameIndex,
                highlight.endGameIndex
            )
        }
    }

    PpCard(
        accent = accent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(PpSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PpTextStyles.Title,
                    color = PpTextHigh,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                    color = PpTextMid
                )
            }
        }
    }
}

@Composable
private fun DualTeamBar(
    teamALabel: String,
    teamBLabel: String,
    valueA: Int,
    valueB: Int,
    suffixA: String = "",
    suffixB: String = "",
    title: String? = null
) {
    val total = (valueA + valueB).coerceAtLeast(1)
    val ratioA = (valueA.toFloat() / total).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = PpTextStyles.Micro,
                    color = PpTextMid
                )
                Text(
                    text = "$valueA / $valueB",
                    style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                    color = PpTextHigh
                )
            }
            Spacer(Modifier.height(PpSpacing.sm))
        }
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
        Spacer(Modifier.height(PpSpacing.xs + 2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$teamALabel: $valueA$suffixA",
                style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
                color = PpLime,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(PpSpacing.sm))
            Text(
                text = "$teamBLabel: $valueB$suffixB",
                style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
                color = PpGlassCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SetMetricPill(
    text: String,
    tint: Color
) {
    Box(
        modifier = Modifier
            .clip(PpRadius.pill)
            .background(PpCourtGlass)
            .border(1.dp, PpCourtLine, PpRadius.pill)
            .padding(horizontal = PpSpacing.md, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
fun InsightMetaRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
