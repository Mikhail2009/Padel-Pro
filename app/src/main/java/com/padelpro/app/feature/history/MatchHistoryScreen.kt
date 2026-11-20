package com.padelpro.app.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpFilterChip
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpMatchCard
import com.padelpro.app.core.designsystem.components.PpSectionHeader
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

    BackHandler(onBack = onBack)

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
                            onClick = onBack,
                            modifier = Modifier.testTag("history_back_button")
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
                            state.filteredCount,
                            state.filteredCount
                        ),
                        style = PpTextStyles.Micro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("history_count_label")
                    )
                }
            }

            item {
                PpTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearch,
                    placeholder = stringResource(R.string.history_search_hint),
                    leadingIcon = Icons.Rounded.Search,
                    modifier = Modifier.testTag("history_search_input")
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        PpFilterChip(
                            label = stringResource(R.string.history_filter_all),
                            selected = state.isAllSelected,
                            onClick = viewModel::selectAllFilters,
                            modifier = Modifier.testTag("history_filter_all")
                        )
                    }
                    item {
                        PpFilterChip(
                            label = stringResource(R.string.history_filter_wins),
                            selected = state.resultFilter == HistoryResultFilter.WINS,
                            onClick = { viewModel.toggleResultFilter(HistoryResultFilter.WINS) },
                            modifier = Modifier.testTag("history_filter_wins")
                        )
                    }
                    item {
                        PpFilterChip(
                            label = stringResource(R.string.history_filter_losses),
                            selected = state.resultFilter == HistoryResultFilter.LOSSES,
                            onClick = { viewModel.toggleResultFilter(HistoryResultFilter.LOSSES) },
                            modifier = Modifier.testTag("history_filter_losses")
                        )
                    }
                    item {
                        PpFilterChip(
                            label = stringResource(R.string.history_filter_singles),
                            selected = state.formatFilter == HistoryFormatFilter.SINGLES,
                            onClick = { viewModel.toggleFormatFilter(HistoryFormatFilter.SINGLES) },
                            modifier = Modifier.testTag("history_filter_singles")
                        )
                    }
                    item {
                        PpFilterChip(
                            label = stringResource(R.string.history_filter_doubles),
                            selected = state.formatFilter == HistoryFormatFilter.DOUBLES,
                            onClick = { viewModel.toggleFormatFilter(HistoryFormatFilter.DOUBLES) },
                            modifier = Modifier.testTag("history_filter_doubles")
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
                        onCtaClick = onStartMatch,
                        modifier = Modifier.testTag("history_empty_state")
                    )
                }
            } else if (state.groupedFilteredMatches.isEmpty()) {
                item {
                    PpEmptyState(
                        icon = Icons.Rounded.Search,
                        title = stringResource(R.string.history_empty_filter_title),
                        subtitle = stringResource(R.string.history_empty_filter_body),
                        ctaText = stringResource(R.string.action_clear_filters),
                        onCtaClick = viewModel::clearFilters,
                        modifier = Modifier.testTag("history_empty_filter_state")
                    )
                }
            } else {
                state.groupedFilteredMatches.forEach { group ->
                    item(key = "header_${group.monthLabel}") {
                        PpSectionHeader(
                            title = group.monthLabel,
                            modifier = Modifier.padding(top = PpSpacing.xs)
                        )
                    }
                    items(group.matches, key = { it.id }) { match ->
                        PpMatchCard(
                            match = match,
                            dateText = viewModel.formatMatchDate(match.endedAt ?: match.startedAt),
                            onClick = { onOpenMatchDetails(match.id) },
                            modifier = Modifier.testTag("history_match_card_${match.id}")
                        )
                    }
                }
            }
        }
    }
}
