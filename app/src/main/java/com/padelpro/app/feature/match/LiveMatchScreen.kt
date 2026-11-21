package com.padelpro.app.feature.match

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpBottomSheet
import com.padelpro.app.core.designsystem.components.PpConfirmDialog
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpOnAccent
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.engine.MatchEngineState
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.TeamSide
import java.util.Locale

@Composable
fun LiveMatchScreen(
    viewModel: LiveMatchViewModel,
    onBack: () -> Unit,
    onMatchFinished: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = uiState.snapshot
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showEndEarlyDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is LiveMatchNavEvent.Finished -> onMatchFinished(event.matchId)
                LiveMatchNavEvent.Cancelled -> onBack()
            }
        }
    }

    DisposableEffect(uiState.keepScreenOn, snapshot != null) {
        val window = context.findHostActivity()?.window
        if (uiState.keepScreenOn && snapshot != null) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(color = PpLime)
            }

            snapshot == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .padding(PpSpacing.lg),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PpEmptyState(
                        icon = Icons.Rounded.SportsTennis,
                        title = stringResource(R.string.live_match_no_active),
                        subtitle = stringResource(R.string.match_center_start_sub)
                    )
                    Spacer(Modifier.height(PpSpacing.lg))
                    PpPrimaryButton(
                        text = stringResource(R.string.live_match_return_center),
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            else -> {
                val engineState = snapshot.state
                val teamALabel = stringResource(R.string.live_match_team_a_placeholder)
                val teamBLabel = stringResource(R.string.live_match_team_b_placeholder)
                val teamAPlayersText = snapshot.teamAPlayers.joinToString(" и ") { it.displayName }
                val teamBPlayersText = snapshot.teamBPlayers.joinToString(" и ") { it.displayName }

                val sideA = LiveSideUi(
                    teamLabel = teamALabel,
                    players = teamAPlayersText,
                    score = localizePointScore(engineState.displayScoreA()),
                    games = engineState.gamesA,
                    setsWon = engineState.setsWonA,
                    serving = engineState.servingTeam == TeamSide.A,
                    serverName = if (engineState.servingTeam == TeamSide.A) engineState.servingPlayerName else null,
                    setPoint = engineState.isSetPointA,
                    matchPoint = engineState.isMatchPointA,
                    accent = PpLime,
                    pointsWon = engineState.totalPointsWonA
                )
                val sideB = LiveSideUi(
                    teamLabel = teamBLabel,
                    players = teamBPlayersText,
                    score = localizePointScore(engineState.displayScoreB()),
                    games = engineState.gamesB,
                    setsWon = engineState.setsWonB,
                    serving = engineState.servingTeam == TeamSide.B,
                    serverName = if (engineState.servingTeam == TeamSide.B) engineState.servingPlayerName else null,
                    setPoint = engineState.isSetPointB,
                    matchPoint = engineState.isMatchPointB,
                    accent = PpGlassCyan,
                    pointsWon = engineState.totalPointsWonB
                )

                val swapped = uiState.swapped
                val left = if (swapped) sideB else sideA
                val right = if (swapped) sideA else sideB

                val formatLabel = when (engineState.config.format) {
                    MatchFormat.SINGLES -> stringResource(R.string.match_format_singles_short)
                    MatchFormat.DOUBLES -> stringResource(R.string.match_format_doubles_short)
                }
                val deuceLabel = when (engineState.config.deuceMode) {
                    DeuceMode.ADVANTAGE -> stringResource(R.string.live_match_deuce_short_adv)
                    DeuceMode.GOLDEN_POINT -> stringResource(R.string.live_match_deuce_short_golden)
                    DeuceMode.STAR_POINT -> stringResource(R.string.live_match_deuce_short_star)
                }
                val completedSets = engineState.completedSets.map { set ->
                    val tb = if (set.tbA != null && set.tbB != null) "(${set.tbA}:${set.tbB})" else ""
                    "${stringResource(R.string.live_match_set_chip_format, set.setIndex)} ${set.gamesA}-${set.gamesB}$tb"
                }

                val rail = LiveRailUi(
                    timer = formatElapsedTimer(uiState.elapsedMillis),
                    paused = engineState.isPaused,
                    canUndo = snapshot.canUndo,
                    busy = uiState.isMutating,
                    setLabel = stringResource(R.string.live_match_set_label_format, engineState.currentSetIndex),
                    formatLabel = formatLabel,
                    deuceLabel = deuceLabel,
                    banner = resolveRailBanner(
                        state = engineState,
                        wasRestored = snapshot.wasRestored,
                        teamALabel = teamAPlayersText.ifBlank { teamALabel },
                        teamBLabel = teamBPlayersText.ifBlank { teamBLabel }
                    ),
                    autoSwap = uiState.autoSideSwap,
                    swapped = swapped,
                    sets = completedSets,
                    recentPoints = uiState.recentPoints
                )

                LiveMatchLandscape(
                    left = left,
                    right = right,
                    rail = rail,
                    onLeftTap = {
                        if (uiState.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        viewModel.scorePoint(if (swapped) TeamSide.B else TeamSide.A)
                    },
                    onRightTap = {
                        if (uiState.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        viewModel.scorePoint(if (swapped) TeamSide.A else TeamSide.B)
                    },
                    onUndo = viewModel::undoLastPoint,
                    onPauseToggle = viewModel::togglePause,
                    onSwapSides = viewModel::swapSidesManually,
                    onSettings = { showSettingsSheet = true },
                    onFinish = { showEndEarlyDialog = true }
                )
            }
        }
    }

    if (showEndEarlyDialog) {
        PpConfirmDialog(
            title = stringResource(R.string.live_match_end_early_title),
            body = stringResource(R.string.live_match_end_early_body),
            confirmText = stringResource(R.string.live_match_end_early_confirm),
            isDestructive = true,
            onConfirm = {
                showEndEarlyDialog = false
                viewModel.cancelMatchEarly()
            },
            onDismiss = { showEndEarlyDialog = false }
        )
    }

    if (showSettingsSheet) {
        PpBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            title = stringResource(R.string.live_match_settings_sheet_title)
        ) {
            LiveSettingRow(
                label = stringResource(R.string.profile_setting_haptics),
                checked = uiState.hapticsEnabled,
                onCheckedChange = viewModel::setHapticsEnabled
            )
            Spacer(Modifier.height(PpSpacing.sm))
            LiveSettingRow(
                label = stringResource(R.string.profile_setting_keep_screen_on),
                checked = uiState.keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOn
            )
            Spacer(Modifier.height(PpSpacing.sm))
            LiveSettingRow(
                label = stringResource(R.string.live_match_setting_auto_side_swap),
                subtitle = stringResource(R.string.match_setup_auto_side_swap_sub),
                checked = uiState.autoSideSwap,
                onCheckedChange = viewModel::setAutoSideSwap
            )
        }
    }
}

