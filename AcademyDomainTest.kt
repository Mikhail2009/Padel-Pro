package com.padelpro.app

import com.padelpro.app.core.common.LevelUtils
import com.padelpro.app.domain.academy.AcademyContentParser
import com.padelpro.app.domain.academy.AchievementConditionType
import com.padelpro.app.domain.academy.AchievementDefinition
import com.padelpro.app.domain.academy.AchievementEngine
import com.padelpro.app.domain.academy.AchievementEvaluationState
import com.padelpro.app.domain.academy.QuizQuestion
import com.padelpro.app.domain.academy.QuizValidator
import com.padelpro.app.domain.academy.StreakCalculator
import com.padelpro.app.domain.academy.UnlockCalculator
import com.padelpro.app.domain.academy.XpCalculator
import com.padelpro.app.domain.model.RulebookCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class AcademyDomainTest {

    @Test
    fun `xp calculator awards lesson quiz and course bonus strictly once`() {
        val firstCompletion = XpCalculator.calculateLessonCompletionXp(
            lessonXpAlreadyAwarded = false,
            isFirstQuizAttempt = true,
            correctAnswers = 3,
            totalQuestions = 3,
            courseJustCompleted = false
        )
        assertEquals(20, firstCompletion.lessonXp)
        assertEquals(15, firstCompletion.quizXp)
        assertEquals(0, firstCompletion.courseBonusXp)
        assertEquals(35, firstCompletion.totalXp)

        val courseFinisher = XpCalculator.calculateLessonCompletionXp(
            lessonXpAlreadyAwarded = false,
            isFirstQuizAttempt = true,
            correctAnswers = 2,
            totalQuestions = 3,
            courseJustCompleted = true
        )
        assertEquals(20, courseFinisher.lessonXp)
        assertEquals(10, courseFinisher.quizXp)
        assertEquals(50, courseFinisher.courseBonusXp)
        assertEquals(80, courseFinisher.totalXp)

        val retake = XpCalculator.calculateLessonCompletionXp(
            lessonXpAlreadyAwarded = true,
            isFirstQuizAttempt = false,
            correctAnswers = 3,
            totalQuestions = 3,
            courseJustCompleted = false
        )
        assertEquals(0, retake.lessonXp)
        assertEquals(0, retake.quizXp)
        assertEquals(0, retake.courseBonusXp)
        assertEquals(0, retake.totalXp)
    }

    @Test
    fun `level utils respects quadratic threshold boundaries`() {
        assertEquals(1, LevelUtils.calculateLevel(0))
        assertEquals(1, LevelUtils.calculateLevel(99))
        assertEquals(2, LevelUtils.calculateLevel(100))
        assertEquals(2, LevelUtils.calculateLevel(249))
        assertEquals(3, LevelUtils.calculateLevel(250))
        assertEquals(5, LevelUtils.calculateLevel(700))
    }

    @Test
    fun `streak calculator handles first day same day consecutive day and gaps`() {
        val d1 = LocalDate.of(2026, 11, 17)
        val first = StreakCalculator.calculateNextStreak(0, null, d1)
        assertEquals(1, first.streakCount)
        assertEquals("2026-11-17", first.lastLearningDateIso)

        val sameDay = StreakCalculator.calculateNextStreak(1, first.lastLearningDateIso, d1)
        assertEquals(1, sameDay.streakCount)

        val nextDay = StreakCalculator.calculateNextStreak(1, sameDay.lastLearningDateIso, d1.plusDays(1))
        assertEquals(2, nextDay.streakCount)
        assertEquals("2026-11-18", nextDay.lastLearningDateIso)

        val afterGap = StreakCalculator.calculateNextStreak(2, nextDay.lastLearningDateIso, d1.plusDays(3))
        assertEquals(1, afterGap.streakCount)
        assertEquals("2026-11-20", afterGap.lastLearningDateIso)
    }

    @Test
    fun `quiz validator evaluates score ratio and perfect state`() {
        val questions = listOf(
            QuizQuestion("q1", "Q1", listOf("A", "B", "C", "D"), 1, "Exp 1"),
            QuizQuestion("q2", "Q2", listOf("A", "B", "C", "D"), 2, "Exp 2"),
            QuizQuestion("q3", "Q3", listOf("A", "B", "C", "D"), 0, "Exp 3")
        )
        val perfect = QuizValidator.evaluate(questions, mapOf("q1" to 1, "q2" to 2, "q3" to 0))
        assertEquals(3, perfect.correctCount)
        assertEquals(100, perfect.scorePercent)
        assertTrue(perfect.isPerfect)

        val partial = QuizValidator.evaluate(questions, mapOf("q1" to 1, "q2" to 0, "q3" to 0))
        assertEquals(2, partial.correctCount)
        assertEquals(67, partial.scorePercent)
        assertFalse(partial.isPerfect)
    }

    @Test
    fun `achievement engine unlocks unmet thresholds and ignores already unlocked ids`() {
        val defs = listOf(
            AchievementDefinition("FIRST_LESSON", "T1", "D1", "school", AchievementConditionType.COMPLETED_LESSONS, 1),
            AchievementDefinition("STREAK_3", "T2", "D2", "flame", AchievementConditionType.LEARNING_STREAK, 3),
            AchievementDefinition("LEVEL_5", "T3", "D3", "star", AchievementConditionType.LEVEL_REACHED, 5)
        )
        val state = AchievementEvaluationState(
            completedLessons = 2,
            learningStreak = 3,
            currentLevel = 4
        )
        val firstPass = AchievementEngine.evaluateNewlyUnlocked(defs, emptySet(), state)
        assertEquals(listOf("FIRST_LESSON", "STREAK_3"), firstPass.map { it.id })

        val secondPass = AchievementEngine.evaluateNewlyUnlocked(defs, setOf("FIRST_LESSON", "STREAK_3"), state)
        assertTrue(secondPass.isEmpty())
    }

    @Test
    fun `bundled json assets parse cleanly and obey unlock invariants`() {
        val assetsDir = resolveAssetsContentDir()
        val pack = AcademyContentParser.parse(
            versionJson = File(assetsDir, "content_version.json").readText(Charsets.UTF_8),
            coursesJson = File(assetsDir, "courses.json").readText(Charsets.UTF_8),
            rulebookJson = File(assetsDir, "rulebook.json").readText(Charsets.UTF_8),
            achievementsJson = File(assetsDir, "achievements.json").readText(Charsets.UTF_8)
        )

        assertEquals(1, pack.version)
        assertEquals(8, pack.courses.size)
        assertEquals(32, pack.totalLessons)
        assertEquals(48, pack.rules.size)
        assertEquals(14, pack.achievements.size)

        val expectedCategoryCounts = mapOf(
            RulebookCategory.COURT to 8,
            RulebookCategory.EQUIPMENT to 8,
            RulebookCategory.SCORING to 8,
            RulebookCategory.SERVING to 8,
            RulebookCategory.WALLS to 8,
            RulebookCategory.FOULS to 8
        )
        expectedCategoryCounts.forEach { (category, count) ->
            assertEquals(count, pack.rules.count { it.category == category })
        }

        // Initial unlock state: only Course 1 & Lesson 1 unlocked
        val initialCourses = UnlockCalculator.buildCourseStates(pack.courses, emptySet())
        assertTrue(initialCourses[0].isUnlocked)
        assertTrue(initialCourses.drop(1).none { it.isUnlocked })
        assertTrue(UnlockCalculator.isLessonUnlocked(pack.courses, "course_fundamentals_l1", emptySet()))
        assertFalse(UnlockCalculator.isLessonUnlocked(pack.courses, "course_fundamentals_l2", emptySet()))
        assertFalse(UnlockCalculator.isLessonUnlocked(pack.courses, "course_court_equipment_l1", emptySet()))
        assertEquals("course_fundamentals_l1", UnlockCalculator.nextUnlockedUncompletedLessonId(pack.courses, emptySet()))

        // Completing all 4 lessons of Course 1 unlocks Course 2 Lesson 1
        val course1Done = pack.courses.first().lessons.map { it.id }.toSet()
        assertTrue(UnlockCalculator.isCourseUnlocked(pack.courses, "course_court_equipment", course1Done))
        assertEquals(
            "course_court_equipment_l1",
            UnlockCalculator.nextLessonAfter(pack.courses, "course_fundamentals_l4", course1Done)
        )

        // Completing all 32 lessons yields null nextLessonId
        val allDone = pack.courses.flatMap { c -> c.lessons.map { it.id } }.toSet()
        assertNull(UnlockCalculator.nextUnlockedUncompletedLessonId(pack.courses, allDone))
        assertNull(UnlockCalculator.nextLessonAfter(pack.courses, "course_advanced_tactics_l4", allDone))
    }

    private fun resolveAssetsContentDir(): File {
        val candidates = listOf(
            File("src/main/assets/content"),
            File("app/src/main/assets/content")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate assets/content directory from ${File(".").absolutePath}")
    }
}
