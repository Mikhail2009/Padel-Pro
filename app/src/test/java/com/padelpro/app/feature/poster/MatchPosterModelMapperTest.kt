package com.padelpro.app.feature.poster

import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.domain.engine.MatchConfig
import com.padelpro.app.domain.engine.MatchEngine
import com.padelpro.app.domain.engine.MatchEngineEvent
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.IncompleteSetScore
import com.padelpro.app.domain.model.MatchDetailsData
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchParticipantDetail
import com.padelpro.app.domain.model.MatchStatsCalculator
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.Player
import com.padelpro.app.domain.model.SetScore
import com.padelpro.app.domain.model.TeamSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MatchPosterModelMapperTest {

    private class FakeTimeProvider : TimeProvider {
        override fun nowMillis(): Long = 1_750_000_000_000L
        override fun currentHourOfDay(): Int = 18
        override fun currentLocalDate(): LocalDate = LocalDate.of(2026, 6, 15)
        override fun formatMonthYear(epochMillis: Long): String = "июнь 2026"
        override fun formatShortDate(epochMillis: Long): String = "date:$epochMillis"
        override fun formatShortDateTime(epochMillis: Long): String = "datetime:$epochMillis"
    }

    private val mapper = MatchPosterModelMapper(FakeTimeProvider())

    private fun player(
        id: String,
        name: String,
        avatarId: Int,
        isUser: Boolean = false
    ) = MatchParticipantDetail(
        player = Player(
            id = id,
            displayName = name,
            avatarId = avatarId,
            createdAt = 1_000L,
            preferredFormat = MatchFormat.DOUBLES,
            isUser = isUser
        ),
        isUser = isUser
    )

    private fun buildDetailsWithRealAnalytics(
        matchId: String = "m1",
        format: MatchFormat = MatchFormat.SINGLES,
        deuceMode: DeuceMode = DeuceMode.GOLDEN_POINT,
        bestOf: Int = 3,
        teamA: List<MatchParticipantDetail>,
        teamB: List<MatchParticipantDetail>,
        events: List<MatchEngineEvent>,
        manualFinish: Boolean = false,
        manualDurationMs: Long? = null,
        startedAt: Long = 10_000L,
        endedAt: Long? = null
    ): MatchDetailsData {
        val config = MatchConfig(
            matchId = matchId,
            format = format,
            deuceMode = deuceMode,
            bestOf = bestOf,
            firstServerTeam = TeamSide.A,
            startedAtMillis = startedAt,
            teamAPlayerNames = teamA.map { it.player.displayName },
            teamBPlayerNames = teamB.map { it.player.displayName }
        )
        val state = MatchEngine.replay(config, events)
        val winner = if (manualFinish) null else state.winnerTeam
        val userTeam = when {
            teamA.any { it.isUser } -> TeamSide.A
            teamB.any { it.isUser } -> TeamSide.B
            else -> null
        }
        val userWon = if (winner != null && userTeam != null) winner == userTeam else null
        val isUserParticipant = userTeam != null
        val incompleteSet = if (manualFinish) {
            val hasActivity = state.gamesA > 0 || state.gamesB > 0 ||
                state.pointsA > 0 || state.pointsB > 0 ||
                (state.isTieBreak && (state.tbPointsA > 0 || state.tbPointsB > 0))
            if (hasActivity) {
                IncompleteSetScore(
                    setIndex = state.currentSetIndex,
                    gamesA = state.gamesA,
                    gamesB = state.gamesB,
                    scoreA = state.displayScoreA(),
                    scoreB = state.displayScoreB(),
                    isTieBreak = state.isTieBreak
                )
            } else null
        } else null

        val resolvedEndedAt = endedAt ?: events.lastOrNull()?.timestampMillis
        val resolvedDuration = if (manualFinish) {
            manualDurationMs
        } else {
            resolvedEndedAt?.let { state.elapsedDurationMillis(it) }
        }

        val summary = MatchSummary(
            id = matchId,
            format = format,
            deuceMode = deuceMode,
            status = MatchStatus.COMPLETED,
            startedAt = startedAt,
            endedAt = resolvedEndedAt,
            durationMs = resolvedDuration,
            winnerTeam = winner,
            userWon = userWon,
            teamANames = teamA.map { it.player.displayName },
            teamBNames = teamB.map { it.player.displayName },
            sets = state.completedSets,
            isUserParticipant = isUserParticipant,
            incompleteSet = incompleteSet
        )
        val analytics = MatchStatsCalculator.computeAnalytics(config, summary, events)
        return MatchDetailsData(
            summary = summary,
            teamAPlayers = teamA,
            teamBPlayers = teamB,
            stats = analytics.eventStats,
            analytics = analytics
        )
    }

    private fun winSetLoveEvents(team: TeamSide, startTs: Long, stepMs: Long = 5_000L): List<MatchEngineEvent> =
        List(24) { idx ->
            MatchEngineEvent.PointWon(team, startTs + (idx + 1) * stepMs)
        }

    @Test
    fun `singles normal Team A win with user on Team A`() {
        val events = winSetLoveEvents(TeamSide.A, 10_000L) +
            winSetLoveEvents(TeamSide.A, 130_000L)
        val details = buildDetailsWithRealAnalytics(
            format = MatchFormat.SINGLES,
            teamA = listOf(player("u1", "Максим", avatarId = 3, isUser = true)),
            teamB = listOf(player("f1", "Илья", avatarId = 7, isUser = false)),
            events = events
        )

        val poster = mapper.map(details)

        assertEquals(MatchFormat.SINGLES, poster.format)
        assertEquals(3, poster.bestOf)
        assertEquals(PosterOutcomeKind.USER_WIN, poster.outcome.kind)
        assertEquals(TeamSide.A, poster.outcome.winnerTeam)
        assertEquals(true, poster.outcome.userWon)
        assertTrue(poster.outcome.isUserParticipant)
        assertFalse(poster.outcome.isManuallyCompleted)
        assertTrue(poster.teamA.isWinner)
        assertFalse(poster.teamB.isWinner)
        assertTrue(poster.teamA.hasUser)
        assertFalse(poster.teamB.hasUser)
        assertEquals(2, poster.setsWonA)
        assertEquals(0, poster.setsWonB)
        assertEquals(2, poster.completedSets.size)
        assertNull(poster.incompleteSet)
        assertEquals(48, poster.pointStats?.totalPoints)
        assertEquals(100f, poster.pointStats?.teamAPercent)
        assertEquals(0f, poster.pointStats?.teamBPercent)
        assertEquals(48, poster.momentum.size)
        assertEquals("datetime:250000", poster.dateTimeText)
        assertEquals("date:250000", poster.dateText)
        assertNotNull(poster.durationText)
    }

    @Test
    fun `singles normal Team B win with user on Team A results in loss`() {
        val events = winSetLoveEvents(TeamSide.B, 10_000L) +
            winSetLoveEvents(TeamSide.B, 130_000L)
        val details = buildDetailsWithRealAnalytics(
            format = MatchFormat.SINGLES,
            teamA = listOf(player("u1", "Максим", avatarId = 2, isUser = true)),
            teamB = listOf(player("f1", "Дмитрий", avatarId = 9, isUser = false)),
            events = events
        )

        val poster = mapper.map(details)

        assertEquals(PosterOutcomeKind.USER_LOSS, poster.outcome.kind)
        assertEquals(TeamSide.B, poster.outcome.winnerTeam)
        assertEquals(false, poster.outcome.userWon)
        assertFalse(poster.teamA.isWinner)
        assertTrue(poster.teamB.isWinner)
        assertEquals(0, poster.setsWonA)
        assertEquals(2, poster.setsWonB)
    }

    @Test
    fun `user on Team B winning produces USER_WIN and preserves team side`() {
        val events = winSetLoveEvents(TeamSide.B, 10_000L) +
            winSetLoveEvents(TeamSide.B, 130_000L)
        val details = buildDetailsWithRealAnalytics(
            format = MatchFormat.SINGLES,
            teamA = listOf(player("f1", "Артём", avatarId = 1, isUser = false)),
            teamB = listOf(player("u1", "Анна", avatarId = 11, isUser = true)),
            events = events
        )

        val poster = mapper.map(details)

        assertEquals(PosterOutcomeKind.USER_WIN, poster.outcome.kind)
        assertEquals(TeamSide.B, poster.outcome.winnerTeam)
        assertEquals(true, poster.outcome.userWon)
        assertFalse(poster.teamA.hasUser)
        assertTrue(poster.teamB.hasUser)
        assertTrue(poster.teamB.isWinner)
        assertEquals(11, poster.teamB.players.single().avatarId)
    }

    @Test
    fun `doubles preserves four players in exact participant order with avatars and user flags`() {
        val events = winSetLoveEvents(TeamSide.A, 10_000L) +
            winSetLoveEvents(TeamSide.A, 130_000L)
        val details = buildDetailsWithRealAnalytics(
            format = MatchFormat.DOUBLES,
            teamA = listOf(
                player("p1", "Алексей", avatarId = 0, isUser = false),
                player("p2", "Екатерина", avatarId = 5, isUser = true)
            ),
            teamB = listOf(
                player("p3", "Никита", avatarId = 8, isUser = false),
                player("p4", "Ольга", avatarId = 10, isUser = false)
            ),
            events = events
        )

        val poster = mapper.map(details)

        assertEquals(MatchFormat.DOUBLES, poster.format)
        assertEquals(listOf("Алексей", "Екатерина"), poster.teamA.players.map { it.displayName })
        assertEquals(listOf(0, 5), poster.teamA.players.map { it.avatarId })
        assertEquals(listOf(false, true), poster.teamA.players.map { it.isUser })
        assertEquals(listOf("Никита", "Ольга"), poster.teamB.players.map { it.displayName })
        assertEquals(listOf(8, 10), poster.teamB.players.map { it.avatarId })
        assertEquals(listOf(false, false), poster.teamB.players.map { it.isUser })
    }

    @Test
    fun `non-participant match uses TEAM_WIN without inventing personal win or loss`() {
        val events = winSetLoveEvents(TeamSide.A, 10_000L) +
            winSetLoveEvents(TeamSide.A, 130_000L)
        val details = buildDetailsWithRealAnalytics(
            format = MatchFormat.DOUBLES,
            teamA = listOf(
                player("p1", "Игрок 1", avatarId = 1),
                player("p2", "Игрок 2", avatarId = 2)
            ),
            teamB = listOf(
                player("p3", "Игрок 3", avatarId = 3),
                player("p4", "Игрок 4", avatarId = 4)
            ),
            events = events
        )

        val poster = mapper.map(details)

        assertEquals(PosterOutcomeKind.TEAM_WIN, poster.outcome.kind)
        assertEquals(TeamSide.A, poster.outcome.winnerTeam)
        assertNull(poster.outcome.userWon)
        assertFalse(poster.outcome.isUserParticipant)
        assertFalse(poster.outcome.isManuallyCompleted)
        assertTrue(poster.teamA.isWinner)
        assertFalse(poster.teamB.isWinner)
    }

    @Test
    fun `manual completion with no completed sets produces neutral outcome and manual badge`() {
        // 1 completed game for A + 2 points in current game
        val events = List(6) { idx ->
            MatchEngineEvent.PointWon(TeamSide.A, 10_000L + (idx + 1) * 10_000L)
        }
        val details = buildDetailsWithRealAnalytics(
            format = MatchFormat.SINGLES,
            teamA = listOf(player("u1", "Вы", avatarId = 4, isUser = true)),
            teamB = listOf(player("f1", "Соперник", avatarId = 6)),
            events = events,
            manualFinish = true,
            manualDurationMs = 700_000L,
            endedAt = 710_000L
        )

        val poster = mapper.map(details)

        assertEquals(PosterOutcomeKind.MANUAL_NEUTRAL, poster.outcome.kind)
        assertNull(poster.outcome.winnerTeam)
        assertNull(poster.outcome.userWon)
        assertTrue(poster.outcome.isUserParticipant)
        assertTrue(poster.outcome.isManuallyCompleted)
        assertFalse(poster.teamA.isWinner)
        assertFalse(poster.teamB.isWinner)
        assertTrue(poster.completedSets.isEmpty())
        assertEquals(0, poster.setsWonA)
        assertEquals(0, poster.setsWonB)
        assertNotNull(poster.incompleteSet)
        assertEquals(1, poster.incompleteSet?.setIndex)
        assertEquals(1, poster.incompleteSet?.gamesA)
        assertEquals(0, poster.incompleteSet?.gamesB)
        assertEquals("30", poster.incompleteSet?.scoreA)
        assertEquals("0", poster.incompleteSet?.scoreB)
        assertFalse(poster.incompleteSet?.isTieBreak ?: true)
        assertEquals(6, poster.incompleteSet?.pointsPlayed)
        assertEquals(700_000L, poster.incompleteSet?.durationMs)
    }

    @Test
    fun `manual completion with completed sets and incomplete set keeps them strictly separate`() {
        // Set 1 won 6-0 by A (24 pts), then 2 games + tie-break/points in Set 2
        val set1Events = winSetLoveEvents(TeamSide.A, 10_000L)
        // Reach 6-6 in set 2 and play 3 tie-break points
        var ts = 150_000L
        val set2Events = mutableListOf<MatchEngineEvent>()
        repeat(6) {
            repeat(4) { ts += 5_000L; set2Events += MatchEngineEvent.PointWon(TeamSide.A, ts) }
            repeat(4) { ts += 5_000L; set2Events += MatchEngineEvent.PointWon(TeamSide.B, ts) }
        }
        ts += 5_000L; set2Events += MatchEngineEvent.PointWon(TeamSide.A, ts)
        ts += 5_000L; set2Events += MatchEngineEvent.PointWon(TeamSide.A, ts)
        ts += 5_000L; set2Events += MatchEngineEvent.PointWon(TeamSide.B, ts)

        val details = buildDetailsWithRealAnalytics(
            format = MatchFormat.DOUBLES,
            teamA = listOf(player("u1", "Аня", 1, true), player("p2", "Борис", 2)),
            teamB = listOf(player("p3", "Виктор", 3), player("p4", "Галина", 4)),
            events = set1Events + set2Events,
            manualFinish = true,
            manualDurationMs = 900_000L,
            endedAt = 910_000L
        )

        val poster = mapper.map(details)

        assertEquals(PosterOutcomeKind.MANUAL_NEUTRAL, poster.outcome.kind)
        assertNull(poster.outcome.winnerTeam)
        assertTrue(poster.outcome.isManuallyCompleted)
        // Only completed set 1 counts toward setsWon
        assertEquals(1, poster.completedSets.size)
        assertEquals(1, poster.setsWonA)
        assertEquals(0, poster.setsWonB)
        // Incomplete set 2 is kept separate with tie-break state
        val inc = poster.incompleteSet
        assertNotNull(inc)
        assertEquals(2, inc?.setIndex)
        assertEquals(6, inc?.gamesA)
        assertEquals(6, inc?.gamesB)
        assertEquals("2", inc?.scoreA)
        assertEquals("1", inc?.scoreB)
        assertTrue(inc?.isTieBreak == true)
        assertEquals(51, inc?.pointsPlayed)
    }

    @Test
    fun `no points played produces null percentages empty momentum and empty highlights`() {
        val details = buildDetailsWithRealAnalytics(
            format = MatchFormat.SINGLES,
            teamA = listOf(player("u1", "Павел", 0, true)),
            teamB = listOf(player("f1", "Роман", 1)),
            events = emptyList(),
            manualFinish = true,
            manualDurationMs = null,
            endedAt = null
        )

        val poster = mapper.map(details)

        assertEquals(0, poster.pointStats?.totalPoints)
        assertEquals(0, poster.pointStats?.teamAPoints)
        assertEquals(0, poster.pointStats?.teamBPoints)
        assertNull(poster.pointStats?.teamAPercent)
        assertNull(poster.pointStats?.teamBPercent)
        assertTrue(poster.momentum.isEmpty())
        assertTrue(poster.highlights.isEmpty())
        assertNull(poster.durationMs)
        assertNull(poster.durationText)
        assertNull(poster.incompleteSet)
        assertEquals("date:10000", poster.dateText)
    }

    @Test
    fun `missing optional analytics handles null gracefully without recalculating`() {
        val summary = MatchSummary(
            id = "m_no_analytics",
            format = MatchFormat.SINGLES,
            deuceMode = DeuceMode.ADVANTAGE,
            status = MatchStatus.COMPLETED,
            startedAt = 1_000L,
            endedAt = 601_000L,
            durationMs = 600_000L,
            winnerTeam = TeamSide.A,
            userWon = true,
            teamANames = listOf("Иван"),
            teamBNames = listOf("Пётр"),
            sets = listOf(
                SetScore(setIndex = 1, gamesA = 7, gamesB = 6, tbA = 7, tbB = 4),
                SetScore(setIndex = 2, gamesA = 6, gamesB = 3)
            ),
            isUserParticipant = true,
            incompleteSet = null
        )
        val details = MatchDetailsData(
            summary = summary,
            teamAPlayers = listOf(player("u1", "Иван", 2, isUser = true)),
            teamBPlayers = listOf(player("f1", "Пётр", 5, isUser = false)),
            stats = null,
            analytics = null
        )

        val poster = mapper.map(details)

        assertEquals(PosterOutcomeKind.USER_WIN, poster.outcome.kind)
        assertEquals(DeuceMode.ADVANTAGE, poster.deuceMode)
        assertNull(poster.bestOf)
        assertEquals(2, poster.setsWonA)
        assertEquals(0, poster.setsWonB)
        assertEquals(2, poster.completedSets.size)
        assertEquals(7, poster.completedSets[0].tbA)
        assertEquals(4, poster.completedSets[0].tbB)
        assertNull(poster.completedSets[0].durationText)
        assertNull(poster.completedSets[0].pointsPlayed)
        assertNull(poster.pointStats)
        assertNull(poster.gameStats)
        assertTrue(poster.momentum.isEmpty())
        assertTrue(poster.highlights.isEmpty())
        assertEquals("10 мин 00 сек", poster.durationText)
    }

    @Test
    fun `tiebreak set scores and long Cyrillic player names are preserved intact`() {
        var ts = 10_000L
        val setEvents = mutableListOf<MatchEngineEvent>()
        repeat(6) {
            repeat(4) { ts += 2_000L; setEvents += MatchEngineEvent.PointWon(TeamSide.A, ts) }
            repeat(4) { ts += 2_000L; setEvents += MatchEngineEvent.PointWon(TeamSide.B, ts) }
        }
        // Tie-break 7-5 for A
        repeat(5) {
            ts += 2_000L; setEvents += MatchEngineEvent.PointWon(TeamSide.A, ts)
            ts += 2_000L; setEvents += MatchEngineEvent.PointWon(TeamSide.B, ts)
        }
        ts += 2_000L; setEvents += MatchEngineEvent.PointWon(TeamSide.A, ts)
        ts += 2_000L; setEvents += MatchEngineEvent.PointWon(TeamSide.A, ts)
        // Second set 6-0 for A
        val set2Events = winSetLoveEvents(TeamSide.A, ts, stepMs = 2_000L)

        val longNameA = "Александр Константинопольский-Старший"
        val longNameB = "Вениамин Александровский-младший"

        val details = buildDetailsWithRealAnalytics(
            format = MatchFormat.SINGLES,
            deuceMode = DeuceMode.STAR_POINT,
            teamA = listOf(player("u1", longNameA, avatarId = 9, isUser = true)),
            teamB = listOf(player("f1", longNameB, avatarId = 6, isUser = false)),
            events = setEvents + set2Events
        )

        val poster = mapper.map(details)

        assertEquals(DeuceMode.STAR_POINT, poster.deuceMode)
        assertEquals(longNameA, poster.teamA.players.single().displayName)
        assertEquals(longNameB, poster.teamB.players.single().displayName)
        assertEquals(9, poster.teamA.players.single().avatarId)
        assertEquals(6, poster.teamB.players.single().avatarId)
        val firstSet = poster.completedSets.first()
        assertEquals(7, firstSet.gamesA)
        assertEquals(6, firstSet.gamesB)
        assertEquals(7, firstSet.tbA)
        assertEquals(5, firstSet.tbB)
        assertEquals(60, firstSet.pointsPlayed)
        assertNotNull(firstSet.durationText)
    }

    @Test
    fun `missing or fallback player records are preserved without inventing fake identities`() {
        val summary = MatchSummary(
            id = "m_missing_players",
            format = MatchFormat.SINGLES,
            deuceMode = DeuceMode.GOLDEN_POINT,
            status = MatchStatus.COMPLETED,
            startedAt = 1_000L,
            endedAt = 121_000L,
            durationMs = 120_000L,
            winnerTeam = TeamSide.B,
            userWon = null,
            teamANames = emptyList(),
            teamBNames = emptyList(),
            sets = listOf(SetScore(1, 3, 6)),
            isUserParticipant = false,
            incompleteSet = null
        )
        val details = MatchDetailsData(
            summary = summary,
            teamAPlayers = listOf(player("missing_a", "Игрок", avatarId = 0, isUser = false)),
            teamBPlayers = emptyList(),
            stats = null,
            analytics = null
        )

        val poster = mapper.map(details)

        assertEquals(1, poster.teamA.players.size)
        assertEquals("Игрок", poster.teamA.players.single().displayName)
        assertEquals(0, poster.teamA.players.single().avatarId)
        assertTrue(poster.teamB.players.isEmpty())
        assertEquals(PosterOutcomeKind.TEAM_WIN, poster.outcome.kind)
        assertEquals(TeamSide.B, poster.outcome.winnerTeam)
        assertNull(poster.outcome.userWon)
    }

    @Test
    fun `full multi-set match maps game stats highlights and hour duration accurately`() {
        // Set 1: A wins 6-0; Set 2: B wins 6-4; Set 3: A wins 6-2 (total 3 sets -> highlights populated)
        val log = mutableListOf<MatchEngineEvent>()
        var ts = 10_000L
        fun addPoints(team: TeamSide, count: Int, stepMs: Long = 20_000L) {
            repeat(count) {
                ts += stepMs
                log += MatchEngineEvent.PointWon(team, ts)
            }
        }
        // Set 1: 6-0 A
        addPoints(TeamSide.A, 24)
        // Set 2: alternating to 4-4, then B wins 2 games -> 4-6 B
        repeat(4) {
            addPoints(TeamSide.A, 4)
            addPoints(TeamSide.B, 4)
        }
        addPoints(TeamSide.B, 8)
        // Set 3: A wins 6-2
        repeat(2) {
            addPoints(TeamSide.B, 4)
            addPoints(TeamSide.A, 4)
        }
        addPoints(TeamSide.A, 16)

        val details = buildDetailsWithRealAnalytics(
            format = MatchFormat.DOUBLES,
            teamA = listOf(player("u1", "Вы", 0, true), player("p2", "Партнёр", 1)),
            teamB = listOf(player("p3", "Соперник 1", 2), player("p4", "Соперник 2", 3)),
            events = log
        )

        val poster = mapper.map(details)

        assertEquals(2, poster.setsWonA)
        assertEquals(1, poster.setsWonB)
        assertEquals(3, poster.completedSets.size)
        assertEquals(16, poster.gameStats?.gamesWonA)
        assertEquals(8, poster.gameStats?.gamesWonB)
        assertTrue((poster.gameStats?.gamesLeadChanges ?: 0) >= 0)
        assertNotNull(poster.gameStats?.biggestGamesLead)
        assertNotNull(poster.gameStats?.longestSet)
        assertNotNull(poster.gameStats?.shortestSet)
        assertEquals(3, poster.gameStats?.setPerformances?.size)
        assertTrue(poster.highlights.isNotEmpty())
        assertTrue((poster.pointStats?.maxPointStreakA ?: 0) >= 16)
        assertEquals(poster.momentum.size, poster.pointStats?.totalPoints)
        assertTrue(poster.durationText?.contains("мин") == true || poster.durationText?.contains("ч") == true)
    }

    @Test
    fun `duration formatting supports seconds and hours`() {
        val shortSummary = MatchSummary(
            id = "m_sec",
            format = MatchFormat.SINGLES,
            deuceMode = DeuceMode.GOLDEN_POINT,
            status = MatchStatus.COMPLETED,
            startedAt = 1_000L,
            endedAt = 46_000L,
            durationMs = 45_000L,
            winnerTeam = TeamSide.A,
            userWon = true,
            teamANames = listOf("А"),
            teamBNames = listOf("Б"),
            sets = emptyList(),
            isUserParticipant = true
        )
        val longSummary = shortSummary.copy(
            id = "m_hr",
            endedAt = 3_901_000L,
            durationMs = 3_900_000L // 1h 05m
        )

        val shortPoster = mapper.map(
            MatchDetailsData(shortSummary, listOf(player("a", "А", 0, true)), listOf(player("b", "Б", 1)), null, null)
        )
        val longPoster = mapper.map(
            MatchDetailsData(longSummary, listOf(player("a", "А", 0, true)), listOf(player("b", "Б", 1)), null, null)
        )

        assertEquals("45 сек", shortPoster.durationText)
        assertEquals("1 ч 05 мин", longPoster.durationText)
    }
}
