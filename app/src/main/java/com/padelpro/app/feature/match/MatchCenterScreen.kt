package com.padelpro.app.feature.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpBottomSheet
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpCourtGraphic
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpMatchCard
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.MatchFormat

@Composable
fun MatchCenterScreen(
    viewModel: MatchViewModel,
    listState: LazyListState,
    onOpenSetup: (MatchFormat) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMatchDetails: (String) -> Unit,
    onResumeMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFormatSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(PpSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
        ) {
            item {
                Text(
                    text = stringResource(R.string.match_center_title),
                    style = PpTextStyles.H1,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (state.activeMatch != null) {
                item {
                    PpCard(
                        borderColor = PpPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.home_resume_match_title),
                                style = PpTextStyles.Micro,
                                color = PpPrimary
                            )
                            PpPrimaryButton(
                                text = stringResource(R.string.home_resume_cta),
                                onClick = onResumeMatch,
                                fullWidth = false
                            )
                        }
                    }
                }
            }

            item {
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    PpCourtGraphic(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                    Spacer(Modifier.height(PpSpacing.md))
                    Text(
                        text = stringResource(R.string.match_center_start_sub),
                        style = PpTextStyles.Label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(PpSpacing.md))
                    PpPrimaryButton(
                        text = stringResource(R.string.match_center_start_new),
                        icon = Icons.Rounded.SportsTennis,
                        onClick = { showFormatSheet = true },
                        modifier = Modifier.testTag("match_center_start_button")
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    FormatModeCard(
                        title = stringResource(R.string.match_center_singles),
                        meta = stringResource(R.string.match_center_singles_meta),
                        badge = null,
                        icon = Icons.Rounded.Person,
                        onClick = { onOpenSetup(MatchFormat.SINGLES) },
                        modifier = Modifier.weight(1f)
                    )
                    FormatModeCard(
                        title = stringResource(R.string.match_center_doubles),
                        meta = stringResource(R.string.match_center_doubles_meta),
                        badge = stringResource(R.string.match_center_most_popular),
                        icon = Icons.Rounded.Group,
                        onClick = { onOpenSetup(MatchFormat.DOUBLES) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                PpSectionHeader(
                    title = stringResource(R.string.match_center_recent_matches),
                    actionText = stringResource(R.string.match_center_view_all),
                    onActionClick = onOpenHistory
                )
            }

            if (state.recentMatches.isEmpty()) {
                item {
                    PpEmptyState(
                        icon = Icons.Rounded.SportsTennis,
                        title = stringResource(R.string.history_empty_title),
                        subtitle = stringResource(R.string.match_center_empty_recent)
                    )
                }
            } else {
                items(state.recentMatches, key = { it.id }) { match ->
                    PpMatchCard(
                        match = match,
                        dateText = viewModel.formatMatchDate(match.endedAt ?: match.startedAt),
                        onClick = { onOpenMatchDetails(match.id) }
                    )
                }
            }
        }
    }

    if (showFormatSheet) {
        PpBottomSheet(
            onDismissRequest = { showFormatSheet = false },
            title = stringResource(R.string.match_center_format_sheet_title)
        ) {
            PpPrimaryButton(
                text = "${stringResource(R.string.match_center_doubles)} (${stringResource(R.string.match_center_doubles_meta)})",
                onClick = {
                    showFormatSheet = false
                    onOpenSetup(MatchFormat.DOUBLES)
                }
            )
            Spacer(Modifier.height(PpSpacing.md))
            PpSecondaryButton(
                text = "${stringResource(R.string.match_center_singles)} (${stringResource(R.string.match_center_singles_meta)})",
                onClick = {
                    showFormatSheet = false
                    onOpenSetup(MatchFormat.SINGLES)
                }
            )
        }
    }
}

@Composable
private fun FormatModeCard(
    title: String,
    meta: String,
    badge: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PpCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, PpOutline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PpPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(PpSpacing.md))
        Text(
            text = title,
            style = PpTextStyles.Title,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = meta,
            style = PpTextStyles.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (badge != null) {
            Spacer(Modifier.height(PpSpacing.sm))
            Box(
                modifier = Modifier
                    .clip(PpRadius.pill)
                    .background(PpPrimaryTint)
                    .padding(horizontal = PpSpacing.sm, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    style = PpTextStyles.Micro,
                    color = PpPrimary
                )
            }
        }
    }
}
