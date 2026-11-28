package com.padelpro.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.data.repository.MatchRepositoryImpl
import com.padelpro.app.data.repository.PlayerRepositoryImpl
import com.padelpro.app.data.repository.StatsRepositoryImpl
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormOutcome
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchHighlight
import com.padelpro.app.domain.model.StartMatchParticipant
import com.padelpro.app.domain.model.StartMatchRequest
import com.padelpro.app.domain.model.StartMatchResult
import com.padelpro.app.domain.model.TeamSide
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatchAnalyticsPersistenceRobolectricTest {

    private class AnalyticsTestTimeProvider(var currentNow: Long = 10_000L) : TimeProvider {
        override fun nowMillis(): Long = currentNow
        override fun currentHourOfDay(): Int = 12
        override fun currentLocalDate(): LocalDate = LocalDate.of(2026, 11, 28)
        override fun formatMonthYear(epochMillis: Long): String = "нояб 2026"
        override fun formatShortDate(epochMillis: Long): String = "28 нояб 2026"
    }

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var db: PadelProDatabase
    private lateinit var timeProvider: AnalyticsTestTimeProvider
    private lateinit var playerRepo: PlayerRepositoryImpl
    private lateinit var matchRepo: MatchRepositoryImpl
    private lateinit var statsRepo: StatsRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.cacheDir, "test_padel_analytics_${System.nanoTime()}.db")
        db = Room.databaseBuilder(context, PadelProDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        timeProvider = AnalyticsTestTimeProvider()
        playerRepo = PlayerRepositoryImpl(db, SettingsRepository(context), timeProvider)
        matchRepo = MatchRepositoryImpl(db, timeProvider)
        statsRepo = StatsRepositoryImpl(matchRepo, timeProvider, ZoneId.of("UTC"))
    }

    @After
    fun teardown() {
        if (db.isOpen) db.close()
        dbFile.delete()
    }

    private suspend fun startSingles(): String {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        return (
            matchRepo.startMatch(
                StartMatchRequest(
                    format = MatchFormat.SINGLES,
                    deuceMode = DeuceMode.GOLDEN_POINT,
                    firstServerTeam = TeamSide.A,
                    participants = listOf(
                        StartMatchParticipant(user.id, TeamSide.A, 0, isUser = true),
                        StartMatchParticipant(opp.id, TeamSide.B, 0, isUser = false)
                    )
                )
            ) as StartMatchResult.Started
            ).matchId
    }

    private suspend fun startDoubles(): String {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.DOUBLES)
        val partner = playerRepo.createFriend("Иван", 1)
        val opp1 = playerRepo.createFriend("Марк", 2)
        val opp2 = playerRepo.createFriend("Лев", 3)
        return (
            matchRepo.startMatch(
                StartMatchRequest(
                    format = MatchFormat.DOUBLES,
                    deuceMode = DeuceMode.GOLDEN_POINT,
                    firstServerTeam = TeamSide.A,
                    participants = listOf(
                        StartMatchParticipant(user.id, TeamSide.A, 0, isUser = true),
                        StartMatchParticipant(partner.id, TeamSide.A, 1, isUser = false),
                        StartMatchParticipant(opp1.id, TeamSide.B, 0, isUser = false),
                        StartMatchParticipant(opp2.id, TeamSide.B, 1, isUser = false)
                    )
                )
            ) as StartMatchResult.Started
            ).matchId
    }

    private suspend fun addPoints(team: TeamSide, n: Int, stepMs: Long = 30_000L) {
        repeat(n) {
            timeProvider.currentNow += stepMs
            matchRepo.addPoint(team)
        }
    }

    @Test
    fun `auto completed win produces full analytics and keeps legacy stats intact`() = runTest {
        val matchId = startSingles()
        addPoints(TeamSide.A, 48, stepMs = 1_000L)

        val details = matchRepo.observeMatchDetails(matchId).first()!!
        val analytics = details.analytics
        assertNotNull(analytics)
        analytics!!

        assertEquals(TeamSide.A, analytics.overview.winnerTeam)
        assertFalse(analytics.overview.isManuallyCompleted)
        assertEquals(48_000L, analytics.overview.durationMs)
        assertEquals(48, analytics.pointStats.totalPoints)
        assertEquals(100f, analytics.pointStats.teamAPointPercent!!, 0.0001f)
        assertEquals(0f, analytics.pointStats.teamBPointPercent!!, 0.0001f)
        assertEquals(48, analytics.momentum.size)
        assertEquals(48, analytics.momentum.last().pointDifference)
        assertEquals(12, analytics.gameStats.gamesWonA)
        assertEquals(2, analytics.gameStats.setsWonA)

        val streak = analytics.highlights
            .filterIsInstance<MatchHighlight.LongestPointStreak>().single()
        assertEquals(TeamSide.A, streak.team)
        assertEquals(48, streak.length)

        // Существующая карточка статистики не изменилась
        assertEquals(48, details.stats!!.totalPoints)
        assertEquals(48, details.stats!!.teamAPoints)
        assertEquals(0, details.stats!!.teamBPoints)
        assertEquals(analytics.eventStats, details.stats)

        // XP-логика не тронута: 30 + 20
        assertEquals(50, playerRepo.observeUserProgress().first()?.totalXp)
        assertEquals(MatchFormOutcome.WIN, statsRepo.observePerformanceSummary().first().recentForm.first())
    }

    @Test
    fun `auto completed loss keeps winner B and LOSS form for user`() = runTest {
        val matchId = startSingles()
        addPoints(TeamSide.B, 48, stepMs = 1_000L)

        val details = matchRepo.observeMatchDetails(matchId).first()!!
        val analytics = details.analytics!!
        assertEquals(TeamSide.B, analytics.overview.winnerTeam)
        assertEquals(-48, analytics.pointStats.pointDifference)
        assertEquals(false, details.summary.userWon)
        assertEquals(30, playerRepo.observeUserProgress().first()?.totalXp)
        assertEquals(MatchFormOutcome.LOSS, statsRepo.observePerformanceSummary().first().recentForm.first())
    }

    @Test
    fun `manual finish with incomplete set exposes honest draw analytics`() = runTest {
        val matchId = startSingles()
        addPoints(TeamSide.A, 24) // сет 1: 6-0
        addPoints(TeamSide.A, 8)  // сет 2: 2 гейма A
        addPoints(TeamSide.B, 4)  // 1 гейм B
        addPoints(TeamSide.A, 2)  // 30
        addPoints(TeamSide.B, 1)  // 15
        timeProvider.currentNow += 60_000L
        matchRepo.finishActiveMatchManually()

        val details = matchRepo.observeMatchDetails(matchId).first()!!
        val analytics = details.analytics!!
        assertTrue(analytics.overview.isManuallyCompleted)
        assertNull(analytics.overview.winnerTeam)      // победитель не выдуман
        assertNull(details.summary.userWon)
        assertEquals(1, analytics.overview.setsWonA)
        assertEquals(0, analytics.overview.setsWonB)
        assertEquals(8, analytics.gameStats.gamesWonA) // только реальные геймы
        assertEquals(1, analytics.gameStats.gamesWonB)
        assertEquals(1, analytics.overview.sets.size)  // незаконченный сет не добавлен
        val inc = analytics.overview.incompleteSet!!
        assertEquals(2, inc.setIndex)
        assertEquals("30", inc.scoreA)
        assertEquals("15", inc.scoreB)
        assertEquals(39, analytics.pointStats.totalPoints)
        assertEquals(39, analytics.momentum.size)
        assertEquals(15, analytics.overview.incompleteSetPointsPlayed)
        assertEquals(39 * 30_000L + 60_000L, analytics.overview.durationMs)
        assertEquals(24 * 30_000L, analytics.gameStats.setPerformances.single().durationMs)
        assertEquals(15 * 30_000L + 60_000L, analytics.overview.incompleteSetDurationMs)
        assertEquals(39, details.stats!!.totalPoints) // legacy совместимость (как в старом тесте)

        // Реальное правило приложения: ручное завершение = DRAW, XP 15
        assertEquals(MatchFormOutcome.DRAW, statsRepo.observePerformanceSummary().first().recentForm.first())
        assertEquals(15, playerRepo.observeUserProgress().first()?.totalXp)
    }

    @Test
    fun `manual finish with clear leader still has no winner - WIN and LOSS only automatic`() = runTest {
        val matchId = startSingles()
        addPoints(TeamSide.A, 24) // 12 минут, сет 6-0
        matchRepo.finishActiveMatchManually()

        val details = matchRepo.observeMatchDetails(matchId).first()!!
        val analytics = details.analytics!!
        assertTrue(analytics.overview.isManuallyCompleted)
        assertNull(analytics.overview.winnerTeam)
        assertNull(analytics.overview.incompleteSet) // сет завершён ровно на границе
        assertEquals(1, analytics.overview.setsWonA)
        assertEquals(MatchFormOutcome.DRAW, statsRepo.observePerformanceSummary().first().recentForm.first())
    }

    @Test
    fun `undo removes points from analytics without double counting before and after manual finish`() = runTest {
        val matchId = startSingles()
        addPoints(TeamSide.A, 3, stepMs = 1_000L)
        addPoints(TeamSide.B, 2, stepMs = 1_000L)

        assertTrue(matchRepo.undoLastPoint())
        assertTrue(matchRepo.undoLastPoint())

        val activeDetails = matchRepo.observeMatchDetails(matchId).first()!!
        val activeAnalytics = activeDetails.analytics!!
        assertEquals(3, activeAnalytics.pointStats.totalPoints)
        assertEquals(3, activeAnalytics.momentum.size)
        assertEquals(0, activeAnalytics.pointStats.teamBPoints)
        val persistedPoints = db.matchDao().getEventsForMatch(matchId)
            .count { it.type == MatchRepositoryImpl.EVENT_POINT_WON }
        assertEquals(3, persistedPoints)
        assertEquals(persistedPoints, activeAnalytics.pointStats.totalPoints)

        timeProvider.currentNow += 11 * 60_000L
        matchRepo.finishActiveMatchManually()

        val finished = matchRepo.observeMatchDetails(matchId).first()!!.analytics!!
        assertEquals(3, finished.pointStats.totalPoints)
        assertEquals(3, finished.momentum.size)
        assertEquals(3, finished.pointStats.maxPointStreakA)
        assertEquals(100f, finished.pointStats.teamAPointPercent!!, 0.0001f)
        assertTrue(finished.overview.isManuallyCompleted)
        assertEquals(15, playerRepo.observeUserProgress().first()?.totalXp)
    }

    @Test
    fun `paused time excluded from analytics durations matches persisted duration_ms`() = runTest {
        val matchId = startSingles()
        addPoints(TeamSide.A, 24)          // 12 мин
        assertTrue(matchRepo.pauseActiveMatch())
        timeProvider.currentNow += 300_000L // 5 мин паузы
        assertTrue(matchRepo.resumeActiveMatch())
        addPoints(TeamSide.A, 24)          // 12 мин

        val details = matchRepo.observeMatchDetails(matchId).first()!!
        val analytics = details.analytics!!
        assertEquals(24 * 60_000L, details.summary.durationMs)
        assertEquals(details.summary.durationMs, analytics.overview.durationMs)
        assertEquals(
            24 * 60_000L,
            analytics.gameStats.setPerformances.sumOf { it.durationMs }
        )
        assertEquals(48, analytics.pointStats.totalPoints)
    }

    @Test
    fun `active match during first set exposes incompleteSet games and duration without completed sets`() = runTest {
        val matchId = startDoubles()
        addPoints(TeamSide.A, 8, stepMs = 15_000L) // 2 гейма A (120 c)
        addPoints(TeamSide.B, 4, stepMs = 15_000L) // 1 гейм B (60 c)
        addPoints(TeamSide.A, 2, stepMs = 15_000L) // 30
        addPoints(TeamSide.B, 1, stepMs = 15_000L) // 15

        val details = requireNotNull(matchRepo.observeMatchDetails(matchId).first())
        val analytics = requireNotNull(details.analytics)
        val overview = analytics.overview

        assertFalse(overview.isCompleted)
        assertFalse(overview.isManuallyCompleted)
        assertNull(overview.winnerTeam)
        assertEquals(MatchFormat.DOUBLES, overview.format)
        assertTrue(overview.sets.isEmpty())
        assertEquals(0, overview.setsWonA)
        assertEquals(0, overview.setsWonB)
        assertEquals(2, analytics.gameStats.gamesWonA)
        assertEquals(1, analytics.gameStats.gamesWonB)
        assertEquals(15, analytics.pointStats.totalPoints)
        assertEquals(15, analytics.momentum.size)

        val inc = requireNotNull(overview.incompleteSet)
        assertEquals(1, inc.setIndex)
        assertEquals(2, inc.gamesA)
        assertEquals(1, inc.gamesB)
        assertEquals("30", inc.scoreA)
        assertEquals("15", inc.scoreB)
        assertEquals(15 * 15_000L, overview.durationMs)
        assertEquals(15, overview.incompleteSetPointsPlayed)
        assertEquals(15 * 15_000L, overview.incompleteSetDurationMs)
    }

    @Test
    fun `active match restores set 1 on boundary with null incompleteSet then tracks set 2 after first rally`() = runTest {
        val matchId = startSingles()
        addPoints(TeamSide.A, 24, stepMs = 10_000L) // Сет 1: 6-0 (240 с)

        // Граница сетов: 2-й сет ещё не начался -> incompleteSet == null
        val boundaryDetails = requireNotNull(matchRepo.observeMatchDetails(matchId).first())
        val boundaryAnalytics = requireNotNull(boundaryDetails.analytics)
        assertEquals(1, boundaryAnalytics.overview.sets.size)
        assertEquals(1, boundaryAnalytics.overview.setsWonA)
        assertEquals(0, boundaryAnalytics.overview.setsWonB)
        assertNull(boundaryAnalytics.overview.incompleteSet)
        assertNull(boundaryAnalytics.overview.incompleteSetPointsPlayed)
        assertNull(boundaryAnalytics.overview.incompleteSetDurationMs)
        assertEquals(6, boundaryAnalytics.gameStats.gamesWonA)
        assertEquals(0, boundaryAnalytics.gameStats.gamesWonB)
        assertEquals(240_000L, boundaryAnalytics.overview.durationMs)

        // Первый розыгрыш 2-го сета -> incompleteSet появляется, в sets не добавляется
        addPoints(TeamSide.B, 1, stepMs = 10_000L) // 0-15 во 2-м сете
        addPoints(TeamSide.B, 4, stepMs = 10_000L) // 1 гейм B и 0-15 в следующем гейме

        val set2Details = requireNotNull(matchRepo.observeMatchDetails(matchId).first())
        val set2Analytics = requireNotNull(set2Details.analytics)
        assertEquals(1, set2Analytics.overview.sets.size)
        assertEquals(1, set2Analytics.overview.setsWonA)
        assertEquals(0, set2Analytics.overview.setsWonB)
        assertEquals(6, set2Analytics.gameStats.gamesWonA)
        assertEquals(1, set2Analytics.gameStats.gamesWonB)
        assertEquals(29, set2Analytics.pointStats.totalPoints)
        assertEquals(290_000L, set2Analytics.overview.durationMs)
        assertEquals(5, set2Analytics.overview.incompleteSetPointsPlayed)
        assertEquals(50_000L, set2Analytics.overview.incompleteSetDurationMs)

        val inc = requireNotNull(set2Analytics.overview.incompleteSet)
        assertEquals(2, inc.setIndex)
        assertEquals(0, inc.gamesA)
        assertEquals(1, inc.gamesB)
        assertEquals("0", inc.scoreA)
        assertEquals("15", inc.scoreB)
    }

    @Test
    fun `active match after two completed sets and during third set restores both sets and highlights`() = runTest {
        val matchId = startSingles()
        addPoints(TeamSide.A, 24, stepMs = 10_000L) // Сет 1: 6-0 (240 с)
        addPoints(TeamSide.B, 24, stepMs = 20_000L) // Сет 2: 0-6 (480 с) -> 1:1 по сетам
        addPoints(TeamSide.A, 8, stepMs = 15_000L)  // Сет 3: 2-0 (120 с)
        addPoints(TeamSide.B, 1, stepMs = 15_000L)  // 0-15 (15 с)

        val details = requireNotNull(matchRepo.observeMatchDetails(matchId).first())
        val analytics = requireNotNull(details.analytics)
        val overview = analytics.overview

        assertFalse(overview.isCompleted)
        assertEquals(2, overview.sets.size)
        assertEquals(1, overview.setsWonA)
        assertEquals(1, overview.setsWonB)
        assertEquals(8, analytics.gameStats.gamesWonA)
        assertEquals(6, analytics.gameStats.gamesWonB)
        assertEquals(57, analytics.pointStats.totalPoints)
        assertEquals(855_000L, overview.durationMs)
        assertEquals(9, overview.incompleteSetPointsPlayed)
        assertEquals(135_000L, overview.incompleteSetDurationMs)

        val inc = requireNotNull(overview.incompleteSet)
        assertEquals(3, inc.setIndex)
        assertEquals(2, inc.gamesA)
        assertEquals(0, inc.gamesB)
        assertEquals("0", inc.scoreA)
        assertEquals("15", inc.scoreB)

        assertEquals(2, analytics.gameStats.setPerformances.size)
        assertEquals(2, analytics.gameStats.longestSet?.setIndex)
        assertEquals(1, analytics.gameStats.shortestSet?.setIndex)
        assertTrue(analytics.highlights.any { it is MatchHighlight.MostDominantSet })
        assertTrue(analytics.highlights.any { it is MatchHighlight.ClosestSet })
    }

    @Test
    fun `active match undo across completed set boundary reverts set back to incompleteSet`() = runTest {
        val matchId = startSingles()
        addPoints(TeamSide.A, 24, stepMs = 10_000L) // Сет 1 завершён 6-0

        val completedSetDetails = requireNotNull(matchRepo.observeMatchDetails(matchId).first())
        val beforeAnalytics = requireNotNull(completedSetDetails.analytics)
        assertEquals(1, beforeAnalytics.overview.sets.size)
        assertEquals(1, beforeAnalytics.overview.setsWonA)
        assertNull(beforeAnalytics.overview.incompleteSet)

        // Откатываем решающее очко 1-го сета
        assertTrue(matchRepo.undoLastPoint())

        val revertedDetails = requireNotNull(matchRepo.observeMatchDetails(matchId).first())
        val afterAnalytics = requireNotNull(revertedDetails.analytics)
        assertTrue(afterAnalytics.overview.sets.isEmpty())
        assertEquals(0, afterAnalytics.overview.setsWonA)
        assertEquals(5, afterAnalytics.gameStats.gamesWonA)
        assertEquals(23, afterAnalytics.pointStats.totalPoints)
        assertEquals(23, afterAnalytics.momentum.size)
        assertTrue(afterAnalytics.gameStats.setPerformances.isEmpty())
        assertNull(afterAnalytics.gameStats.longestSet)

        val inc = requireNotNull(afterAnalytics.overview.incompleteSet)
        assertEquals(1, inc.setIndex)
        assertEquals(5, inc.gamesA)
        assertEquals(0, inc.gamesB)
        assertEquals("40", inc.scoreA)
        assertEquals("0", inc.scoreB)
        assertEquals(23, afterAnalytics.overview.incompleteSetPointsPlayed)
    }

    @Test
    fun `active match during pause keeps exact play duration excluding pause interval on resubscribe`() = runTest {
        val matchId = startSingles()
        addPoints(TeamSide.A, 10, stepMs = 30_000L) // 5 мин активной игры (2-0 и 30-0)

        assertTrue(matchRepo.pauseActiveMatch())
        timeProvider.currentNow += 10 * 60_000L // 10 мин паузы

        // Новая подписка на observeMatchDetails во время паузы
        val pausedDetails = requireNotNull(matchRepo.observeMatchDetails(matchId).first())
        val pausedAnalytics = requireNotNull(pausedDetails.analytics)
        assertEquals(300_000L, pausedAnalytics.overview.durationMs)
        assertEquals(300_000L, pausedAnalytics.overview.incompleteSetDurationMs)
        assertEquals(2, pausedAnalytics.gameStats.gamesWonA)
        val incPaused = requireNotNull(pausedAnalytics.overview.incompleteSet)
        assertEquals(1, incPaused.setIndex)
        assertEquals(2, incPaused.gamesA)
        assertEquals("30", incPaused.scoreA)

        // Возобновляем и играем ещё 1 минуту (1 очко -> 40-0)
        assertTrue(matchRepo.resumeActiveMatch())
        addPoints(TeamSide.A, 1, stepMs = 60_000L)

        val resumedDetails = requireNotNull(matchRepo.observeMatchDetails(matchId).first())
        val resumedAnalytics = requireNotNull(resumedDetails.analytics)
        assertEquals(360_000L, resumedAnalytics.overview.durationMs)
        assertEquals(360_000L, resumedAnalytics.overview.incompleteSetDurationMs)
        assertEquals(11, resumedAnalytics.pointStats.totalPoints)
    }
}