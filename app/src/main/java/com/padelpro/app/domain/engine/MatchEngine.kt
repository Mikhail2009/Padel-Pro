package com.padelpro.app.domain.engine

import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.SetScore
import com.padelpro.app.domain.model.TeamSide

object MatchEngine {

    fun initialState(config: MatchConfig): MatchEngineState {
        val raw = RawState(
            config = config,
            status = MatchStatus.ACTIVE,
            winnerTeam = null,
            completedSets = emptyList(),
            currentSetIndex = 1,
            gamesA = 0,
            gamesB = 0,
            pointsA = 0,
            pointsB = 0,
            advantageTeam = null,
            deuceAdvantagesLost = 0,
            isTieBreak = false,
            tbPointsA = 0,
            tbPointsB = 0,
            currentGameServerTeam = config.firstServerTeam,
            teamAServiceGamesInSet = 0,
            teamBServiceGamesInSet = 0,
            tbFirstServerTeam = null,
            totalPointsWonA = 0,
            totalPointsWonB = 0,
            isPaused = false,
            accumulatedPlayMs = 0L,
            activeSegmentStartedAtMillis = config.startedAtMillis
        )
        return materialize(raw)
    }

    fun reduce(state: MatchEngineState, event: MatchEngineEvent): EngineReduceResult {
        return when (event) {
            is MatchEngineEvent.PointWon -> {
                if (state.status == MatchStatus.COMPLETED) {
                    return EngineReduceResult.Rejected(state, RejectReason.MATCH_ALREADY_COMPLETED)
                }
                if (state.isPaused) {
                    return EngineReduceResult.Rejected(state, RejectReason.MATCH_PAUSED)
                }
                val nextRaw = applyPointRaw(state.toRaw(), event.team)
                EngineReduceResult.Accepted(materialize(nextRaw))
            }

            is MatchEngineEvent.Paused -> {
                if (state.status == MatchStatus.COMPLETED) {
                    return EngineReduceResult.Rejected(state, RejectReason.MATCH_ALREADY_COMPLETED)
                }
                if (state.isPaused) {
                    return EngineReduceResult.Rejected(state, RejectReason.ALREADY_PAUSED)
                }
                val start = state.activeSegmentStartedAtMillis ?: event.timestampMillis
                val delta = (event.timestampMillis - start).coerceAtLeast(0L)
                val nextRaw = state.toRaw().copy(
                    isPaused = true,
                    accumulatedPlayMs = state.accumulatedPlayMs + delta,
                    activeSegmentStartedAtMillis = null
                )
                EngineReduceResult.Accepted(materialize(nextRaw))
            }

            is MatchEngineEvent.Resumed -> {
                if (state.status == MatchStatus.COMPLETED) {
                    return EngineReduceResult.Rejected(state, RejectReason.MATCH_ALREADY_COMPLETED)
                }
                if (!state.isPaused) {
                    return EngineReduceResult.Rejected(state, RejectReason.NOT_PAUSED)
                }
                val nextRaw = state.toRaw().copy(
                    isPaused = false,
                    activeSegmentStartedAtMillis = event.timestampMillis
                )
                EngineReduceResult.Accepted(materialize(nextRaw))
            }
        }
    }

    fun replay(config: MatchConfig, events: List<MatchEngineEvent>): MatchEngineState {
        var current = initialState(config)
        for (event in events) {
            current = reduce(current, event).state
        }
        return current
    }

    fun undoLastPoint(
        config: MatchConfig,
        events: List<MatchEngineEvent>
    ): Pair<List<MatchEngineEvent>, MatchEngineState>? {
        val lastPointIdx = events.indexOfLast { it is MatchEngineEvent.PointWon }
        if (lastPointIdx < 0) return null
        val updatedEvents = events.toMutableList().apply { removeAt(lastPointIdx) }
        return updatedEvents to replay(config, updatedEvents)
    }

    private data class RawState(
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
        val activeSegmentStartedAtMillis: Long?
    )

    private fun MatchEngineState.toRaw(): RawState = RawState(
        config = config,
        status = status,
        winnerTeam = winnerTeam,
        completedSets = completedSets,
        currentSetIndex = currentSetIndex,
        gamesA = gamesA,
        gamesB = gamesB,
        pointsA = pointsA,
        pointsB = pointsB,
        advantageTeam = advantageTeam,
        deuceAdvantagesLost = deuceAdvantagesLost,
        isTieBreak = isTieBreak,
        tbPointsA = tbPointsA,
        tbPointsB = tbPointsB,
        currentGameServerTeam = currentGameServerTeam,
        teamAServiceGamesInSet = teamAServiceGamesInSet,
        teamBServiceGamesInSet = teamBServiceGamesInSet,
        tbFirstServerTeam = tbFirstServerTeam,
        totalPointsWonA = totalPointsWonA,
        totalPointsWonB = totalPointsWonB,
        isPaused = isPaused,
        accumulatedPlayMs = accumulatedPlayMs,
        activeSegmentStartedAtMillis = activeSegmentStartedAtMillis
    )

