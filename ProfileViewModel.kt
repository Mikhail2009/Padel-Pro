package com.padelpro.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.common.LevelUtils
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.datastore.AppSettings
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.domain.model.AchievementItem
import com.padelpro.app.domain.model.BackupImportOutcome
import com.padelpro.app.domain.model.BackupPreview
import com.padelpro.app.domain.model.BackupValidationError
import com.padelpro.app.domain.model.BackupValidationResult
import com.padelpro.app.domain.model.DeleteFriendResult
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.FriendActionError
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.PerformanceSummary
import com.padelpro.app.domain.model.Player
import com.padelpro.app.domain.model.UserProgress
import com.padelpro.app.domain.repository.AcademyRepository
import com.padelpro.app.domain.repository.BackupRepository
import com.padelpro.app.domain.repository.PlayerRepository
import com.padelpro.app.domain.repository.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ProfileBackupFeedback {
    data object ExportSuccess : ProfileBackupFeedback
    data object ExportError : ProfileBackupFeedback
    data class ImportSuccess(val matchesCount: Int, val playersCount: Int) : ProfileBackupFeedback
    data class ImportValidationError(val error: BackupValidationError) : ProfileBackupFeedback
    data object ReadError : ProfileBackupFeedback
}

private data class ProfileTransientState(
    val friendError: FriendActionError? = null,
    val pendingImportPreview: BackupPreview? = null,
    val pendingImportJson: String? = null,
    val isBackupBusy: Boolean = false,
    val backupFeedback: ProfileBackupFeedback? = null
)

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
    val defaultDeuceMode: DeuceMode = DeuceMode.GOLDEN_POINT,
    val friendError: FriendActionError? = null,
    val backupLastExportedText: String? = null,
    val pendingImportPreview: BackupPreview? = null,
    val isBackupBusy: Boolean = false,
    val backupFeedback: ProfileBackupFeedback? = null
)

