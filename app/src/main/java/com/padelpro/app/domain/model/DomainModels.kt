package com.padelpro.app.domain.model

enum class MatchFormat {
    SINGLES,
    DOUBLES
}

enum class DeuceMode {
    ADVANTAGE,
    GOLDEN_POINT,
    STAR_POINT
}

enum class MatchStatus {
    ACTIVE,
    COMPLETED
}

enum class TeamSide {
    A,
    B
}

enum class CourseCategory {
    ALL,
    FUNDAMENTALS,
    RULES,
    SERVING,
    TECHNIQUE,
    TACTICS
}

enum class RulebookCategory {
    ALL,
    COURT,
    EQUIPMENT,
    SCORING,
    SERVING,
    WALLS,
    FOULS,
    FAVORITES
}

data class Player(
    val id: String,
    val displayName: String,
    val avatarId: Int,
    val createdAt: Long,
    val preferredFormat: MatchFormat,
    val isUser: Boolean
)

data class UserProgress(
    val userId: String,
    val totalXp: Int,
    val streakCount: Int,
    val lastLearningDate: String?
)

data class AcademyCourse(
    val id: String,
    val order: Int,
    val category: CourseCategory,
    val lessonCount: Int,
    val estimatedMinutes: Int,
    val xpReward: Int,
    val completedLessons: Int,
    val isUnlocked: Boolean
) {
    val progressFraction: Float
        get() = if (lessonCount == 0) 0f else (completedLessons.toFloat() / lessonCount.toFloat()).coerceIn(0f, 1f)

    val isCompleted: Boolean
        get() = lessonCount > 0 && completedLessons >= lessonCount
}

data class AcademyLessonStub(
    val id: String,
    val courseId: String,
    val order: Int,
    val isCompleted: Boolean,
    val isUnlocked: Boolean
)

data class AchievementItem(
    val id: String,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null
)

data class SetScore(
    val setIndex: Int,
    val gamesA: Int,
    val gamesB: Int,
    val tbA: Int? = null,
    val tbB: Int? = null
)

data class MatchSummary(
    val id: String,
    val format: MatchFormat,
    val deuceMode: DeuceMode,
    val status: MatchStatus,
    val startedAt: Long,
    val endedAt: Long?,
    val durationMs: Long?,
    val winnerTeam: TeamSide?,
    val userWon: Boolean?,
    val teamANames: List<String>,
    val teamBNames: List<String>,
    val sets: List<SetScore>
)

data class MonthlyMatchCount(
    val label: String,
    val count: Int,
    val isCurrentMonth: Boolean
)

data class PerformanceSummary(
    val totalMatches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winRatePercent: Int? = null,
    val currentStreakLabel: String? = null,
    val currentStreakCount: Int = 0,
    val isCurrentStreakWin: Boolean? = null,
    val bestWinStreak: Int = 0,
    val singlesMatches: Int = 0,
    val singlesWinRatePercent: Int? = null,
    val doublesMatches: Int = 0,
    val doublesWinRatePercent: Int? = null,
    val recentForm: List<Boolean> = emptyList(),
    val monthlyActivity: List<MonthlyMatchCount> = emptyList()
)

data class StartMatchParticipant(
    val playerId: String,
    val team: TeamSide,
    val position: Int,
    val isUser: Boolean
)

data class StartMatchRequest(
    val format: MatchFormat,
    val deuceMode: DeuceMode,
    val bestOf: Int = 3,
    val firstServerTeam: TeamSide,
    val participants: List<StartMatchParticipant>
)

sealed interface StartMatchResult {
    data class Started(val matchId: String) : StartMatchResult
    data class ActiveMatchExists(val activeMatchId: String) : StartMatchResult
}

data class MatchCompletionResult(
    val matchId: String,
    val winnerTeam: TeamSide,
    val xpAwarded: Int,
    val alreadyCompleted: Boolean
)

data class LiveMatchSnapshot(
    val matchId: String,
    val state: com.padelpro.app.domain.engine.MatchEngineState,
    val teamAPlayers: List<Player>,
    val teamBPlayers: List<Player>,
    val canUndo: Boolean,
    val wasRestored: Boolean
)

enum class DeleteFriendResult {
    DELETED,
    IN_USE_BY_MATCHES,
    NOT_FOUND
}
