package com.padelpro.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.common.LevelUtils
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.datastore.AppSettings
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.domain.model.AchievementItem
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.Player
import com.padelpro.app.domain.repository.AcademyRepository
import com.padelpro.app.domain.repository.PlayerRepository
import com.padelpro.app.domain.repository.StatsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val displayName: String = "",
    val avatarId: Int = 0,
    val memberSinceText: String = "",
    val preferredFormat: MatchFormat = MatchFormat.DOUBLES,
    val level: Int = 1,
    val levelTitle: String = "ROOKIE",
    val totalXp: Int = 0,
    val nextLevelXp: Int = 100,
    val levelProgress: Float = 0f,
    val totalMatches: Int = 0,
    val winRatePercent: Int? = null,
    val completedLessons: Int = 0,
    val totalLessons: Int = 32,
    val achievements: List<AchievementItem> = emptyList(),
    val friends: List<Player> = emptyList(),
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val defaultDeuceMode: DeuceMode = DeuceMode.GOLDEN_POINT
)

class ProfileViewModel(
    private val playerRepository: PlayerRepository,
    academyRepository: AcademyRepository,
    statsRepository: StatsRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _resetCompleteEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetCompleteEvent: SharedFlow<Unit> = _resetCompleteEvent.asSharedFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        playerRepository.observeUserProfile(),
        playerRepository.observeFriends(),
        playerRepository.observeUserProgress(),
        academyRepository.observeCompletedLessonsCount(),
        academyRepository.observeAchievements(),
        statsRepository.observePerformanceSummary(),
        settingsRepository.settings
    ) { values ->
        val profile = values[0] as? Player
        @Suppress("UNCHECKED_CAST")
        val friends = values[1] as List<Player>
        val progress = values[2] as? com.padelpro.app.domain.model.UserProgress
        val completedLessons = values[3] as Int
        @Suppress("UNCHECKED_CAST")
        val achievements = values[4] as List<AchievementItem>
        val perf = values[5] as com.padelpro.app.domain.model.PerformanceSummary
        val settings = values[6] as AppSettings

        val xp = progress?.totalXp ?: 0
        val level = LevelUtils.calculateLevel(xp)
        val memberSince = profile?.createdAt?.let { timeProvider.formatMonthYear(it) } ?: ""

        ProfileUiState(
            displayName = profile?.displayName ?: "",
            avatarId = profile?.avatarId ?: 0,
            memberSinceText = memberSince,
            preferredFormat = profile?.preferredFormat ?: settings.defaultFormat,
            level = level,
            levelTitle = LevelUtils.levelTitle(level),
            totalXp = xp,
            nextLevelXp = LevelUtils.nextLevelXp(level),
            levelProgress = LevelUtils.levelProgressFraction(xp),
            totalMatches = perf.totalMatches,
            winRatePercent = perf.winRatePercent,
            completedLessons = completedLessons,
            totalLessons = academyRepository.observeTotalLessonsCount(),
            achievements = achievements,
            friends = friends,
            hapticsEnabled = settings.hapticsEnabled,
            keepScreenOn = settings.keepScreenOn,
            defaultDeuceMode = settings.defaultDeuceMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun saveProfile(displayName: String, avatarId: Int) {
        viewModelScope.launch {
            playerRepository.updateUserProfile(displayName, avatarId)
        }
    }

    fun selectPreferredFormat(format: MatchFormat) {
        viewModelScope.launch {
            playerRepository.updatePreferredFormat(format)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(enabled)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(enabled)
        }
    }

    fun setDefaultDeuceMode(mode: DeuceMode) {
        viewModelScope.launch {
            settingsRepository.setDefaultDeuceMode(mode)
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            playerRepository.resetAllData()
            _resetCompleteEvent.tryEmit(Unit)
        }
    }
}
