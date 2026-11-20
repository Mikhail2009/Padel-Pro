package com.padelpro.app.domain.model

import com.padelpro.app.domain.engine.EngineReduceResult
import com.padelpro.app.domain.engine.MatchConfig
import com.padelpro.app.domain.engine.MatchEngine
import com.padelpro.app.domain.engine.MatchEngineEvent

object MatchStatsCalculator {

    fun computeFromEvents(
        config: MatchConfig,
        events: List<MatchEngineEvent>
    ): MatchEventStats? {
        val pointEvents = events.filterIsInstance<MatchEngineEvent.PointWon>()
        if (pointEvents.isEmpty()) {
            return null
        }

        val teamAPoints = pointEvents.count { it.team == TeamSide.A }
        val teamBPoints = pointEvents.count { it.team == TeamSide.B }
        val totalPoints = teamAPoints + teamBPoints

        var state = MatchEngine.initialState(config)
        var previousSetEndCumulativeMs = 0L
        val completedSetDurations = mutableListOf<Pair<Int, Long>>()

        for (event in events) {
            when (val result = MatchEngine.reduce(state, event)) {
                is EngineReduceResult.Rejected -> Unit
                is EngineReduceResult.Accepted -> {
                    val nextState = result.state
                    if (nextState.completedSets.size > state.completedSets.size) {
                        val finishedSet = nextState.completedSets.last()
                        val cumulativeMs = nextState.elapsedDurationMillis(event.timestampMillis)
                        val setDurationMs = (cumulativeMs - previousSetEndCumulativeMs).coerceAtLeast(0L)
                        completedSetDurations.add(finishedSet.setIndex to setDurationMs)
                        previousSetEndCumulativeMs = cumulativeMs
                    }
                    state = nextState
                }
            }
        }

        val longestSet = completedSetDurations.maxByOrNull { it.second }
        return MatchEventStats(
            totalPoints = totalPoints,
            teamAPoints = teamAPoints,
            teamBPoints = teamBPoints,
            longestSetIndex = longestSet?.first,
            longestSetDurationMs = longestSet?.second
        )
    }
}
