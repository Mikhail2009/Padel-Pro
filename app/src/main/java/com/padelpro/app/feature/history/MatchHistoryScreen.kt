package com.padelpro.app.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpFilterChip
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpMatchCard
import com.padelpro.app.core.designsystem.components.PpTextField
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun MatchHistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onStartMatch: () -> Unit,
    onOpenMatchDetails: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PpBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(PpSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PpIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            onClick = onBack
                        )
                        Spacer(Modifier.width(PpSpacing.sm))
                        Text(
                            text = stringResource(R.string.history_title),
                            style = PpTextStyles.H1,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = pluralStringResource(
                            R.plurals.history_count_format,
                            state.matches.size,
                            state.matches.size
                        ),
                        style = PpTextStyles.Micro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                PpTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearch,
                    placeholder = stringResource(R.string.history_search_hint),
                    leadingIcon = Icons.Rounded.Search
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(HistoryFilter.entries) { filter ->
                        PpFilterChip(
                            label = stringResource(filterLabelRes(filter)),
                            selected = state.selectedFilter == filter,
                            onClick = { viewModel.selectFilter(filter) }
                        )
                    }
                }
            }

            if (state.matches.isEmpty()) {
                item {
                    PpEmptyState(
                        icon = Icons.Rounded.SportsTennis,
                        title = stringResource(R.string.history_empty_title),
                        subtitle = stringResource(R.string.history_empty_body),
                        ctaText = stringResource(R.string.action_start_a_match),
                        onCtaClick = onStartMatch
                    )
                }
            } else if (state.filteredMatches.isEmpty()) {
                item {
                    PpEmptyState(
                        icon = Icons.Rounded.Search,
                        title = stringResource(R.string.history_empty_filter_title),
                        subtitle = stringResource(R.string.history_empty_body),
                        ctaText = stringResource(R.string.action_clear_filters),
                        onCtaClick = viewModel::clearFilters
                    )
                }
            } else {
                items(state.filteredMatches, key = { it.id }) { match ->
                    PpMatchCard(
                        match = match,
                        dateText = viewModel.formatMatchDate(match.endedAt ?: match.startedAt),
                        onClick = { onOpenMatchDetails(match.id) }
                    )
                }
            }
        }
    }
}

private fun filterLabelRes(filter: HistoryFilter): Int = when (filter) {
    HistoryFilter.ALL -> R.string.history_filter_all
    HistoryFilter.WINS -> R.string.history_filter_wins
    HistoryFilter.LOSSES -> R.string.history_filter_losses
    HistoryFilter.SINGLES -> R.string.history_filter_singles
    HistoryFilter.DOUBLES -> R.string.history_filter_doubles
}
