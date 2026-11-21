package com.padelpro.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.domain.model.MatchDetailsData
import com.padelpro.app.domain.repository.MatchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MatchDetailsUiState(
    val isLoading: Boolean = true,
    val details: MatchDetailsData? = null,
    val isDeleting: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MatchDetailsViewModel(
    private val matchRepository: MatchRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val currentMatchId = MutableStateFlow<String?>(null)
    private val isDeleting = MutableStateFlow(false)

    private val _deletedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deletedEvent: SharedFlow<Unit> = _deletedEvent.asSharedFlow()

    val uiState: StateFlow<MatchDetailsUiState> = combine(
        currentMatchId.flatMapLatest { id ->
            if (id == null) {
                flowOf(true to null)
            } else {
                matchRepository.observeMatchDetails(id).map { details -> false to details }
            }
        },
        isDeleting
    ) { (loading, details), deleting ->
        MatchDetailsUiState(
            isLoading = loading,
            details = details,
            isDeleting = deleting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MatchDetailsUiState()
    )

    fun loadMatch(matchId: String) {
        if (currentMatchId.value != matchId) {
            currentMatchId.value = matchId
        }
    }

    fun deleteMatch() {
        val matchId = currentMatchId.value ?: return
        if (isDeleting.value) return
        isDeleting.value = true
        viewModelScope.launch {
            val deleted = matchRepository.deleteMatch(matchId)
            isDeleting.value = false
            if (deleted) {
                _deletedEvent.tryEmit(Unit)
            }
        }
    }

    fun formatDateTime(epochMillis: Long): String = timeProvider.formatShortDateTime(epochMillis)
}
