package com.padelpro.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.datastore.AppSettings
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.data.backup.BackupRepositoryImpl
import com.padelpro.app.data.content.AssetAcademyContentLoader
import com.padelpro.app.data.repository.AcademyRepositoryImpl
import com.padelpro.app.data.repository.MatchRepositoryImpl
import com.padelpro.app.data.repository.PlayerRepositoryImpl
import com.padelpro.app.data.repository.StatsRepositoryImpl
import com.padelpro.app.domain.academy.ContentPack
import com.padelpro.app.domain.model.BackupImportOutcome
import com.padelpro.app.domain.model.BackupValidationError
import com.padelpro.app.domain.model.BackupValidationResult
import com.padelpro.app.domain.model.DeleteFriendResult
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.StartMatchParticipant
import com.padelpro.app.domain.model.StartMatchRequest
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
class Iteration4PersistenceRobolectricTest {

    private class TestTimeProvider(
        var now: Long = LocalDate.of(2026, 11, 20).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var settings: SettingsRepository
    private lateinit var contentPack: ContentPack
    private lateinit var playerRepo: PlayerRepositoryImpl
    private lateinit var academyRepo: AcademyRepositoryImpl
    private lateinit var matchRepo: MatchRepositoryImpl
    private lateinit var statsRepo: StatsRepositoryImpl
    private lateinit var backupRepo: BackupRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.cacheDir, "test_padel_iter4_${System.nanoTime()}.db")
        db = Room.databaseBuilder(context, PadelProDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        timeProvider = TestTimeProvider()
        settings = SettingsRepository(context)
        contentPack = AssetAcademyContentLoader(context).load()
        playerRepo = PlayerRepositoryImpl(db, settings, timeProvider)
        academyRepo = AcademyRepositoryImpl(db, contentPack, timeProvider)
        matchRepo = MatchRepositoryImpl(db, timeProvider) { contentPack }
        statsRepo = StatsRepositoryImpl(matchRepo, timeProvider)
        backupRepo = BackupRepositoryImpl(db, settings, timeProvider) { contentPack }
    }

    @After
    fun teardown() {
        if (db.isOpen) {
            db.close()
        }
        dbFile.delete()
    }

    private suspend fun playQuickSinglesMatch(
        userId: String,
        opponentId: String,
        winner: TeamSide,
        deuceMode: DeuceMode = DeuceMode.GOLDEN_POINT
    ): String {
        matchRepo.startMatch(
            StartMatchRequest(
                format = MatchFormat.SINGLES,
                deuceMode = deuceMode,
                firstServerTeam = TeamSide.A,
                participants = listOf(
                    StartMatchParticipant(userId, TeamSide.A, 0, isUser = true),
                    StartMatchParticipant(opponentId, TeamSide.B, 0, isUser = false)
                )
            )
        )
        // Best of 3 Golden Point: 2 sets * 6 games * 4 points = 48 points
        repeat(48) {
            timeProvider.now += 2_000L
            matchRepo.addPoint(winner)
        }
        val active = matchRepo.observeActiveMatch().first()
        assertNull("Match should be completed after 2 sets", active)
        return matchRepo.observeCompletedMatches().first().first().id
    }

