package com.padelpro.app.feature.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.DeleteFriendConfirmDialog
import com.padelpro.app.core.designsystem.components.FriendFormFields
import com.padelpro.app.core.designsystem.components.FriendInUseErrorBanner
import com.padelpro.app.core.designsystem.components.PlayerManagementRow
import com.padelpro.app.core.designsystem.components.PpAchievementBadge
import com.padelpro.app.core.designsystem.components.PpAvatar
import com.padelpro.app.core.designsystem.components.PpAvatarPickerGrid
import com.padelpro.app.core.designsystem.components.PpBottomSheet
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpConfirmDialog
import com.padelpro.app.core.designsystem.components.PpDangerOutlineButton
import com.padelpro.app.core.designsystem.components.PpFilterChip
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.components.PpStatCard
import com.padelpro.app.core.designsystem.components.PpTextField
import com.padelpro.app.core.designsystem.components.PpXpIndicator
import com.padelpro.app.core.designsystem.components.levelTitleRes
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpCourtNight
import com.padelpro.app.core.designsystem.theme.PpDanger
import com.padelpro.app.core.designsystem.theme.PpErrorTint
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpOnAccent
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.BackupValidationError
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.FriendActionError
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.Player
import java.nio.charset.StandardCharsets

private sealed interface FriendSheetState {
    data object Create : FriendSheetState
    data class Edit(val player: Player) : FriendSheetState
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onResetComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showEditSheet by remember { mutableStateOf(false) }
    var showScoringSheet by remember { mutableStateOf(false) }
    var friendSheet by remember { mutableStateOf<FriendSheetState?>(null) }
    var deleteFriendCandidate by remember { mutableStateOf<Player?>(null) }
    var resetStep by remember { mutableIntStateOf(0) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup { json ->
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(StandardCharsets.UTF_8))
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(StandardCharsets.UTF_8).readText()
                }
            }.getOrNull()
            viewModel.onImportFileRead(content)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetCompleteEvent.collect {
            onResetComplete()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.needsOnboardingEvent.collect {
            onResetComplete()
        }
    }

    BackHandler(onBack = onBack)

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
            verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PpIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            onClick = onBack,
                            modifier = Modifier.testTag("profile_back_button")
                        )
                        Spacer(Modifier.width(PpSpacing.sm))
                        Text(
                            text = stringResource(R.string.profile_title),
                            style = PpTextStyles.H1,
                            color = PpTextHigh
                        )
                    }
                    PpIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = PpGlassCyan,
                        onClick = { showEditSheet = true },
                        modifier = Modifier.testTag("profile_edit_button")
                    )
                }
            }

            item {
                PpCard(
                    accent = PpGlassCyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PpAvatar(
                            avatarId = state.avatarId,
                            size = 80.dp,
                            highlighted = true,
                            onClick = { showEditSheet = true }
                        )
                        Spacer(Modifier.height(PpSpacing.md))
                        Text(
                            text = state.displayName.ifBlank { stringResource(R.string.profile_default_player_name) },
                            style = PpTextStyles.H2,
                            color = PpTextHigh
                        )
                        if (state.memberSinceText.isNotBlank()) {
                            Spacer(Modifier.height(PpSpacing.xs))
                            Text(
                                text = stringResource(R.string.profile_member_since_format, state.memberSinceText),
                                style = PpTextStyles.Label,
                                color = PpTextMid
                            )
                        }
                        Spacer(Modifier.height(PpSpacing.lg))
                        PpXpIndicator(
                            level = state.level,
                            levelTitle = stringResource(levelTitleRes(state.level)),
                            currentXp = state.totalXp,
                            nextLevelXp = state.nextLevelXp,
                            progress = state.levelProgress
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    PpStatCard(
                        value = state.totalMatches.toString(),
                        label = stringResource(R.string.profile_stat_matches),
                        accent = PpGlassCyan,
                        modifier = Modifier.weight(1f)
                    )
                    PpStatCard(
                        value = state.winRatePercent?.let { "$it%" } ?: stringResource(R.string.dash_placeholder),
                        label = stringResource(R.string.profile_stat_win_rate),
                        accent = PpLime,
                        modifier = Modifier.weight(1f)
                    )
                    PpStatCard(
                        value = "${state.completedLessons}/${state.totalLessons}",
                        label = stringResource(R.string.profile_stat_lessons),
                        accent = PpAmberLed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.profile_section_preferred_format))
                Spacer(Modifier.height(PpSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    PpFilterChip(
                        label = stringResource(R.string.match_center_doubles),
                        selected = state.preferredFormat == MatchFormat.DOUBLES,
                        onClick = { viewModel.selectPreferredFormat(MatchFormat.DOUBLES) },
                        modifier = Modifier.weight(1f)
                    )
                    PpFilterChip(
                        label = stringResource(R.string.match_center_singles),
                        selected = state.preferredFormat == MatchFormat.SINGLES,
                        onClick = { viewModel.selectPreferredFormat(MatchFormat.SINGLES) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                val unlockedCount = state.achievements.count { it.isUnlocked }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PpSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(6.dp)) { drawCircle(PpLime) }
                        Spacer(Modifier.size(PpSpacing.sm))
                        Text(
                            text = stringResource(R.string.profile_section_achievements),
                            style = PpTextStyles.Micro,
                            color = PpTextMid
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.profile_achievements_count_format,
                            unlockedCount,
                            state.achievements.size
                        ),
                        style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
                        color = PpAmberLed
                    )
                }
                Spacer(Modifier.height(PpSpacing.sm))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.achievements, key = { it.id }) { item ->
                        val (titleRes, descRes) = achievementStringRes(item.id)
                        PpAchievementBadge(
                            title = item.title.ifBlank { stringResource(titleRes) },
                            description = item.description.ifBlank { stringResource(descRes) },
                            unlocked = item.isUnlocked,
                            modifier = Modifier.testTag("profile_achievement_${item.id}")
                        )
                    }
                }
            }

            // Friends section ("ИГРОКИ") reusing shared Friend management components
            item {
                PpSectionHeader(title = stringResource(R.string.profile_section_players))
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    if (state.friendError == FriendActionError.IN_USE_BY_MATCHES) {
                        FriendInUseErrorBanner(onDismiss = viewModel::clearFriendError)
                        Spacer(Modifier.height(PpSpacing.md))
                    }

                    if (state.friends.isEmpty()) {
                        Text(
                            text = stringResource(R.string.profile_players_empty),
                            style = PpTextStyles.Label,
                            color = PpTextMid
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
                            state.friends.forEach { friend ->
                                PlayerManagementRow(
                                    player = friend,
                                    onSelect = {
                                        viewModel.clearFriendError()
                                        friendSheet = FriendSheetState.Edit(friend)
                                    },
                                    onEdit = {
                                        viewModel.clearFriendError()
                                        friendSheet = FriendSheetState.Edit(friend)
                                    },
                                    onDelete = {
                                        viewModel.clearFriendError()
                                        deleteFriendCandidate = friend
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(PpSpacing.md))
                    PpSecondaryButton(
                        text = stringResource(R.string.profile_add_player),
                        leadingIcon = Icons.Rounded.PersonAdd,
                        onClick = {
                            viewModel.clearFriendError()
                            friendSheet = FriendSheetState.Create
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_add_friend_button")
                    )
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.profile_section_settings))
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    SettingToggleRow(
                        label = stringResource(R.string.profile_setting_haptics),
                        checked = state.hapticsEnabled,
                        onCheckedChange = viewModel::setHapticsEnabled
                    )
                    Spacer(Modifier.height(PpSpacing.sm))
                    SettingToggleRow(
                        label = stringResource(R.string.profile_setting_keep_screen_on),
                        checked = state.keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn
                    )
                    Spacer(Modifier.height(PpSpacing.sm))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { showScoringSheet = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_setting_default_scoring),
                                style = PpTextStyles.Body,
                                color = PpTextHigh
                            )
                            Text(
                                text = deuceModeLabel(state.defaultDeuceMode),
                                style = PpTextStyles.Label,
                                color = PpGlassCyan
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = PpTextLow
                        )
                    }
                }
            }

            // Data Backup / Restore section ("ДАННЫЕ")
            item {
                PpSectionHeader(title = stringResource(R.string.profile_section_data))
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(
                    accent = PpGlassCyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val exportSubtitle = state.backupLastExportedText?.let { date ->
                        stringResource(R.string.profile_data_last_export_format, date)
                    } ?: stringResource(R.string.profile_data_never_exported)

                    Text(
                        text = exportSubtitle,
                        style = PpTextStyles.Label,
                        color = PpTextMid,
                        modifier = Modifier.testTag("profile_last_export_text")
                    )

                    val feedback = state.backupFeedback
                    if (feedback != null) {
                        Spacer(Modifier.height(PpSpacing.md))
                        BackupFeedbackBanner(
                            feedback = feedback,
                            onDismiss = viewModel::clearBackupFeedback
                        )
                    }

                    Spacer(Modifier.height(PpSpacing.md))
                    DataActionRow(
                        icon = Icons.Rounded.FileUpload,
                        iconTint = PpGlassCyan,
                        title = stringResource(R.string.profile_data_export),
                        subtitle = stringResource(R.string.profile_data_export_sub),
                        enabled = !state.isBackupBusy,
                        onClick = {
                            viewModel.clearBackupFeedback()
                            exportLauncher.launch(viewModel.defaultBackupFilename())
                        },
                        modifier = Modifier.testTag("profile_export_button")
                    )

                    Spacer(Modifier.height(PpSpacing.sm))
                    DataActionRow(
                        icon = Icons.Rounded.FileDownload,
                        iconTint = PpLime,
                        title = stringResource(R.string.profile_data_import),
                        subtitle = stringResource(R.string.profile_data_import_sub),
                        enabled = !state.isBackupBusy,
                        onClick = {
                            viewModel.clearBackupFeedback()
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier.testTag("profile_import_button")
                    )
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.profile_section_danger))
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(
                    accent = PpDanger,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PpDangerOutlineButton(
                        text = stringResource(R.string.profile_reset_btn),
                        onClick = { resetStep = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_reset_button")
                    )
                }
            }
        }
    }

    if (showEditSheet) {
        var editName by remember(state.displayName) { mutableStateOf(state.displayName) }
        var editAvatar by remember(state.avatarId) { mutableIntStateOf(state.avatarId) }
        PpBottomSheet(
            onDismissRequest = { showEditSheet = false },
            title = stringResource(R.string.profile_edit_sheet_title)
        ) {
            PpTextField(
                value = editName,
                onValueChange = { if (it.length <= 20) editName = it },
                placeholder = stringResource(R.string.onboarding_name_hint),
                leadingIcon = Icons.Rounded.Person,
                maxLength = 20
            )
            Spacer(Modifier.height(PpSpacing.lg))
            PpAvatarPickerGrid(
                selectedAvatarId = editAvatar,
                onSelectAvatar = { editAvatar = it }
            )
            Spacer(Modifier.height(PpSpacing.xl))
            PpPrimaryButton(
                text = stringResource(R.string.action_save),
                enabled = editName.trim().isNotEmpty(),
                onClick = {
                    viewModel.saveProfile(editName, editAvatar)
                    showEditSheet = false
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    val activeFriendSheet = friendSheet
    if (activeFriendSheet != null) {
        val initialName = when (activeFriendSheet) {
            FriendSheetState.Create -> ""
            is FriendSheetState.Edit -> activeFriendSheet.player.displayName
        }
        val initialAvatar = when (activeFriendSheet) {
            FriendSheetState.Create -> (state.friends.size + 1) % 12
            is FriendSheetState.Edit -> activeFriendSheet.player.avatarId
        }
        var draftName by remember(activeFriendSheet) { mutableStateOf(initialName) }
        var draftAvatar by remember(activeFriendSheet) { mutableIntStateOf(initialAvatar) }

        PpBottomSheet(
            onDismissRequest = { friendSheet = null },
            title = when (activeFriendSheet) {
                FriendSheetState.Create -> stringResource(R.string.match_setup_new_friend_title)
                is FriendSheetState.Edit -> stringResource(R.string.match_setup_edit_friend_title)
            }
        ) {
            FriendFormFields(
                name = draftName,
                onNameChange = { draftName = it },
                avatarId = draftAvatar,
                onSelectAvatar = { draftAvatar = it },
                onCancel = { friendSheet = null },
                onSave = {
                    when (activeFriendSheet) {
                        FriendSheetState.Create -> viewModel.createFriend(draftName, draftAvatar)
                        is FriendSheetState.Edit -> viewModel.updateFriend(
                            playerId = activeFriendSheet.player.id,
                            displayName = draftName,
                            avatarId = draftAvatar
                        )
                    }
                    friendSheet = null
                },
                nameInputTag = "profile_friend_name_input",
                saveButtonTag = "profile_save_friend_button"
            )
        }
    }

    val pendingDeleteFriend = deleteFriendCandidate
    if (pendingDeleteFriend != null) {
        DeleteFriendConfirmDialog(
            playerName = pendingDeleteFriend.displayName,
            onConfirm = {
                viewModel.deleteFriend(pendingDeleteFriend.id)
                deleteFriendCandidate = null
            },
            onDismiss = { deleteFriendCandidate = null }
        )
    }

    val importPreview = state.pendingImportPreview
    if (importPreview != null) {
        PpConfirmDialog(
            title = stringResource(R.string.profile_data_import_confirm_title),
            body = stringResource(
                R.string.profile_data_import_confirm_body,
                importPreview.matchesCount,
                importPreview.playersCount,
                importPreview.lessonsCompletedCount
            ),
            confirmText = stringResource(R.string.profile_data_import_confirm_btn),
            isDestructive = true,
            onConfirm = viewModel::confirmPendingImport,
            onDismiss = viewModel::dismissPendingImport
        )
    }

    if (showScoringSheet) {
        PpBottomSheet(
            onDismissRequest = { showScoringSheet = false },
            title = stringResource(R.string.profile_scoring_sheet_title)
        ) {
            DeuceMode.entries.forEach { mode ->
                val selected = mode == state.defaultDeuceMode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable {
                            viewModel.setDefaultDeuceMode(mode)
                            showScoringSheet = false
                        }
                        .padding(vertical = PpSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) PpLime else PpTextMid
                    )
                    Spacer(Modifier.width(PpSpacing.md))
                    Text(
                        text = deuceModeLabel(mode),
                        style = PpTextStyles.Body,
                        color = PpTextHigh
                    )
                }
            }
        }
    }

    if (resetStep == 1) {
        PpConfirmDialog(
            title = stringResource(R.string.profile_reset_step1_title),
            body = stringResource(R.string.profile_reset_step1_body),
            confirmText = stringResource(R.string.action_continue),
            isDestructive = true,
            onConfirm = { resetStep = 2 },
            onDismiss = { resetStep = 0 }
        )
    } else if (resetStep == 2) {
        PpConfirmDialog(
            title = stringResource(R.string.profile_reset_step2_title),
            body = stringResource(R.string.profile_reset_step2_body),
            confirmText = stringResource(R.string.profile_reset_confirm),
            isDestructive = true,
            onConfirm = {
                resetStep = 0
                viewModel.resetAllProgress()
            },
            onDismiss = { resetStep = 0 }
        )
    }
}

@Composable
private fun DataActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(PpRadius.input)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(PpCourtNight)
            .border(1.dp, PpCourtLineStrong, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = PpSpacing.md, vertical = PpSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint
        )
        Spacer(Modifier.width(PpSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PpTextStyles.Body,
                color = PpTextHigh
            )
            Text(
                text = subtitle,
                style = PpTextStyles.Label,
                color = PpTextMid
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = PpTextLow
        )
    }
}

