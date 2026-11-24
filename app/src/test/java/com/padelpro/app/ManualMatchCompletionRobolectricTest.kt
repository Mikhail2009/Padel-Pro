package com.padelpro.app

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.database.entity.MatchEntity
import com.padelpro.app.core.database.entity.MatchEventEntity
import com.padelpro.app.core.database.entity.MatchParticipantEntity
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.data.backup.BackupRepositoryImpl
import com.padelpro.app.data.content.AssetAcademyContentLoader
import com.padelpro.app.data.repository.MatchRepositoryImpl
import com.padelpro.app.data.repository.PlayerRepositoryImpl
import com.padelpro.app.data.repository.StatsRepositoryImpl
import com.padelpro.app.domain.academy.ContentPack
import com.padelpro.app.domain.model.BackupImportOutcome
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.ManualFinishResult
import com.padelpro.app.domain.model.MatchFormOutcome
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.StartMatchParticipant
import com.padelpro.app.domain.model.StartMatchRequest
import com.padelpro.app.domain.model.StartMatchResult
import com.padelpro.app.domain.model.TeamSide
import com.padelpro.app.feature.history.HistoryResultFilter
import com.padelpro.app.feature.history.HistoryViewModel
import com.padelpro.app.feature.match.LiveMatchNavEvent
import com.padelpro.app.feature.match.LiveMatchViewModel
import com.padelpro.app.feature.match.MatchResultsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManualMatchCompletionRobolectricTest {

    private class ManualTestTimeProvider(
        var now: Long = LocalDate.of(2026, 11, 20)
            .atTime(12, 0)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
    ) : TimeProvider {
        override fun nowMillis(): Long = now
        override fun currentHourOfDay(): Int = 12
        override fun currentLocalDate(): LocalDate = LocalDate.of(2026, 11, 20)
        override fun formatMonthYear(epochMillis: Long): String = "нояб 2026"
        override fun formatShortDate(epochMillis: Long): String = "20 нояб 2026"
    }

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var db: PadelProDatabase
    private lateinit var timeProvider: ManualTestTimeProvider
    private lateinit var settings: SettingsRepository
    private lateinit var contentPack: ContentPack
    private lateinit var playerRepo: PlayerRepositoryImpl
    private lateinit var matchRepo: MatchRepositoryImpl
    private lateinit var statsRepo: StatsRepositoryImpl
    private lateinit var backupRepo: BackupRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.cacheDir, "test_padel_manual_${System.nanoTime()}.db")
        db = openDb()
        timeProvider = ManualTestTimeProvider()
        settings = SettingsRepository(context)
        contentPack = AssetAcademyContentLoader(context).load()
        playerRepo = PlayerRepositoryImpl(db, settings, timeProvider)
        matchRepo = MatchRepositoryImpl(db, timeProvider) { contentPack }
        statsRepo = StatsRepositoryImpl(matchRepo, timeProvider, ZoneId.of("UTC"))
        backupRepo = BackupRepositoryImpl(db, settings, timeProvider) { contentPack }
    }

    @After
    fun teardown() {
        if (db.isOpen) db.close()
        dbFile.delete()
    }

    private fun openDb(): PadelProDatabase =
        Room.databaseBuilder(context, PadelProDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()

    private suspend fun startSinglesMatch(userId: String, oppId: String): String {
        val res = matchRepo.startMatch(
            StartMatchRequest(
                format = MatchFormat.SINGLES,
                deuceMode = DeuceMode.GOLDEN_POINT,
                firstServerTeam = TeamSide.A,
                participants = listOf(
                    StartMatchParticipant(userId, TeamSide.A, 0, isUser = true),
                    StartMatchParticipant(oppId, TeamSide.B, 0, isUser = false)
                )
            )
        ) as StartMatchResult.Started
        return res.matchId
    }

    private suspend fun winSet(team: TeamSide) {
        repeat(24) {
            matchRepo.addPoint(team)
        }
    }

    private suspend fun awaitCondition(timeoutMs: Long = 5_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate()) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (System.currentTimeMillis() >= deadline) break
            delay(20L)
        }
        assertTrue(predicate())
    }

    @Test
    fun `1 - manual finish at 9m 59s deletes match and awards 0 XP`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val matchId = startSinglesMatch(user.id, opp.id)
        matchRepo.addPoint(TeamSide.A)

        timeProvider.now += (9 * 60 + 59) * 1000L // 599_000 ms
        val result = matchRepo.finishActiveMatchManually()

        assertEquals(ManualFinishResult.DiscardedTooShort, result)
        assertNull(db.matchDao().getMatchByIdOnce(matchId))
        assertNull(matchRepo.observeActiveMatch().first())
        assertTrue(matchRepo.observeCompletedMatches().first().isEmpty())
        assertEquals(0, playerRepo.observeUserProgress().first()?.totalXp)
    }

    @Test
    fun `2 - manual finish at exactly 10m 00s deletes match and awards 0 XP`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val matchId = startSinglesMatch(user.id, opp.id)
        matchRepo.addPoint(TeamSide.B)

        timeProvider.now += 10 * 60 * 1000L // 600_000 ms
        val result = matchRepo.finishActiveMatchManually()

        assertEquals(ManualFinishResult.DiscardedTooShort, result)
        assertNull(db.matchDao().getMatchByIdOnce(matchId))
        assertTrue(matchRepo.observeCompletedMatches().first().isEmpty())
        assertEquals(0, playerRepo.observeUserProgress().first()?.totalXp)
    }

    @Test
    fun `3 - manual finish at 10m 01s saves match and awards 15 XP`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val matchId = startSinglesMatch(user.id, opp.id)
        matchRepo.addPoint(TeamSide.A)

        timeProvider.now += (10 * 60 + 1) * 1000L // 601_000 ms
        val result = matchRepo.finishActiveMatchManually()

        assertTrue(result is ManualFinishResult.Saved)
        val saved = result as ManualFinishResult.Saved
        assertEquals(matchId, saved.matchId)
        assertEquals(15, saved.xpAwarded)
        assertFalse(saved.alreadyCompleted)
        assertEquals(15, playerRepo.observeUserProgress().first()?.totalXp)

        val summary = matchRepo.observeMatchById(matchId).first()!!
        assertEquals(MatchStatus.COMPLETED, summary.status)
        assertNull(summary.winnerTeam)
        assertTrue(summary.isManuallyCompleted)
        assertEquals(601_000L, summary.durationMs)
    }

    @Test
    fun `4 - 60m elapsed with pauses counts only active play duration`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)

        // Case A: 5 min active + 55 min paused = 60 min wall clock -> discarded!
        val shortMatchId = startSinglesMatch(user.id, opp.id)
        timeProvider.now += 5 * 60 * 1000L
        assertTrue(matchRepo.pauseActiveMatch())
        timeProvider.now += 55 * 60 * 1000L
        val resShort = matchRepo.finishActiveMatchManually()
        assertEquals(ManualFinishResult.DiscardedTooShort, resShort)
        assertNull(db.matchDao().getMatchByIdOnce(shortMatchId))
        assertEquals(0, playerRepo.observeUserProgress().first()?.totalXp)

        // Case B: 15 min active + 45 min paused = 60 min wall clock -> saved with durationMs = 15 min!
        val longMatchId = startSinglesMatch(user.id, opp.id)
        timeProvider.now += 8 * 60 * 1000L
        assertTrue(matchRepo.pauseActiveMatch())
        timeProvider.now += 45 * 60 * 1000L
        assertTrue(matchRepo.resumeActiveMatch())
        timeProvider.now += 7 * 60 * 1000L
        val resLong = matchRepo.finishActiveMatchManually()
        assertTrue(resLong is ManualFinishResult.Saved)
        assertEquals(15, (resLong as ManualFinishResult.Saved).xpAwarded)

        val savedSummary = matchRepo.observeMatchById(longMatchId).first()!!
        assertEquals(15 * 60 * 1000L, savedSummary.durationMs)
    }

    @Test
    fun `5 - two played sets at 1-1 save as draw without winner`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val matchId = startSinglesMatch(user.id, opp.id)

        winSet(TeamSide.A) // 6-0
        winSet(TeamSide.B) // 0-6 -> 1:1 in sets
        timeProvider.now += 15 * 60 * 1000L

        val result = matchRepo.finishActiveMatchManually()
        assertTrue(result is ManualFinishResult.Saved)

        val summary = matchRepo.observeMatchById(matchId).first()!!
        assertNull(summary.winnerTeam)
        assertNull(summary.userWon)
        assertTrue(summary.isManuallyCompleted)
        assertEquals(2, summary.sets.size)
        assertEquals(6, summary.sets[0].gamesA)
        assertEquals(0, summary.sets[0].gamesB)
        assertEquals(0, summary.sets[1].gamesA)
        assertEquals(6, summary.sets[1].gamesB)
        assertNull(summary.incompleteSet)
    }

    @Test
    fun `6 - incomplete current set preserves intermediate score separately from completed sets`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val matchId = startSinglesMatch(user.id, opp.id)

        winSet(TeamSide.A) // Set 1: 6-0 (completed)
        // Set 2: 2-1 in games, 30-15 in current game
        repeat(8) { matchRepo.addPoint(TeamSide.A) } // 2 games A
        repeat(4) { matchRepo.addPoint(TeamSide.B) } // 1 game B
        repeat(2) { matchRepo.addPoint(TeamSide.A) } // 30
        matchRepo.addPoint(TeamSide.B) // 15

        timeProvider.now += 20 * 60 * 1000L
        val result = matchRepo.finishActiveMatchManually()
        assertTrue(result is ManualFinishResult.Saved)

        // Only Set 1 is in set_results / summary.sets
        val dbSetResults = db.matchDao().getSetResultsForMatch(matchId)
        assertEquals(1, dbSetResults.size)

        val summary = matchRepo.observeMatchById(matchId).first()!!
        assertEquals(1, summary.sets.size)
        val inc = summary.incompleteSet
        assertNotNull(inc)
        assertEquals(2, inc!!.setIndex)
        assertEquals(2, inc.gamesA)
        assertEquals(1, inc.gamesB)
        assertEquals("30", inc.scoreA)
        assertEquals("15", inc.scoreB)

        val details = matchRepo.observeMatchDetails(matchId).first()!!
        assertEquals(1, details.summary.sets.size)
        assertEquals(2, details.summary.incompleteSet?.gamesA)
        assertEquals(39, details.stats!!.totalPoints) // 24 + 8 + 4 + 3 = 39 points
    }

    @Test
    fun `7 - manually completed match appears in history and responds to Draws filter`() = runBlocking {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val manualId = startSinglesMatch(user.id, opp.id)
        matchRepo.addPoint(TeamSide.A)
        timeProvider.now += 12 * 60 * 1000L
        matchRepo.finishActiveMatchManually()

        val history = matchRepo.observeCompletedMatches().first()
        assertEquals(1, history.size)
        assertEquals(manualId, history.first().id)
        assertTrue(history.first().isManuallyCompleted)

        val historyVm = HistoryViewModel(matchRepo, timeProvider)
        val collector = launch { historyVm.uiState.collect { } }
        try {
            awaitCondition { historyVm.uiState.value.matches.size == 1 }
            historyVm.toggleResultFilter(HistoryResultFilter.DRAWS)
            awaitCondition {
                historyVm.uiState.value.resultFilter == HistoryResultFilter.DRAWS &&
                    historyVm.uiState.value.filteredCount == 1
            }

            historyVm.toggleResultFilter(HistoryResultFilter.WINS)
            awaitCondition {
                historyVm.uiState.value.resultFilter == HistoryResultFilter.WINS &&
                    historyVm.uiState.value.filteredCount == 0
            }
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun `8 & 9 - draw counts in totals, monthly activity, format split, excludes winRate denominator, and interrupts streaks`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)

        // Match 1: Auto win
        startSinglesMatch(user.id, opp.id)
        repeat(48) {
            timeProvider.now += 1_000L
            matchRepo.addPoint(TeamSide.A)
        }
        timeProvider.now += 10_000L

        // Match 2: Manual draw
        startSinglesMatch(user.id, opp.id)
        matchRepo.addPoint(TeamSide.A)
        timeProvider.now += 11 * 60 * 1000L
        matchRepo.finishActiveMatchManually()

        val stats = statsRepo.observePerformanceSummary().first()
        assertEquals(2, stats.totalMatches)
        assertEquals(1, stats.wins)
        assertEquals(0, stats.losses)
        assertEquals(1, stats.draws)
        assertEquals(100, stats.winRatePercent) // 1 win / 1 decided match (draw excluded!)
        assertEquals(2, stats.singlesMatches)
        assertEquals(100, stats.singlesWinRatePercent)
        assertEquals(2, stats.monthlyActivity.last().count)
        assertNull(stats.currentStreakLabel)
        assertEquals(0, stats.currentStreakCount)
        assertNull(stats.isCurrentStreakWin)
        assertEquals(1, stats.bestWinStreak)
        assertEquals(listOf(MatchFormOutcome.DRAW, MatchFormOutcome.WIN), stats.recentForm)
    }

    @Test
    fun `10 - repeated manual completion and reopening match results awards no duplicate XP`() = runBlocking {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val matchId = startSinglesMatch(user.id, opp.id)
        timeProvider.now += 15 * 60 * 1000L

        val first = matchRepo.finishActiveMatchManually()
        assertTrue(first is ManualFinishResult.Saved)
        assertEquals(15, (first as ManualFinishResult.Saved).xpAwarded)
        assertEquals(15, playerRepo.observeUserProgress().first()?.totalXp)

        // Second manual finish call: no active match, XP stays 15
        val second = matchRepo.finishActiveMatchManually()
        assertEquals(ManualFinishResult.NoActiveMatch, second)
        assertEquals(15, playerRepo.observeUserProgress().first()?.totalXp)

        // Calling completeMatchIfFinished on manually completed match awards 0 XP
        val dup = matchRepo.completeMatchIfFinished(matchId)
        assertNotNull(dup)
        assertTrue(dup!!.alreadyCompleted)
        assertEquals(0, dup.xpAwarded)
        assertEquals(15, playerRepo.observeUserProgress().first()?.totalXp)

        // Opening MatchResultsViewModel reports 15 XP and doesn't mutate user_progress
        val resultsVm = MatchResultsViewModel(matchRepo, timeProvider)
        resultsVm.loadMatch(matchId)
        val job = launch { resultsVm.uiState.collect { } }
        try {
            awaitCondition { !resultsVm.uiState.value.isLoading && resultsVm.uiState.value.match != null }
            assertEquals(15, resultsVm.uiState.value.xpEarned)
            assertEquals(15, playerRepo.observeUserProgress().first()?.totalXp)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `11 - active match and CONTINUE banner disappear after manual finish`() = runBlocking {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val matchId = startSinglesMatch(user.id, opp.id)

        assertNotNull(matchRepo.observeActiveMatch().first())
        assertNotNull(matchRepo.observeActiveMatchLive().first())

        timeProvider.now += 15 * 60 * 1000L
        val vm = LiveMatchViewModel(matchRepo, settings, timeProvider)
        val events = mutableListOf<LiveMatchNavEvent>()
        val job = launch { vm.navEvents.collect { events.add(it) } }
        try {
            vm.finishMatchManually()
            awaitCondition { events.isNotEmpty() }
            assertEquals(listOf(LiveMatchNavEvent.Finished(matchId)), events)
            assertNull(matchRepo.observeActiveMatch().first())
            assertNull(matchRepo.observeActiveMatchLive().first())
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `12 - automatic completion still assigns winner and awards 30 plus 20 XP`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val matchId = startSinglesMatch(user.id, opp.id)

        var completion = null as com.padelpro.app.domain.model.MatchCompletionResult?
        repeat(48) { idx ->
            timeProvider.now += 5_000L
            val res = matchRepo.addPoint(TeamSide.A)
            if (idx == 47) completion = res
        }

        val completed = completion
        assertNotNull(completed)
        assertEquals(TeamSide.A, completed!!.winnerTeam)
        assertEquals(50, completed.xpAwarded)
        assertEquals(50, playerRepo.observeUserProgress().first()?.totalXp)

        val summary = matchRepo.observeMatchById(matchId).first()!!
        assertFalse(summary.isManuallyCompleted)
        assertEquals(TeamSide.A, summary.winnerTeam)
        assertEquals(true, summary.userWon)
    }

    @Test
    fun `upgrade safety - pre-existing ACTIVE match survives DB reopen and finishes manually with backup roundtrip`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)

        // Pre-seed an ACTIVE match directly as it exists prior to upgrade
        val legacyMatchId = "legacy_active_match"
        val startedAt = timeProvider.now - 15 * 60 * 1000L
        db.matchDao().insertMatch(
            MatchEntity(
                id = legacyMatchId,
                format = MatchFormat.SINGLES.name,
                status = MatchStatus.ACTIVE.name,
                deuceMode = DeuceMode.GOLDEN_POINT.name,
                bestOf = 3,
                firstServerTeam = TeamSide.A.name,
                startedAt = startedAt,
                endedAt = null,
                durationMs = null,
                winnerTeam = null
            )
        )
        db.matchDao().insertParticipants(
            listOf(
                MatchParticipantEntity(legacyMatchId, user.id, TeamSide.A.name, 0, 1),
                MatchParticipantEntity(legacyMatchId, opp.id, TeamSide.B.name, 0, 0)
            )
        )
        db.matchDao().insertEvent(
            MatchEventEntity(
                matchId = legacyMatchId,
                seq = 1,
                type = MatchRepositoryImpl.EVENT_POINT_WON,
                team = TeamSide.A.name,
                timestamp = startedAt + 60_000L
            )
        )

        // Reopen DB (simulates app restart/upgrade without destructive migration)
        db.close()
        db = openDb()
        val reopenedMatchRepo = MatchRepositoryImpl(db, timeProvider) { contentPack }
        val reopenedBackupRepo = BackupRepositoryImpl(db, settings, timeProvider) { contentPack }

        val active = reopenedMatchRepo.observeActiveMatch().first()
        assertNotNull(active)
        assertEquals(legacyMatchId, active!!.id)

        val finishRes = reopenedMatchRepo.finishActiveMatchManually()
        assertTrue(finishRes is ManualFinishResult.Saved)
        assertEquals(15, (finishRes as ManualFinishResult.Saved).xpAwarded)

        val json = reopenedBackupRepo.exportBackupJson()
        val outcome = reopenedBackupRepo.importBackupJson(json)
        assertTrue(outcome is BackupImportOutcome.Success)

        val restored = reopenedMatchRepo.observeMatchById(legacyMatchId).first()!!
        assertTrue(restored.isManuallyCompleted)
        assertNotNull(restored.incompleteSet)
        assertEquals("15", restored.incompleteSet!!.scoreA)
    }
}
