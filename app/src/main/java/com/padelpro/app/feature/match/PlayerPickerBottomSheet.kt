package com.padelpro.app.feature.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpAvatar
import com.padelpro.app.core.designsystem.components.PpAvatarPickerGrid
import com.padelpro.app.core.designsystem.components.PpBottomSheet
import com.padelpro.app.core.designsystem.components.PpConfirmDialog
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.components.PpTextField
import com.padelpro.app.core.designsystem.theme.PpError
import com.padelpro.app.core.designsystem.theme.PpErrorTint
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.Player

private enum class PickerMode {
    LIST,
    CREATE,
    EDIT
}

@Composable
fun PlayerPickerBottomSheet(
    userProfile: Player?,
    friends: List<Player>,
    assignedPlayerIds: Set<String>,
    currentSlotPlayerId: String?,
    friendError: FriendActionError?,
    onDismissError: () -> Unit,
    onSelectPlayer: (Player) -> Unit,
    onCreateFriend: (name: String, avatarId: Int) -> Unit,
    onUpdateFriend: (playerId: String, name: String, avatarId: Int) -> Unit,
    onDeleteFriend: (playerId: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var mode by remember { mutableStateOf(PickerMode.LIST) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var deleteCandidate by remember { mutableStateOf<Player?>(null) }
    var draftName by remember { mutableStateOf("") }
    var draftAvatarId by remember { mutableIntStateOf(1) }

    val sheetTitle = when (mode) {
        PickerMode.LIST -> stringResource(R.string.match_setup_picker_title)
        PickerMode.CREATE -> stringResource(R.string.match_setup_new_friend_title)
        PickerMode.EDIT -> stringResource(R.string.match_setup_edit_friend_title)
    }

    PpBottomSheet(
        onDismissRequest = {
            onDismissError()
            onDismissRequest()
        },
        title = sheetTitle
    ) {
        when (mode) {
            PickerMode.LIST -> {
                Text(
                    text = stringResource(R.string.match_setup_picker_subtitle),
                    style = PpTextStyles.Label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(PpSpacing.md))

                if (friendError == FriendActionError.IN_USE_BY_MATCHES) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(PpRadius.input))
                            .background(PpErrorTint)
                            .border(1.dp, PpError.copy(alpha = 0.6f), RoundedCornerShape(PpRadius.input))
                            .clickable(onClick = onDismissError)
                            .padding(PpSpacing.md)
                    ) {
                        Text(
                            text = stringResource(R.string.match_setup_friend_in_use),
                            style = PpTextStyles.Label,
                            color = PpError
                        )
                    }
                    Spacer(Modifier.height(PpSpacing.md))
                }

                val allPlayers = buildList {
                    if (userProfile != null) add(userProfile)
                    addAll(friends)
                }

                if (allPlayers.isEmpty() || (allPlayers.size == 1 && userProfile != null)) {
                    if (friends.isEmpty()) {
                        Text(
                            text = stringResource(R.string.match_setup_no_friends_yet),
                            style = PpTextStyles.Label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = PpSpacing.md)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(PpSpacing.sm)
                ) {
                    items(allPlayers, key = { it.id }) { player ->
                        val isCurrentSlot = player.id == currentSlotPlayerId
                        val isInOtherSlot = !isCurrentSlot && player.id in assignedPlayerIds

                        PickerPlayerRow(
                            player = player,
                            isCurrentSlot = isCurrentSlot,
                            isInOtherSlot = isInOtherSlot,
                            onSelect = {
                                onDismissError()
                                onSelectPlayer(player)
                            },
                            onEdit = if (!player.isUser) {
                                {
                                    onDismissError()
                                    editingPlayer = player
                                    draftName = player.displayName
                                    draftAvatarId = player.avatarId
                                    mode = PickerMode.EDIT
                                }
                            } else null,
                            onDelete = if (!player.isUser) {
                                {
                                    onDismissError()
                                    deleteCandidate = player
                                }
                            } else null
                        )
                    }
                }

                Spacer(Modifier.height(PpSpacing.lg))
                PpPrimaryButton(
                    text = stringResource(R.string.match_setup_add_friend_btn),
                    icon = Icons.Rounded.PersonAdd,
                    onClick = {
                        onDismissError()
                        draftName = ""
                        draftAvatarId = (friends.size + 1) % 12
                        mode = PickerMode.CREATE
                    },
                    modifier = Modifier.testTag("setup_add_friend_button")
                )
            }

            PickerMode.CREATE, PickerMode.EDIT -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_name_label),
                        style = PpTextStyles.Micro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.onboarding_name_counter, draftName.length),
                        style = PpTextStyles.Label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(PpSpacing.xs))
                PpTextField(
                    value = draftName,
                    onValueChange = { if (it.length <= 20) draftName = it },
                    placeholder = stringResource(R.string.match_setup_friend_name_hint),
                    leadingIcon = Icons.Rounded.Person,
                    maxLength = 20,
                    modifier = Modifier.testTag("setup_friend_name_input")
                )
                Spacer(Modifier.height(PpSpacing.lg))
                Text(
                    text = stringResource(R.string.onboarding_avatar_label),
                    style = PpTextStyles.Micro,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(PpSpacing.sm))
                PpAvatarPickerGrid(
                    selectedAvatarId = draftAvatarId,
                    onSelectAvatar = { draftAvatarId = it }
                )
                Spacer(Modifier.height(PpSpacing.xl))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    PpSecondaryButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = { mode = PickerMode.LIST },
                        modifier = Modifier.weight(1f)
                    )
                    PpPrimaryButton(
                        text = stringResource(R.string.action_save),
                        enabled = draftName.trim().isNotEmpty(),
                        onClick = {
                            val trimmed = draftName.trim().take(20)
                            if (mode == PickerMode.CREATE) {
                                onCreateFriend(trimmed, draftAvatarId)
                            } else {
                                val target = editingPlayer
                                if (target != null) {
                                    onUpdateFriend(target.id, trimmed, draftAvatarId)
                                }
                                mode = PickerMode.LIST
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_save_friend_button")
                    )
                }
            }
        }
    }

    val pendingDelete = deleteCandidate
    if (pendingDelete != null) {
        PpConfirmDialog(
            title = stringResource(R.string.match_setup_delete_friend_title),
            body = stringResource(R.string.match_setup_delete_friend_body, pendingDelete.displayName),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = {
                onDeleteFriend(pendingDelete.id)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
private fun PickerPlayerRow(
    player: Player,
    isCurrentSlot: Boolean,
    isInOtherSlot: Boolean,
    onSelect: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val shape = RoundedCornerShape(PpRadius.input)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (isCurrentSlot) PpPrimaryTint else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = if (isCurrentSlot) PpPrimary else PpOutline,
                shape = shape
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = PpSpacing.md, vertical = PpSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PpAvatar(avatarId = player.avatarId, size = 38.dp, highlighted = isCurrentSlot)
        Spacer(Modifier.width(PpSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = player.displayName,
                    style = PpTextStyles.Body,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (player.isUser) {
                    Spacer(Modifier.width(PpSpacing.sm))
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
                }
            }
            if (isInOtherSlot) {
                Text(
                    text = stringResource(R.string.match_setup_already_selected),
                    style = PpTextStyles.Label,
                    color = PpPrimary
                )
            }
        }
        if (onEdit != null) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
