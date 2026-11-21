package com.padelpro.app.feature.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.datastore.AppSettings
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.domain.engine.SideSwap
import com.padelpro.app.domain.model.LiveMatchSnapshot
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.TeamSide
import com.padelpro.app.domain.repository.MatchRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface LiveMatchNavEvent {
    data class Finished(val matchId: String) : LiveMatchNavEvent
    data object Cancelled : LiveMatchNavEvent
}

data class LiveMatchUiState(
    val isLoading: Boolean = true,
    val snapshot: LiveMatchSnapshot? = null,
    val elapsedMillis: Long = 0L,
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val autoSideSwap: Boolean = true,
    val swapped: Boolean = false,
    val recentPoints: List<Boolean> = emptyList(),
    val isMutating: Boolean = false
)

class LiveMatchViewModel(
    private val matchRepository: MatchRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val clockMillis = MutableStateFlow(timeProvider.nowMillis())
    private val isMutating = MutableStateFlow(false)
    private val _manualSwapTaps = MutableStateFlow(0)
    val manualSwapTaps: StateFlow<Int> = _manualSwapTaps.asStateFlow()

    private val _navEvents = MutableSharedFlow<LiveMatchNavEvent>(extraBufferCapacity = 2)
    val navEvents: SharedFlow<LiveMatchNavEvent> = _navEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                clockMillis.value = timeProvider.nowMillis()
                delay(500L)
            }
        }
    }

    val uiState: StateFlow<LiveMatchUiState> = combine(
        matchRepository.observeActiveMatchLive(),
        settingsRepository.settings,
        clockMillis,
        isMutating,
        _manualSwapTaps
    ) { snapshot: LiveMatchSnapshot?, settings: AppSettings, now: Long, mutating: Boolean, swapTaps: Int ->
        val elapsed = snapshot?.state?.elapsedDurationMillis(now) ?: 0L
        val swapped = snapshot?.state?.let { engineState ->
            SideSwap.isSwapped(engineState, settings.autoSideSwap, swapTaps)
        } ?: false
        LiveMatchUiState(
            isLoading = false,
            snapshot = snapshot,
            elapsedMillis = elapsed,
            hapticsEnabled = settings.hapticsEnabled,
            keepScreenOn = settings.keepScreenOn,
            autoSideSwap = settings.autoSideSwap,
            swapped = swapped,
            recentPoints = snapshot?.recentPoints.orEmpty(),
            isMutating = mutating
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LiveMatchUiState()
    )

    fun scorePoint(team: TeamSide) {
        val current = uiState.value
        val snap = current.snapshot ?: return
        if (current.isMutating || snap.state.isPaused || snap.state.status != MatchStatus.ACTIVE) {
            return
        }
        viewModelScope.launch {
            isMutating.value = true
            try {
                if (snap.wasRestored) {
                    matchRepository.dismissRestoredBanner(snap.matchId)
                }
                val completion = matchRepository.addPoint(team)
                clockMillis.value = timeProvider.nowMillis()
                if (completion != null) {
                    _navEvents.emit(LiveMatchNavEvent.Finished(completion.matchId))
                }
            } finally {
                isMutating.value = false
            }
        }
    }

    fun undoLastPoint() {
        val current = uiState.value
        val snap = current.snapshot ?: return
        if (current.isMutating || !snap.canUndo) return
        viewModelScope.launch {
            isMutating.value = true
            try {
                matchRepository.undoLastPoint()
                clockMillis.value = timeProvider.nowMillis()
            } finally {
                isMutating.value = false
            }
        }
    }

    fun togglePause() {
        val current = uiState.value
        val snap = current.snapshot ?: return
        if (current.isMutating || snap.state.status != MatchStatus.ACTIVE) return
        viewModelScope.launch {
            isMutating.value = true
            try {
                if (snap.state.isPaused) {
                    matchRepository.resumeActiveMatch()
                } else {
                    matchRepository.pauseActiveMatch()
                }
                clockMillis.value = timeProvider.nowMillis()
            } finally {
                isMutating.value = false
            }
        }
    }

    fun swapSidesManually() {
        _manualSwapTaps.value += 1
    }

    fun cancelMatchEarly() {
        if (isMutating.value) return
        viewModelScope.launch {
            isMutating.value = true
            try {
                matchRepository.cancelActiveMatch()
                _navEvents.emit(LiveMatchNavEvent.Cancelled)
            } finally {
                isMutating.value = false
            }
        }
    }

    fun dismissRestoredBanner() {
        val matchId = uiState.value.snapshot?.matchId ?: return
        matchRepository.dismissRestoredBanner(matchId)
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

    fun setAutoSideSwap(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSideSwap(enabled)
        }
    }
}
