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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpDangerOutlineButton
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.components.PpSetScoreRow
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun MatchDetailsScreen(
    matchId: String,
    onBack: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PpIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack
                    )
                    Spacer(Modifier.width(PpSpacing.sm))
                    Text(
                        text = stringResource(R.string.details_title),
                        style = PpTextStyles.H1,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            item {
                PpEmptyState(
                    icon = Icons.Rounded.SportsTennis,
                    title = stringResource(R.string.details_empty_title),
                    subtitle = stringResource(R.string.details_empty_body)
                )
            }

            item {
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.details_table_team),
                            style = PpTextStyles.Micro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.details_table_set1),
                            style = PpTextStyles.Micro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(44.dp)
                        )
                        Text(
                            text = stringResource(R.string.details_table_set2),
                            style = PpTextStyles.Micro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(44.dp)
                        )
                        Text(
                            text = stringResource(R.string.details_table_set3),
                            style = PpTextStyles.Micro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(44.dp)
                        )
                    }
                    Spacer(Modifier.height(PpSpacing.sm))
                    PpSetScoreRow(
                        teamLabel = stringResource(R.string.live_match_team_a_placeholder),
                        setScores = listOf("—", "—", "—"),
                        isWinner = false
                    )
                    Spacer(Modifier.height(PpSpacing.xs))
                    PpSetScoreRow(
                        teamLabel = stringResource(R.string.live_match_team_b_placeholder),
                        setScores = listOf("—", "—", "—"),
                        isWinner = false
                    )
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.details_section_stats))
                Spacer(Modifier.height(PpSpacing.sm))
                PpDangerOutlineButton(
                    text = stringResource(R.string.details_delete_btn),
                    icon = Icons.Rounded.DeleteOutline,
                    onClick = {},
                    enabled = false
                )
            }
        }
    }
}