@Composable
private fun LiveSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = PpTextStyles.Body,
                color = PpTextHigh
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PpTextStyles.Label,
                    color = PpTextMid
                )
            }
        }
        Spacer(Modifier.width(PpSpacing.md))
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
private fun localizePointScore(rawScore: String): String {
    return if (rawScore == "AD") stringResource(R.string.live_score_advantage_short) else rawScore
}

@Composable
private fun resolveRailBanner(
    state: MatchEngineState,
    wasRestored: Boolean,
    teamALabel: String,
    teamBLabel: String
): String? {
    return when {
        state.isPaused -> stringResource(R.string.live_match_paused_banner)
        wasRestored -> stringResource(R.string.live_match_restored_banner)
        state.isGoldenPointActive -> stringResource(R.string.live_match_golden_banner)
        state.isStarPointDeciderActive -> stringResource(R.string.live_match_star_banner)
        state.advantageTeam == TeamSide.A -> stringResource(R.string.live_match_advantage_banner, teamALabel)
        state.advantageTeam == TeamSide.B -> stringResource(R.string.live_match_advantage_banner, teamBLabel)
        state.isDeuce -> stringResource(R.string.live_match_deuce_banner)
        state.shouldChangeEnds -> stringResource(R.string.live_match_change_ends)
        state.isTieBreak -> stringResource(R.string.live_match_tiebreak_banner)
        else -> null
    }
}

private fun formatElapsedTimer(elapsedMillis: Long): String {
    val totalSeconds = (elapsedMillis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
