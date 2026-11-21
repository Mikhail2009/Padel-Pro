package com.padelpro.app.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpAvatarPickerGrid
import com.padelpro.app.core.designsystem.components.PpBallTrailDivider
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpCourtGraphic
import com.padelpro.app.core.designsystem.components.PpFilterChip
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpScrollColumn
import com.padelpro.app.core.designsystem.components.PpTextField
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.MatchFormat

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val motion = LocalPpMotion.current

    LaunchedEffect(Unit) {
        viewModel.completedEvent.collect {
            onOnboardingComplete()
        }
    }

    BackHandler(enabled = state.step > 0) {
        viewModel.previousStep()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = PpSpacing.lg, vertical = PpSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    fadeIn(tween(motion.base)) togetherWith fadeOut(tween(motion.fast))
                },
                modifier = Modifier.weight(1f),
                label = "OnboardingStep"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(
                        onGetStarted = viewModel::nextStep,
                        onSkip = viewModel::skipToProfileStep
                    )
                    1 -> ValueStep(
                        onBack = viewModel::previousStep,
                        onContinue = viewModel::nextStep
                    )
                    else -> ProfileSetupStep(
                        state = state,
                        onBack = viewModel::previousStep,
                        onNameChange = viewModel::updateName,
                        onSelectAvatar = viewModel::selectAvatar,
                        onSelectFormat = viewModel::selectFormat,
                        onFinish = viewModel::completeOnboarding
                    )
                }
            }

            Spacer(Modifier.height(PpSpacing.md))
            PageDots(activeIndex = state.step, count = 3)
            Spacer(Modifier.height(PpSpacing.sm))
            Text(
                text = stringResource(R.string.offline_footer),
                style = PpTextStyles.Label,
                color = PpTextLow,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    PpScrollColumn(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PpSpacing.lg)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0x1FC2F54C), Color(0x1472B7F0)))
                    )
                    .border(
                        1.5.dp,
                        Brush.sweepGradient(listOf(PpGlassCyan, PpLime, PpGlassCyan)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SportsTennis,
                    contentDescription = stringResource(R.string.cd_padel_logo),
                    tint = PpLime,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(PpSpacing.lg))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = PpTextHigh)) {
                        append(stringResource(R.string.brand_padel))
                        append(" ")
                    }
                    withStyle(SpanStyle(color = PpLime)) {
                        append(stringResource(R.string.brand_pro))
                    }
                },
                style = PpTextStyles.H1.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(PpSpacing.xs))
            Text(
                text = stringResource(R.string.brand_tagline),
                style = PpTextStyles.Micro,
                color = PpGlassCyan,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(PpSpacing.sm))
            PpBallTrailDivider(modifier = Modifier.height(26.dp))
        }

        PpCard(
            accent = PpGlassCyan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PpSpacing.md)
        ) {
            PpCourtGraphic(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
        ) {
            FeatureRow(
                icon = Icons.Rounded.School,
                accent = PpGlassCyan,
                title = stringResource(R.string.onboarding_feature_learn_title),
                description = stringResource(R.string.onboarding_feature_learn_desc)
            )
            FeatureRow(
                icon = Icons.Rounded.SportsTennis,
                accent = PpLime,
                title = stringResource(R.string.onboarding_feature_play_title),
                description = stringResource(R.string.onboarding_feature_play_desc)
            )
            FeatureRow(
                icon = Icons.Rounded.BarChart,
                accent = PpAmberLed,
                title = stringResource(R.string.onboarding_feature_track_title),
                description = stringResource(R.string.onboarding_feature_track_desc)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PpSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PpPrimaryButton(
                text = stringResource(R.string.onboarding_cta_get_started),
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_get_started")
            )
            Spacer(Modifier.height(PpSpacing.sm))
            Text(
                text = stringResource(R.string.onboarding_cta_already_play),
                style = PpTextStyles.Label,
                color = PpTextMid,
                modifier = Modifier
                    .clip(PpRadius.pill)
                    .clickable(onClick = onSkip)
                    .testTag("onboarding_skip_to_profile")
                    .padding(horizontal = PpSpacing.lg, vertical = PpSpacing.md)
            )
        }
    }
}