    private fun applyPointRaw(raw: RawState, team: TeamSide): RawState {
        if (raw.status == MatchStatus.COMPLETED) return raw

        val withTotals = if (team == TeamSide.A) {
            raw.copy(totalPointsWonA = raw.totalPointsWonA + 1)
        } else {
            raw.copy(totalPointsWonB = raw.totalPointsWonB + 1)
        }

        return if (withTotals.isTieBreak) {
            applyTieBreakPoint(withTotals, team)
        } else {
            applyRegularGamePoint(withTotals, team)
        }
    }

    private fun applyRegularGamePoint(raw: RawState, team: TeamSide): RawState {
        val pA = raw.pointsA
        val pB = raw.pointsB

        // Not yet at deuce (40-40)
        if (pA < 3 || pB < 3) {
            val nextA = if (team == TeamSide.A) pA + 1 else pA
            val nextB = if (team == TeamSide.B) pB + 1 else pB
            return if (nextA >= 4 || nextB >= 4) {
                awardRegularGame(raw, team)
            } else {
                raw.copy(pointsA = nextA, pointsB = nextB)
            }
        }

        // At 40-40 (deuce) or Advantage
        return when (raw.config.deuceMode) {
            DeuceMode.GOLDEN_POINT -> {
                awardRegularGame(raw, team)
            }

            DeuceMode.ADVANTAGE -> {
                when (raw.advantageTeam) {
                    null -> raw.copy(advantageTeam = team)
                    team -> awardRegularGame(raw, team)
                    else -> raw.copy(
                        advantageTeam = null,
                        deuceAdvantagesLost = raw.deuceAdvantagesLost + 1
                    )
                }
            }

            DeuceMode.STAR_POINT -> {
                if (raw.deuceAdvantagesLost >= 2) {
                    // Third time at deuce -> Star Point decider wins immediately
                    awardRegularGame(raw, team)
                } else {
                    when (raw.advantageTeam) {
                        null -> raw.copy(advantageTeam = team)
                        team -> awardRegularGame(raw, team)
                        else -> raw.copy(
                            advantageTeam = null,
                            deuceAdvantagesLost = raw.deuceAdvantagesLost + 1
                        )
                    }
                }
            }
        }
    }

    private fun awardRegularGame(raw: RawState, winner: TeamSide): RawState {
        val nextGamesA = if (winner == TeamSide.A) raw.gamesA + 1 else raw.gamesA
        val nextGamesB = if (winner == TeamSide.B) raw.gamesB + 1 else raw.gamesB
        val nextSrvA = if (raw.currentGameServerTeam == TeamSide.A) raw.teamAServiceGamesInSet + 1 else raw.teamAServiceGamesInSet
        val nextSrvB = if (raw.currentGameServerTeam == TeamSide.B) raw.teamBServiceGamesInSet + 1 else raw.teamBServiceGamesInSet
        val nextServerTeam = raw.currentGameServerTeam.other()

        // Check regular set win: 6+ games with 2+ game lead
        val setWinner = when {
            nextGamesA >= 6 && nextGamesA - nextGamesB >= 2 -> TeamSide.A
            nextGamesB >= 6 && nextGamesB - nextGamesA >= 2 -> TeamSide.B
            else -> null
        }

        if (setWinner != null) {
            val finishedSet = SetScore(
                setIndex = raw.currentSetIndex,
                gamesA = nextGamesA,
                gamesB = nextGamesB,
                tbA = null,
                tbB = null
            )
            return finishSet(raw, finishedSet, nextSetFirstServer = nextServerTeam)
        }

        // Check 6-6 -> Tie-Break
        if (nextGamesA == 6 && nextGamesB == 6) {
            return raw.copy(
                gamesA = 6,
                gamesB = 6,
                pointsA = 0,
                pointsB = 0,
                advantageTeam = null,
                deuceAdvantagesLost = 0,
                isTieBreak = true,
                tbPointsA = 0,
                tbPointsB = 0,
                currentGameServerTeam = nextServerTeam,
                teamAServiceGamesInSet = nextSrvA,
                teamBServiceGamesInSet = nextSrvB,
                tbFirstServerTeam = nextServerTeam
            )
        }

        return raw.copy(
            gamesA = nextGamesA,
            gamesB = nextGamesB,
            pointsA = 0,
            pointsB = 0,
            advantageTeam = null,
            deuceAdvantagesLost = 0,
            currentGameServerTeam = nextServerTeam,
            teamAServiceGamesInSet = nextSrvA,
            teamBServiceGamesInSet = nextSrvB
        )
    }

