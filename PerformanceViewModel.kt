package com.padelpro.app.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.domain.model.PerformanceSummary
import com.padelpro.app.domain.repository.StatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PerformanceViewModel(
    statsRepository: StatsRepository
) : ViewModel() {

    val uiState: StateFlow<PerformanceSummary> = statsRepository.observePerformanceSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PerformanceSummary()
        )
}
