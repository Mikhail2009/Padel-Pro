package com.padelpro.app.feature.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.data.repository.MatchRepositoryImpl
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.repository.MatchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MatchResultsUiState(
    val isLoading: Boolean = true,
    val match: MatchSummary? = null,
    val xpEarned: Int = 0
)

class MatchResultsViewModel(
    private val matchRepository: MatchRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val matchIdFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MatchResultsUiState> = matchIdFlow
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) {
                flowOf(MatchResultsUiState(isLoading = false, match = null))
            } else {
                matchRepository.observeMatchById(id).map { summary ->
                    val xp = when {
                        summary == null -> 0
                        summary.isManuallyCompleted && summary.isUserParticipant ->
                            MatchRepositoryImpl.XP_MATCH_MANUAL_PARTICIPATION
                        summary.userWon == true ->
                            MatchRepositoryImpl.XP_MATCH_PARTICIPATION + MatchRepositoryImpl.XP_MATCH_WIN_BONUS
                        summary.userWon == false ->
                            MatchRepositoryImpl.XP_MATCH_PARTICIPATION
                        else -> 0
                    }
                    MatchResultsUiState(
                        isLoading = false,
                        match = summary,
                        xpEarned = xp
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MatchResultsUiState()
        )

    fun loadMatch(matchId: String) {
        if (matchIdFlow.value != matchId) {
            matchIdFlow.value = matchId
        }
    }

    fun formatDate(epochMillis: Long): String = timeProvider.formatShortDate(epochMillis)
}