    @Test
    fun `deleting a match cascades child rows, keeps other matches, preserves XP and achievements, and recomputes stats`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 1, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 2)

        val match1Id = playQuickSinglesMatch(user.id, opp.id, winner = TeamSide.A)
        timeProvider.now += 10_000L
        val match2Id = playQuickSinglesMatch(user.id, opp.id, winner = TeamSide.B)

        val xpBefore = playerRepo.observeUserProgress().first()!!.totalXp
        assertTrue("Expected XP from completed matches (>0), was $xpBefore", xpBefore >= 50)
        val unlockedBefore = db.academyDao().getUnlockedAchievementsOnce().map { it.achievementId }.toSet()
        assertTrue(unlockedBefore.contains("FIRST_MATCH"))
        assertTrue(unlockedBefore.contains("FIRST_WIN"))

        val details1 = matchRepo.observeMatchDetails(match1Id).first()
        assertNotNull(details1)
        val stats1 = details1!!.stats!!
        assertEquals(48, stats1.totalPoints)
        assertEquals(48, stats1.teamAPoints)
        assertEquals(0, stats1.teamBPoints)
        assertNotNull(stats1.longestSetIndex)

        assertEquals(2, statsRepo.observePerformanceSummary().first().totalMatches)
        assertEquals(50, statsRepo.observePerformanceSummary().first().winRatePercent)

        // Delete match1
        assertTrue(matchRepo.deleteMatch(match1Id))

        // match1 and all its children are gone
        assertNull(db.matchDao().getMatchByIdOnce(match1Id))
        assertTrue(db.matchDao().getEventsForMatch(match1Id).isEmpty())
        assertTrue(db.matchDao().getSetResultsForMatch(match1Id).isEmpty())
        assertTrue(db.matchDao().getParticipantsForMatch(match1Id).isEmpty())

        // match2 remains intact
        assertNotNull(db.matchDao().getMatchByIdOnce(match2Id))
        assertTrue(db.matchDao().getEventsForMatch(match2Id).isNotEmpty())
        assertTrue(db.matchDao().getSetResultsForMatch(match2Id).isNotEmpty())
        assertEquals(2, db.matchDao().getParticipantsForMatch(match2Id).size)

        // XP & achievements are NOT revoked (ADR-011)
        assertEquals(xpBefore, playerRepo.observeUserProgress().first()!!.totalXp)
        assertEquals(
            unlockedBefore,
            db.academyDao().getUnlockedAchievementsOnce().map { it.achievementId }.toSet()
        )

        // Stats recompute immediately to reflect remaining match2 (loss)
        val afterStats = statsRepo.observePerformanceSummary().first()
        assertEquals(1, afterStats.totalMatches)
        assertEquals(0, afterStats.wins)
        assertEquals(1, afterStats.losses)
        assertEquals(0, afterStats.winRatePercent)
        assertEquals("П1", afterStats.currentStreakLabel)
    }

    @Test
    fun `backup export and import roundtrips all tables and DataStore settings`() = runTest {
        val user = playerRepo.createLocalProfile("Елена", 5, MatchFormat.DOUBLES)
        val friend = playerRepo.createFriend("София", 7)
        val matchId = playQuickSinglesMatch(user.id, friend.id, winner = TeamSide.A)

        // Academy progress + favorite rule
        val firstLesson = contentPack.courses.first().lessons.first()
        academyRepo.completeLesson(
            lessonId = firstLesson.id,
            selectedAnswers = firstLesson.quiz.associate { q -> q.id to q.correctIndex }
        )
        val firstRule = contentPack.rules.first()
        academyRepo.toggleRuleFavorite(firstRule.id)

        // Custom DataStore settings
        settings.setHapticsEnabled(false)
        settings.setKeepScreenOn(false)
        settings.setDefaultDeuceMode(DeuceMode.STAR_POINT)
        settings.setBackupLastExportedAt(123_456_789L)

        val expectedXp = playerRepo.observeUserProgress().first()!!.totalXp
        val expectedAchievements = db.academyDao().getUnlockedAchievementsOnce().map { it.achievementId }.toSet()

        val json = backupRepo.exportBackupJson()
        val validation = backupRepo.validateBackupJson(json)
        assertTrue(validation is BackupValidationResult.Valid)
        val preview = (validation as BackupValidationResult.Valid).preview
        assertEquals(1, preview.matchesCount)
        assertEquals(2, preview.playersCount)
        assertEquals(1, preview.lessonsCompletedCount)
        assertTrue(preview.hasUserProfile)

        // Wipe everything completely
        playerRepo.resetAllData()
        assertNull(playerRepo.observeUserProfile().first())
        assertTrue(matchRepo.observeCompletedMatches().first().isEmpty())
        assertEquals(0, statsRepo.observePerformanceSummary().first().totalMatches)

        // Restore from backup JSON
        val outcome = backupRepo.importBackupJson(json)
        assertTrue(outcome is BackupImportOutcome.Success)
        val success = outcome as BackupImportOutcome.Success
        assertTrue(success.hasUserProfile)
        assertEquals(1, success.matchesCount)
        assertEquals(2, success.playersCount)

        // Verify full restoration across DB & DataStore
        val restoredUser = playerRepo.observeUserProfile().first()
        assertNotNull(restoredUser)
        assertEquals("Елена", restoredUser!!.displayName)
        assertEquals(5, restoredUser.avatarId)
        assertEquals(MatchFormat.DOUBLES, restoredUser.preferredFormat)

        val restoredFriends = playerRepo.observeFriends().first()
        assertEquals(listOf("София"), restoredFriends.map { it.displayName })

        assertEquals(expectedXp, playerRepo.observeUserProgress().first()!!.totalXp)
        assertEquals(
            expectedAchievements,
            db.academyDao().getUnlockedAchievementsOnce().map { it.achievementId }.toSet()
        )
        assertEquals(1, academyRepo.observeCompletedLessonsCount().first())
        assertEquals(setOf(firstRule.id), db.academyDao().getAllRuleFavoritesOnce().map { it.ruleId }.toSet())

        val restoredDetails = matchRepo.observeMatchDetails(matchId).first()
        assertNotNull(restoredDetails)
        assertEquals(48, restoredDetails!!.stats!!.totalPoints)

        val restoredStats = statsRepo.observePerformanceSummary().first()
        assertEquals(1, restoredStats.totalMatches)
        assertEquals(100, restoredStats.winRatePercent)

        val restoredSettings: AppSettings = settings.settings.first()
        assertFalse(restoredSettings.hapticsEnabled)
        assertFalse(restoredSettings.keepScreenOn)
        assertEquals(DeuceMode.STAR_POINT, restoredSettings.defaultDeuceMode)
        assertEquals(123_456_789L, restoredSettings.backupLastExportedAt)
        assertTrue(restoredSettings.onboardingCompleted)
    }

    @Test
    fun `invalid backup import leaves existing DB and settings untouched`() = runTest {
        playerRepo.createLocalProfile("Антон", 3, MatchFormat.SINGLES)
        settings.setDefaultDeuceMode(DeuceMode.ADVANTAGE)

        val validJson = backupRepo.exportBackupJson()

        val corruptResult = backupRepo.importBackupJson("{ not valid json")
        assertTrue(corruptResult is BackupImportOutcome.Error)
        assertEquals(BackupValidationError.INVALID_JSON, (corruptResult as BackupImportOutcome.Error).error)

        val badVersionJson = validJson.replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")
        val badVersionResult = backupRepo.importBackupJson(badVersionJson)
        assertTrue(badVersionResult is BackupImportOutcome.Error)
        assertEquals(
            BackupValidationError.UNSUPPORTED_SCHEMA_VERSION,
            (badVersionResult as BackupImportOutcome.Error).error
        )

        val badContentJson = validJson.replace(
            "\"lesson_progress\": []",
            "\"lesson_progress\": [{\"lessonId\": \"unknown_lesson_999\", \"status\": \"COMPLETED\", \"xpAwarded\": 20}]"
        )
        val badContentResult = backupRepo.importBackupJson(badContentJson)
        assertTrue(badContentResult is BackupImportOutcome.Error)
        assertEquals(
            BackupValidationError.UNKNOWN_CONTENT_REFERENCE,
            (badContentResult as BackupImportOutcome.Error).error
        )

        // Existing DB & settings remain untouched
        assertEquals("Антон", playerRepo.observeUserProfile().first()!!.displayName)
        assertEquals(DeuceMode.ADVANTAGE, settings.settings.first().defaultDeuceMode)
    }

    @Test
    fun `friend delete is blocked when friend has match history and succeeds otherwise`() = runTest {
        val user = playerRepo.createLocalProfile("Максим", 0, MatchFormat.SINGLES)
        val usedFriend = playerRepo.createFriend("Денис", 4)
        val unusedFriend = playerRepo.createFriend("Олег", 6)

        playQuickSinglesMatch(user.id, usedFriend.id, winner = TeamSide.A)

        // Blocked for friend in matches
        assertEquals(DeleteFriendResult.IN_USE_BY_MATCHES, playerRepo.deleteFriend(usedFriend.id))
        assertTrue(playerRepo.observeFriends().first().any { it.id == usedFriend.id })

        // Blocked for user profile
        assertEquals(DeleteFriendResult.NOT_FOUND, playerRepo.deleteFriend(user.id))
        assertNotNull(playerRepo.observeUserProfile().first())

        // Allowed for unused friend
        assertEquals(DeleteFriendResult.DELETED, playerRepo.deleteFriend(unusedFriend.id))
        assertFalse(playerRepo.observeFriends().first().any { it.id == unusedFriend.id })
    }

    @Test
    fun `match details hides stats when completed match has no POINT_WON events`() = runTest {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)

        db.matchDao().insertMatch(
            com.padelpro.app.core.database.entity.MatchEntity(
                id = "no_events_match",
                format = MatchFormat.SINGLES.name,
                status = com.padelpro.app.domain.model.MatchStatus.COMPLETED.name,
                deuceMode = DeuceMode.ADVANTAGE.name,
                bestOf = 3,
                firstServerTeam = TeamSide.A.name,
                startedAt = 1_000L,
                endedAt = 61_000L,
                durationMs = 60_000L,
                winnerTeam = TeamSide.A.name
            )
        )
        db.matchDao().insertParticipants(
            listOf(
                com.padelpro.app.core.database.entity.MatchParticipantEntity("no_events_match", user.id, TeamSide.A.name, 0, 1),
                com.padelpro.app.core.database.entity.MatchParticipantEntity("no_events_match", opp.id, TeamSide.B.name, 0, 0)
            )
        )
        db.matchDao().insertSetResults(
            listOf(
                com.padelpro.app.core.database.entity.SetResultEntity("no_events_match", 1, 6, 4, null, null),
                com.padelpro.app.core.database.entity.SetResultEntity("no_events_match", 2, 7, 6, 7, 4)
            )
        )

        val details = matchRepo.observeMatchDetails("no_events_match").first()
        assertNotNull(details)
        assertNull("Stats must be null when no POINT_WON events exist", details!!.stats)
        assertEquals(2, details.summary.sets.size)
        assertEquals(7, details.summary.sets[1].tbA)
        assertEquals(4, details.summary.sets[1].tbB)
    }
}