@Composable
private fun ValueStep(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    PpScrollColumn(modifier = Modifier.fillMaxSize()) {
        PpIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack
        )
        Spacer(Modifier.height(PpSpacing.lg))
        Text(
            text = stringResource(R.string.onboarding_step2_title),
            style = PpTextStyles.H1,
            color = PpTextHigh
        )
        Spacer(Modifier.height(PpSpacing.sm))
        Text(
            text = stringResource(R.string.onboarding_step2_subtitle),
            style = PpTextStyles.Body,
            color = PpTextMid
        )
        PpBallTrailDivider(modifier = Modifier.height(24.dp))
        Spacer(Modifier.height(PpSpacing.md))

        Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.md)) {
            ValueCard(
                accent = PpGlassCyan,
                title = stringResource(R.string.onboarding_step2_p1_title),
                body = stringResource(R.string.onboarding_step2_p1_desc)
            )
            ValueCard(
                accent = PpLime,
                title = stringResource(R.string.onboarding_step2_p2_title),
                body = stringResource(R.string.onboarding_step2_p2_desc)
            )
            ValueCard(
                accent = PpAmberLed,
                title = stringResource(R.string.onboarding_step2_p3_title),
                body = stringResource(R.string.onboarding_step2_p3_desc)
            )
        }

        Spacer(Modifier.height(PpSpacing.xl))
        PpPrimaryButton(
            text = stringResource(R.string.action_continue),
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_step2_continue")
        )
    }
}

@Composable
private fun ProfileSetupStep(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onSelectAvatar: (Int) -> Unit,
    onSelectFormat: (MatchFormat) -> Unit,
    onFinish: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    PpScrollColumn(modifier = Modifier.fillMaxSize()) {
        PpIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack
        )
        Spacer(Modifier.height(PpSpacing.md))
        Text(
            text = stringResource(R.string.onboarding_profile_title),
            style = PpTextStyles.H1,
            color = PpTextHigh
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = stringResource(R.string.onboarding_profile_subtitle),
            style = PpTextStyles.Body,
            color = PpTextMid
        )
        Spacer(Modifier.height(PpSpacing.xl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.onboarding_name_label),
                style = PpTextStyles.Micro,
                color = PpTextMid
            )
            Text(
                text = stringResource(R.string.onboarding_name_counter, state.displayName.length),
                style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
                color = PpGlassCyan
            )
        }
        Spacer(Modifier.height(PpSpacing.sm))
        PpTextField(
            value = state.displayName,
            onValueChange = onNameChange,
            placeholder = stringResource(R.string.onboarding_name_hint),
            leadingIcon = Icons.Rounded.Person,
            maxLength = 20,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboard?.hide()
                if (state.isNameValid) onFinish()
            }),
            modifier = Modifier.testTag("onboarding_name_input")
        )

        Spacer(Modifier.height(PpSpacing.xl))
        Text(
            text = stringResource(R.string.onboarding_avatar_label),
            style = PpTextStyles.Micro,
            color = PpTextMid
        )
        Spacer(Modifier.height(PpSpacing.md))
        PpAvatarPickerGrid(
            selectedAvatarId = state.selectedAvatarId,
            onSelectAvatar = onSelectAvatar
        )

        Spacer(Modifier.height(PpSpacing.xl))
        Text(
            text = stringResource(R.string.onboarding_format_label),
            style = PpTextStyles.Micro,
            color = PpTextMid
        )
        Spacer(Modifier.height(PpSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
        ) {
            PpFilterChip(
                label = stringResource(R.string.match_center_doubles),
                selected = state.preferredFormat == MatchFormat.DOUBLES,
                onClick = { onSelectFormat(MatchFormat.DOUBLES) },
                modifier = Modifier.weight(1f)
            )
            PpFilterChip(
                label = stringResource(R.string.match_center_singles),
                selected = state.preferredFormat == MatchFormat.SINGLES,
                onClick = { onSelectFormat(MatchFormat.SINGLES) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(PpSpacing.xl))
        PpPrimaryButton(
            text = stringResource(R.string.onboarding_cta_finish),
            onClick = {
                keyboard?.hide()
                onFinish()
            },
            enabled = state.isNameValid && !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_finish_button")
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f))
                .border(1.dp, accent.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(PpSpacing.md))
        Column {
            Text(
                text = title,
                style = PpTextStyles.Micro,
                color = accent
            )
            Text(
                text = description,
                style = PpTextStyles.Body,
                color = PpTextHigh
            )
        }
    }
}

@Composable
private fun ValueCard(accent: Color, title: String, body: String) {
    PpCard(
        accent = accent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = PpTextStyles.Title,
            color = PpTextHigh
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = body,
            style = PpTextStyles.Body,
            color = PpTextMid
        )
    }
}

@Composable
private fun PageDots(activeIndex: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
        for (i in 0 until count) {
            val active = i == activeIndex
            Box(
                modifier = Modifier
                    .size(width = if (active) 22.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (active) PpLime else PpCourtLine)
            )
        }
    }
}
