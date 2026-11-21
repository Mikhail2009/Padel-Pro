package com.padelpro.app.feature.match

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpBottomSheet
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpConfirmDialog
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpScoreHalf
import com.padelpro.app.core.designsystem.components.PpScrollColumn
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.components.PpServeIndicator
import com.padelpro.app.core.designsystem.components.PpSetStrip
import com.padelpro.app.core.designsystem.components.accentRail
import com.padelpro.app.core.designsystem.components.glassPanel
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpCourtPanel
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpOnAccent
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpScrimVertical
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.engine.MatchEngineState
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.LiveMatchSnapshot
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

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (snapshot != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PpScrimVertical)
                        .navigationBarsPadding()
                        .padding(horizontal = PpSpacing.lg, vertical = PpSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PpSecondaryButton(
                        text = stringResource(R.string.live_match_undo),
                        leadingIcon = Icons.AutoMirrored.Rounded.RotateLeft,
                        onClick = viewModel::undoLastPoint,
                        enabled = snapshot.canUndo && !uiState.isMutating,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("live_undo_button")
                    )
                    PpIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.live_match_settings),
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("live_settings_button")
                    )
                    PpSecondaryButton(
                        text = stringResource(R.string.live_match_finish),
                        onClick = { showEndEarlyDialog = true },
                        enabled = !uiState.isMutating,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("live_finish_button")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PpLime)
                    }
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
                    LiveScoreboardContent(
                        snapshot = snapshot,
                        elapsedMillis = uiState.elapsedMillis,
                        isMutating = uiState.isMutating,
                        onPointA = {
                            if (uiState.hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            viewModel.scorePoint(TeamSide.A)
                        },
                        onPointB = {
                            if (uiState.hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            viewModel.scorePoint(TeamSide.B)
                        },
                        onTogglePause = viewModel::togglePause,
                        onDismissRestored = viewModel::dismissRestoredBanner
                    )
                }
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
        }
    }
}

