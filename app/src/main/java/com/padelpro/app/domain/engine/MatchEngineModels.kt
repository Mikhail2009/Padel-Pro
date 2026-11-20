package com.padelpro.app.domain.engine

import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.SetScore
import com.padelpro.app.domain.model.TeamSide

fun TeamSide.other(): TeamSide = when (this) {
    TeamSide.A -> TeamSide.B
    TeamSide.B -> TeamSide.A
}

data class MatchConfig(
    val matchId: String,
    val format: MatchFormat,
    val deuceMode: DeuceMode,
    val bestOf: Int = 3,
    val firstServerTeam: TeamSide,
    val startedAtMillis: Long,
    val teamAPlayerNames: List<String> = emptyList(),
    val teamBPlayerNames: List<String> = emptyList()
) {
    init {
        require(bestOf >= 1 && bestOf % 2 == 1) { "bestOf must be a positive odd number" }
    }

    val setsToWin: Int get() = (bestOf / 2) + 1
}

sealed interface MatchEngineEvent {
    val timestampMillis: Long

    data class PointWon(
        val team: TeamSide,
        override val timestampMillis: Long
    ) : MatchEngineEvent

    data class Paused(
        override val timestampMillis: Long
    ) : MatchEngineEvent

    data class Resumed(
        override val timestampMillis: Long
    ) : MatchEngineEvent
}

enum class RejectReason {
    MATCH_ALREADY_COMPLETED,
    MATCH_PAUSED,
    ALREADY_PAUSED,
    NOT_PAUSED
}

sealed interface EngineReduceResult {
    val state: MatchEngineState

    data class Accepted(override val state: MatchEngineState) : EngineReduceResult
    data class Rejected(
        override val state: MatchEngineState,
        val reason: RejectReason
    ) : EngineReduceResult
}

data class MatchEngineState(
    val config: MatchConfig,
    val status: MatchStatus,
    val winnerTeam: TeamSide?,
    val completedSets: List<SetScore>,
    val currentSetIndex: Int,
    val gamesA: Int,
    val gamesB: Int,
    val pointsA: Int,
    val pointsB: Int,
    val advantageTeam: TeamSide?,
    val deuceAdvantagesLost: Int,
    val isTieBreak: Boolean,
    val tbPointsA: Int,
    val tbPointsB: Int,
    val currentGameServerTeam: TeamSide,
    val teamAServiceGamesInSet: Int,
    val teamBServiceGamesInSet: Int,
    val tbFirstServerTeam: TeamSide?,
    val totalPointsWonA: Int,
    val totalPointsWonB: Int,
    val isPaused: Boolean,
    val accumulatedPlayMs: Long,
    val activeSegmentStartedAtMillis: Long?,
    // Derived flags & serving info
    val isDeuce: Boolean,
    val isGoldenPointActive: Boolean,
    val isStarPointDeciderActive: Boolean,
    val isSetPointA: Boolean,
    val isSetPointB: Boolean,
    val isMatchPointA: Boolean,
    val isMatchPointB: Boolean,
    val shouldChangeEnds: Boolean,
    val servingTeam: TeamSide,
    val servingSlotInTeam: Int,
    val servingPlayerName: String,
    val serveSideFromRight: Boolean
) {
    val setsWonA: Int
        get() = completedSets.count { it.gamesA > it.gamesB }

    val setsWonB: Int
        get() = completedSets.count { it.gamesB > it.gamesA }

    val isSetPoint: Boolean
        get() = isSetPointA || isSetPointB

    val isMatchPoint: Boolean
        get() = isMatchPointA || isMatchPointB

    fun displayScoreA(): String = formatTeamPoint(isTeamA = true)

    fun displayScoreB(): String = formatTeamPoint(isTeamA = false)

    private fun formatTeamPoint(isTeamA: Boolean): String {
        if (isTieBreak) {
            return (if (isTeamA) tbPointsA else tbPointsB).toString()
        }
        val team = if (isTeamA) TeamSide.A else TeamSide.B
        if (advantageTeam == team) return "AD"
        if (advantageTeam != null && advantageTeam != team) return "40"
        val pts = if (isTeamA) pointsA else pointsB
        return when (pts) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            else -> "40"
        }
    }

    fun elapsedDurationMillis(nowMillis: Long): Long {
        if (isPaused || activeSegmentStartedAtMillis == null) {
            return accumulatedPlayMs.coerceAtLeast(0L)
        }
        val running = (nowMillis - activeSegmentStartedAtMillis).coerceAtLeast(0L)
        return (accumulatedPlayMs + running).coerceAtLeast(0L)
    }

    fun allSetsForDisplay(): List<SetScore> {
        if (status == MatchStatus.COMPLETED) return completedSets
        val current = SetScore(
            setIndex = currentSetIndex,
            gamesA = gamesA,
            gamesB = gamesB,
            tbA = if (isTieBreak) tbPointsA else null,
            tbB = if (isTieBreak) tbPointsB else null
        )
        return completedSets + current
    }
}
