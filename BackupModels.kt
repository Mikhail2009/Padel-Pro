package com.padelpro.app.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: Long,
    val players: List<BackupPlayer> = emptyList(),
    @SerialName("user_progress")
    val userProgress: BackupUserProgress? = null,
    val matches: List<BackupMatch> = emptyList(),
    @SerialName("match_participants")
    val matchParticipants: List<BackupMatchParticipant> = emptyList(),
    @SerialName("match_events")
    val matchEvents: List<BackupMatchEvent> = emptyList(),
    @SerialName("set_results")
    val setResults: List<BackupSetResult> = emptyList(),
    @SerialName("lesson_progress")
    val lessonProgress: List<BackupLessonProgress> = emptyList(),
    @SerialName("quiz_attempts")
    val quizAttempts: List<BackupQuizAttempt> = emptyList(),
    @SerialName("achievements_unlocked")
    val achievementsUnlocked: List<BackupAchievementUnlocked> = emptyList(),
    @SerialName("rule_favorites")
    val ruleFavorites: List<BackupRuleFavorite> = emptyList(),
    val settings: BackupSettings
)

@Serializable
data class BackupPlayer(
    val id: String,
    val displayName: String,
    val avatarId: Int,
    val createdAt: Long,
    val preferredFormat: String? = null,
    val isUser: Int
)

@Serializable
data class BackupUserProgress(
    val userId: String,
    val totalXp: Int,
    val streakCount: Int,
    val lastLearningDate: String? = null
)

@Serializable
data class BackupMatch(
    val id: String,
    val format: String,
    val status: String,
    val deuceMode: String,
    val bestOf: Int = 3,
    val firstServerTeam: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationMs: Long? = null,
    val winnerTeam: String? = null
)

@Serializable
data class BackupMatchParticipant(
    val matchId: String,
    val playerId: String,
    val team: String,
    val position: Int,
    val isUser: Int
)

@Serializable
data class BackupMatchEvent(
    val id: Long = 0,
    val matchId: String,
    val seq: Int,
    val type: String,
    val team: String? = null,
    val timestamp: Long
)

@Serializable
data class BackupSetResult(
    val matchId: String,
    val setIndex: Int,
    val gamesA: Int,
    val gamesB: Int,
    val tbA: Int? = null,
    val tbB: Int? = null
)

@Serializable
data class BackupLessonProgress(
    val lessonId: String,
    val status: String,
    val completedAt: Long? = null,
    val bestQuizScore: Float? = null,
    val xpAwarded: Int
)

@Serializable
data class BackupQuizAttempt(
    val id: Long = 0,
    val lessonId: String,
    val attemptedAt: Long,
    val correct: Int,
    val total: Int
)

@Serializable
data class BackupAchievementUnlocked(
    val achievementId: String,
    val unlockedAt: Long
)

@Serializable
data class BackupRuleFavorite(
    val ruleId: String
)

@Serializable
data class BackupSettings(
    val onboardingCompleted: Boolean,
    val hapticsEnabled: Boolean,
    val keepScreenOn: Boolean,
    val defaultFormat: String,
    val defaultDeuceMode: String,
    val contentVersion: Int = 1,
    val backupLastExportedAt: Long? = null
)
