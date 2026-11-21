package com.padelpro.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.common.LevelUtils
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.repository.AcademyRepository
import com.padelpro.app.domain.repository.MatchRepository
import com.padelpro.app.domain.repository.PlayerRepository
import com.padelpro.app.domain.repository.StatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class GreetingPeriod {
    MORNING,
    AFTERNOON,
    EVENING
}

data class HomeUiState(
    val greetingPeriod: GreetingPeriod = GreetingPeriod.EVENING,
    val displayName: String = "",
    val avatarId: Int = 0,
    val level: Int = 1,
    val levelTitle: String = "ROOKIE",
    val totalXp: Int = 0,
    val nextLevelXp: Int = 100,
    val levelProgress: Float = 0f,
    val streakDays: Int = 0,
    val completedLessons: Int = 0,
    val totalLessons: Int = 32,
    val nextUnlockedLessonId: String? = null,
    val activeMatch: MatchSummary? = null,
    val recentMatch: MatchSummary? = null,
    val totalMatches: Int = 0,
    val winRatePercent: Int? = null,
    val currentStreakLabel: String? = null,
    val currentStreakCount: Int = 0,
    val isCurrentStreakWin: Boolean? = null
) {
    val academyPercent: Int
        get() = if (totalLessons == 0) 0 else ((completedLessons.toFloat() / totalLessons.toFloat()) * 100f).toInt()
}

class HomeViewModel(
    playerRepository: PlayerRepository,
    academyRepository: AcademyRepository,
    matchRepository: MatchRepository,
    statsRepository: StatsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        playerRepository.observeUserProfile(),
        playerRepository.observeUserProgress(),
        academyRepository.observeCompletedLessonsCount(),
        academyRepository.observeNextUnlockedLessonId(),
        matchRepository.observeActiveMatch(),
        matchRepository.observeRecentMatches(limit = 1),
        statsRepository.observePerformanceSummary()
    ) { values ->
        val profile = values[0] as? com.padelpro.app.domain.model.Player
        val progress = values[1] as? com.padelpro.app.domain.model.UserProgress
        val completedLessons = values[2] as Int
        val nextLessonId = values[3] as? String
        val activeMatch = values[4] as? MatchSummary
        @Suppress("UNCHECKED_CAST")
        val recentMatches = values[5] as List<MatchSummary>
        val perf = values[6] as com.padelpro.app.domain.model.PerformanceSummary

        val xp = progress?.totalXp ?: 0
        val level = LevelUtils.calculateLevel(xp)
        val hour = timeProvider.currentHourOfDay()
        val period = when {
            hour < 12 -> GreetingPeriod.MORNING
            hour < 18 -> GreetingPeriod.AFTERNOON
            else -> GreetingPeriod.EVENING
        }

        HomeUiState(
            greetingPeriod = period,
            displayName = profile?.displayName ?: "",
            avatarId = profile?.avatarId ?: 0,
            level = level,
            levelTitle = LevelUtils.levelTitle(level),
            totalXp = xp,
            nextLevelXp = LevelUtils.nextLevelXp(level),
            levelProgress = LevelUtils.levelProgressFraction(xp),
            streakDays = progress?.streakCount ?: 0,
            completedLessons = completedLessons,
            totalLessons = academyRepository.observeTotalLessonsCount(),
            nextUnlockedLessonId = nextLessonId,
            activeMatch = activeMatch,
            recentMatch = recentMatches.firstOrNull(),
            totalMatches = perf.totalMatches,
            winRatePercent = perf.winRatePercent,
            currentStreakLabel = perf.currentStreakLabel,
            currentStreakCount = perf.currentStreakCount,
            isCurrentStreakWin = perf.isCurrentStreakWin
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun formatMatchDate(epochMillis: Long): String = timeProvider.formatShortDate(epochMillis)
}
