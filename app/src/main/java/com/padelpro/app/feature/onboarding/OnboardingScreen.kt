package com.padelpro.app.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpCourtGraphic
import com.padelpro.app.core.designsystem.components.PpFilterChip
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpTextField
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.MatchFormat

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            .background(PpBackground)
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
                transitionSpec = { fadeIn() togetherWith fadeOut() },
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = PpSpacing.lg)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PpPrimaryTint)
                    .border(1.dp, PpPrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SportsTennis,
                    contentDescription = stringResource(R.string.cd_padel_logo),
                    tint = PpPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(PpSpacing.lg))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                        append(stringResource(R.string.brand_padel))
                        append(" ")
                    }
                    withStyle(SpanStyle(color = PpPrimary)) {
                        append(stringResource(R.string.brand_pro))
                    }
                },
                style = PpTextStyles.H1.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(PpSpacing.xs))
            Text(
                text = stringResource(R.string.brand_tagline),
                style = PpTextStyles.Micro,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        PpCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PpSpacing.lg)
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
                title = stringResource(R.string.onboarding_feature_learn_title),
                description = stringResource(R.string.onboarding_feature_learn_desc)
            )
            FeatureRow(
                icon = Icons.Rounded.SportsTennis,
                title = stringResource(R.string.onboarding_feature_play_title),
                description = stringResource(R.string.onboarding_feature_play_desc)
            )
            FeatureRow(
                icon = Icons.Rounded.BarChart,
                title = stringResource(R.string.onboarding_feature_track_title),
                description = stringResource(R.string.onboarding_feature_track_desc)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PpSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PpPrimaryButton(
                text = stringResource(R.string.onboarding_cta_get_started),
                onClick = onGetStarted,
                modifier = Modifier.testTag("onboarding_get_started")
            )
            Spacer(Modifier.height(PpSpacing.sm))
            Text(
                text = stringResource(R.string.onboarding_cta_already_play),
                style = PpTextStyles.Label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            PpIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack
            )
            Spacer(Modifier.height(PpSpacing.lg))
            Text(
                text = stringResource(R.string.onboarding_step2_title),
                style = PpTextStyles.H1,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(PpSpacing.sm))
            Text(
                text = stringResource(R.string.onboarding_step2_subtitle),
                style = PpTextStyles.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(PpSpacing.xxl))

            Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.md)) {
                ValueCard(
                    title = stringResource(R.string.onboarding_step2_p1_title),
                    body = stringResource(R.string.onboarding_step2_p1_desc)
                )
                ValueCard(
                    title = stringResource(R.string.onboarding_step2_p2_title),
                    body = stringResource(R.string.onboarding_step2_p2_desc)
                )
                ValueCard(
                    title = stringResource(R.string.onboarding_step2_p3_title),
                    body = stringResource(R.string.onboarding_step2_p3_desc)
                )
            }
        }

        PpPrimaryButton(
            text = stringResource(R.string.action_continue),
            onClick = onContinue,
            modifier = Modifier
                .padding(top = PpSpacing.xl)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            PpIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack
            )
            Spacer(Modifier.height(PpSpacing.md))
            Text(
                text = stringResource(R.string.onboarding_profile_title),
                style = PpTextStyles.H1,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(PpSpacing.xs))
            Text(
                text = stringResource(R.string.onboarding_profile_subtitle),
                style = PpTextStyles.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.onboarding_name_counter, state.displayName.length),
                    style = PpTextStyles.Micro,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }

        PpPrimaryButton(
            text = stringResource(R.string.onboarding_cta_finish),
            onClick = {
                keyboard?.hide()
                onFinish()
            },
            enabled = state.isNameValid && !state.isSaving,
            modifier = Modifier
                .padding(top = PpSpacing.xl)
                .testTag("onboarding_finish_button")
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, PpOutline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PpPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(PpSpacing.md))
        Column {
            Text(
                text = title,
                style = PpTextStyles.Micro,
                color = PpPrimary
            )
            Text(
                text = description,
                style = PpTextStyles.Body,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun ValueCard(title: String, body: String) {
    PpCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = PpTextStyles.Title,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(PpSpacing.xs))
        Text(
            text = body,
            style = PpTextStyles.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    .size(width = if (active) 20.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (active) PpPrimary else PpOutline)
            )
        }
    }
}