@Composable
private fun BackupFeedbackBanner(
    feedback: ProfileBackupFeedback,
    onDismiss: () -> Unit
) {
    val isSuccess = feedback is ProfileBackupFeedback.ExportSuccess ||
        feedback is ProfileBackupFeedback.ImportSuccess
    val text = when (feedback) {
        ProfileBackupFeedback.ExportSuccess -> stringResource(R.string.profile_data_export_success)
        ProfileBackupFeedback.ExportError -> stringResource(R.string.profile_data_export_error)
        is ProfileBackupFeedback.ImportSuccess -> stringResource(
            R.string.profile_data_import_success,
            feedback.matchesCount,
            feedback.playersCount
        )
        ProfileBackupFeedback.ReadError -> stringResource(R.string.profile_data_error_read)
        is ProfileBackupFeedback.ImportValidationError -> when (feedback.error) {
            BackupValidationError.INVALID_JSON -> stringResource(R.string.profile_data_error_invalid_json)
            BackupValidationError.UNSUPPORTED_SCHEMA_VERSION -> stringResource(R.string.profile_data_error_schema_version)
            BackupValidationError.UNKNOWN_CONTENT_REFERENCE -> stringResource(R.string.profile_data_error_unknown_content)
            BackupValidationError.INVALID_DATA -> stringResource(R.string.profile_data_error_invalid_data)
        }
    }
    val shape = RoundedCornerShape(PpRadius.input)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSuccess) PpLime.copy(alpha = 0.14f) else PpErrorTint)
            .border(
                1.dp,
                if (isSuccess) PpLime.copy(alpha = 0.6f) else PpDanger.copy(alpha = 0.6f),
                shape
            )
            .clickable(onClick = onDismiss)
            .padding(PpSpacing.md)
            .testTag("profile_backup_feedback_banner")
    ) {
        Text(
            text = text,
            style = PpTextStyles.Label,
            color = if (isSuccess) PpLime else PpDanger
        )
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = PpTextStyles.Body,
            color = PpTextHigh,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PpOnAccent,
                checkedTrackColor = PpLime,
                uncheckedThumbColor = PpTextMid,
                uncheckedTrackColor = PpCourtLineStrong
            )
        )
    }
}