class ProfileViewModel(
    private val playerRepository: PlayerRepository,
    academyRepository: AcademyRepository,
    statsRepository: StatsRepository,
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val transient = MutableStateFlow(ProfileTransientState())

    private val _resetCompleteEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetCompleteEvent: SharedFlow<Unit> = _resetCompleteEvent.asSharedFlow()

    private val _needsOnboardingEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val needsOnboardingEvent: SharedFlow<Unit> = _needsOnboardingEvent.asSharedFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        playerRepository.observeUserProfile(),
        playerRepository.observeFriends(),
        playerRepository.observeUserProgress(),
        academyRepository.observeCompletedLessonsCount(),
        academyRepository.observeAchievements(),
        statsRepository.observePerformanceSummary(),
        settingsRepository.settings,
        transient
    ) { values ->
        val profile = values[0] as? Player
        @Suppress("UNCHECKED_CAST")
        val friends = values[1] as List<Player>
        val progress = values[2] as? UserProgress
        val completedLessons = values[3] as Int
        @Suppress("UNCHECKED_CAST")
        val achievements = values[4] as List<AchievementItem>
        val perf = values[5] as PerformanceSummary
        val settings = values[6] as AppSettings
        val local = values[7] as ProfileTransientState

        val xp = progress?.totalXp ?: 0
        val level = LevelUtils.calculateLevel(xp)
        val memberSince = profile?.createdAt?.let { timeProvider.formatMonthYear(it) } ?: ""
        val lastExportText = settings.backupLastExportedAt?.let {
            timeProvider.formatShortDateTime(it)
        }

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
            defaultDeuceMode = settings.defaultDeuceMode,
            friendError = local.friendError,
            backupLastExportedText = lastExportText,
            pendingImportPreview = local.pendingImportPreview,
            isBackupBusy = local.isBackupBusy,
            backupFeedback = local.backupFeedback
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

    fun createFriend(displayName: String, avatarId: Int) {
        val trimmed = displayName.trim().take(20)
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            playerRepository.createFriend(trimmed, avatarId)
            transient.value = transient.value.copy(friendError = null)
        }
    }

    fun updateFriend(playerId: String, displayName: String, avatarId: Int) {
        val trimmed = displayName.trim().take(20)
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            playerRepository.updateFriend(playerId, trimmed, avatarId)
            transient.value = transient.value.copy(friendError = null)
        }
    }

    fun deleteFriend(playerId: String) {
        viewModelScope.launch {
            when (playerRepository.deleteFriend(playerId)) {
                DeleteFriendResult.DELETED -> {
                    transient.value = transient.value.copy(friendError = null)
                }
                DeleteFriendResult.IN_USE_BY_MATCHES -> {
                    transient.value = transient.value.copy(
                        friendError = FriendActionError.IN_USE_BY_MATCHES
                    )
                }
                DeleteFriendResult.NOT_FOUND -> Unit
            }
        }
    }

    fun clearFriendError() {
        transient.value = transient.value.copy(friendError = null)
    }

    fun defaultBackupFilename(): String = "padel_pro_backup_${timeProvider.currentLocalDate()}.json"

    fun exportBackup(writeContent: suspend (String) -> Boolean) {
        if (transient.value.isBackupBusy) return
        transient.value = transient.value.copy(
            isBackupBusy = true,
            backupFeedback = null
        )
        viewModelScope.launch {
            val succeeded = try {
                val json = backupRepository.exportBackupJson()
                val written = withContext(Dispatchers.IO) { writeContent(json) }
                if (written) {
                    settingsRepository.setBackupLastExportedAt(timeProvider.nowMillis())
                }
                written
            } catch (_: Exception) {
                false
            }
            transient.value = transient.value.copy(
                isBackupBusy = false,
                backupFeedback = if (succeeded) {
                    ProfileBackupFeedback.ExportSuccess
                } else {
                    ProfileBackupFeedback.ExportError
                }
            )
        }
    }

    fun onImportFileRead(rawJson: String?) {
        if (rawJson == null) {
            transient.value = transient.value.copy(
                backupFeedback = ProfileBackupFeedback.ReadError
            )
            return
        }
        when (val validation = backupRepository.validateBackupJson(rawJson)) {
            is BackupValidationResult.Invalid -> {
                transient.value = transient.value.copy(
                    pendingImportPreview = null,
                    pendingImportJson = null,
                    backupFeedback = ProfileBackupFeedback.ImportValidationError(validation.error)
                )
            }
            is BackupValidationResult.Valid -> {
                transient.value = transient.value.copy(
                    pendingImportPreview = validation.preview,
                    pendingImportJson = rawJson,
                    backupFeedback = null
                )
            }
        }
    }

    fun confirmPendingImport() {
        val rawJson = transient.value.pendingImportJson ?: return
        if (transient.value.isBackupBusy) return
        transient.value = transient.value.copy(
            isBackupBusy = true,
            pendingImportPreview = null,
            pendingImportJson = null
        )
        viewModelScope.launch {
            when (val outcome = backupRepository.importBackupJson(rawJson)) {
                is BackupImportOutcome.Error -> {
                    transient.value = transient.value.copy(
                        isBackupBusy = false,
                        backupFeedback = ProfileBackupFeedback.ImportValidationError(outcome.error)
                    )
                }
                is BackupImportOutcome.Success -> {
                    transient.value = transient.value.copy(
                        isBackupBusy = false,
                        backupFeedback = ProfileBackupFeedback.ImportSuccess(
                            matchesCount = outcome.matchesCount,
                            playersCount = outcome.playersCount
                        )
                    )
                    if (!outcome.hasUserProfile) {
                        _needsOnboardingEvent.tryEmit(Unit)
                    }
                }
            }
        }
    }

    fun dismissPendingImport() {
        transient.value = transient.value.copy(
            pendingImportPreview = null,
            pendingImportJson = null
        )
    }

    fun clearBackupFeedback() {
        transient.value = transient.value.copy(backupFeedback = null)
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            playerRepository.resetAllData()
            _resetCompleteEvent.tryEmit(Unit)
        }
    }
}
