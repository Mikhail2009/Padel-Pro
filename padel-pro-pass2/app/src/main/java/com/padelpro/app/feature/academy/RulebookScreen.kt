package com.padelpro.app.feature.academy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpCourtGraphic
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpFilterChip
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpTextField
import com.padelpro.app.core.designsystem.components.accentRail
import com.padelpro.app.core.designsystem.components.glassPanel
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpCourtPanel
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.RulebookCategory
import com.padelpro.app.domain.model.RulebookItem

@Composable
fun RulebookScreen(
    viewModel: AcademyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expandedRuleIds by remember { mutableStateOf(emptySet<String>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                        onClick = onBack,
                        modifier = Modifier.testTag("rulebook_back_button")
                    )
                    Spacer(Modifier.width(PpSpacing.sm))
                    Text(
                        text = stringResource(R.string.rulebook_title),
                        style = PpTextStyles.H1,
                        color = PpTextHigh
                    )
                }
            }

            item {
                PpTextField(
                    value = state.rulebookSearch,
                    onValueChange = viewModel::updateRulebookSearch,
                    placeholder = stringResource(R.string.rulebook_search_hint),
                    leadingIcon = Icons.Rounded.Search,
                    modifier = Modifier.testTag("rulebook_search_input")
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
                                        tint = if (state.rulebookCategory == cat) PpAmberLed else PpTextMid,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("rulebook_chip_${cat.name.lowercase()}")
                        )
                    }
                }
            }

            if (state.rulebookItems.isEmpty()) {
                item {
                    Spacer(Modifier.height(PpSpacing.md))
                    val isFavoritesEmpty = state.rulebookCategory == RulebookCategory.FAVORITES && state.rulebookSearch.isBlank()
                    val isFiltering = state.rulebookSearch.isNotBlank() || state.rulebookCategory != RulebookCategory.ALL
                    PpEmptyState(
                        icon = when {
                            isFavoritesEmpty -> Icons.Rounded.StarOutline
                            isFiltering -> Icons.Rounded.Search
                            else -> Icons.Rounded.MenuBook
                        },
                        title = when {
                            isFavoritesEmpty -> stringResource(R.string.rulebook_favorites_empty_title)
                            isFiltering -> stringResource(R.string.rulebook_empty_search)
                            else -> stringResource(R.string.rulebook_empty_title)
                        },
                        subtitle = if (isFavoritesEmpty) {
                            stringResource(R.string.rulebook_favorites_empty_body)
                        } else {
                            stringResource(R.string.rulebook_empty_body)
                        }
                    )
                }
            } else {
                items(state.rulebookItems, key = { it.id }) { item ->
                    val expanded = item.id in expandedRuleIds
                    RulebookCard(
                        item = item,
                        expanded = expanded,
                        onToggleExpand = {
                            expandedRuleIds = if (expanded) {
                                expandedRuleIds - item.id
                            } else {
                                expandedRuleIds + item.id
                            }
                        },
                        onToggleFavorite = { viewModel.toggleRuleFavorite(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RulebookCard(
    item: RulebookItem,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rule_card_${item.id}")
            .glassPanel(
                radius = PpRadius.card,
                fill = SolidColor(PpCourtPanel),
                edge = SolidColor(if (expanded) PpLime.copy(alpha = 0.45f) else PpCourtLine)
            )
            .then(if (expanded) Modifier.accentRail(PpGlassCyan) else Modifier)
            .clickable(onClick = onToggleExpand)
            .padding(PpSpacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(PpRadius.pill)
                        .background(Color(0x1AC2F54C))
                        .border(1.dp, PpLime.copy(alpha = 0.45f), PpRadius.pill)
                        .padding(horizontal = PpSpacing.sm, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(rulebookCategoryLabelRes(item.category)),
                        style = PpTextStyles.Overline,
                        color = PpLime
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(PpRadius.pill)
                        .background(Color(0x1A72B7F0))
                        .border(1.dp, PpGlassCyan.copy(alpha = 0.55f), PpRadius.pill)
                        .padding(horizontal = PpSpacing.sm, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.rulebook_fip_ref_format, item.fipRef),
                        style = PpTextStyles.Overline,
                        color = PpGlassCyan
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("rule_favorite_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = stringResource(
                            if (item.isFavorite) R.string.rulebook_cd_favorite_remove
                            else R.string.rulebook_cd_favorite_add
                        ),
                        tint = if (item.isFavorite) PpAmberLed else PpTextLow
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (expanded) R.string.rulebook_cd_collapse else R.string.rulebook_cd_expand
                    ),
                    tint = PpTextMid,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = item.title,
            style = PpTextStyles.Title,
            color = PpTextHigh
        )

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = PpSpacing.md)) {
                if (item.diagramId != null) {
                    PpCourtGraphic(
                        diagramId = item.diagramId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                    )
                    Spacer(Modifier.height(PpSpacing.md))
                }
                Text(
                    text = item.body,
                    style = PpTextStyles.Body,
                    color = PpTextMid
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
