package com.padelpro.app.domain.repository

import com.padelpro.app.domain.model.AcademyCourse
import com.padelpro.app.domain.model.AchievementItem
import com.padelpro.app.domain.model.DeleteFriendResult
import com.padelpro.app.domain.model.LiveMatchSnapshot
import com.padelpro.app.domain.model.MatchCompletionResult
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.PerformanceSummary
import com.padelpro.app.domain.model.Player
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
    fun observeCompletedLessonsCount(): Flow<Int>
    fun observeTotalLessonsCount(): Int
    fun observeAchievements(): Flow<List<AchievementItem>>
}

interface MatchRepository {
    fun observeActiveMatch(): Flow<MatchSummary?>
    fun observeRecentMatches(limit: Int = 3): Flow<List<MatchSummary>>
    fun observeCompletedMatches(): Flow<List<MatchSummary>>
    fun observeMatchById(matchId: String): Flow<MatchSummary?>
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
    suspend fun completeMatchIfFinished(matchId: String): MatchCompletionResult?
    fun dismissRestoredBanner(matchId: String)
}

interface StatsRepository {
    fun observePerformanceSummary(): Flow<PerformanceSummary>
}