    private fun applyTieBreakPoint(raw: RawState, winner: TeamSide): RawState {
        val nextTbA = if (winner == TeamSide.A) raw.tbPointsA + 1 else raw.tbPointsA
        val nextTbB = if (winner == TeamSide.B) raw.tbPointsB + 1 else raw.tbPointsB

        val tbWinner = when {
            nextTbA >= 7 && nextTbA - nextTbB >= 2 -> TeamSide.A
            nextTbB >= 7 && nextTbB - nextTbA >= 2 -> TeamSide.B
            else -> null
        }

        if (tbWinner != null) {
            val finishedSet = SetScore(
                setIndex = raw.currentSetIndex,
                gamesA = if (tbWinner == TeamSide.A) 7 else 6,
                gamesB = if (tbWinner == TeamSide.B) 7 else 6,
                tbA = nextTbA,
                tbB = nextTbB
            )
            val tbFirst = raw.tbFirstServerTeam ?: raw.currentGameServerTeam
            // First server of next set is the team that received first in the tie-break
            val nextSetFirstServer = tbFirst.other()
            return finishSet(raw, finishedSet, nextSetFirstServer = nextSetFirstServer)
        }

        return raw.copy(
            tbPointsA = nextTbA,
            tbPointsB = nextTbB
        )
    }

    private fun finishSet(
        raw: RawState,
        finishedSet: SetScore,
        nextSetFirstServer: TeamSide
    ): RawState {
        val allSets = raw.completedSets + finishedSet
        val setsWonA = allSets.count { it.gamesA > it.gamesB }
        val setsWonB = allSets.count { it.gamesB > it.gamesA }
        val setsToWin = raw.config.setsToWin

        val matchWinner = when {
            setsWonA >= setsToWin -> TeamSide.A
            setsWonB >= setsToWin -> TeamSide.B
            else -> null
        }

        if (matchWinner != null) {
            return raw.copy(
                status = MatchStatus.COMPLETED,
                winnerTeam = matchWinner,
                completedSets = allSets,
                gamesA = finishedSet.gamesA,
                gamesB = finishedSet.gamesB,
                pointsA = 0,
                pointsB = 0,
                advantageTeam = null,
                deuceAdvantagesLost = 0,
                isTieBreak = finishedSet.tbA != null,
                tbPointsA = finishedSet.tbA ?: 0,
                tbPointsB = finishedSet.tbB ?: 0,
                activeSegmentStartedAtMillis = null
            )
        }

        return raw.copy(
            completedSets = allSets,
            currentSetIndex = raw.currentSetIndex + 1,
            gamesA = 0,
            gamesB = 0,
            pointsA = 0,
            pointsB = 0,
            advantageTeam = null,
            deuceAdvantagesLost = 0,
            isTieBreak = false,
            tbPointsA = 0,
            tbPointsB = 0,
            currentGameServerTeam = nextSetFirstServer,
            teamAServiceGamesInSet = 0,
            teamBServiceGamesInSet = 0,
            tbFirstServerTeam = null
        )
    }

