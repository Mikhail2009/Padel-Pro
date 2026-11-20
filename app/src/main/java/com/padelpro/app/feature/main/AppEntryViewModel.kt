package com.padelpro.app.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface AppEntryState {
    data object Loading : AppEntryState
    data object NeedsOnboarding : AppEntryState
    data object Ready : AppEntryState
}

class AppEntryViewModel(
    settingsRepository: SettingsRepository,
    playerRepository: PlayerRepository
) : ViewModel() {

    val entryState: StateFlow<AppEntryState> = combine(
        settingsRepository.settings,
        playerRepository.observeUserProfile()
    ) { settings, user ->
        if (settings.onboardingCompleted && user != null) {
            AppEntryState.Ready
        } else {
            AppEntryState.NeedsOnboarding
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppEntryState.Loading
    )
}
