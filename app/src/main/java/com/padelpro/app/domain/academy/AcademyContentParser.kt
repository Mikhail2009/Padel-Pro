package com.padelpro.app.domain.academy

import com.padelpro.app.domain.model.CourseCategory
import com.padelpro.app.domain.model.RulebookCategory
import kotlinx.serialization.json.Json

object AcademyContentParser {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parse(
        versionJson: String,
        coursesJson: String,
        rulebookJson: String,
        achievementsJson: String
    ): ContentPack {
        val versionDto = json.decodeFromString<ContentVersionDto>(versionJson)
        val courseDtos = json.decodeFromString<List<CourseContentDto>>(coursesJson)
        val ruleDtos = json.decodeFromString<List<RuleEntryDto>>(rulebookJson)
        val achievementDtos = json.decodeFromString<List<AchievementDefinitionDto>>(achievementsJson)

        val courses = courseDtos
            .sortedBy { it.order }
            .map { courseDto ->
                CourseContent(
                    id = courseDto.id,
                    order = courseDto.order,
                    category = CourseCategory.valueOf(courseDto.category.uppercase()),
                    title = courseDto.title,
                    description = courseDto.description,
                    estimatedMinutes = courseDto.estimatedMinutes,
                    xpBonus = courseDto.xpBonus,
                    lessons = courseDto.lessons.sortedBy { it.order }.map { lessonDto ->
                        LessonContent(
                            id = lessonDto.id,
                            courseId = lessonDto.courseId,
                            order = lessonDto.order,
                            title = lessonDto.title,
                            estimatedMinutes = lessonDto.estimatedMinutes,
                            xpReward = lessonDto.xpReward,
                            blocks = lessonDto.blocks.map { blockDto ->
                                LessonBlock(
                                    type = LessonBlockType.valueOf(blockDto.type.uppercase()),
                                    title = blockDto.title,
                                    body = blockDto.body,
                                    diagramId = blockDto.diagramId
                                )
                            },
                            keyFacts = lessonDto.keyFacts,
                            quiz = lessonDto.quiz.map { qDto ->
                                QuizQuestion(
                                    id = qDto.id,
                                    text = qDto.text,
                                    options = qDto.options,
                                    correctIndex = qDto.correctIndex,
                                    explanation = qDto.explanation
                                )
                            }
                        )
                    }
                )
            }

        val rules = ruleDtos.map { dto ->
            RuleEntry(
                id = dto.id,
                category = RulebookCategory.valueOf(dto.category.uppercase()),
                title = dto.title,
                body = dto.body,
                diagramId = dto.diagramId,
                fipRef = dto.fipRef
            )
        }

        val achievements = achievementDtos.map { dto ->
            AchievementDefinition(
                id = dto.id,
                title = dto.title,
                description = dto.description,
                icon = dto.icon,
                conditionType = AchievementConditionType.valueOf(dto.condition.type.uppercase()),
                threshold = dto.condition.threshold
            )
        }

        val pack = ContentPack(
            version = versionDto.version,
            courses = courses,
            rules = rules,
            achievements = achievements
        )
        validate(pack)
        return pack
    }

    fun validate(pack: ContentPack) {
        require(pack.version >= 1) { "Content version must be >= 1" }
        require(pack.courses.isNotEmpty()) { "Courses cannot be empty" }
        val courseIds = pack.courses.map { it.id }
        require(courseIds.distinct().size == courseIds.size) { "Duplicate course IDs" }

        val lessonIds = mutableSetOf<String>()
        pack.courses.forEach { course ->
            require(course.title.isNotBlank()) { "Course ${course.id} missing title" }
            require(course.lessons.isNotEmpty()) { "Course ${course.id} has no lessons" }
            course.lessons.forEach { lesson ->
                require(lessonIds.add(lesson.id)) { "Duplicate lesson ID ${lesson.id}" }
                require(lesson.courseId == course.id) { "Lesson ${lesson.id} courseId mismatch" }
                require(lesson.blocks.isNotEmpty()) { "Lesson ${lesson.id} has no blocks" }
                require(lesson.quiz.isNotEmpty()) { "Lesson ${lesson.id} has no quiz" }
                lesson.quiz.forEach { q ->
                    require(q.options.size == 4) { "Question ${q.id} must have 4 options" }
                    require(q.correctIndex in 0..3) { "Question ${q.id} correctIndex out of bounds" }
                    require(q.explanation.isNotBlank()) { "Question ${q.id} missing explanation" }
                }
            }
        }

        val ruleIds = pack.rules.map { it.id }
        require(ruleIds.distinct().size == ruleIds.size) { "Duplicate rule IDs" }
        pack.rules.forEach { rule ->
            require(rule.category != RulebookCategory.ALL && rule.category != RulebookCategory.FAVORITES) {
                "Invalid rule category ${rule.category}"
            }
            require(rule.fipRef.isNotBlank()) { "Rule ${rule.id} missing fipRef" }
        }

        val achIds = pack.achievements.map { it.id }
        require(achIds.distinct().size == achIds.size) { "Duplicate achievement IDs" }
    }
}
