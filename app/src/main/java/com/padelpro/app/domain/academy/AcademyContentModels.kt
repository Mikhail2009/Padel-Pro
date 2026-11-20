package com.padelpro.app.domain.academy

import com.padelpro.app.domain.model.CourseCategory
import com.padelpro.app.domain.model.RulebookCategory
import kotlinx.serialization.Serializable

enum class LessonBlockType {
    TEXT,
    FACT,
    DIAGRAM
}

enum class AchievementConditionType {
    COMPLETED_LESSONS,
    COMPLETED_COURSES,
    PERFECT_QUIZ,
    LEARNING_STREAK,
    MATCHES_PLAYED,
    MATCH_WINS,
    MATCH_WIN_STREAK,
    SINGLES_WINS,
    DOUBLES_WINS,
    LEVEL_REACHED,
    GOLDEN_POINT_WIN
}

@Serializable
data class ContentVersionDto(
    val version: Int
)

@Serializable
data class LessonBlockDto(
    val type: String,
    val title: String? = null,
    val body: String,
    val diagramId: String? = null
)

@Serializable
data class QuizQuestionDto(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@Serializable
data class LessonContentDto(
    val id: String,
    val courseId: String,
    val order: Int,
    val title: String,
    val estimatedMinutes: Int = 5,
    val xpReward: Int = 20,
    val blocks: List<LessonBlockDto>,
    val keyFacts: List<String>,
    val quiz: List<QuizQuestionDto>
)

@Serializable
data class CourseContentDto(
    val id: String,
    val order: Int,
    val category: String,
    val title: String,
    val description: String,
    val estimatedMinutes: Int = 20,
    val xpBonus: Int = 50,
    val lessons: List<LessonContentDto>
)

@Serializable
data class RuleEntryDto(
    val id: String,
    val category: String,
    val title: String,
    val body: String,
    val diagramId: String? = null,
    val fipRef: String
)

@Serializable
data class AchievementConditionDto(
    val type: String,
    val threshold: Int = 1
)

@Serializable
data class AchievementDefinitionDto(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val condition: AchievementConditionDto
)

data class LessonBlock(
    val type: LessonBlockType,
    val title: String?,
    val body: String,
    val diagramId: String?
)

data class QuizQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class LessonContent(
    val id: String,
    val courseId: String,
    val order: Int,
    val title: String,
    val estimatedMinutes: Int,
    val xpReward: Int,
    val blocks: List<LessonBlock>,
    val keyFacts: List<String>,
    val quiz: List<QuizQuestion>
)

data class CourseContent(
    val id: String,
    val order: Int,
    val category: CourseCategory,
    val title: String,
    val description: String,
    val estimatedMinutes: Int,
    val xpBonus: Int,
    val lessons: List<LessonContent>
) {
    val maxXpReward: Int
        get() = lessons.sumOf { it.xpReward + it.quiz.size * XpCalculator.XP_PER_CORRECT_QUIZ_ANSWER } + xpBonus
}

data class RuleEntry(
    val id: String,
    val category: RulebookCategory,
    val title: String,
    val body: String,
    val diagramId: String?,
    val fipRef: String
)

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val conditionType: AchievementConditionType,
    val threshold: Int
)

data class ContentPack(
    val version: Int,
    val courses: List<CourseContent>,
    val rules: List<RuleEntry>,
    val achievements: List<AchievementDefinition>
) {
    val totalLessons: Int
        get() = courses.sumOf { it.lessons.size }

    fun findCourse(courseId: String): CourseContent? = courses.firstOrNull { it.id == courseId }

    fun findLesson(lessonId: String): LessonContent? =
        courses.firstNotNullOfOrNull { c -> c.lessons.firstOrNull { it.id == lessonId } }
}
