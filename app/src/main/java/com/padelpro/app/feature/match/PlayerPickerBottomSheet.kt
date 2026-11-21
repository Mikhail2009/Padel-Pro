package com.padelpro.app.feature.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.DeleteFriendConfirmDialog
import com.padelpro.app.core.designsystem.components.FriendFormFields
import com.padelpro.app.core.designsystem.components.FriendInUseErrorBanner
import com.padelpro.app.core.designsystem.components.PlayerManagementRow
import com.padelpro.app.core.designsystem.components.PpBottomSheet
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.FriendActionError
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
                    FriendInUseErrorBanner(onDismiss = onDismissError)
                    Spacer(Modifier.height(PpSpacing.md))
                }

                val allPlayers = buildList {
                    if (userProfile != null) add(userProfile)
                    addAll(friends)
                }

                if (friends.isEmpty()) {
                    Text(
                        text = stringResource(R.string.match_setup_no_friends_yet),
                        style = PpTextStyles.Label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = PpSpacing.md)
                    )
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

                        PlayerManagementRow(
                            player = player,
                            isHighlighted = isCurrentSlot,
                            subtitle = if (isInOtherSlot) {
                                stringResource(R.string.match_setup_already_selected)
                            } else {
                                null
                            },
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
                    leadingIcon = Icons.Rounded.PersonAdd,
                    onClick = {
                        onDismissError()
                        draftName = ""
                        draftAvatarId = (friends.size + 1) % 12
                        mode = PickerMode.CREATE
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_add_friend_button")
                )
            }

            PickerMode.CREATE, PickerMode.EDIT -> {
                FriendFormFields(
                    name = draftName,
                    onNameChange = { draftName = it },
                    avatarId = draftAvatarId,
                    onSelectAvatar = { draftAvatarId = it },
                    onCancel = { mode = PickerMode.LIST },
                    onSave = {
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
                    nameInputTag = "setup_friend_name_input",
                    saveButtonTag = "setup_save_friend_button"
                )
            }
        }
    }

    val pendingDelete = deleteCandidate
    if (pendingDelete != null) {
        DeleteFriendConfirmDialog(
            playerName = pendingDelete.displayName,
            onConfirm = {
                onDeleteFriend(pendingDelete.id)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}
