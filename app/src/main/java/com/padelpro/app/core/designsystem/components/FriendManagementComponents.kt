package com.padelpro.app.core.designsystem.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpError
import com.padelpro.app.core.designsystem.theme.PpErrorTint
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.Player

@Composable
fun FriendInUseErrorBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PpRadius.input))
            .background(PpErrorTint)
            .border(1.dp, PpError.copy(alpha = 0.6f), RoundedCornerShape(PpRadius.input))
            .clickable(onClick = onDismiss)
            .padding(PpSpacing.md)
            .testTag("friend_in_use_error_banner")
    ) {
        Text(
            text = stringResource(R.string.match_setup_friend_in_use),
            style = PpTextStyles.Label,
            color = PpError
        )
    }
}

@Composable
fun PlayerManagementRow(
    player: Player,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    subtitle: String? = null,
    onSelect: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(PpRadius.input)
    val baseModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .clip(shape)
        .background(if (isHighlighted) PpPrimaryTint else MaterialTheme.colorScheme.surface)
        .border(
            width = 1.dp,
            color = if (isHighlighted) PpPrimary else PpOutline,
            shape = shape
        )
    val interactiveModifier = if (onSelect != null) {
        baseModifier.clickable(onClick = onSelect)
    } else {
        baseModifier
    }

    Row(
        modifier = interactiveModifier.padding(horizontal = PpSpacing.md, vertical = PpSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PpAvatar(avatarId = player.avatarId, size = 38.dp, highlighted = isHighlighted)
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
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
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
                    .testTag("friend_edit_${player.id}")
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
                    .testTag("friend_delete_${player.id}")
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

@Composable
fun FriendFormFields(
    name: String,
    onNameChange: (String) -> Unit,
    avatarId: Int,
    onSelectAvatar: (Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    nameInputTag: String = "friend_name_input",
    saveButtonTag: String = "friend_save_button"
) {
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
            text = stringResource(R.string.onboarding_name_counter, name.length),
            style = PpTextStyles.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(PpSpacing.xs))
    PpTextField(
        value = name,
        onValueChange = { if (it.length <= 20) onNameChange(it) },
        placeholder = stringResource(R.string.match_setup_friend_name_hint),
        leadingIcon = Icons.Rounded.Person,
        maxLength = 20,
        modifier = Modifier.testTag(nameInputTag)
    )
    Spacer(Modifier.height(PpSpacing.lg))
    Text(
        text = stringResource(R.string.onboarding_avatar_label),
        style = PpTextStyles.Micro,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(PpSpacing.sm))
    PpAvatarPickerGrid(
        selectedAvatarId = avatarId,
        onSelectAvatar = onSelectAvatar
    )
    Spacer(Modifier.height(PpSpacing.xl))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
    ) {
        PpSecondaryButton(
            text = stringResource(R.string.action_cancel),
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        )
        PpPrimaryButton(
            text = stringResource(R.string.action_save),
            enabled = name.trim().isNotEmpty(),
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .testTag(saveButtonTag)
        )
    }
}

@Composable
fun DeleteFriendConfirmDialog(
    playerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PpConfirmDialog(
        title = stringResource(R.string.match_setup_delete_friend_title),
        body = stringResource(R.string.match_setup_delete_friend_body, playerName),
        confirmText = stringResource(R.string.action_delete),
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
