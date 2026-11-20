package com.padelpro.app.data.repository

import androidx.room.withTransaction
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.database.entity.LessonProgressEntity
import com.padelpro.app.core.database.entity.QuizAttemptEntity
import com.padelpro.app.core.database.entity.RuleFavoriteEntity
import com.padelpro.app.core.database.entity.UserProgressEntity
import com.padelpro.app.domain.academy.ContentPack
import com.padelpro.app.domain.academy.QuizValidator
import com.padelpro.app.domain.academy.StreakCalculator
import com.padelpro.app.domain.academy.UnlockCalculator
import com.padelpro.app.domain.academy.XpCalculator
import com.padelpro.app.domain.model.AcademyCourse
import com.padelpro.app.domain.model.AchievementItem
import com.padelpro.app.domain.model.CourseDetailData
import com.padelpro.app.domain.model.CourseLessonItem
import com.padelpro.app.domain.model.LessonCompletionOutcome
import com.padelpro.app.domain.model.LessonDetailData
import com.padelpro.app.domain.model.RulebookCategory
import com.padelpro.app.domain.model.RulebookItem
import com.padelpro.app.domain.repository.AcademyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class AcademyRepositoryImpl(
    private val database: PadelProDatabase,
    private val contentPack: ContentPack,
    private val timeProvider: TimeProvider
) : AcademyRepository {

    private val academyDao = database.academyDao()
    private val playerDao = database.playerDao()
    private val userProgressDao = database.userProgressDao()

    private val recentOutcomes = ConcurrentHashMap<String, LessonCompletionOutcome>()

    override fun observeCourses(): Flow<List<AcademyCourse>> =
        academyDao.observeLessonProgress().map { progressList ->
            val completedIds = progressList
                .filter { it.status == STATUS_COMPLETED }
                .map { it.lessonId }
                .toSet()
            UnlockCalculator.buildCourseStates(contentPack.courses, completedIds)
        }

    override fun observeCourseDetail(courseId: String): Flow<CourseDetailData?> =
        academyDao.observeLessonProgress().map { progressList ->
            val courseContent = contentPack.findCourse(courseId) ?: return@map null
            val progressByLessonId = progressList.associateBy { it.lessonId }
            val completedIds = progressList
                .filter { it.status == STATUS_COMPLETED }
                .map { it.lessonId }
                .toSet()

            val courseState = UnlockCalculator.buildCourseStates(contentPack.courses, completedIds)
                .firstOrNull { it.id == courseId } ?: return@map null

            val orderedLessons = courseContent.lessons.sortedBy { it.order }
            val firstUncompletedIndex = orderedLessons.indexOfFirst { it.id !in completedIds }

            val lessonItems = orderedLessons.mapIndexed { idx, lesson ->
                val entity = progressByLessonId[lesson.id]
                val isCompleted = entity?.status == STATUS_COMPLETED
                val isUnlocked = UnlockCalculator.isLessonUnlocked(
                    courses = contentPack.courses,
                    lessonId = lesson.id,
                    completedLessonIds = completedIds
                )
                val isCurrent = isUnlocked && !isCompleted && idx == firstUncompletedIndex
                CourseLessonItem(
                    id = lesson.id,
                    courseId = lesson.courseId,
                    order = lesson.order,
                    title = lesson.title,
                    estimatedMinutes = lesson.estimatedMinutes,
                    xpReward = lesson.xpReward,
                    quizQuestionCount = lesson.quiz.size,
                    isCompleted = isCompleted,
                    isCurrent = isCurrent,
                    isUnlocked = isUnlocked,
                    bestQuizScore = entity?.bestQuizScore
                )
            }

            val nextActionLessonId = lessonItems.firstOrNull { it.isUnlocked && !it.isCompleted }?.id
                ?: lessonItems.firstOrNull { it.isUnlocked }?.id

            CourseDetailData(
                course = courseState,
                lessons = lessonItems,
                nextActionLessonId = nextActionLessonId
            )
        }

    override fun observeLesson(lessonId: String): Flow<LessonDetailData?> =
        academyDao.observeLessonProgress().map { progressList ->
            val lesson = contentPack.findLesson(lessonId) ?: return@map null
            val course = contentPack.findCourse(lesson.courseId) ?: return@map null
            val isCompleted = progressList.any { it.lessonId == lessonId && it.status == STATUS_COMPLETED }
            LessonDetailData(
                id = lesson.id,
                courseId = lesson.courseId,
                courseOrder = course.order,
                courseTitle = course.title,
                order = lesson.order,
                title = lesson.title,
                estimatedMinutes = lesson.estimatedMinutes,
                xpReward = lesson.xpReward,
                blocks = lesson.blocks,
                keyFacts = lesson.keyFacts,
                quiz = lesson.quiz,
                isCompleted = isCompleted
            )
        }

    override fun observeNextUnlockedLessonId(): Flow<String?> =
        academyDao.observeLessonProgress().map { progressList ->
            val completedIds = progressList
                .filter { it.status == STATUS_COMPLETED }
                .map { it.lessonId }
                .toSet()
            UnlockCalculator.nextUnlockedUncompletedLessonId(contentPack.courses, completedIds)
        }

    override fun observeCompletedLessonsCount(): Flow<Int> =
        academyDao.observeLessonProgress().map { list ->
            list.count { it.status == STATUS_COMPLETED }
        }

    override fun observeTotalLessonsCount(): Int = contentPack.totalLessons

    override fun observeAchievements(): Flow<List<AchievementItem>> =
        academyDao.observeUnlockedAchievements().map { unlocked ->
            val map = unlocked.associateBy { it.achievementId }
            contentPack.achievements.map { def ->
                val entity = map[def.id]
                AchievementItem(
                    id = def.id,
                    isUnlocked = entity != null,
                    unlockedAt = entity?.unlockedAt,
                    title = def.title,
                    description = def.description,
                    icon = def.icon
                )
            }
        }

    override fun observeRulebook(
        searchQuery: String,
        category: RulebookCategory
    ): Flow<List<RulebookItem>> =
        academyDao.observeRuleFavorites().map { favorites ->
            val favoriteIds = favorites.map { it.ruleId }.toSet()
            val normalizedQuery = searchQuery.trim().lowercase()
            contentPack.rules
                .asSequence()
                .filter { rule ->
                    when (category) {
                        RulebookCategory.ALL -> true
                        RulebookCategory.FAVORITES -> rule.id in favoriteIds
                        else -> rule.category == category
                    }
                }
                .filter { rule ->
                    if (normalizedQuery.isEmpty()) {
                        true
                    } else {
                        rule.title.lowercase().contains(normalizedQuery) ||
                            rule.body.lowercase().contains(normalizedQuery) ||
                            rule.fipRef.lowercase().contains(normalizedQuery)
                    }
                }
                .map { rule ->
                    RulebookItem(
                        id = rule.id,
                        category = rule.category,
                        title = rule.title,
                        body = rule.body,
                        diagramId = rule.diagramId,
                        fipRef = rule.fipRef,
                        isFavorite = rule.id in favoriteIds
                    )
                }
                .toList()
        }

    override suspend fun completeLesson(
        lessonId: String,
        selectedAnswers: Map<String, Int>
    ): LessonCompletionOutcome = withContext(Dispatchers.IO) {
        val lesson = requireNotNull(contentPack.findLesson(lessonId)) {
            "Unknown lessonId: $lessonId"
        }
        val course = requireNotNull(contentPack.findCourse(lesson.courseId)) {
            "Unknown courseId: ${lesson.courseId}"
        }
        val nowMillis = timeProvider.nowMillis()
        val today = timeProvider.currentLocalDate()

        val outcome = database.withTransaction {
            val existingLesson = academyDao.getLessonProgressOnce(lessonId)
            val allProgressBefore = academyDao.getAllLessonProgressOnce()
            val priorAttemptCount = academyDao.getQuizAttemptCountForLesson(lessonId)
            val existingUserProgress = userProgressDao.getUserProgressOnce()

            val isReviewRepeat = existingLesson?.status == STATUS_COMPLETED
            val lessonXpAlreadyAwarded = existingLesson?.xpAwarded == 1
            val isFirstQuizAttempt = priorAttemptCount == 0

            val evaluation = QuizValidator.evaluate(lesson.quiz, selectedAnswers)

            val completedBefore = allProgressBefore
                .filter { it.status == STATUS_COMPLETED }
                .map { it.lessonId }
                .toSet()
            val wasCourseCompletedBefore = course.lessons.isNotEmpty() &&
                course.lessons.all { it.id in completedBefore }
            val completedAfter = completedBefore + lessonId
            val isCourseCompletedAfter = course.lessons.isNotEmpty() &&
                course.lessons.all { it.id in completedAfter }
            val courseJustCompleted = !wasCourseCompletedBefore && isCourseCompletedAfter

            val xpBreakdown = XpCalculator.calculateLessonCompletionXp(
                lessonXpAlreadyAwarded = lessonXpAlreadyAwarded,
                isFirstQuizAttempt = isFirstQuizAttempt,
                correctAnswers = evaluation.correctCount,
                totalQuestions = evaluation.totalQuestions,
                courseJustCompleted = courseJustCompleted,
                lessonXpReward = lesson.xpReward,
                courseBonusReward = course.xpBonus
            )

            val bestScore = maxOf(existingLesson?.bestQuizScore ?: 0f, evaluation.scoreRatio)
            academyDao.upsertLessonProgress(
                LessonProgressEntity(
                    lessonId = lessonId,
                    status = STATUS_COMPLETED,
                    completedAt = existingLesson?.completedAt ?: nowMillis,
                    bestQuizScore = bestScore,
                    xpAwarded = 1
                )
            )

            academyDao.insertQuizAttempt(
                QuizAttemptEntity(
                    lessonId = lessonId,
                    attemptedAt = nowMillis,
                    correct = evaluation.correctCount,
                    total = evaluation.totalQuestions
                )
            )

            val streakUpdate = StreakCalculator.calculateNextStreak(
                previousStreak = existingUserProgress?.streakCount ?: 0,
                lastLearningDateIso = existingUserProgress?.lastLearningDate,
                currentDate = today
            )
            val targetUserId = existingUserProgress?.userId
                ?: playerDao.getUserProfileOnce()?.id
                ?: DEFAULT_USER_ID
            val updatedTotalXp = (existingUserProgress?.totalXp ?: 0) + xpBreakdown.totalXp

            userProgressDao.upsertUserProgress(
                UserProgressEntity(
                    userId = targetUserId,
                    totalXp = updatedTotalXp,
                    streakCount = streakUpdate.streakCount,
                    lastLearningDate = streakUpdate.lastLearningDateIso
                )
            )

            val newlyUnlocked = AchievementSyncHelper.evaluateAndUnlock(
                database = database,
                contentPack = contentPack,
                unlockedAtMillis = nowMillis
            )

            val courseCompletedCount = course.lessons.count { it.id in completedAfter }
            val nextLessonId = UnlockCalculator.nextLessonAfter(
                courses = contentPack.courses,
                currentLessonId = lessonId,
                completedLessonIds = completedAfter
            )

            LessonCompletionOutcome(
                lessonId = lesson.id,
                lessonTitle = lesson.title,
                courseId = course.id,
                courseOrder = course.order,
                xpLesson = xpBreakdown.lessonXp,
                xpQuiz = xpBreakdown.quizXp,
                xpCourseBonus = xpBreakdown.courseBonusXp,
                totalXpGained = xpBreakdown.totalXp,
                correctAnswers = evaluation.correctCount,
                totalQuestions = evaluation.totalQuestions,
                scorePercent = evaluation.scorePercent,
                courseCompletedLessons = courseCompletedCount,
                courseTotalLessons = course.lessons.size,
                newlyUnlockedAchievements = newlyUnlocked,
                nextLessonId = nextLessonId,
                isReviewRepeat = isReviewRepeat
            )
        }

        recentOutcomes[lessonId] = outcome
        outcome
    }

    override suspend fun getCompletionOutcome(lessonId: String): LessonCompletionOutcome? =
        withContext(Dispatchers.IO) {
            recentOutcomes[lessonId]?.let { return@withContext it }

            val lesson = contentPack.findLesson(lessonId) ?: return@withContext null
            val course = contentPack.findCourse(lesson.courseId) ?: return@withContext null
            val progressEntity = academyDao.getLessonProgressOnce(lessonId) ?: return@withContext null
            val latestAttempt = academyDao.getLatestQuizAttemptForLesson(lessonId)
            val completedIds = academyDao.getAllLessonProgressOnce()
                .filter { it.status == STATUS_COMPLETED }
                .map { it.lessonId }
                .toSet()

            val correct = latestAttempt?.correct ?: 0
            val total = latestAttempt?.total ?: lesson.quiz.size
            val percent = if (total > 0) ((correct.toFloat() / total.toFloat()) * 100f).roundToInt() else 0
            val courseCompletedCount = course.lessons.count { it.id in completedIds }
            val nextLessonId = UnlockCalculator.nextLessonAfter(
                courses = contentPack.courses,
                currentLessonId = lessonId,
                completedLessonIds = completedIds
            )

            LessonCompletionOutcome(
                lessonId = lesson.id,
                lessonTitle = lesson.title,
                courseId = course.id,
                courseOrder = course.order,
                xpLesson = 0,
                xpQuiz = 0,
                xpCourseBonus = 0,
                totalXpGained = 0,
                correctAnswers = correct,
                totalQuestions = total,
                scorePercent = percent,
                courseCompletedLessons = courseCompletedCount,
                courseTotalLessons = course.lessons.size,
                newlyUnlockedAchievements = emptyList(),
                nextLessonId = nextLessonId,
                isReviewRepeat = progressEntity.xpAwarded == 1
            )
        }

    override suspend fun toggleRuleFavorite(ruleId: String): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            val exists = academyDao.isRuleFavorite(ruleId)
            if (exists) {
                academyDao.deleteRuleFavorite(ruleId)
                false
            } else {
                academyDao.insertRuleFavorite(RuleFavoriteEntity(ruleId = ruleId))
                true
            }
        }
    }

    companion object {
        private const val STATUS_COMPLETED = "COMPLETED"
        private const val DEFAULT_USER_ID = "local_user"
    }
}
