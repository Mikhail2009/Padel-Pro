package com.padelpro.app.feature.poster

import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.domain.model.MatchAnalytics
import com.padelpro.app.domain.model.MatchDetailsData
import com.padelpro.app.domain.model.MatchParticipantDetail
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.SetPerformance
import com.padelpro.app.domain.model.TeamSide
import java.util.Locale

class MatchPosterModelMapper(
    private val timeProvider: TimeProvider
) {

    fun map(details: MatchDetailsData): MatchPosterData {
        val summary = details.summary
        val analytics = details.analytics
        val overview = analytics?.overview

        val outcome = mapOutcome(summary, details.teamAPlayers, details.teamBPlayers)

        val teamAPlayers = details.teamAPlayers.map(::mapPlayer)
        val teamBPlayers = details.teamBPlayers.map(::mapPlayer)

        val teamA = PosterTeamData(
            side = TeamSide.A,
            players = teamAPlayers,
            isWinner = outcome.winnerTeam == TeamSide.A,
            hasUser = teamAPlayers.any { it.isUser }
        )
        val teamB = PosterTeamData(
            side = TeamSide.B,
            players = teamBPlayers,
            isWinner = outcome.winnerTeam == TeamSide.B,
            hasUser = teamBPlayers.any { it.isUser }
        )

        val eventTimestamp = summary.endedAt ?: summary.startedAt
        val dateText = timeProvider.formatShortDate(eventTimestamp)
        val dateTimeText = timeProvider.formatShortDateTime(eventTimestamp)
        val durationMs = summary.durationMs
        val durationText = durationMs?.let(::formatDurationRu)

        val setPerformancesByIndex = analytics?.gameStats?.setPerformances
            ?.associateBy { it.setIndex }
            .orEmpty()

        val completedSets = summary.sets.map { set ->
            val perf = setPerformancesByIndex[set.setIndex]
            PosterSetData(
                setIndex = set.setIndex,
                gamesA = set.gamesA,
                gamesB = set.gamesB,
                tbA = set.tbA,
                tbB = set.tbB,
                durationMs = perf?.durationMs,
                durationText = perf?.durationMs?.let(::formatDurationRu),
                pointsPlayed = perf?.pointsPlayed
            )
        }

        val incompleteSet = summary.incompleteSet?.let { inc ->
            val incDurationMs = overview?.incompleteSetDurationMs
            PosterIncompleteSetData(
                setIndex = inc.setIndex,
                gamesA = inc.gamesA,
                gamesB = inc.gamesB,
                scoreA = inc.scoreA,
                scoreB = inc.scoreB,
                isTieBreak = inc.isTieBreak,
                pointsPlayed = overview?.incompleteSetPointsPlayed,
                durationMs = incDurationMs,
                durationText = incDurationMs?.let(::formatDurationRu)
            )
        }

        val setsWonA = overview?.setsWonA ?: summary.sets.count { it.gamesA > it.gamesB }
        val setsWonB = overview?.setsWonB ?: summary.sets.count { it.gamesB > it.gamesA }

        return MatchPosterData(
            matchId = summary.id,
            format = summary.format,
            deuceMode = summary.deuceMode,
            bestOf = overview?.bestOf,
            status = summary.status,
            dateText = dateText,
            dateTimeText = dateTimeText,
            durationMs = durationMs,
            durationText = durationText,
            teamA = teamA,
            teamB = teamB,
            outcome = outcome,
            setsWonA = setsWonA,
            setsWonB = setsWonB,
            completedSets = completedSets,
            incompleteSet = incompleteSet,
            pointStats = analytics?.let(::mapPointStats),
            gameStats = analytics?.let(::mapGameStats),
            momentum = analytics?.momentum?.toList().orEmpty(),
            highlights = analytics?.highlights?.toList().orEmpty()
        )
    }

    private fun mapPlayer(detail: MatchParticipantDetail): PosterPlayerData =
        PosterPlayerData(
            id = detail.player.id,
            displayName = detail.player.displayName,
            avatarId = detail.player.avatarId,
            isUser = detail.isUser
        )

    private fun mapOutcome(
        summary: MatchSummary,
        teamAPlayers: List<MatchParticipantDetail>,
        teamBPlayers: List<MatchParticipantDetail>
    ): PosterOutcome {
        val isUserParticipant = summary.isUserParticipant ||
            teamAPlayers.any { it.isUser } ||
            teamBPlayers.any { it.isUser }

        if (summary.isManuallyCompleted) {
            return PosterOutcome(
                kind = PosterOutcomeKind.MANUAL_NEUTRAL,
                winnerTeam = null,
                userWon = null,
                isUserParticipant = isUserParticipant,
                isManuallyCompleted = true
            )
        }

        val winnerTeam = summary.winnerTeam
        val kind = when {
            winnerTeam == null -> PosterOutcomeKind.NEUTRAL_COMPLETED
            isUserParticipant && summary.userWon == true -> PosterOutcomeKind.USER_WIN
            isUserParticipant && summary.userWon == false -> PosterOutcomeKind.USER_LOSS
            else -> PosterOutcomeKind.TEAM_WIN
        }

        return PosterOutcome(
            kind = kind,
            winnerTeam = winnerTeam,
            userWon = if (isUserParticipant) summary.userWon else null,
            isUserParticipant = isUserParticipant,
            isManuallyCompleted = false
        )
    }

    private fun mapPointStats(analytics: MatchAnalytics): PosterPointStats {
        val ps = analytics.pointStats
        return PosterPointStats(
            totalPoints = ps.totalPoints,
            teamAPoints = ps.teamAPoints,
            teamBPoints = ps.teamBPoints,
            teamAPercent = ps.teamAPointPercent,
            teamBPercent = ps.teamBPointPercent,
            pointDifference = ps.pointDifference,
            maxPointStreakA = ps.maxPointStreakA,
            maxPointStreakB = ps.maxPointStreakB,
            longestLeadSpanA = ps.longestLeadSpanA,
            longestLeadSpanB = ps.longestLeadSpanB,
            pointLeadChanges = ps.pointLeadChanges
        )
    }

    private fun mapGameStats(analytics: MatchAnalytics): PosterGameStats {
        val gs = analytics.gameStats
        return PosterGameStats(
            gamesWonA = gs.gamesWonA,
            gamesWonB = gs.gamesWonB,
            setsWonA = gs.setsWonA,
            setsWonB = gs.setsWonB,
            maxGameStreakA = gs.maxGameStreakA,
            maxGameStreakB = gs.maxGameStreakB,
            gamesLeadChanges = gs.gamesLeadChanges,
            biggestGamesLead = gs.biggestGamesLead,
            setPerformances = gs.setPerformances.map(::mapSetPerformance),
            longestSet = gs.longestSet?.let(::mapSetPerformance),
            shortestSet = gs.shortestSet?.let(::mapSetPerformance)
        )
    }

    private fun mapSetPerformance(perf: SetPerformance): PosterSetPerformance =
        PosterSetPerformance(
            setIndex = perf.setIndex,
            gamesA = perf.gamesA,
            gamesB = perf.gamesB,
            tbA = perf.tbA,
            tbB = perf.tbB,
            winnerTeam = perf.winnerTeam,
            durationMs = perf.durationMs,
            durationText = formatDurationRu(perf.durationMs),
            pointsPlayed = perf.pointsPlayed
        )

    private fun formatDurationRu(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val locale = Locale.forLanguageTag("ru")
        return when {
            hours > 0L -> String.format(locale, "%d ч %02d мин", hours, minutes)
            minutes > 0L -> String.format(locale, "%d мин %02d сек", minutes, seconds)
            else -> String.format(locale, "%d сек", seconds)
        }
    }
}
