package com.padelpro.app.data.repository

import com.padelpro.app.core.database.dao.AcademyDao
import com.padelpro.app.domain.model.AcademyCourse
import com.padelpro.app.domain.model.AchievementItem
import com.padelpro.app.domain.model.CourseCategory
import com.padelpro.app.domain.repository.AcademyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AcademyRepositoryImpl(
    private val academyDao: AcademyDao
) : AcademyRepository {

    private data class CourseBlueprint(
        val id: String,
        val order: Int,
        val category: CourseCategory,
        val lessonCount: Int = 4,
        val estimatedMinutes: Int = 20,
        val xpReward: Int = 180
    )

    private val courseBlueprints = listOf(
        CourseBlueprint("course_fundamentals", 1, CourseCategory.FUNDAMENTALS),
        CourseBlueprint("course_court_equipment", 2, CourseCategory.FUNDAMENTALS),
        CourseBlueprint("course_rules_scoring", 3, CourseCategory.RULES),
        CourseBlueprint("course_serving", 4, CourseCategory.SERVING),
        CourseBlueprint("course_techniques", 5, CourseCategory.TECHNIQUE),
        CourseBlueprint("course_positioning", 6, CourseCategory.TACTICS),
        CourseBlueprint("course_doubles_strategy", 7, CourseCategory.TACTICS),
        CourseBlueprint("course_advanced_tactics", 8, CourseCategory.TACTICS)
    )

    private val achievementIds = listOf(
        "FIRST_LESSON",
        "FIRST_COURSE",
        "PERFECT_QUIZ",
        "STREAK_3",
        "STREAK_7",
        "FIRST_MATCH",
        "FIRST_WIN",
        "WIN_STREAK_3",
        "SINGLES_5_WINS",
        "DOUBLES_5_WINS",
        "MATCHES_10",
        "MATCHES_25",
        "LEVEL_5",
        "GOLDEN_WIN"
    )

    override fun observeCourses(): Flow<List<AcademyCourse>> =
        academyDao.observeLessonProgress().map { progressList ->
            val completedIds = progressList
                .filter { it.status == "COMPLETED" }
                .map { it.lessonId }
                .toSet()

            var previousCompleted = true
            courseBlueprints.map { bp ->
                val completedInCourse = (1..bp.lessonCount).count { lessonIdx ->
                    "${bp.id}_l$lessonIdx" in completedIds
                }
                val isUnlocked = bp.order == 1 || previousCompleted
                previousCompleted = completedInCourse >= bp.lessonCount
                AcademyCourse(
                    id = bp.id,
                    order = bp.order,
                    category = bp.category,
                    lessonCount = bp.lessonCount,
                    estimatedMinutes = bp.estimatedMinutes,
                    xpReward = bp.xpReward,
                    completedLessons = completedInCourse,
                    isUnlocked = isUnlocked
                )
            }
        }

    override fun observeCompletedLessonsCount(): Flow<Int> =
        academyDao.observeLessonProgress().map { list ->
            list.count { it.status == "COMPLETED" }
        }

    override fun observeTotalLessonsCount(): Int = courseBlueprints.sumOf { it.lessonCount }

    override fun observeAchievements(): Flow<List<AchievementItem>> =
        academyDao.observeUnlockedAchievements().map { unlocked ->
            val map = unlocked.associateBy { it.achievementId }
            achievementIds.map { id ->
                val entity = map[id]
                AchievementItem(
                    id = id,
                    isUnlocked = entity != null,
                    unlockedAt = entity?.unlockedAt
                )
            }
        }
}
