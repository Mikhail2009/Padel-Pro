package com.padelpro.app.feature.academy

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpFilterChip
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpTextField
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.RulebookCategory

@Composable
fun RulebookScreen(
    viewModel: AcademyViewModel,
    onBack: () -> Unit
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PpIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack
                    )
                    Spacer(Modifier.width(PpSpacing.sm))
                    Text(
                        text = stringResource(R.string.rulebook_title),
                        style = PpTextStyles.H1,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            item {
                PpTextField(
                    value = state.rulebookSearch,
                    onValueChange = viewModel::updateRulebookSearch,
                    placeholder = stringResource(R.string.rulebook_search_hint),
                    leadingIcon = Icons.Rounded.Search
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(RulebookCategory.entries) { cat ->
                        PpFilterChip(
                            label = stringResource(rulebookCategoryLabelRes(cat)),
                            selected = state.rulebookCategory == cat,
                            onClick = { viewModel.selectRulebookCategory(cat) },
                            leadingContent = if (cat == RulebookCategory.FAVORITES) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = if (state.rulebookCategory == cat) PpPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(PpSpacing.md))
                val isFiltering = state.rulebookSearch.isNotBlank() || state.rulebookCategory != RulebookCategory.ALL
                PpEmptyState(
                    icon = if (isFiltering) Icons.Rounded.Search else Icons.Rounded.MenuBook,
                    title = if (isFiltering) stringResource(R.string.rulebook_empty_search)
                    else stringResource(R.string.rulebook_empty_title),
                    subtitle = stringResource(R.string.rulebook_empty_body)
                )
            }
        }
    }
}

private fun rulebookCategoryLabelRes(category: RulebookCategory): Int = when (category) {
    RulebookCategory.ALL -> R.string.rulebook_cat_all
    RulebookCategory.COURT -> R.string.rulebook_cat_court
    RulebookCategory.EQUIPMENT -> R.string.rulebook_cat_equipment
    RulebookCategory.SCORING -> R.string.rulebook_cat_scoring
    RulebookCategory.SERVING -> R.string.rulebook_cat_serving
    RulebookCategory.WALLS -> R.string.rulebook_cat_walls
    RulebookCategory.FOULS -> R.string.rulebook_cat_fouls
    RulebookCategory.FAVORITES -> R.string.rulebook_cat_favorites
}
