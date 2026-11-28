package com.padelpro.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.domain.model.MatchDetailsData
import com.padelpro.app.domain.repository.MatchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MatchInsightsUiState(
    val isLoading: Boolean = true,
    val details: MatchDetailsData? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MatchInsightsViewModel(
    private val matchRepository: MatchRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val currentMatchId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MatchInsightsUiState> = currentMatchId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(MatchInsightsUiState(isLoading = true, details = null))
            } else {
                matchRepository.observeMatchDetails(id).map { details ->
                    MatchInsightsUiState(isLoading = false, details = details)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MatchInsightsUiState()
        )

    fun loadMatch(matchId: String) {
        if (currentMatchId.value != matchId) {
            currentMatchId.value = matchId
        }
    }

    fun formatDateTime(epochMillis: Long): String = timeProvider.formatShortDateTime(epochMillis)
}
