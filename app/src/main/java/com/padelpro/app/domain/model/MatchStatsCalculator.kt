package com.padelpro.app.domain.model

import com.padelpro.app.domain.engine.EngineReduceResult
import com.padelpro.app.domain.engine.MatchConfig
import com.padelpro.app.domain.engine.MatchEngine
import com.padelpro.app.domain.engine.MatchEngineEvent
import com.padelpro.app.domain.engine.MatchEngineState
import kotlin.math.abs

object MatchStatsCalculator {

    /** Существующий расчёт. Поведение полностью сохранено для обратной совместимости. */
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

    /**
     * Расширенная аналитика Advanced Match Analytics.
     *
     * Чистая доменная функция: один проход реплея через реальный MatchEngine,
     * поэтому все правила (DeuceMode, тай-брейки, паузы, Undo) совпадают с игровым движком.
     * Учитываются только принятые (Accepted) события — отвергнутые очки не попадают в статистику.
     *
     * Для вручную завершённых матчей (summary.isManuallyCompleted):
     *  - победитель не определяется (winnerTeam берётся из summary и остаётся null);
     *  - незаконченный сет не считается завершённым и не дозаполняется;
     *  - сеты/геймы считаются только по реально сыгранным данным.
     */
    fun computeAnalytics(
        config: MatchConfig,
        summary: MatchSummary,
        events: List<MatchEngineEvent>
    ): MatchAnalytics {
        var state = MatchEngine.initialState(config)
        var rallyIndex = 0
        var previousSetEndElapsedMs = 0L
        var previousSetEndRally = 0

        val setPerformances = mutableListOf<SetPerformance>()
        val momentum = mutableListOf<MomentumPoint>()
        val gameWinners = mutableListOf<TeamSide>()

        var streakTeam: TeamSide? = null
        var streakLength = 0
        var maxStreakA = 0
        var maxStreakB = 0
        var bestStreakA: MatchHighlight.LongestPointStreak? = null
        var bestStreakB: MatchHighlight.LongestPointStreak? = null

        var leadSign = 0
        var leadRun = 0
        var longestLeadSpanA = 0
        var longestLeadSpanB = 0
        var lastNonZeroLeadSign = 0
        var pointLeadChanges = 0
        var biggestLeadA = 0
        var biggestLeadARally = 0
        var biggestLeadB = 0
        var biggestLeadBRally = 0

        for (event in events) {
            val result = MatchEngine.reduce(state, event)
            if (result !is EngineReduceResult.Accepted) continue
            val next = result.state

            if (event is MatchEngineEvent.PointWon) {
                rallyIndex += 1
                val diff = next.totalPointsWonA - next.totalPointsWonB
                val postGamesA = cumulativeGamesA(next)
                val postGamesB = cumulativeGamesB(next)

                momentum += MomentumPoint(
                    rallyIndex = rallyIndex,
                    winner = event.team,
                    pointDifference = diff,
                    gamesA = postGamesA,
                    gamesB = postGamesB,
                    setIndex = state.currentSetIndex,
                    timestampMillis = event.timestampMillis
                )

                // Серии очков подряд
                if (streakTeam == event.team) {
                    streakLength += 1
                } else {
                    streakTeam = event.team
                    streakLength = 1
                }
                if (event.team == TeamSide.A && streakLength > maxStreakA) {
                    maxStreakA = streakLength
                    bestStreakA = MatchHighlight.LongestPointStreak(
                        team = TeamSide.A,
                        length = streakLength,
                        startRallyIndex = rallyIndex - streakLength + 1,
                        endRallyIndex = rallyIndex
                    )
                }
                if (event.team == TeamSide.B && streakLength > maxStreakB) {
                    maxStreakB = streakLength
                    bestStreakB = MatchHighlight.LongestPointStreak(
                        team = TeamSide.B,
                        length = streakLength,
                        startRallyIndex = rallyIndex - streakLength + 1,
                        endRallyIndex = rallyIndex
                    )
                }

                // Удержание лидерства и смены лидера по очкам
                val sign = when {
                    diff > 0 -> 1
                    diff < 0 -> -1
                    else -> 0
                }
                leadRun = when {
                    sign == 0 -> 0
                    sign == leadSign -> leadRun + 1
                    else -> 1
                }
                leadSign = sign
                if (sign > 0 && leadRun > longestLeadSpanA) longestLeadSpanA = leadRun
                if (sign < 0 && leadRun > longestLeadSpanB) longestLeadSpanB = leadRun
                if (sign != 0) {
                    if (lastNonZeroLeadSign != 0 && sign != lastNonZeroLeadSign) pointLeadChanges += 1
                    lastNonZeroLeadSign = sign
                }
                if (diff > biggestLeadA) {
                    biggestLeadA = diff
                    biggestLeadARally = rallyIndex
                }
                if (-diff > biggestLeadB) {
                    biggestLeadB = -diff
                    biggestLeadBRally = rallyIndex
                }

                // Завершение гейма: кумулятивные геймы выросли ровно на этот розыгрыш.
                // Победитель гейма == победитель решающего очка (включая тай-брейк).
                val preGamesTotal = cumulativeGamesA(state) + cumulativeGamesB(state)
                if (postGamesA + postGamesB > preGamesTotal) {
                    gameWinners += event.team
                }
            }

            if (next.completedSets.size > state.completedSets.size) {
                val finished = next.completedSets.last()
                val elapsed = next.elapsedDurationMillis(event.timestampMillis)
                val duration = (elapsed - previousSetEndElapsedMs).coerceAtLeast(0L)
                setPerformances += SetPerformance(
                    setIndex = finished.setIndex,
                    gamesA = finished.gamesA,
                    gamesB = finished.gamesB,
                    tbA = finished.tbA,
                    tbB = finished.tbB,
                    winnerTeam = if (finished.gamesA > finished.gamesB) TeamSide.A else TeamSide.B,
                    durationMs = duration,
                    pointsPlayed = rallyIndex - previousSetEndRally
                )
                previousSetEndElapsedMs = elapsed
                previousSetEndRally = rallyIndex
            }

            state = next
        }

        // Серии геймов, смены лидера и максимальное преимущество по геймам
        var gameStreakTeam: TeamSide? = null
        var gameStreakLength = 0
        var maxGameStreakA = 0
        var maxGameStreakB = 0
        var bestGameStreakA: MatchHighlight.BestGameStreak? = null
        var bestGameStreakB: MatchHighlight.BestGameStreak? = null
        var lastNonZeroGamesSign = 0
        var gamesLeadChanges = 0
        var biggestGamesLead: GamesLead? = null
        var cumGamesA = 0
        var cumGamesB = 0
        gameWinners.forEachIndexed { index, team ->
            val gameIndex = index + 1
            if (team == TeamSide.A) cumGamesA += 1 else cumGamesB += 1

            if (gameStreakTeam == team) {
                gameStreakLength += 1
            } else {
                gameStreakTeam = team
                gameStreakLength = 1
            }
            if (team == TeamSide.A && gameStreakLength > maxGameStreakA) {
                maxGameStreakA = gameStreakLength
                bestGameStreakA = MatchHighlight.BestGameStreak(
                    team = TeamSide.A,
                    length = gameStreakLength,
                    startGameIndex = gameIndex - gameStreakLength + 1,
                    endGameIndex = gameIndex
                )
            }
            if (team == TeamSide.B && gameStreakLength > maxGameStreakB) {
                maxGameStreakB = gameStreakLength
                bestGameStreakB = MatchHighlight.BestGameStreak(
                    team = TeamSide.B,
                    length = gameStreakLength,
                    startGameIndex = gameIndex - gameStreakLength + 1,
                    endGameIndex = gameIndex
                )
            }

            val diff = cumGamesA - cumGamesB
            val sign = when {
                diff > 0 -> 1
                diff < 0 -> -1
                else -> 0
            }
            if (sign != 0) {
                if (lastNonZeroGamesSign != 0 && sign != lastNonZeroGamesSign) gamesLeadChanges += 1
                lastNonZeroGamesSign = sign
            }
            val lead = abs(diff)
            if (lead > (biggestGamesLead?.lead ?: 0)) {
                biggestGamesLead = GamesLead(
                    team = if (diff > 0) TeamSide.A else TeamSide.B,
                    lead = lead,
                    gameIndex = gameIndex
                )
            }
        }

        // Очки: только принятые движком розыгрыши
        val teamAPoints = state.totalPointsWonA
        val teamBPoints = state.totalPointsWonB
        val totalPoints = teamAPoints + teamBPoints
        val pointStats = PointStats(
            totalPoints = totalPoints,
            teamAPoints = teamAPoints,
            teamBPoints = teamBPoints,
            teamAPointPercent = percentOf(teamAPoints, totalPoints),
            teamBPointPercent = percentOf(teamBPoints, totalPoints),
            pointDifference = teamAPoints - teamBPoints,
            maxPointStreakA = maxStreakA,
            maxPointStreakB = maxStreakB,
            longestLeadSpanA = longestLeadSpanA,
            longestLeadSpanB = longestLeadSpanB,
            pointLeadChanges = pointLeadChanges
        )

        // Геймы/сеты: из сохранённых данных summary (set_results + незаконченный сет)
        val completedSets = summary.sets
        val setsWonA = completedSets.count { it.gamesA > it.gamesB }
        val setsWonB = completedSets.count { it.gamesB > it.gamesA }
        val activeSetGamesA = summary.incompleteSet?.gamesA
            ?: if (summary.status == MatchStatus.ACTIVE) state.gamesA else 0
        val activeSetGamesB = summary.incompleteSet?.gamesB
            ?: if (summary.status == MatchStatus.ACTIVE) state.gamesB else 0
        val gamesWonA = completedSets.sumOf { it.gamesA } + activeSetGamesA
        val gamesWonB = completedSets.sumOf { it.gamesB } + activeSetGamesB
        val longestSet = setPerformances.maxByOrNull { it.durationMs }
        val shortestSet = setPerformances.minByOrNull { it.durationMs }
        val mostDominantSet = completedSets.maxByOrNull { abs(it.gamesA - it.gamesB) }
        val closestSet = completedSets.minByOrNull { abs(it.gamesA - it.gamesB) }

        val gameStats = GameStats(
            gamesWonA = gamesWonA,
            gamesWonB = gamesWonB,
            setsWonA = setsWonA,
            setsWonB = setsWonB,
            maxGameStreakA = maxGameStreakA,
            maxGameStreakB = maxGameStreakB,
            gamesLeadChanges = gamesLeadChanges,
            biggestGamesLead = biggestGamesLead,
            setPerformances = setPerformances,
            longestSet = longestSet,
            shortestSet = shortestSet,
            mostDominantSet = mostDominantSet,
            closestSet = closestSet
        )

        val incompleteSetPointsPlayed = summary.incompleteSet?.let {
            (totalPoints - setPerformances.sumOf { s -> s.pointsPlayed }).coerceAtLeast(0)
        }
        val incompleteSetDurationMs = if (summary.incompleteSet != null) {
            summary.durationMs?.let { total ->
                (total - setPerformances.sumOf { it.durationMs }).coerceAtLeast(0L)
            }
        } else {
            null
        }

        val overview = MatchOverview(
            matchId = summary.id,
            format = summary.format,
            deuceMode = summary.deuceMode,
            bestOf = config.bestOf,
            teamANames = summary.teamANames,
            teamBNames = summary.teamBNames,
            startedAt = summary.startedAt,
            endedAt = summary.endedAt,
            durationMs = summary.durationMs,
            isCompleted = summary.status == MatchStatus.COMPLETED,
            isManuallyCompleted = summary.isManuallyCompleted,
            winnerTeam = summary.winnerTeam,
            setsWonA = setsWonA,
            setsWonB = setsWonB,
            sets = completedSets,
            incompleteSet = summary.incompleteSet,
            totalPoints = totalPoints,
            incompleteSetPointsPlayed = incompleteSetPointsPlayed,
            incompleteSetDurationMs = incompleteSetDurationMs
        )

        // Достижения: только то, что достоверно вычислено
        val highlights = buildList {
            bestStreakA?.takeIf { it.length >= 2 }?.let { add(it) }
            bestStreakB?.takeIf { it.length >= 2 }?.let { add(it) }
            if (biggestLeadA > 0) {
                add(MatchHighlight.BiggestPointLead(TeamSide.A, biggestLeadA, biggestLeadARally))
            }
            if (biggestLeadB > 0) {
                add(MatchHighlight.BiggestPointLead(TeamSide.B, biggestLeadB, biggestLeadBRally))
            }
            if (completedSets.size >= 2) {
                mostDominantSet?.let { set ->
                    add(
                        MatchHighlight.MostDominantSet(
                            setIndex = set.setIndex,
                            winnerTeam = if (set.gamesA > set.gamesB) TeamSide.A else TeamSide.B,
                            gamesWinner = maxOf(set.gamesA, set.gamesB),
                            gamesLoser = minOf(set.gamesA, set.gamesB)
                        )
                    )
                }
                closestSet?.let { set ->
                    add(
                        MatchHighlight.ClosestSet(
                            setIndex = set.setIndex,
                            gamesA = set.gamesA,
                            gamesB = set.gamesB,
                            hadTieBreak = set.tbA != null && set.tbB != null
                        )
                    )
                }
            }
            longestSet?.takeIf { it.durationMs > 0 }?.let {
                add(MatchHighlight.LongestSet(it.setIndex, it.durationMs))
            }
            bestGameStreakA?.takeIf { it.length >= 2 }?.let { add(it) }
            bestGameStreakB?.takeIf { it.length >= 2 }?.let { add(it) }
        }

        // Legacy-статистика: идентична computeFromEvents (сырые POINT_WON, как и раньше)
        val rawPoints = events.filterIsInstance<MatchEngineEvent.PointWon>()
        val eventStats = if (rawPoints.isEmpty()) {
            null
        } else {
            MatchEventStats(
                totalPoints = rawPoints.size,
                teamAPoints = rawPoints.count { it.team == TeamSide.A },
                teamBPoints = rawPoints.count { it.team == TeamSide.B },
                longestSetIndex = longestSet?.setIndex,
                longestSetDurationMs = longestSet?.durationMs
            )
        }

        return MatchAnalytics(
            overview = overview,
            pointStats = pointStats,
            gameStats = gameStats,
            momentum = momentum,
            highlights = highlights,
            eventStats = eventStats
        )
    }

    private fun percentOf(part: Int, total: Int): Float? =
        if (total <= 0) null else part * 100f / total

    private fun cumulativeGamesA(state: MatchEngineState): Int =
        state.completedSets.sumOf { it.gamesA } +
            if (state.status == MatchStatus.ACTIVE) state.gamesA else 0

    private fun cumulativeGamesB(state: MatchEngineState): Int =
        state.completedSets.sumOf { it.gamesB } +
            if (state.status == MatchStatus.ACTIVE) state.gamesB else 0
}