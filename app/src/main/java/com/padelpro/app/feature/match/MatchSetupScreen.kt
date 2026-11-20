package com.padelpro.app.feature.match

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpAvatar
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpConfirmDialog
import com.padelpro.app.core.designsystem.components.PpFilterChip
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.Player

@Composable
fun MatchSetupScreen(
    initialFormat: String,
    viewModel: MatchViewModel,
    onBack: () -> Unit,
    onStartMatch: () -> Unit,
    onResumeActiveMatch: () -> Unit = onStartMatch
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activePickerSlot by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(initialFormat) {
        runCatching { MatchFormat.valueOf(initialFormat) }
            .getOrNull()
            ?.let { viewModel.setFormat(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                MatchSetupNavEvent.NavigateToLiveMatch -> onStartMatch()
            }
        }
    }

    val visibleSlotIndices = if (state.selectedFormat == MatchFormat.SINGLES) {
        listOf(0, 2)
    } else {
        listOf(0, 1, 2, 3)
    }
    val assignedIds = visibleSlotIndices.mapNotNull { state.playerForSlot(it)?.id }.toSet()

    Scaffold(
        containerColor = PpBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PpBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = PpSpacing.lg, vertical = PpSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (state.canStartMatch) {
                        stringResource(R.string.match_setup_ready_hint)
                    } else {
                        stringResource(R.string.match_setup_fill_all_hint)
                    },
                    style = PpTextStyles.Label,
                    color = if (state.canStartMatch) PpPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(PpSpacing.sm))
                PpPrimaryButton(
                    text = stringResource(R.string.match_setup_start_btn),
                    onClick = { viewModel.startMatch(cancelExisting = false) },
                    enabled = state.canStartMatch,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .testTag("match_setup_start_button")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                            onClick = onBack,
                            modifier = Modifier.testTag("match_setup_back_button")
                        )
                        Spacer(Modifier.width(PpSpacing.sm))
                        Text(
                            text = stringResource(R.string.match_setup_title),
                            style = PpTextStyles.H1,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
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
                                    color = PpPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                PpSecondaryButton(
                                    text = stringResource(R.string.match_setup_resume_existing_btn),
                                    onClick = onResumeActiveMatch,
                                    fullWidth = false
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                    ) {
                        PpFilterChip(
                            label = stringResource(R.string.match_center_singles),
                            selected = state.selectedFormat == MatchFormat.SINGLES,
                            onClick = { viewModel.setFormat(MatchFormat.SINGLES) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("match_setup_format_singles")
                        )
                        PpFilterChip(
                            label = stringResource(R.string.match_center_doubles),
                            selected = state.selectedFormat == MatchFormat.DOUBLES,
                            onClick = { viewModel.setFormat(MatchFormat.DOUBLES) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("match_setup_format_doubles")
                        )
                    }
                }

                item {
                    PpCard(
                        borderColor = PpPrimary.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.match_setup_team_a),
                            style = PpTextStyles.Micro,
                            color = PpPrimary
                        )
                        Spacer(Modifier.height(PpSpacing.md))
                        PlayerSlotRow(
                            player = state.playerForSlot(0),
                            onClick = { activePickerSlot = 0 },
                            modifier = Modifier.testTag("match_setup_slot_a1")
                        )
                        if (state.selectedFormat == MatchFormat.DOUBLES) {
                            Spacer(Modifier.height(PpSpacing.sm))
                            PlayerSlotRow(
                                player = state.playerForSlot(1),
                                onClick = { activePickerSlot = 1 },
                                modifier = Modifier.testTag("match_setup_slot_a2")
                            )
                        }
                    }

                    Spacer(Modifier.height(PpSpacing.sm))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(PpRadius.pill)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, PpOutline, PpRadius.pill)
                                .padding(horizontal = PpSpacing.md, vertical = PpSpacing.xs)
                        ) {
                            Text(
                                text = stringResource(R.string.label_vs),
                                style = PpTextStyles.Micro,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(PpSpacing.sm))

                    PpCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.match_setup_team_b),
                            style = PpTextStyles.Micro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(PpSpacing.md))
                        PlayerSlotRow(
                            player = state.playerForSlot(2),
                            onClick = { activePickerSlot = 2 },
                            modifier = Modifier.testTag("match_setup_slot_b1")
                        )
                        if (state.selectedFormat == MatchFormat.DOUBLES) {
                            Spacer(Modifier.height(PpSpacing.sm))
                            PlayerSlotRow(
                                player = state.playerForSlot(3),
                                onClick = { activePickerSlot = 3 },
                                modifier = Modifier.testTag("match_setup_slot_b2")
                            )
                        }
                    }
                }

                item {
                    PpSectionHeader(title = stringResource(R.string.match_setup_first_server))
                    Spacer(Modifier.height(PpSpacing.sm))
                    val serverSlots = if (state.selectedFormat == MatchFormat.SINGLES) {
                        listOf(0 to 0, 1 to 2)
                    } else {
                        listOf(0 to 0, 1 to 1, 2 to 2, 3 to 3)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)
                    ) {
                        serverSlots.forEach { (serverIdx, internalSlot) ->
                            val player = state.playerForSlot(internalSlot)
                            val selected = state.firstServerIndex == serverIdx
                            val label = player?.displayName
                                ?: stringResource(R.string.match_setup_server_slot_placeholder, serverIdx + 1)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(PpRadius.input))
                                    .background(if (selected) PpPrimaryTint else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) PpPrimary else PpOutline,
                                        shape = RoundedCornerShape(PpRadius.input)
                                    )
                                    .clickable { viewModel.setFirstServerIndex(serverIdx) }
                                    .padding(vertical = PpSpacing.sm, horizontal = PpSpacing.xs),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                PpAvatar(
                                    avatarId = player?.avatarId ?: serverIdx,
                                    size = 44.dp,
                                    highlighted = selected
                                )
                                Spacer(Modifier.height(PpSpacing.xs))
                                Text(
                                    text = label,
                                    style = PpTextStyles.Label,
                                    color = if (selected) PpPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                item {
                    PpSectionHeader(title = stringResource(R.string.match_setup_scoring))
                    Spacer(Modifier.height(PpSpacing.sm))
                    PpCard(modifier = Modifier.fillMaxWidth()) {
                        DeuceOptionRow(
                            label = stringResource(R.string.match_setup_deuce_advantage),
                            tag = null,
                            selected = state.selectedDeuceMode == DeuceMode.ADVANTAGE,
                            onClick = { viewModel.setDeuceMode(DeuceMode.ADVANTAGE) }
                        )
                        Spacer(Modifier.height(PpSpacing.sm))
                        DeuceOptionRow(
                            label = stringResource(R.string.match_setup_deuce_golden),
                            tag = null,
                            selected = state.selectedDeuceMode == DeuceMode.GOLDEN_POINT,
                            onClick = { viewModel.setDeuceMode(DeuceMode.GOLDEN_POINT) }
                        )
                        Spacer(Modifier.height(PpSpacing.sm))
                        DeuceOptionRow(
                            label = stringResource(R.string.match_setup_deuce_star),
                            tag = stringResource(R.string.match_setup_fip_tag),
                            selected = state.selectedDeuceMode == DeuceMode.STAR_POINT,
                            onClick = { viewModel.setDeuceMode(DeuceMode.STAR_POINT) }
                        )
                        Spacer(Modifier.height(PpSpacing.md))
                        Text(
                            text = stringResource(R.string.match_setup_decider_hint),
                            style = PpTextStyles.Label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    val pickerSlot = activePickerSlot
    if (pickerSlot != null) {
        PlayerPickerBottomSheet(
            userProfile = state.userProfile,
            friends = state.friends,
            assignedPlayerIds = assignedIds,
            currentSlotPlayerId = state.playerForSlot(pickerSlot)?.id,
            friendError = state.friendError,
            onDismissError = viewModel::clearFriendError,
            onSelectPlayer = { selectedPlayer ->
                viewModel.assignPlayerToSlot(pickerSlot, selectedPlayer)
                activePickerSlot = null
            },
            onCreateFriend = { name, avatarId ->
                viewModel.createAndAssignFriend(
                    displayName = name,
                    avatarId = avatarId,
                    targetInternalSlotIndex = pickerSlot
                )
                activePickerSlot = null
            },
            onUpdateFriend = { playerId, name, avatarId ->
                viewModel.updateFriend(playerId, name, avatarId)
            },
            onDeleteFriend = { playerId ->
                viewModel.deleteFriend(playerId)
            },
            onDismissRequest = { activePickerSlot = null }
        )
    }

    if (state.showActiveConflictDialog) {
        PpConfirmDialog(
            title = stringResource(R.string.match_setup_active_conflict_title),
            body = stringResource(R.string.match_setup_active_conflict_body),
            confirmText = stringResource(R.string.match_setup_replace_active_confirm),
            isDestructive = true,
            onConfirm = { viewModel.startMatch(cancelExisting = true) },
            onDismiss = viewModel::dismissActiveConflictDialog
        )
    }
}

@Composable
private fun PlayerSlotRow(
    player: Player?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(PpRadius.input))
            .clickable(onClick = onClick)
            .padding(horizontal = PpSpacing.xs, vertical = PpSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (player != null) {
            PpAvatar(avatarId = player.avatarId, size = 38.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, PpOutline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(PpSpacing.md))
        Text(
            text = player?.displayName ?: stringResource(R.string.match_setup_slot_empty),
            style = PpTextStyles.Body,
            color = if (player != null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (player?.isUser == true) {
            Box(
                modifier = Modifier
                    .clip(PpRadius.pill)
                    .background(PpPrimaryTint)
                    .padding(horizontal = PpSpacing.sm, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_you),
                    style = PpTextStyles.Micro,
                    color = PpPrimary
                )
            }
            Spacer(Modifier.width(PpSpacing.sm))
        }
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = stringResource(R.string.action_edit),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DeuceOptionRow(
    label: String,
    tag: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(PpRadius.pill)
            .clickable(onClick = onClick)
            .padding(vertical = PpSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) PpPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(PpSpacing.md))
        Text(
            text = label,
            style = PpTextStyles.Body,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (tag != null) {
            Box(
                modifier = Modifier
                    .clip(PpRadius.pill)
                    .background(PpPrimaryTint)
                    .padding(horizontal = PpSpacing.sm, vertical = 2.dp)
            ) {
                Text(
                    text = tag,
                    style = PpTextStyles.Micro,
                    color = PpPrimary
                )
            }
        }
    }
}
