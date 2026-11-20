package com.padelpro.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,
    val displayName: String = "",
    val selectedAvatarId: Int = 0,
    val preferredFormat: MatchFormat = MatchFormat.DOUBLES,
    val isSaving: Boolean = false
) {
    val isNameValid: Boolean
        get() = displayName.trim().isNotEmpty() && displayName.trim().length <= 20
}

class OnboardingViewModel(
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _completedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completedEvent: SharedFlow<Unit> = _completedEvent.asSharedFlow()

    fun nextStep() {
        _uiState.update { state ->
            state.copy(step = (state.step + 1).coerceAtMost(2))
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            state.copy(step = (state.step - 1).coerceAtLeast(0))
        }
    }

    fun skipToProfileStep() {
        _uiState.update { it.copy(step = 2) }
    }

    fun updateName(name: String) {
        if (name.length <= 20) {
            _uiState.update { it.copy(displayName = name) }
        }
    }

    fun selectAvatar(avatarId: Int) {
        _uiState.update { it.copy(selectedAvatarId = avatarId.coerceIn(0, 11)) }
    }

    fun selectFormat(format: MatchFormat) {
        _uiState.update { it.copy(preferredFormat = format) }
    }

    fun completeOnboarding() {
        val current = _uiState.value
        if (!current.isNameValid || current.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                playerRepository.createLocalProfile(
                    displayName = current.displayName,
                    avatarId = current.selectedAvatarId,
                    preferredFormat = current.preferredFormat
                )
                _completedEvent.tryEmit(Unit)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
