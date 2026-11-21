package com.padelpro.app.domain.academy

import com.padelpro.app.domain.model.AcademyCourse
import java.time.LocalDate
import kotlin.math.roundToInt

data class QuizEvaluation(
    val correctCount: Int,
    val totalQuestions: Int,
    val scorePercent: Int,
    val scoreRatio: Float,
    val isPerfect: Boolean
)

object QuizValidator {
    fun isCorrect(question: QuizQuestion, selectedOptionIndex: Int?): Boolean {
        return selectedOptionIndex != null && selectedOptionIndex == question.correctIndex
    }

    fun evaluate(questions: List<QuizQuestion>, selectedAnswers: Map<String, Int>): QuizEvaluation {
        if (questions.isEmpty()) {
            return QuizEvaluation(
                correctCount = 0,
                totalQuestions = 0,
                scorePercent = 0,
                scoreRatio = 0f,
                isPerfect = false
            )
        }
        val correct = questions.count { q -> isCorrect(q, selectedAnswers[q.id]) }
        val ratio = (correct.toFloat() / questions.size.toFloat()).coerceIn(0f, 1f)
        val percent = (ratio * 100f).roundToInt().coerceIn(0, 100)
        return QuizEvaluation(
            correctCount = correct,
            totalQuestions = questions.size,
            scorePercent = percent,
            scoreRatio = ratio,
            isPerfect = correct == questions.size
        )
    }
}

data class LessonXpBreakdown(
    val lessonXp: Int,
    val quizXp: Int,
    val courseBonusXp: Int
) {
    val totalXp: Int
        get() = lessonXp + quizXp + courseBonusXp
}

object XpCalculator {
    const val XP_PER_LESSON = 20
    const val XP_PER_CORRECT_QUIZ_ANSWER = 5
    const val XP_COURSE_COMPLETION_BONUS = 50

    fun calculateLessonCompletionXp(
        lessonXpAlreadyAwarded: Boolean,
        isFirstQuizAttempt: Boolean,
        correctAnswers: Int,
        totalQuestions: Int,
        courseJustCompleted: Boolean,
        lessonXpReward: Int = XP_PER_LESSON,
        courseBonusReward: Int = XP_COURSE_COMPLETION_BONUS
    ): LessonXpBreakdown {
        val lessonXp = if (!lessonXpAlreadyAwarded) lessonXpReward else 0
        val clampedCorrect = correctAnswers.coerceIn(0, totalQuestions.coerceAtLeast(0))
        val quizXp = if (isFirstQuizAttempt) clampedCorrect * XP_PER_CORRECT_QUIZ_ANSWER else 0
        val courseXp = if (courseJustCompleted) courseBonusReward else 0
        return LessonXpBreakdown(
            lessonXp = lessonXp,
            quizXp = quizXp,
            courseBonusXp = courseXp
        )
    }
}

data class StreakUpdate(
    val streakCount: Int,
    val lastLearningDateIso: String
)

object StreakCalculator {
    fun calculateNextStreak(
        previousStreak: Int,
        lastLearningDateIso: String?,
        currentDate: LocalDate
    ): StreakUpdate {
        val todayIso = currentDate.toString()
        val lastDate = lastLearningDateIso?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }
        val nextStreak = when {
            lastDate == null -> 1
            lastDate == currentDate -> previousStreak.coerceAtLeast(1)
            lastDate == currentDate.minusDays(1) -> previousStreak.coerceAtLeast(0) + 1
            else -> 1
        }
        return StreakUpdate(
            streakCount = nextStreak,
            lastLearningDateIso = todayIso
        )
    }
}

object UnlockCalculator {
    fun buildCourseStates(
        courses: List<CourseContent>,
        completedLessonIds: Set<String>
    ): List<AcademyCourse> {
        var previousCourseCompleted = true
        return courses.sortedBy { it.order }.map { course ->
            val completedCount = course.lessons.count { it.id in completedLessonIds }
            val unlocked = course.order == 1 || previousCourseCompleted
            previousCourseCompleted = course.lessons.isNotEmpty() && completedCount >= course.lessons.size
            AcademyCourse(
                id = course.id,
                order = course.order,
                category = course.category,
                lessonCount = course.lessons.size,
                estimatedMinutes = course.estimatedMinutes,
                xpReward = course.maxXpReward,
                completedLessons = completedCount,
                isUnlocked = unlocked,
                title = course.title,
                description = course.description
            )
        }
    }

    fun isCourseUnlocked(
        courses: List<CourseContent>,
        targetCourseId: String,
        completedLessonIds: Set<String>
    ): Boolean {
        return buildCourseStates(courses, completedLessonIds)
            .firstOrNull { it.id == targetCourseId }
            ?.isUnlocked == true
    }

    fun isLessonUnlocked(
        courses: List<CourseContent>,
        lessonId: String,
        completedLessonIds: Set<String>
    ): Boolean {
        val orderedCourses = courses.sortedBy { it.order }
        val course = orderedCourses.firstOrNull { c -> c.lessons.any { it.id == lessonId } }
            ?: return false
        if (!isCourseUnlocked(orderedCourses, course.id, completedLessonIds)) {
            return false
        }
        val orderedLessons = course.lessons.sortedBy { it.order }
        val targetIndex = orderedLessons.indexOfFirst { it.id == lessonId }
        if (targetIndex < 0) return false
        if (targetIndex == 0) return true
        return orderedLessons[targetIndex - 1].id in completedLessonIds
    }

    fun nextUnlockedUncompletedLessonId(
        courses: List<CourseContent>,
        completedLessonIds: Set<String>
    ): String? {
        val courseStates = buildCourseStates(courses, completedLessonIds)
        val stateById = courseStates.associateBy { it.id }
        for (course in courses.sortedBy { it.order }) {
            val unlocked = stateById[course.id]?.isUnlocked == true
            if (!unlocked) continue
            for (lesson in course.lessons.sortedBy { it.order }) {
                if (lesson.id !in completedLessonIds) {
                    return lesson.id
                }
            }
        }
        return null
    }

    fun nextLessonAfter(
        courses: List<CourseContent>,
        currentLessonId: String,
        completedLessonIds: Set<String>
    ): String? {
        val orderedCourses = courses.sortedBy { it.order }
        val currentCourseIdx = orderedCourses.indexOfFirst { c -> c.lessons.any { it.id == currentLessonId } }
        if (currentCourseIdx < 0) return null
        val currentCourse = orderedCourses[currentCourseIdx]
        val orderedLessons = currentCourse.lessons.sortedBy { it.order }
        val currentLessonIdx = orderedLessons.indexOfFirst { it.id == currentLessonId }
        if (currentLessonIdx in 0 until orderedLessons.lastIndex) {
            val candidate = orderedLessons[currentLessonIdx + 1]
            if (isLessonUnlocked(orderedCourses, candidate.id, completedLessonIds)) {
                return candidate.id
            }
        }
        if (currentCourseIdx < orderedCourses.lastIndex) {
            val nextCourse = orderedCourses[currentCourseIdx + 1]
            if (isCourseUnlocked(orderedCourses, nextCourse.id, completedLessonIds)) {
                return nextCourse.lessons.sortedBy { it.order }.firstOrNull()?.id
            }
        }
        return null
    }
}
