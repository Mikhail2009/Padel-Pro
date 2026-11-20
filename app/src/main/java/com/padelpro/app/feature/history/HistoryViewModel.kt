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
import java.util.Locale

enum class HistoryResultFilter {
    ALL,
    WINS,
    LOSSES
}

enum class HistoryFormatFilter {
    ALL,
    SINGLES,
    DOUBLES
}

data class MonthMatchGroup(
    val monthLabel: String,
    val matches: List<MatchSummary>
)

data class HistoryUiState(
    val matches: List<MatchSummary> = emptyList(),
    val searchQuery: String = "",
    val resultFilter: HistoryResultFilter = HistoryResultFilter.ALL,
    val formatFilter: HistoryFormatFilter = HistoryFormatFilter.ALL,
    val groupedFilteredMatches: List<MonthMatchGroup> = emptyList(),
    val filteredCount: Int = 0
) {
    val isAllSelected: Boolean
        get() = resultFilter == HistoryResultFilter.ALL && formatFilter == HistoryFormatFilter.ALL
}

class HistoryViewModel(
    matchRepository: MatchRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val ruLocale = Locale.forLanguageTag("ru")
    private val searchQuery = MutableStateFlow("")
    private val resultFilter = MutableStateFlow(HistoryResultFilter.ALL)
    private val formatFilter = MutableStateFlow(HistoryFormatFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        matchRepository.observeCompletedMatches(),
        searchQuery,
        resultFilter,
        formatFilter
    ) { matches, query, resFilter, fmtFilter ->
        val trimmed = query.trim()
        val filtered = matches.filter { match ->
            val matchesQuery = trimmed.isEmpty() ||
                (match.teamANames + match.teamBNames).any { name ->
                    name.contains(trimmed, ignoreCase = true)
                }
            val matchesResult = when (resFilter) {
                HistoryResultFilter.ALL -> true
                HistoryResultFilter.WINS -> match.userWon == true
                HistoryResultFilter.LOSSES -> match.userWon == false
            }
            val matchesFormat = when (fmtFilter) {
                HistoryFormatFilter.ALL -> true
                HistoryFormatFilter.SINGLES -> match.format == MatchFormat.SINGLES
                HistoryFormatFilter.DOUBLES -> match.format == MatchFormat.DOUBLES
            }
            matchesQuery && matchesResult && matchesFormat
        }

        val groups = filtered
            .groupBy { match ->
                timeProvider.formatFullMonthYear(match.endedAt ?: match.startedAt)
                    .uppercase(ruLocale)
            }
            .map { (monthLabel, groupMatches) ->
                MonthMatchGroup(monthLabel = monthLabel, matches = groupMatches)
            }

        HistoryUiState(
            matches = matches,
            searchQuery = query,
            resultFilter = resFilter,
            formatFilter = fmtFilter,
            groupedFilteredMatches = groups,
            filteredCount = filtered.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun updateSearch(query: String) {
        searchQuery.value = query
    }

    fun selectAllFilters() {
        resultFilter.value = HistoryResultFilter.ALL
        formatFilter.value = HistoryFormatFilter.ALL
    }

    fun toggleResultFilter(target: HistoryResultFilter) {
        resultFilter.value = if (resultFilter.value == target) {
            HistoryResultFilter.ALL
        } else {
            target
        }
    }

    fun toggleFormatFilter(target: HistoryFormatFilter) {
        formatFilter.value = if (formatFilter.value == target) {
            HistoryFormatFilter.ALL
        } else {
            target
        }
    }

    fun clearFilters() {
        searchQuery.value = ""
        resultFilter.value = HistoryResultFilter.ALL
        formatFilter.value = HistoryFormatFilter.ALL
    }

    fun formatMatchDate(epochMillis: Long): String = timeProvider.formatShortDate(epochMillis)
}
