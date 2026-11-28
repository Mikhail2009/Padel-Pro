package com.padelpro.app.feature.poster

import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.GamesLead
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchHighlight
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.MomentumPoint
import com.padelpro.app.domain.model.TeamSide

enum class PosterTemplate {
    COURT_ATMOSPHERE,
    PREMIUM_MATCH,
    MATCH_PERFORMANCE
}

enum class PosterOutcomeKind {
    USER_WIN,
    USER_LOSS,
    TEAM_WIN,
    MANUAL_NEUTRAL,
    NEUTRAL_COMPLETED
}

data class PosterPlayerData(
    val id: String,
    val displayName: String,
    val avatarId: Int,
    val isUser: Boolean
)

data class PosterTeamData(
    val side: TeamSide,
    val players: List<PosterPlayerData>,
    val isWinner: Boolean,
    val hasUser: Boolean
)

data class PosterOutcome(
    val kind: PosterOutcomeKind,
    val winnerTeam: TeamSide?,
    val userWon: Boolean?,
    val isUserParticipant: Boolean,
    val isManuallyCompleted: Boolean
)

data class PosterSetData(
    val setIndex: Int,
    val gamesA: Int,
    val gamesB: Int,
    val tbA: Int?,
    val tbB: Int?,
    val durationMs: Long?,
    val durationText: String?,
    val pointsPlayed: Int?
)

data class PosterIncompleteSetData(
    val setIndex: Int,
    val gamesA: Int,
    val gamesB: Int,
    val scoreA: String,
    val scoreB: String,
    val isTieBreak: Boolean,
    val pointsPlayed: Int?,
    val durationMs: Long?,
    val durationText: String?
)

data class PosterPointStats(
    val totalPoints: Int,
    val teamAPoints: Int,
    val teamBPoints: Int,
    val teamAPercent: Float?,
    val teamBPercent: Float?,
    val pointDifference: Int,
    val maxPointStreakA: Int,
    val maxPointStreakB: Int,
    val longestLeadSpanA: Int,
    val longestLeadSpanB: Int,
    val pointLeadChanges: Int
)

data class PosterSetPerformance(
    val setIndex: Int,
    val gamesA: Int,
    val gamesB: Int,
    val tbA: Int?,
    val tbB: Int?,
    val winnerTeam: TeamSide,
    val durationMs: Long,
    val durationText: String,
    val pointsPlayed: Int
)

data class PosterGameStats(
    val gamesWonA: Int,
    val gamesWonB: Int,
    val setsWonA: Int,
    val setsWonB: Int,
    val maxGameStreakA: Int,
    val maxGameStreakB: Int,
    val gamesLeadChanges: Int,
    val biggestGamesLead: GamesLead?,
    val setPerformances: List<PosterSetPerformance>,
    val longestSet: PosterSetPerformance?,
    val shortestSet: PosterSetPerformance?
)

data class MatchPosterData(
    val matchId: String,
    val format: MatchFormat,
    val deuceMode: DeuceMode,
    val bestOf: Int?,
    val status: MatchStatus,
    val dateText: String,
    val dateTimeText: String,
    val durationMs: Long?,
    val durationText: String?,
    val teamA: PosterTeamData,
    val teamB: PosterTeamData,
    val outcome: PosterOutcome,
    val setsWonA: Int,
    val setsWonB: Int,
    val completedSets: List<PosterSetData>,
    val incompleteSet: PosterIncompleteSetData?,
    val pointStats: PosterPointStats?,
    val gameStats: PosterGameStats?,
    val momentum: List<MomentumPoint>,
    val highlights: List<MatchHighlight>
)
