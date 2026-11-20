package com.padelpro.app

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.database.entity.MatchEventEntity
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.data.repository.MatchRepositoryImpl
import com.padelpro.app.data.repository.PlayerRepositoryImpl
import com.padelpro.app.domain.model.DeleteFriendResult
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchStatus
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

private class FakeTimeProvider(var currentNow: Long = 10_000L) : TimeProvider {
    override fun nowMillis(): Long = currentNow
    override fun currentHourOfDay(): Int = 12
    override fun currentLocalDate(): LocalDate = LocalDate.of(2026, 11, 17)
    override fun formatMonthYear(epochMillis: Long): String = "нояб 2026"
    override fun formatShortDate(epochMillis: Long): String = "17 нояб 2026"
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatchPersistenceRobolectricTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var db: PadelProDatabase
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var settings: SettingsRepository
    private lateinit var playerRepo: PlayerRepositoryImpl
    private lateinit var matchRepo: MatchRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.cacheDir, "test_padel_match_${System.nanoTime()}.db")
        db = openDatabase()
        timeProvider = FakeTimeProvider(10_000L)
        settings = SettingsRepository(context)
        playerRepo = PlayerRepositoryImpl(db, settings, timeProvider)
        matchRepo = MatchRepositoryImpl(db, timeProvider)
    }

    @After
    fun teardown() {
        if (db.isOpen) {
            db.close()
        }
        dbFile.delete()
    }

    private fun openDatabase(): PadelProDatabase =
        Room.databaseBuilder(context, PadelProDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()

    @Test
    fun `kill and reopen DB restores exact match state, elapsed timer, and restored banner`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 2)

        val startRes = matchRepo.startMatch(
            StartMatchRequest(
                format = MatchFormat.SINGLES,
                deuceMode = DeuceMode.GOLDEN_POINT,
                firstServerTeam = TeamSide.A,
                participants = listOf(
                    StartMatchParticipant(user.id, TeamSide.A, 0, isUser = true),
                    StartMatchParticipant(opp.id, TeamSide.B, 0, isUser = false)
                )
            )
        )
        assertTrue(startRes is StartMatchResult.Started)

        timeProvider.currentNow = 15_000L // +5s
        matchRepo.addPoint(TeamSide.A) // 15-0
        timeProvider.currentNow = 20_000L // +10s played total
        assertTrue(matchRepo.pauseActiveMatch())

        // Verify non-restored during original session
        val beforeKill = matchRepo.observeActiveMatchLive().first()
        assertNotNull(beforeKill)
        assertFalse(beforeKill!!.wasRestored)
        assertTrue(beforeKill.state.isPaused)
        assertEquals("15", beforeKill.state.displayScoreA())

        // Simulate process kill: close DB and instantiate fresh DB + repository
        db.close()
        db = openDatabase()
        timeProvider.currentNow = 90_000L // 70s elapsed while killed & paused -> should stay 10s!
        val restoredRepo = MatchRepositoryImpl(db, timeProvider)
        val restored = restoredRepo.observeActiveMatchLive().first()

        assertNotNull(restored)
        assertTrue(restored!!.wasRestored)
        assertTrue(restored.state.isPaused)
        assertEquals("15", restored.state.displayScoreA())
        assertEquals("0", restored.state.displayScoreB())
        assertEquals(10_000L, restored.state.elapsedDurationMillis(timeProvider.currentNow))
        assertEquals("Алекс", restored.state.servingPlayerName)

        // Resume & advance clock
        assertTrue(restoredRepo.resumeActiveMatch())
        timeProvider.currentNow = 102_000L // +12s -> total 22s
        val resumed = restoredRepo.observeActiveMatchLive().first()!!
        assertFalse(resumed.state.isPaused)
        assertEquals(22_000L, resumed.state.elapsedDurationMillis(timeProvider.currentNow))
    }

    @Test
    fun `match completion is atomic and idempotent with XP awarded strictly once`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Олег", 1)

        val started = matchRepo.startMatch(
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

        // Play 2 sets (6-0, 6-0 = 48 points) for Team A (user's team)
        var completionResult = null as com.padelpro.app.domain.model.MatchCompletionResult?
        repeat(48) { idx ->
            timeProvider.currentNow += 1_000L
            val res = matchRepo.addPoint(TeamSide.A)
            if (idx < 47) assertNull(res) else completionResult = res
        }

        assertNotNull(completionResult)
        assertEquals(TeamSide.A, completionResult!!.winnerTeam)
        assertEquals(50, completionResult.xpAwarded) // 30 participation + 20 win
        assertFalse(completionResult.alreadyCompleted)

        // Calling completeMatchIfFinished a second time MUST NOT award XP again!
        val duplicate = matchRepo.completeMatchIfFinished(started.matchId)
        assertNotNull(duplicate)
        assertTrue(duplicate!!.alreadyCompleted)
        assertEquals(0, duplicate.xpAwarded)

        val progress = playerRepo.observeUserProgress().first()
        assertEquals(50, progress?.totalXp)

        val summary = matchRepo.observeMatchById(started.matchId).first()
        assertNotNull(summary)
        assertEquals(MatchStatus.COMPLETED, summary!!.status)
        assertEquals(listOf("Алекс"), summary.teamANames)
        assertEquals(listOf("Олег"), summary.teamBNames)
        assertEquals(2, summary.sets.size)
        assertEquals(6, summary.sets[0].gamesA)
    }

    @Test
    fun `single ACTIVE match invariant blocks second active match unless cancelExisting is true`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp1 = playerRepo.createFriend("Марк", 1)
        val opp2 = playerRepo.createFriend("Лев", 2)

        val first = matchRepo.startMatch(
            StartMatchRequest(
                format = MatchFormat.SINGLES,
                deuceMode = DeuceMode.ADVANTAGE,
                firstServerTeam = TeamSide.A,
                participants = listOf(
                    StartMatchParticipant(user.id, TeamSide.A, 0, isUser = true),
                    StartMatchParticipant(opp1.id, TeamSide.B, 0, isUser = false)
                )
            )
        ) as StartMatchResult.Started

        val conflict = matchRepo.startMatch(
            StartMatchRequest(
                format = MatchFormat.SINGLES,
                deuceMode = DeuceMode.STAR_POINT,
                firstServerTeam = TeamSide.B,
                participants = listOf(
                    StartMatchParticipant(user.id, TeamSide.A, 0, isUser = true),
                    StartMatchParticipant(opp2.id, TeamSide.B, 0, isUser = false)
                )
            ),
            cancelExisting = false
        )
        assertTrue(conflict is StartMatchResult.ActiveMatchExists)
        assertEquals(first.matchId, (conflict as StartMatchResult.ActiveMatchExists).activeMatchId)

        val replaced = matchRepo.startMatch(
            StartMatchRequest(
                format = MatchFormat.SINGLES,
                deuceMode = DeuceMode.STAR_POINT,
                firstServerTeam = TeamSide.B,
                participants = listOf(
                    StartMatchParticipant(user.id, TeamSide.A, 0, isUser = true),
                    StartMatchParticipant(opp2.id, TeamSide.B, 0, isUser = false)
                )
            ),
            cancelExisting = true
        )
        assertTrue(replaced is StartMatchResult.Started)
        assertNull(db.matchDao().getMatchByIdOnce(first.matchId))
        val active = matchRepo.observeActiveMatchLive().first()
        assertEquals((replaced as StartMatchResult.Started).matchId, active?.matchId)
    }

    @Test
    fun `unique index on match_events(match_id, seq) rejects duplicate sequence`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val started = matchRepo.startMatch(
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

        db.matchDao().insertEvent(
            MatchEventEntity(
                matchId = started.matchId,
                seq = 1,
                type = MatchRepositoryImpl.EVENT_POINT_WON,
                team = "A",
                timestamp = 11_000L
            )
        )

        var threwConstraint = false
        try {
            db.matchDao().insertEvent(
                MatchEventEntity(
                    matchId = started.matchId,
                    seq = 1,
                    type = MatchRepositoryImpl.EVENT_POINT_WON,
                    team = "B",
                    timestamp = 12_000L
                )
            )
        } catch (_: SQLiteConstraintException) {
            threwConstraint = true
        }
        assertTrue("Expected SQLiteConstraintException on duplicate (match_id, seq)", threwConstraint)
    }

    @Test
    fun `cancelActiveMatch deletes active match without history entry or XP`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        val started = matchRepo.startMatch(
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

        matchRepo.addPoint(TeamSide.A)
        assertTrue(matchRepo.cancelActiveMatch())

        assertNull(matchRepo.observeActiveMatchLive().first())
        assertNull(db.matchDao().getMatchByIdOnce(started.matchId))
        assertTrue(matchRepo.observeCompletedMatches().first().isEmpty())
        assertEquals(0, playerRepo.observeUserProgress().first()?.totalXp)
    }

    @Test
    fun `friend CRUD supports edit and delete when unused and blocks delete when in matches`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val unusedFriend = playerRepo.createFriend("Игорь", 3)
        playerRepo.updateFriend(unusedFriend.id, "Игорь Про", 7)

        val friendsAfterUpdate = playerRepo.observeFriends().first()
        assertEquals(1, friendsAfterUpdate.size)
        assertEquals("Игорь Про", friendsAfterUpdate[0].displayName)
        assertEquals(7, friendsAfterUpdate[0].avatarId)

        assertEquals(DeleteFriendResult.DELETED, playerRepo.deleteFriend(unusedFriend.id))
        assertTrue(playerRepo.observeFriends().first().isEmpty())

        val activeFriend = playerRepo.createFriend("Денис", 4)
        matchRepo.startMatch(
            StartMatchRequest(
                format = MatchFormat.SINGLES,
                deuceMode = DeuceMode.ADVANTAGE,
                firstServerTeam = TeamSide.A,
                participants = listOf(
                    StartMatchParticipant(user.id, TeamSide.A, 0, isUser = true),
                    StartMatchParticipant(activeFriend.id, TeamSide.B, 0, isUser = false)
                )
            )
        )

        assertEquals(DeleteFriendResult.IN_USE_BY_MATCHES, playerRepo.deleteFriend(activeFriend.id))
        assertEquals(1, playerRepo.observeFriends().first().size)
    }

    @Test
    fun `undo deletes last point across game boundary and updates replay`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
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
        )

        repeat(4) { matchRepo.addPoint(TeamSide.A) }
        val afterGame = matchRepo.observeActiveMatchLive().first()!!
        assertEquals(1, afterGame.state.gamesA)
        assertEquals("0", afterGame.state.displayScoreA())

        assertTrue(matchRepo.undoLastPoint())
        val afterUndo = matchRepo.observeActiveMatchLive().first()!!
        assertEquals(0, afterUndo.state.gamesA)
        assertEquals("40", afterUndo.state.displayScoreA())
    }
}