    private fun materialize(raw: RawState): MatchEngineState {
        val isActive = raw.status == MatchStatus.ACTIVE
        val atDeuce = isActive && !raw.isTieBreak && raw.pointsA == 3 && raw.pointsB == 3 && raw.advantageTeam == null
        val goldenActive = atDeuce && raw.config.deuceMode == DeuceMode.GOLDEN_POINT
        val starActive = atDeuce && raw.config.deuceMode == DeuceMode.STAR_POINT && raw.deuceAdvantagesLost >= 2

        val setPointA: Boolean
        val setPointB: Boolean
        val matchPointA: Boolean
        val matchPointB: Boolean
        if (isActive) {
            val nextA = applyPointRaw(raw, TeamSide.A)
            val nextB = applyPointRaw(raw, TeamSide.B)
            matchPointA = nextA.status == MatchStatus.COMPLETED && nextA.winnerTeam == TeamSide.A
            matchPointB = nextB.status == MatchStatus.COMPLETED && nextB.winnerTeam == TeamSide.B
            setPointA = nextA.completedSets.size > raw.completedSets.size &&
                nextA.completedSets.last().let { it.gamesA > it.gamesB }
            setPointB = nextB.completedSets.size > raw.completedSets.size &&
                nextB.completedSets.last().let { it.gamesB > it.gamesA }
        } else {
            setPointA = false
            setPointB = false
            matchPointA = false
            matchPointB = false
        }

        val changeEnds = if (!isActive) {
            false
        } else if (raw.isTieBreak) {
            val totalTb = raw.tbPointsA + raw.tbPointsB
            totalTb > 0 && totalTb % 6 == 0
        } else if (raw.pointsA == 0 && raw.pointsB == 0) {
            val gamesSum = raw.gamesA + raw.gamesB
            if (gamesSum > 0) {
                gamesSum % 2 == 1
            } else {
                // Start of new set after previous set: change ends if previous set had odd total games
                raw.completedSets.lastOrNull()?.let { (it.gamesA + it.gamesB) % 2 == 1 } ?: false
            }
        } else {
            false
        }

        val (srvTeam, srvSlot) = resolveServer(raw)
        val serveFromRight = if (raw.isTieBreak) {
            (raw.tbPointsA + raw.tbPointsB) % 2 == 0
        } else {
            (raw.pointsA + raw.pointsB) % 2 == 0
        }
        val srvName = resolvePlayerName(raw.config, srvTeam, srvSlot)

        return MatchEngineState(
            config = raw.config,
            status = raw.status,
            winnerTeam = raw.winnerTeam,
            completedSets = raw.completedSets,
            currentSetIndex = raw.currentSetIndex,
            gamesA = raw.gamesA,
            gamesB = raw.gamesB,
            pointsA = raw.pointsA,
            pointsB = raw.pointsB,
            advantageTeam = raw.advantageTeam,
            deuceAdvantagesLost = raw.deuceAdvantagesLost,
            isTieBreak = raw.isTieBreak,
            tbPointsA = raw.tbPointsA,
            tbPointsB = raw.tbPointsB,
            currentGameServerTeam = raw.currentGameServerTeam,
            teamAServiceGamesInSet = raw.teamAServiceGamesInSet,
            teamBServiceGamesInSet = raw.teamBServiceGamesInSet,
            tbFirstServerTeam = raw.tbFirstServerTeam,
            totalPointsWonA = raw.totalPointsWonA,
            totalPointsWonB = raw.totalPointsWonB,
            isPaused = raw.isPaused,
            accumulatedPlayMs = raw.accumulatedPlayMs,
            activeSegmentStartedAtMillis = raw.activeSegmentStartedAtMillis,
            isDeuce = atDeuce,
            isGoldenPointActive = goldenActive,
            isStarPointDeciderActive = starActive,
            isSetPointA = setPointA,
            isSetPointB = setPointB,
            isMatchPointA = matchPointA,
            isMatchPointB = matchPointB,
            shouldChangeEnds = changeEnds,
            servingTeam = srvTeam,
            servingSlotInTeam = srvSlot,
            servingPlayerName = srvName,
            serveSideFromRight = serveFromRight
        )
    }

    private fun resolveServer(raw: RawState): Pair<TeamSide, Int> {
        if (!raw.isTieBreak) {
            val srvTeam = raw.currentGameServerTeam
            val slot = if (raw.config.format == MatchFormat.SINGLES) {
                0
            } else {
                val count = if (srvTeam == TeamSide.A) raw.teamAServiceGamesInSet else raw.teamBServiceGamesInSet
                count % 2
            }
            return srvTeam to slot
        }

        // Tie-break rotation: 1st point by tbFirstServerTeam, then 2 points each
        val tbFirst = raw.tbFirstServerTeam ?: raw.currentGameServerTeam
        val k = raw.tbPointsA + raw.tbPointsB
        val turnIndex = if (k == 0) 0 else (k + 1) / 2
        val srvTeam = if (turnIndex % 2 == 0) tbFirst else tbFirst.other()

        val slot = if (raw.config.format == MatchFormat.SINGLES) {
            0
        } else {
            val teamTurnCount = turnIndex / 2
            val baseSlot = if (srvTeam == TeamSide.A) {
                raw.teamAServiceGamesInSet % 2
            } else {
                raw.teamBServiceGamesInSet % 2
            }
            (baseSlot + teamTurnCount) % 2
        }
        return srvTeam to slot
    }

    private fun resolvePlayerName(config: MatchConfig, team: TeamSide, slot: Int): String {
        val names = if (team == TeamSide.A) config.teamAPlayerNames else config.teamBPlayerNames
        return names.getOrNull(slot) ?: names.firstOrNull() ?: team.name
    }
}
