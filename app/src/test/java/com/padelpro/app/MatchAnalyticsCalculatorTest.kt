package com.padelpro.app

import com.padelpro.app.domain.engine.MatchConfig
import com.padelpro.app.domain.engine.MatchEngine
import com.padelpro.app.domain.engine.MatchEngineEvent
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.IncompleteSetScore
import com.padelpro.app.domain.model.MatchAnalytics
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchHighlight
import com.padelpro.app.domain.model.MatchStatsCalculator
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.TeamSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchAnalyticsCalculatorTest {

    private fun config(
        format: MatchFormat = MatchFormat.DOUBLES,
        deuceMode: DeuceMode = DeuceMode.GOLDEN_POINT,
        bestOf: Int = 3,
        firstServer: TeamSide = TeamSide.A
    ) = MatchConfig(
        matchId = "analytics_test_match",
        format = format,
        deuceMode = deuceMode,
        bestOf = bestOf,
        firstServerTeam = firstServer,
        startedAtMillis = 1_000L,
        teamAPlayerNames = if (format == MatchFormat.SINGLES) listOf("Алекс") else listOf("Алекс", "Марк"),
        teamBPlayerNames = if (format == MatchFormat.SINGLES) listOf("Олег") else listOf("Лев", "Игорь")
    )

    /** Журнал событий с реальными timestamp (шаг по умолчанию 1 секунда). */
    private class RallyLog(var ts: Long = 1_000L) {
        val events = mutableListOf<MatchEngineEvent>()
        fun point(team: TeamSide, stepMs: Long = 1_000L): RallyLog {
            ts += stepMs
            events += MatchEngineEvent.PointWon(team, ts)
            return this
        }
        fun points(team: TeamSide, n: Int, stepMs: Long = 1_000L): RallyLog =
            apply { repeat(n) { point(team, stepMs) } }
        fun pause(gapMs: Long = 1_000L): RallyLog {
            ts += gapMs
            events += MatchEngineEvent.Paused(ts)
            return this
        }
        fun resume(gapMs: Long): RallyLog {
            ts += gapMs
            events += MatchEngineEvent.Resumed(ts)
            return this
        }
    }

    /** Гейм при GOLDEN_POINT без deuce: 4 очка одной команды. */
    private fun winGame(log: RallyLog, team: TeamSide, stepMs: Long = 1_000L) =
        log.points(team, 4, stepMs)

    private fun winSetLove(log: RallyLog, team: TeamSide, stepMs: Long = 1_000L) =
        log.points(team, 24, stepMs)

    /** 6-6 чередующимися геймами (48 очков). */
    private fun reachSixSix(log: RallyLog) {
        repeat(6) {
            winGame(log, TeamSide.A)
            winGame(log, TeamSide.B)
        }
    }

    /** Тай-брейк 7-5: по 5 очков каждому, затем 2 очка A. */
    private fun tieBreakSevenFive(log: RallyLog) {
        repeat(5) {
            log.point(TeamSide.A)
            log.point(TeamSide.B)
        }
        log.points(TeamSide.A, 2)
    }

    /** Зеркало логики MatchRepositoryImpl.extractIncompleteSet (private в репозитории). */
    private fun extractIncompleteSet(state: com.padelpro.app.domain.engine.MatchEngineState): IncompleteSetScore? {
        val hasActivity = state.gamesA > 0 || state.gamesB > 0 ||
            state.pointsA > 0 || state.pointsB > 0 ||
            (state.isTieBreak && (state.tbPointsA > 0 || state.tbPointsB > 0))
        if (!hasActivity) return null
        return IncompleteSetScore(
            setIndex = state.currentSetIndex,
            gamesA = state.gamesA,
            gamesB = state.gamesB,
            scoreA = state.displayScoreA(),
            scoreB = state.displayScoreB(),
            isTieBreak = state.isTieBreak
        )
    }

    /** Зеркало сборки MatchSummary в репозитории: manual => winner=null, как в реальной БД. */
    private fun summaryFor(
        config: MatchConfig,
        events: List<MatchEngineEvent>,
        manual: Boolean = false,
        manualFinishTs: Long? = null
    ): MatchSummary {
        val state = MatchEngine.replay(config, events)
        val endedAt = when {
            state.status == MatchStatus.COMPLETED -> events.lastOrNull()?.timestampMillis
            manual -> manualFinishTs
            else -> null
        }
        val durationMs = endedAt?.let { state.elapsedDurationMillis(it) }
        val winner = if (manual) null else state.winnerTeam
        val incomplete = if (manual) extractIncompleteSet(state) else null
        return MatchSummary(
            id = config.matchId,
            format = config.format,
            deuceMode = config.deuceMode,
            status = if (manual) MatchStatus.COMPLETED else state.status,
            startedAt = config.startedAtMillis,
            endedAt = endedAt,
            durationMs = durationMs,
            winnerTeam = winner,
            userWon = null,
            teamANames = config.teamAPlayerNames,
            teamBNames = config.teamBPlayerNames,
            sets = state.completedSets,
            isUserParticipant = false,
            incompleteSet = incomplete
        )
    }

    private fun analyze(
        config: MatchConfig,
        log: RallyLog,
        manual: Boolean = false,
        manualFinishTs: Long? = null
    ): MatchAnalytics = MatchStatsCalculator.computeAnalytics(
        config = config,
        summary = summaryFor(config, log.events, manual, manualFinishTs),
        events = log.events
    )

    // Сценарий 1: обычный завершённый матч, победа Team A
    @Test
    fun `auto completed match team A wins with honest overview and highlights`() {
        val cfg = config()
        val log = RallyLog()
        winSetLove(log, TeamSide.A)
        winSetLove(log, TeamSide.A)

        val analytics = analyze(cfg, log)
        assertEquals(TeamSide.A, analytics.overview.winnerTeam)
        assertTrue(analytics.overview.isCompleted)
        assertFalse(analytics.overview.isManuallyCompleted)
        assertEquals(2, analytics.overview.setsWonA)
        assertEquals(0, analytics.overview.setsWonB)
        assertEquals(12, analytics.gameStats.gamesWonA)
        assertEquals(0, analytics.gameStats.gamesWonB)
        assertEquals(48, analytics.pointStats.totalPoints)
        assertEquals(48_000L, analytics.overview.durationMs)
        assertEquals(48, analytics.momentum.size)
        assertEquals(48, analytics.momentum.last().pointDifference)
        assertEquals(100f, analytics.pointStats.teamAPointPercent!!, 0.0001f)
        assertEquals(0f, analytics.pointStats.teamBPointPercent!!, 0.0001f)
        val streak = analytics.highlights.filterIsInstance<MatchHighlight.LongestPointStreak>()
            .single()
        assertEquals(TeamSide.A, streak.team)
        assertEquals(48, streak.length)
        assertEquals(1, streak.startRallyIndex)
        assertEquals(48, streak.endRallyIndex)
        assertEquals(12, analytics.gameStats.maxGameStreakA)
        // Legacy-значения не изменились
        assertEquals(48, analytics.eventStats!!.totalPoints)
        assertEquals(analytics.eventStats!!.totalPoints, analytics.pointStats.totalPoints)
    }

    // Сценарий 2: обычный завершённый матч, победа Team B
    @Test
    fun `auto completed match team B wins mirrors stats`() {
        val cfg = config(firstServer = TeamSide.B)
        val log = RallyLog()
        winSetLove(log, TeamSide.B)
        winSetLove(log, TeamSide.B)

        val analytics = analyze(cfg, log)
        assertEquals(TeamSide.B, analytics.overview.winnerTeam)
        assertEquals(0, analytics.pointStats.teamAPoints)
        assertEquals(48, analytics.pointStats.teamBPoints)
        assertEquals(-48, analytics.pointStats.pointDifference)
        assertEquals(-48, analytics.momentum.last().pointDifference)
        assertEquals(0, analytics.gameStats.setsWonA)
        assertEquals(2, analytics.gameStats.setsWonB)
        assertEquals(12, analytics.gameStats.maxGameStreakB)
        assertEquals(0f, analytics.pointStats.teamAPointPercent!!, 0.0001f)
        assertEquals(100f, analytics.pointStats.teamBPointPercent!!, 0.0001f)
    }

    // Сценарий 3: матч из нескольких сетов + самый长/короткий сет по реальному времени
    @Test
    fun `three set match computes per set points durations longest and shortest set`() {
        val cfg = config()
        val log = RallyLog()
        winSetLove(log, TeamSide.A, stepMs = 10_000L) // сет 1: 240 c
        winSetLove(log, TeamSide.B, stepMs = 30_000L) // сет 2: 720 c
        winSetLove(log, TeamSide.A, stepMs = 20_000L) // сет 3: 480 c

        val analytics = analyze(cfg, log)
        assertEquals(TeamSide.A, analytics.overview.winnerTeam)
        assertEquals(2, analytics.overview.setsWonA)
        assertEquals(1, analytics.overview.setsWonB)
        assertEquals(12, analytics.gameStats.gamesWonA)
        assertEquals(6, analytics.gameStats.gamesWonB)
        assertEquals(72, analytics.pointStats.totalPoints)
        assertEquals(1_440_000L, analytics.overview.durationMs)

        val sets = analytics.gameStats.setPerformances
        assertEquals(3, sets.size)
        assertTrue(sets.all { it.pointsPlayed == 24 })
        assertEquals(240_000L, sets[0].durationMs)
        assertEquals(720_000L, sets[1].durationMs)
        assertEquals(480_000L, sets[2].durationMs)
        assertEquals(2, analytics.gameStats.longestSet!!.setIndex)
        assertEquals(720_000L, analytics.gameStats.longestSet!!.durationMs)
        assertEquals(1, analytics.gameStats.shortestSet!!.setIndex)
        assertEquals(240_000L, analytics.gameStats.shortestSet!!.durationMs)
        assertEquals(2, analytics.eventStats!!.longestSetIndex)
        assertEquals(720_000L, analytics.eventStats!!.longestSetDurationMs)
        // A вёл 6-0, B лишь сравнял счёт (6-6 по кумулятиву) и не выходил вперёд — смен лидера 0
        assertEquals(0, analytics.gameStats.gamesLeadChanges)
    }

    // Сценарий 4: тай-брейк (bestOf=1, сет 7-6 (7-5))
    @Test
    fun `tie break set is counted as one game and momentum stays consistent`() {
        val cfg = config(bestOf = 1)
        val log = RallyLog()
        reachSixSix(log)
        tieBreakSevenFive(log)

        val analytics = analyze(cfg, log)
        assertEquals(TeamSide.A, analytics.overview.winnerTeam)
        assertEquals(1, analytics.overview.sets.size)
        val set = analytics.overview.sets.single()
        assertEquals(7, set.gamesA)
        assertEquals(6, set.gamesB)
        assertEquals(7, set.tbA)
        assertEquals(5, set.tbB)
        assertEquals(7, analytics.gameStats.gamesWonA)
        assertEquals(6, analytics.gameStats.gamesWonB)
        assertEquals(60, analytics.pointStats.totalPoints)
        assertEquals(31, analytics.pointStats.teamAPoints)
        assertEquals(29, analytics.pointStats.teamBPoints)
        assertEquals(60, analytics.momentum.size)
        assertEquals(2, analytics.momentum.last().pointDifference)
        assertEquals(7, analytics.momentum.last().gamesA)
        assertEquals(6, analytics.momentum.last().gamesB)
        assertEquals(60, analytics.gameStats.setPerformances.single().pointsPlayed)
        // Один сет: доминантный/близкий сет как достижение не показываем
        assertTrue(analytics.highlights.none { it is MatchHighlight.MostDominantSet })
        assertTrue(analytics.highlights.none { it is MatchHighlight.ClosestSet })
    }

    // Сценарий 5: Singles
    @Test
    fun `singles match exposes format and single player names`() {
        val cfg = config(format = MatchFormat.SINGLES)
        val log = RallyLog()
        winSetLove(log, TeamSide.A)
        winSetLove(log, TeamSide.A)

        val analytics = analyze(cfg, log)
        assertEquals(MatchFormat.SINGLES, analytics.overview.format)
        assertEquals(listOf("Алекс"), analytics.overview.teamANames)
        assertEquals(listOf("Олег"), analytics.overview.teamBNames)
        assertEquals(48, analytics.pointStats.totalPoints)
        assertEquals(TeamSide.A, analytics.overview.winnerTeam)
    }

    // Сценарий 6: Doubles
    @Test
    fun `doubles match exposes format and both player names per team`() {
        val cfg = config(format = MatchFormat.DOUBLES)
        val log = RallyLog()
        winSetLove(log, TeamSide.B)
        winSetLove(log, TeamSide.B)

        val analytics = analyze(cfg, log)
        assertEquals(MatchFormat.DOUBLES, analytics.overview.format)
        assertEquals(listOf("Алекс", "Марк"), analytics.overview.teamANames)
        assertEquals(listOf("Лев", "Игорь"), analytics.overview.teamBNames)
        assertEquals(3, analytics.overview.bestOf)
        assertEquals(DeuceMode.GOLDEN_POINT, analytics.overview.deuceMode)
    }

    // Сценарии 7-9 (реальные правила): ручное завершение НИКОГДА не назначает победителя.
    // WIN/LOSS в приложении существуют только для авто-завершения (userWon), ручное = DRAW.
    @Test
    fun `manual finish with big leader keeps null winner and draw semantics`() {
        val cfg = config()
        val log = RallyLog()
        winSetLove(log, TeamSide.A) // сет 1: 6-0
        log.points(TeamSide.A, 8)   // сет 2: 2-0 в геймах

        val analytics = analyze(cfg, log, manual = true, manualFinishTs = log.ts + 60_000L)
        assertTrue(analytics.overview.isManuallyCompleted)
        assertTrue(analytics.overview.isCompleted)
        assertNull(analytics.overview.winnerTeam) // победитель не выдумывается
        assertEquals(1, analytics.overview.setsWonA)
        assertEquals(0, analytics.overview.setsWonB)
        assertEquals(8, analytics.gameStats.gamesWonA) // 6 + 2 реально завершённых гейма
        assertEquals(0, analytics.gameStats.gamesWonB)
        assertNotNull(analytics.overview.incompleteSet)
        assertEquals(2, analytics.overview.incompleteSet!!.setIndex)
        assertEquals(2, analytics.overview.incompleteSet!!.gamesA)
        // Незаконченный сет не добавлен в sets
        assertEquals(1, analytics.overview.sets.size)
        assertEquals(8, analytics.gameStats.maxGameStreakA)
    }

    // Сценарий 10: ручное завершение с незавершённым сетом (зеркало реального теста репозитория)
    @Test
    fun `manual finish with incomplete set preserves score and honest partial metrics`() {
        val cfg = config(format = MatchFormat.SINGLES)
        val log = RallyLog()
        winSetLove(log, TeamSide.A)      // 24 очка, сет 1: 6-0
        log.points(TeamSide.A, 8)        // 2 гейма A во втором сете
        log.points(TeamSide.B, 4)        // 1 гейм B
        log.points(TeamSide.A, 2)        // 30
        log.point(TeamSide.B)            // 15

        val analytics = analyze(cfg, log, manual = true, manualFinishTs = log.ts + 60_000L)
        assertEquals(39, analytics.pointStats.totalPoints)
        assertEquals(34, analytics.pointStats.teamAPoints)
        assertEquals(5, analytics.pointStats.teamBPoints)
        assertEquals(39, analytics.momentum.size)
        assertEquals(29, analytics.momentum.last().pointDifference)

        val inc = analytics.overview.incompleteSet!!
        assertEquals(2, inc.setIndex)
        assertEquals(2, inc.gamesA)
        assertEquals(1, inc.gamesB)
        assertEquals("30", inc.scoreA)
        assertEquals("15", inc.scoreB)
        assertFalse(inc.isTieBreak)

        assertEquals(15, analytics.overview.incompleteSetPointsPlayed)
        assertEquals(99_000L, analytics.overview.durationMs)
        assertEquals(75_000L, analytics.overview.incompleteSetDurationMs)
        assertEquals(32, analytics.pointStats.maxPointStreakA) // 24 + 8 подряд
        assertEquals(39, analytics.eventStats!!.totalPoints)
    }

    // Сценарий 11: пустая история событий (legacy/backup-данные)
    @Test
    fun `empty event history yields zero stats null percents and no highlights`() {
        val cfg = config()
        val log = RallyLog()
        val analytics = analyze(cfg, log, manual = true, manualFinishTs = 661_000L)

        assertEquals(0, analytics.pointStats.totalPoints)
        assertNull(analytics.pointStats.teamAPointPercent)
        assertNull(analytics.pointStats.teamBPointPercent)
        assertTrue(analytics.momentum.isEmpty())
        assertTrue(analytics.highlights.isEmpty())
        assertNull(analytics.eventStats)
        assertNull(analytics.overview.incompleteSet)
        assertNull(analytics.overview.incompleteSetPointsPlayed)
        assertEquals(0, analytics.gameStats.gamesWonA)
        assertEquals(0, analytics.gameStats.setsWonA)
        assertNull(analytics.gameStats.longestSet)
        assertNull(analytics.gameStats.biggestGamesLead)
        assertEquals(660_000L, analytics.overview.durationMs)
    }

    // Сценарий 12: правильность процентов
    @Test
    fun `point percentages are exact and safe`() {
        val cfg = config()
        val log = RallyLog().points(TeamSide.A, 3).point(TeamSide.B)
        val analytics = analyze(cfg, log)
        assertEquals(75f, analytics.pointStats.teamAPointPercent!!, 0.0001f)
        assertEquals(25f, analytics.pointStats.teamBPointPercent!!, 0.0001f)
        assertEquals(2, analytics.pointStats.pointDifference)
    }

    // Сценарий 13: максимальные серии на реальном ADVANTAGE-гейме с deuce
    @Test
    fun `max point streaks respect advantage deuce sequence`() {
        val cfg = config(deuceMode = DeuceMode.ADVANTAGE)
        val log = RallyLog()
        log.points(TeamSide.A, 3).points(TeamSide.B, 3) // 40-40
        log.point(TeamSide.A)                           // AD A
        log.point(TeamSide.B)                           // снова deuce
        log.points(TeamSide.A, 2)                       // AD A -> гейм A

        val analytics = analyze(cfg, log)
        assertEquals(10, analytics.pointStats.totalPoints)
        assertEquals(6, analytics.pointStats.teamAPoints)
        assertEquals(4, analytics.pointStats.teamBPoints)
        assertEquals(3, analytics.pointStats.maxPointStreakA)
        assertEquals(3, analytics.pointStats.maxPointStreakB)
        assertEquals(1, analytics.gameStats.gamesWonA)
        assertEquals(0, analytics.gameStats.gamesWonB)
        assertEquals(0, analytics.pointStats.pointLeadChanges) // B ни разу не вела
        assertEquals(5, analytics.pointStats.longestLeadSpanA) // розыгрыши 1..5 подряд с diff>0
        assertEquals(0, analytics.pointStats.longestLeadSpanB)
    }

    // STAR_POINT: решающее очко после двух потерянных AD — реальное правило движка
    @Test
    fun `star point decider is counted as single extra point`() {
        val cfg = config(deuceMode = DeuceMode.STAR_POINT)
        val log = RallyLog()
        log.points(TeamSide.A, 3).points(TeamSide.B, 3) // deuce
        log.point(TeamSide.A).point(TeamSide.B)         // AD потерян 1
        log.point(TeamSide.A).point(TeamSide.B)         // AD потерян 2
        log.point(TeamSide.A)                           // Star Point -> гейм A

        val analytics = analyze(cfg, log)
        assertEquals(11, analytics.pointStats.totalPoints)
        assertEquals(1, analytics.gameStats.gamesWonA)
        assertEquals(11, analytics.momentum.size)
    }

    // Сценарий 14: точная последовательность Momentum (пример из ТЗ: +1, +2, +1, 0)
        @Test
    fun `momentum sequence matches rally by rally point difference`() {
        val cfg = config()
        val log = RallyLog()
            .point(TeamSide.A)
            .point(TeamSide.A)
            .point(TeamSide.B)
            .point(TeamSide.B)

        val analytics = analyze(cfg, log)
        assertEquals(listOf(1, 2, 1, 0), analytics.momentum.map { it.pointDifference })
        assertEquals(listOf(1, 2, 3, 4), analytics.momentum.map { it.rallyIndex })
        assertEquals(
            listOf(TeamSide.A, TeamSide.A, TeamSide.B, TeamSide.B),
            analytics.momentum.map { it.winner }
        )
        assertTrue(analytics.momentum.all { it.setIndex == 1 })
        assertTrue(analytics.momentum.all { it.gamesA == 0 && it.gamesB == 0 })
        assertEquals(0, analytics.pointStats.pointLeadChanges)
    }
    @Test
    fun `lead changes counted on real leader flips ignoring ties`() {
        val cfg = config()
        // A,A (A ведёт) -> B,B,B (B ведёт, смена 1) -> A,A,A,A (A ведёт, смена 2)
        val log = RallyLog()
            .points(TeamSide.A, 2)
            .points(TeamSide.B, 3)
            .points(TeamSide.A, 4)
        val analytics = analyze(cfg, log)
        assertEquals(2, analytics.pointStats.pointLeadChanges)
        assertEquals(1, analytics.pointStats.longestLeadSpanB)
        assertEquals(3, analytics.pointStats.maxPointStreakB)
        assertEquals(4, analytics.pointStats.maxPointStreakA)
    }

    // Паузы исключаются из длительности сетов и матча
    @Test
    fun `pause time is excluded from set and match durations`() {
        val cfg = config()
        val log = RallyLog()
        winSetLove(log, TeamSide.A, stepMs = 30_000L) // 12 мин
        log.pause(gapMs = 0L)
        log.resume(gapMs = 300_000L)                  // 5 мин паузы
        winSetLove(log, TeamSide.A, stepMs = 30_000L) // 12 мин

        val analytics = analyze(cfg, log)
        assertEquals(1_440_000L, analytics.overview.durationMs)
        assertEquals(
            1_440_000L,
            analytics.gameStats.setPerformances.sumOf { it.durationMs }
        )
        assertEquals(48, analytics.pointStats.totalPoints) // паузы не создают очков
    }

    // Сценарий 15: Undo не приводит к двойному учёту
    @Test
    fun `undo across game boundary removes exactly one point from analytics`() {
        val cfg = config()
        val log = RallyLog().points(TeamSide.A, 4) // гейм A завершён
        val before = analyze(cfg, log)
        assertEquals(1, before.gameStats.gamesWonA)
        assertEquals(4, before.pointStats.totalPoints)

        val undone = MatchEngine.undoLastPoint(cfg, log.events)!!
        val after = MatchStatsCalculator.computeAnalytics(
            cfg, summaryFor(cfg, undone.first), undone.first
        )
        assertEquals(3, after.pointStats.totalPoints)
        assertEquals(0, after.gameStats.gamesWonA)
        assertEquals(3, after.momentum.size)
        assertEquals(3, after.pointStats.maxPointStreakA)
        assertEquals(3, after.eventStats!!.totalPoints)
        assertEquals(after.pointStats.totalPoints, after.momentum.size)
    }

    @Test
    fun `undo across set boundary removes completed set from analytics`() {
        val cfg = config()
        val log = RallyLog()
        winSetLove(log, TeamSide.A)
        val undone = MatchEngine.undoLastPoint(cfg, log.events)!!
        val after = MatchStatsCalculator.computeAnalytics(
            cfg, summaryFor(cfg, undone.first), undone.first
        )
        assertEquals(23, after.pointStats.totalPoints)
        assertEquals(0, after.overview.setsWonA)
        assertTrue(after.overview.sets.isEmpty())
        assertTrue(after.gameStats.setPerformances.isEmpty())
        assertEquals(5, after.gameStats.gamesWonA)
        assertEquals(23, after.momentum.size)
    }

    // Dominant / Closest set на двух реальных сетах, один из которых тай-брейк
    @Test
    fun `most dominant and closest set highlights require two completed sets`() {
        val cfg = config()
        val log = RallyLog()
        winSetLove(log, TeamSide.A) // 6-0
        reachSixSix(log)
        tieBreakSevenFive(log)      // 7-6 (7-5), матч завершён 2-0

        val analytics = analyze(cfg, log)
        assertEquals(84, analytics.pointStats.totalPoints)
        val dominant = analytics.highlights.filterIsInstance<MatchHighlight.MostDominantSet>().single()
        assertEquals(1, dominant.setIndex)
        assertEquals(6, dominant.gamesWinner)
        assertEquals(0, dominant.gamesLoser)
        val closest = analytics.highlights.filterIsInstance<MatchHighlight.ClosestSet>().single()
        assertEquals(2, closest.setIndex)
        assertTrue(closest.hadTieBreak)
        assertEquals(13, analytics.gameStats.gamesWonA)
        assertEquals(6, analytics.gameStats.gamesWonB)
    }

    // Совместимость: legacy computeFromEvents не изменился
    @Test
    fun `legacy computeFromEvents returns identical values as analytics eventStats`() {
        val cfg = config()
        val log = RallyLog()
        winSetLove(log, TeamSide.A, stepMs = 10_000L)
        winSetLove(log, TeamSide.B, stepMs = 30_000L)
        winSetLove(log, TeamSide.A, stepMs = 20_000L)

        val legacy = MatchStatsCalculator.computeFromEvents(cfg, log.events)
        val analytics = analyze(cfg, log)
        assertEquals(legacy, analytics.eventStats)
    }
}