@Composable
private fun deuceModeLabel(mode: DeuceMode): String = when (mode) {
    DeuceMode.ADVANTAGE -> stringResource(R.string.match_setup_deuce_advantage)
    DeuceMode.GOLDEN_POINT -> stringResource(R.string.match_setup_deuce_golden)
    DeuceMode.STAR_POINT -> stringResource(R.string.match_setup_deuce_star)
}

private fun achievementStringRes(id: String): Pair<Int, Int> = when (id) {
    "FIRST_LESSON" -> R.string.ach_first_lesson_title to R.string.ach_first_lesson_desc
    "FIRST_COURSE" -> R.string.ach_first_course_title to R.string.ach_first_course_desc
    "PERFECT_QUIZ" -> R.string.ach_perfect_quiz_title to R.string.ach_perfect_quiz_desc
    "STREAK_3" -> R.string.ach_streak_3_title to R.string.ach_streak_3_desc
    "STREAK_7" -> R.string.ach_streak_7_title to R.string.ach_streak_7_desc
    "FIRST_MATCH" -> R.string.ach_first_match_title to R.string.ach_first_match_desc
    "FIRST_WIN" -> R.string.ach_first_win_title to R.string.ach_first_win_desc
    "WIN_STREAK_3" -> R.string.ach_win_streak_3_title to R.string.ach_win_streak_3_desc
    "SINGLES_5_WINS" -> R.string.ach_singles_5_title to R.string.ach_singles_5_desc
    "DOUBLES_5_WINS" -> R.string.ach_doubles_5_title to R.string.ach_doubles_5_desc
    "MATCHES_10" -> R.string.ach_matches_10_title to R.string.ach_matches_10_desc
    "MATCHES_25" -> R.string.ach_matches_25_title to R.string.ach_matches_25_desc
    "LEVEL_5" -> R.string.ach_level_5_title to R.string.ach_level_5_desc
    else -> R.string.ach_golden_win_title to R.string.ach_golden_win_desc
}
