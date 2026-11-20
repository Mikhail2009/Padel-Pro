package com.padelpro.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.data.content.AssetAcademyContentLoader
import com.padelpro.app.data.repository.AcademyRepositoryImpl
import com.padelpro.app.data.repository.MatchRepositoryImpl
import com.padelpro.app.data.repository.PlayerRepositoryImpl
import com.padelpro.app.domain.academy.ContentPack
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.RulebookCategory
import com.padelpro.app.domain.model.StartMatchParticipant
import com.padelpro.app.domain.model.StartMatchRequest
import com.padelpro.app.domain.model.StartMatchResult
import com.padelpro.app.domain.model.TeamSide
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

private class MutableAcademyTimeProvider(
    var now: Long = 100_000L,
    var date: LocalDate = LocalDate.of(2026, 11, 17)
) : TimeProvider {
    override fun nowMillis(): Long = now
    override fun currentHourOfDay(): Int = 14
    override fun currentLocalDate(): LocalDate = date
    override fun formatMonthYear(epochMillis: Long): String = "нояб 2026"
    override fun formatShortDate(epochMillis: Long): String = "17 нояб 2026"
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AcademyPersistenceRobolectricTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var db: PadelProDatabase
    private lateinit var timeProvider: MutableAcademyTimeProvider
    private lateinit var settings: SettingsRepository
    private lateinit var contentPack: ContentPack
    private lateinit var playerRepo: PlayerRepositoryImpl
    private lateinit var academyRepo: AcademyRepositoryImpl
    private lateinit var matchRepo: MatchRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.cacheDir, "test_academy_${System.nanoTime()}.db")
        db = openDatabase()
        timeProvider = MutableAcademyTimeProvider()
        settings = SettingsRepository(context)
        contentPack = AssetAcademyContentLoader(context).load()
        playerRepo = PlayerRepositoryImpl(db, settings, timeProvider)
        academyRepo = AcademyRepositoryImpl(db, contentPack, timeProvider)
        matchRepo = MatchRepositoryImpl(db, timeProvider) { contentPack }
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
    fun `completing lesson grants XP once, preserves best score on retake, and persists across reopen`() = runTest {
        playerRepo.createLocalProfile("Михаил", 0, MatchFormat.DOUBLES)

        val lesson1 = contentPack.findLesson("course_fundamentals_l1")!!
        val allCorrectAnswers = lesson1.quiz.associate { it.id to it.correctIndex }

        val firstOutcome = academyRepo.completeLesson(lesson1.id, allCorrectAnswers)
        assertEquals(20, firstOutcome.xpLesson)
        assertEquals(lesson1.quiz.size * 5, firstOutcome.xpQuiz)
        assertEquals(0, firstOutcome.xpCourseBonus)
        assertEquals(100, firstOutcome.scorePercent)
        assertFalse(firstOutcome.isReviewRepeat)
        assertTrue(firstOutcome.newlyUnlockedAchievements.any { it.id == "FIRST_LESSON" })
        assertTrue(firstOutcome.newlyUnlockedAchievements.any { it.id == "PERFECT_QUIZ" })

        val xpAfterFirst = firstOutcome.totalXpGained
        val progressAfterFirst = playerRepo.observeUserProgress().first()!!
        assertEquals(xpAfterFirst, progressAfterFirst.totalXp)
        assertEquals(1, progressAfterFirst.streakCount)

        // Retake with wrong answers: 0 XP gained, bestQuizScore kept at 1.0, no achievements re-unlocked
        val wrongAnswers = lesson1.quiz.associate { it.id to ((it.correctIndex + 1) % 4) }
        val retakeOutcome = academyRepo.completeLesson(lesson1.id, wrongAnswers)
        assertEquals(0, retakeOutcome.totalXpGained)
        assertTrue(retakeOutcome.isReviewRepeat)
        assertTrue(retakeOutcome.newlyUnlockedAchievements.isEmpty())

        // Reopen database from disk
        db.close()
        db = openDatabase()
        val reopenedAcademy = AcademyRepositoryImpl(db, contentPack, timeProvider)
        val reopenedPlayer = PlayerRepositoryImpl(db, settings, timeProvider)

        val reopenedProgress = reopenedPlayer.observeUserProgress().first()!!
        assertEquals(xpAfterFirst, reopenedProgress.totalXp)

        val courseDetail = reopenedAcademy.observeCourseDetail("course_fundamentals").first()!!
        assertEquals(1f, courseDetail.lessons[0].bestQuizScore ?: 0f, 0.001f)
        assertTrue(courseDetail.lessons[1].isUnlocked)
        assertEquals("course_fundamentals_l2", courseDetail.nextActionLessonId)
    }

    @Test
    fun `completing full course grants 50 XP bonus once and unlocks next course and FIRST_COURSE`() = runTest {
        playerRepo.createLocalProfile("Михаил", 1, MatchFormat.DOUBLES)
        val course1 = contentPack.findCourse("course_fundamentals")!!

        course1.lessons.dropLast(1).forEach { lesson ->
            val answers = lesson.quiz.associate { it.id to it.correctIndex }
            val outcome = academyRepo.completeLesson(lesson.id, answers)
            assertEquals(0, outcome.xpCourseBonus)
        }

        val lastLesson = course1.lessons.last()
        val lastAnswers = lastLesson.quiz.associate { it.id to it.correctIndex }
        val finalOutcome = academyRepo.completeLesson(lastLesson.id, lastAnswers)
        assertEquals(50, finalOutcome.xpCourseBonus)
        assertTrue(finalOutcome.newlyUnlockedAchievements.any { it.id == "FIRST_COURSE" })
        assertEquals("course_court_equipment_l1", finalOutcome.nextLessonId)

        // Repeat last lesson -> bonus is NOT awarded again
        val repeatOutcome = academyRepo.completeLesson(lastLesson.id, lastAnswers)
        assertEquals(0, repeatOutcome.xpCourseBonus)
        assertEquals(0, repeatOutcome.totalXpGained)

        val courses = academyRepo.observeCourses().first()
        assertTrue(courses[0].isCompleted)
        assertTrue(courses[1].isUnlocked)
    }

    @Test
    fun `3 day consecutive streak unlocks STREAK_3 and rule favorites persist`() = runTest {
        playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)

        val l1 = contentPack.findLesson("course_fundamentals_l1")!!
        val l2 = contentPack.findLesson("course_fundamentals_l2")!!
        val l3 = contentPack.findLesson("course_fundamentals_l3")!!

        academyRepo.completeLesson(l1.id, l1.quiz.associate { it.id to it.correctIndex })
        timeProvider.date = timeProvider.date.plusDays(1)
        academyRepo.completeLesson(l2.id, l2.quiz.associate { it.id to it.correctIndex })
        timeProvider.date = timeProvider.date.plusDays(1)
        val day3Outcome = academyRepo.completeLesson(l3.id, l3.quiz.associate { it.id to it.correctIndex })

        assertTrue(day3Outcome.newlyUnlockedAchievements.any { it.id == "STREAK_3" })
        assertEquals(3, playerRepo.observeUserProgress().first()!!.streakCount)

        // Rule favorites toggle & persistence
        val targetRule = contentPack.rules.first()
        val added = academyRepo.toggleRuleFavorite(targetRule.id)
        assertTrue(added)

        db.close()
        db = openDatabase()
        val reopenedAcademy = AcademyRepositoryImpl(db, contentPack, timeProvider)
        val favorites = reopenedAcademy.observeRulebook(category = RulebookCategory.FAVORITES).first()
        assertEquals(listOf(targetRule.id), favorites.map { it.id })

        val removed = reopenedAcademy.toggleRuleFavorite(targetRule.id)
        assertFalse(removed)
        assertTrue(reopenedAcademy.observeRulebook(category = RulebookCategory.FAVORITES).first().isEmpty())
    }

    @Test
    fun `match win unlocks match achievements and resetAllData clears academy tables`() = runTest {
        val user = playerRepo.createLocalProfile("Михаил", 0, MatchFormat.SINGLES)
        val opponent = playerRepo.createFriend("Соперник", 2)

        val started = matchRepo.startMatch(
            StartMatchRequest(
                format = MatchFormat.SINGLES,
                deuceMode = DeuceMode.GOLDEN_POINT,
                bestOf = 3,
                firstServerTeam = TeamSide.A,
                participants = listOf(
                    StartMatchParticipant(user.id, TeamSide.A, 0, isUser = true),
                    StartMatchParticipant(opponent.id, TeamSide.B, 0, isUser = false)
                )
            )
        )
        assertTrue(started is StartMatchResult.Started)

        // Win 2 sets cleanly (48 points)
        repeat(48) { idx ->
            timeProvider.now += 1_000L
            matchRepo.addPoint(TeamSide.A)
        }

        val unlockedIds = academyRepo.observeAchievements().first()
            .filter { it.isUnlocked }
            .map { it.id }
            .toSet()
        assertTrue("FIRST_MATCH" in unlockedIds)
        assertTrue("FIRST_WIN" in unlockedIds)
        assertTrue("GOLDEN_WIN" in unlockedIds)

        // Also add favorite and verify full wipe on resetAllData
        academyRepo.toggleRuleFavorite(contentPack.rules.first().id)
        playerRepo.resetAllData()

        assertEquals(0, academyRepo.observeCompletedLessonsCount().first())
        assertTrue(academyRepo.observeRulebook(category = RulebookCategory.FAVORITES).first().isEmpty())
        assertTrue(academyRepo.observeAchievements().first().none { it.isUnlocked })
    }
}
