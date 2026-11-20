package com.padelpro.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class HistoryFilter {
    ALL,
    WINS,
    LOSSES,
    SINGLES,
    DOUBLES
}

data class HistoryUiState(
    val matches: List<MatchSummary> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: HistoryFilter = HistoryFilter.ALL
) {
    val filteredMatches: List<MatchSummary>
        get() = matches.filter { match ->
            val matchesQuery = searchQuery.isBlank() ||
                (match.teamANames + match.teamBNames).any {
                    it.contains(searchQuery.trim(), ignoreCase = true)
                }
            val matchesFilter = when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.WINS -> match.userWon == true
                HistoryFilter.LOSSES -> match.userWon == false
                HistoryFilter.SINGLES -> match.format == MatchFormat.SINGLES
                HistoryFilter.DOUBLES -> match.format == MatchFormat.DOUBLES
            }
            matchesQuery && matchesFilter
        }
}

class HistoryViewModel(
    matchRepository: MatchRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        matchRepository.observeCompletedMatches(),
        searchQuery,
        selectedFilter
    ) { matches, query, filter ->
        HistoryUiState(
            matches = matches,
            searchQuery = query,
            selectedFilter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun updateSearch(query: String) {
        searchQuery.value = query
    }

    fun selectFilter(filter: HistoryFilter) {
        selectedFilter.value = filter
    }

    fun clearFilters() {
        searchQuery.value = ""
        selectedFilter.value = HistoryFilter.ALL
    }

    fun formatMatchDate(epochMillis: Long): String = timeProvider.formatShortDate(epochMillis)
}