@Composable
private fun LiveScoreboardContent(
    snapshot: LiveMatchSnapshot,
    elapsedMillis: Long,
    isMutating: Boolean,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onTogglePause: () -> Unit,
    onDismissRestored: () -> Unit
) {
    val engineState = snapshot.state
    val formatLabel = when (engineState.config.format) {
        MatchFormat.SINGLES -> stringResource(R.string.match_format_singles_short)
        MatchFormat.DOUBLES -> stringResource(R.string.match_format_doubles_short)
    }.uppercase()
    val deuceShort = when (engineState.config.deuceMode) {
        DeuceMode.ADVANTAGE -> stringResource(R.string.live_match_deuce_short_adv)
        DeuceMode.GOLDEN_POINT -> stringResource(R.string.live_match_deuce_short_golden)
        DeuceMode.STAR_POINT -> stringResource(R.string.live_match_deuce_short_star)
    }

    val teamALabel = snapshot.teamAPlayers.joinToString(" и ") { it.displayName }
        .ifBlank { stringResource(R.string.live_match_team_a_placeholder) }
    val teamBLabel = snapshot.teamBPlayers.joinToString(" и ") { it.displayName }
        .ifBlank { stringResource(R.string.live_match_team_b_placeholder) }

    val canTapPoint = !engineState.isPaused && !isMutating

    val allSets = engineState.allSetsForDisplay()
    val completedSetChips = allSets
        .filter { it.setIndex != engineState.currentSetIndex }
        .map { set ->
            val tb = if (set.tbA != null && set.tbB != null) "(${set.tbA}:${set.tbB})" else ""
            "${stringResource(R.string.live_match_set_chip_format, set.setIndex)}: ${set.gamesA}-${set.gamesB}$tb"
        }
    val currentSetChip = allSets
        .firstOrNull { it.setIndex == engineState.currentSetIndex }
        ?.let { set ->
            val tb = if (set.tbA != null && set.tbB != null) "(${set.tbA}:${set.tbB})" else ""
            "${stringResource(R.string.live_match_set_chip_format, set.setIndex)}: ${set.gamesA}-${set.gamesB}$tb"
        }

    PpScrollColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PpSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PpIconButton(
                    icon = if (engineState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = stringResource(
                        if (engineState.isPaused) R.string.live_match_resume_desc else R.string.live_match_pause_desc
                    ),
                    tint = if (engineState.isPaused) PpAmberLed else PpTextHigh,
                    onClick = onTogglePause,
                    modifier = Modifier.testTag("live_pause_button")
                )
                Text(
                    text = stringResource(
                        R.string.live_match_header_format,
                        engineState.currentSetIndex,
                        formatLabel,
                        deuceShort
                    ),
                    style = PpTextStyles.Micro,
                    color = PpTextMid,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = PpSpacing.sm)
                )
                Text(
                    text = formatElapsedTimer(elapsedMillis),
                    style = PpTextStyles.Title.copy(fontFeatureSettings = "tnum"),
                    color = if (engineState.isPaused) PpAmberLed else PpTextHigh,
                    modifier = Modifier.testTag("live_timer_text")
                )
            }

            if (snapshot.wasRestored) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassPanel(
                            radius = PpRadius.input,
                            fill = Brush.horizontalGradient(
                                listOf(PpGlassCyan.copy(alpha = 0.18f), PpCourtPanel)
                            ),
                            edge = Brush.linearGradient(
                                listOf(PpGlassCyan.copy(alpha = 0.7f), PpCourtLine)
                            )
                        )
                        .accentRail(PpGlassCyan)
                        .padding(horizontal = PpSpacing.md, vertical = PpSpacing.xs)
                        .testTag("live_restored_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restore,
                        contentDescription = null,
                        tint = PpGlassCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(PpSpacing.sm))
                    Text(
                        text = stringResource(R.string.live_match_restored_banner),
                        style = PpTextStyles.Label,
                        color = PpGlassCyan,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissRestored,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = PpGlassCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (engineState.isPaused) {
                PpCard(
                    accent = PpAmberLed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_paused_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.live_match_paused_banner),
                            style = PpTextStyles.Micro,
                            color = PpAmberLed,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(PpSpacing.md))
                        PpPrimaryButton(
                            text = stringResource(R.string.live_match_resume_btn),
                            onClick = onTogglePause
                        )
                    }
                }
            }

            PpCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        R.string.live_match_sets_won_format,
                        engineState.setsWonA,
                        engineState.setsWonB
                    ),
                    style = PpTextStyles.Micro,
                    color = PpTextMid
                )
                Spacer(Modifier.height(PpSpacing.sm))
                PpSetStrip(
                    sets = completedSetChips,
                    currentSet = currentSetChip
                )

                Spacer(Modifier.height(PpSpacing.md))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.live_match_games_label),
                        style = PpTextStyles.Micro,
                        color = PpTextMid
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.live_match_games_score_format,
                            engineState.gamesA,
                            engineState.gamesB
                        ),
                        style = PpTextStyles.DisplayNum.copy(fontFeatureSettings = "tnum"),
                        color = PpTextHigh,
                        modifier = Modifier.testTag("live_games_score")
                    )
                }

                Spacer(Modifier.height(PpSpacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PpServeIndicator(isServing = true)
                    Spacer(Modifier.width(PpSpacing.xs))
                    val sideLabel = stringResource(
                        if (engineState.serveSideFromRight) R.string.live_match_serve_right
                        else R.string.live_match_serve_left
                    )
                    Text(
                        text = stringResource(
                            R.string.live_match_serve_player_format,
                            engineState.servingPlayerName,
                            sideLabel
                        ),
                        style = PpTextStyles.Label,
                        color = PpAmberLed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("live_server_label")
                    )
                }
            }

            if (engineState.shouldChangeEnds) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PpRadius.pill)
                        .background(PpAmberLed.copy(alpha = 0.14f))
                        .border(1.dp, PpAmberLed.copy(alpha = 0.65f), PpRadius.pill)
                        .padding(horizontal = PpSpacing.md, vertical = PpSpacing.xs)
                        .testTag("live_change_ends_banner"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        tint = PpAmberLed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(PpSpacing.xs))
                    Text(
                        text = stringResource(R.string.live_match_change_ends),
                        style = PpTextStyles.Micro,
                        color = PpAmberLed
                    )
                }
            }

            val scoreTextA = localizePointScore(engineState.displayScoreA())
            val scoreTextB = localizePointScore(engineState.displayScoreB())
            val callout = resolveCalloutBanner(engineState, teamALabel, teamBLabel)

            PpScoreHalf(
                teamLabel = teamALabel,
                score = scoreTextA,
                serving = engineState.servingTeam == TeamSide.A,
                onTap = onPointA,
                enabled = canTapPoint,
                accent = PpLime,
                modifier = Modifier
                    .alpha(if (canTapPoint) 1f else 0.6f)
                    .testTag("live_score_team_a_zone")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(PpCourtLine)
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = PpSpacing.md)
                        .then(
                            if (callout != null) {
                                Modifier
                                    .clip(PpRadius.pill)
                                    .background(PpGlassCyan.copy(alpha = 0.14f))
                                    .border(1.dp, PpGlassCyan.copy(alpha = 0.55f), PpRadius.pill)
                                    .padding(horizontal = PpSpacing.md, vertical = PpSpacing.xs)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = callout ?: stringResource(R.string.label_vs),
                        style = PpTextStyles.Micro,
                        color = if (callout != null) PpGlassCyan else PpTextLow,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.testTag("live_callout_label")
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(PpCourtLine)
                )
            }

            PpScoreHalf(
                teamLabel = teamBLabel,
                score = scoreTextB,
                serving = engineState.servingTeam == TeamSide.B,
                onTap = onPointB,
                enabled = canTapPoint,
                accent = PpGlassCyan,
                modifier = Modifier
                    .alpha(if (canTapPoint) 1f else 0.6f)
                    .testTag("live_score_team_b_zone")
            )
        }
    }
}

@Composable
private fun LiveSettingRow(
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
private fun localizePointScore(rawScore: String): String {
    return if (rawScore == "AD") stringResource(R.string.live_score_advantage_short) else rawScore
}

@Composable
private fun resolveCalloutBanner(
    state: MatchEngineState,
    teamALabel: String,
    teamBLabel: String
): String? {
    return when {
        state.isMatchPointA -> stringResource(R.string.live_match_match_point_banner, teamALabel.uppercase())
        state.isMatchPointB -> stringResource(R.string.live_match_match_point_banner, teamBLabel.uppercase())
        state.isSetPointA -> stringResource(R.string.live_match_set_point_banner, teamALabel.uppercase())
        state.isSetPointB -> stringResource(R.string.live_match_set_point_banner, teamBLabel.uppercase())
        state.isGoldenPointActive -> stringResource(R.string.live_match_golden_banner)
        state.isStarPointDeciderActive -> stringResource(R.string.live_match_star_banner)
        state.advantageTeam == TeamSide.A -> stringResource(R.string.live_match_advantage_banner, teamALabel.uppercase())
        state.advantageTeam == TeamSide.B -> stringResource(R.string.live_match_advantage_banner, teamBLabel.uppercase())
        state.isDeuce -> stringResource(R.string.live_match_deuce_banner)
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
