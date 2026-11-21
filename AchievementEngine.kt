package com.padelpro.app.domain.academy

data class AchievementEvaluationState(
    val completedLessons: Int = 0,
    val completedCourses: Int = 0,
    val hasPerfectQuiz: Boolean = false,
    val learningStreak: Int = 0,
    val matchesPlayed: Int = 0,
    val matchWins: Int = 0,
    val bestWinStreak: Int = 0,
    val singlesWins: Int = 0,
    val doublesWins: Int = 0,
    val currentLevel: Int = 1,
    val hasGoldenPointWin: Boolean = false
)

object AchievementEngine {

    fun isConditionMet(
        definition: AchievementDefinition,
        state: AchievementEvaluationState
    ): Boolean {
        return when (definition.conditionType) {
            AchievementConditionType.COMPLETED_LESSONS -> state.completedLessons >= definition.threshold
            AchievementConditionType.COMPLETED_COURSES -> state.completedCourses >= definition.threshold
            AchievementConditionType.PERFECT_QUIZ -> state.hasPerfectQuiz
            AchievementConditionType.LEARNING_STREAK -> state.learningStreak >= definition.threshold
            AchievementConditionType.MATCHES_PLAYED -> state.matchesPlayed >= definition.threshold
            AchievementConditionType.MATCH_WINS -> state.matchWins >= definition.threshold
            AchievementConditionType.MATCH_WIN_STREAK -> state.bestWinStreak >= definition.threshold
            AchievementConditionType.SINGLES_WINS -> state.singlesWins >= definition.threshold
            AchievementConditionType.DOUBLES_WINS -> state.doublesWins >= definition.threshold
            AchievementConditionType.LEVEL_REACHED -> state.currentLevel >= definition.threshold
            AchievementConditionType.GOLDEN_POINT_WIN -> state.hasGoldenPointWin
        }
    }

    fun evaluateNewlyUnlocked(
        definitions: List<AchievementDefinition>,
        alreadyUnlockedIds: Set<String>,
        state: AchievementEvaluationState
    ): List<AchievementDefinition> {
        return definitions.filter { def ->
            def.id !in alreadyUnlockedIds && isConditionMet(def, state)
        }
    }
}
