package com.padelpro.app.feature.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
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
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpError
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onResetComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showEditSheet by remember { mutableStateOf(false) }
    var showScoringSheet by remember { mutableStateOf(false) }
    var resetStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.resetCompleteEvent.collect {
            onResetComplete()
        }
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PpIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            onClick = onBack
                        )
                        Spacer(Modifier.width(PpSpacing.sm))
                        Text(
                            text = stringResource(R.string.profile_title),
                            style = PpTextStyles.H1,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    PpIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        onClick = { showEditSheet = true },
                        modifier = Modifier.testTag("profile_edit_button")
                    )
                }
            }

            item {
                PpCard(modifier = Modifier.fillMaxWidth()) {
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
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (state.memberSinceText.isNotBlank()) {
                            Spacer(Modifier.height(PpSpacing.xs))
                            Text(
                                text = stringResource(R.string.profile_member_since_format, state.memberSinceText),
                                style = PpTextStyles.Label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        modifier = Modifier.weight(1f)
                    )
                    PpStatCard(
                        value = state.winRatePercent?.let { "$it%" } ?: stringResource(R.string.dash_placeholder),
                        label = stringResource(R.string.profile_stat_win_rate),
                        accentValue = state.winRatePercent != null,
                        modifier = Modifier.weight(1f)
                    )
                    PpStatCard(
                        value = "${state.completedLessons}/${state.totalLessons}",
                        label = stringResource(R.string.profile_stat_lessons),
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
                PpSectionHeader(
                    title = stringResource(R.string.profile_section_achievements),
                    actionText = stringResource(R.string.profile_achievements_count_format, unlockedCount, state.achievements.size),
                    onActionClick = {}
                )
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

            item {
                PpSectionHeader(title = stringResource(R.string.profile_section_players))
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.profile_players_empty),
                        style = PpTextStyles.Label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(PpSpacing.md))
                    PpSecondaryButton(
                        text = stringResource(R.string.profile_add_player),
                        icon = Icons.Rounded.PersonAdd,
                        onClick = {},
                        enabled = false
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
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = deuceModeLabel(state.defaultDeuceMode),
                                style = PpTextStyles.Label,
                                color = PpPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.profile_section_data))
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .alpha(0.5f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.profile_data_export),
                            style = PpTextStyles.Body,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.label_next_iteration_badge),
                            style = PpTextStyles.Micro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(PpSpacing.sm))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .alpha(0.5f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.profile_data_import),
                            style = PpTextStyles.Body,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.label_next_iteration_badge),
                            style = PpTextStyles.Micro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                PpSectionHeader(title = stringResource(R.string.profile_section_danger))
                Spacer(Modifier.height(PpSpacing.sm))
                PpCard(
                    borderColor = PpError.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PpDangerOutlineButton(
                        text = stringResource(R.string.profile_reset_btn),
                        onClick = { resetStep = 1 },
                        modifier = Modifier.testTag("profile_reset_button")
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
                }
            )
        }
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
                        tint = if (selected) PpPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(PpSpacing.md))
                    Text(
                        text = deuceModeLabel(mode),
                        style = PpTextStyles.Body,
                        color = MaterialTheme.colorScheme.onBackground
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
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PpBackground,
                checkedTrackColor = PpPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = PpOutline
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
