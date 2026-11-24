package com.padelpro.app.domain.repository

import com.padelpro.app.domain.model.AcademyCourse
import com.padelpro.app.domain.model.AchievementItem
import com.padelpro.app.domain.model.BackupImportOutcome
import com.padelpro.app.domain.model.BackupValidationResult
import com.padelpro.app.domain.model.CourseDetailData
import com.padelpro.app.domain.model.DeleteFriendResult
import com.padelpro.app.domain.model.LessonCompletionOutcome
import com.padelpro.app.domain.model.LessonDetailData
import com.padelpro.app.domain.model.LiveMatchSnapshot
import com.padelpro.app.domain.model.ManualFinishResult
import com.padelpro.app.domain.model.MatchCompletionResult
import com.padelpro.app.domain.model.MatchDetailsData
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.PerformanceSummary
import com.padelpro.app.domain.model.Player
import com.padelpro.app.domain.model.RulebookCategory
import com.padelpro.app.domain.model.RulebookItem
import com.padelpro.app.domain.model.StartMatchRequest
import com.padelpro.app.domain.model.StartMatchResult
import com.padelpro.app.domain.model.TeamSide
import com.padelpro.app.domain.model.UserProgress
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun observeUserProfile(): Flow<Player?>
    fun observeFriends(): Flow<List<Player>>
    fun observeUserProgress(): Flow<UserProgress?>
    suspend fun createLocalProfile(
        displayName: String,
        avatarId: Int,
        preferredFormat: MatchFormat
    ): Player

    suspend fun updateUserProfile(
        displayName: String,
        avatarId: Int
    )

    suspend fun updatePreferredFormat(format: MatchFormat)

    suspend fun createFriend(
        displayName: String,
        avatarId: Int
    ): Player

    suspend fun updateFriend(
        playerId: String,
        displayName: String,
        avatarId: Int
    )

    suspend fun deleteFriend(playerId: String): DeleteFriendResult

    suspend fun resetAllData()
}

interface AcademyRepository {
    fun observeCourses(): Flow<List<AcademyCourse>>
    fun observeCourseDetail(courseId: String): Flow<CourseDetailData?>
    fun observeLesson(lessonId: String): Flow<LessonDetailData?>
    fun observeNextUnlockedLessonId(): Flow<String?>
    fun observeCompletedLessonsCount(): Flow<Int>
    fun observeTotalLessonsCount(): Int
    fun observeAchievements(): Flow<List<AchievementItem>>
    fun observeRulebook(searchQuery: String = "", category: RulebookCategory = RulebookCategory.ALL): Flow<List<RulebookItem>>

    suspend fun completeLesson(
        lessonId: String,
        selectedAnswers: Map<String, Int>
    ): LessonCompletionOutcome

    suspend fun getCompletionOutcome(lessonId: String): LessonCompletionOutcome?

    suspend fun toggleRuleFavorite(ruleId: String): Boolean
}

interface MatchRepository {
    fun observeActiveMatch(): Flow<MatchSummary?>
    fun observeRecentMatches(limit: Int = 3): Flow<List<MatchSummary>>
    fun observeCompletedMatches(): Flow<List<MatchSummary>>
    fun observeMatchById(matchId: String): Flow<MatchSummary?>
    fun observeMatchDetails(matchId: String): Flow<MatchDetailsData?>
    fun observeActiveMatchLive(): Flow<LiveMatchSnapshot?>

    suspend fun startMatch(
        request: StartMatchRequest,
        cancelExisting: Boolean = false
    ): StartMatchResult

    suspend fun addPoint(team: TeamSide): MatchCompletionResult?
    suspend fun undoLastPoint(): Boolean
    suspend fun pauseActiveMatch(): Boolean
    suspend fun resumeActiveMatch(): Boolean
    suspend fun cancelActiveMatch(): Boolean
    suspend fun finishActiveMatchManually(): ManualFinishResult
    suspend fun deleteMatch(matchId: String): Boolean
    suspend fun completeMatchIfFinished(matchId: String): MatchCompletionResult?
    fun dismissRestoredBanner(matchId: String)
}

interface StatsRepository {
    fun observePerformanceSummary(): Flow<PerformanceSummary>
}

interface BackupRepository {
    suspend fun exportBackupJson(): String
    fun validateBackupJson(json: String): BackupValidationResult
    suspend fun importBackupJson(json: String): BackupImportOutcome
}
