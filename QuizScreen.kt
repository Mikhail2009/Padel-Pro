package com.padelpro.app.feature.academy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpConfirmDialog
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpProgressBar
import com.padelpro.app.core.designsystem.components.PpScrollColumn
import com.padelpro.app.core.designsystem.components.accentRail
import com.padelpro.app.core.designsystem.components.glassPanel
import com.padelpro.app.core.designsystem.theme.PpBallTrail
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpCourtPanel
import com.padelpro.app.core.designsystem.theme.PpDanger
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpScrimVertical
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpSuccess
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun QuizScreen(
    lessonId: String,
    viewModel: QuizViewModel,
    onExit: () -> Unit,
    onCompleteQuiz: (String) -> Unit
) {
    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    LaunchedEffect(Unit) {
        viewModel.quizCompletedEvent.collect { completedLessonId ->
            onCompleteQuiz(completedLessonId)
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val question = state.currentQuestion
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler {
        showExitConfirm = true
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PpScrimVertical)
                    .navigationBarsPadding()
                    .padding(horizontal = PpSpacing.lg, vertical = PpSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                val buttonText = when {
                    state.isSubmitting -> stringResource(R.string.quiz_btn_saving)
                    !state.isChecked -> stringResource(R.string.quiz_btn_check)
                    state.isLastQuestion -> stringResource(R.string.quiz_btn_see_results)
                    else -> stringResource(R.string.quiz_btn_next)
                }
                val buttonEnabled = if (!state.isChecked) {
                    question != null && state.selectedOptionIndex != null && !state.isSubmitting
                } else {
                    !state.isSubmitting
                }
                PpPrimaryButton(
                    text = buttonText,
                    onClick = {
                        if (!state.isChecked) {
                            viewModel.checkAnswer()
                        } else {
                            viewModel.advanceOrFinish()
                        }
                    },
                    enabled = buttonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .testTag("quiz_primary_button")
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
            PpScrollColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PpSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(PpSpacing.lg)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PpIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                            onClick = { showExitConfirm = true },
                            modifier = Modifier.testTag("quiz_close_button")
                        )
                        Spacer(Modifier.width(PpSpacing.sm))
                        val progress = if (state.totalQuestions > 0) {
                            val answeredSteps = state.currentQuestionIndex + if (state.isChecked) 1 else 0
                            answeredSteps.toFloat() / state.totalQuestions.toFloat()
                        } else 0f
                        PpProgressBar(
                            progress = progress,
                            brush = PpBallTrail,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(PpSpacing.sm))
                        Text(
                            text = if (state.totalQuestions > 0) {
                                stringResource(
                                    R.string.quiz_question_counter_format,
                                    state.currentQuestionIndex + 1,
                                    state.totalQuestions
                                )
                            } else {
                                stringResource(R.string.quiz_question_counter_format, 0, 0)
                            },
                            style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
                            color = PpGlassCyan
                        )
                    }

                    Text(
                        text = if (state.lesson?.isCompleted == true) {
                            stringResource(R.string.quiz_review_mode_hint)
                        } else {
                            stringResource(R.string.quiz_xp_hint)
                        },
                        style = PpTextStyles.Label,
                        color = PpTextMid
                    )

                    if (question == null) {
                        PpEmptyState(
                            icon = Icons.Rounded.Quiz,
                            title = stringResource(R.string.quiz_empty_title),
                            subtitle = stringResource(R.string.quiz_empty_body)
                        )
                    } else {
                        Text(
                            text = question.text,
                            style = PpTextStyles.H2,
                            color = PpTextHigh
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.md)) {
                            question.options.forEachIndexed { index, optionText ->
                                val isSelected = state.selectedOptionIndex == index
                                val isCorrectOption = index == question.correctIndex
                                val accentColor: Color? = when {
                                    state.isChecked && isCorrectOption -> PpSuccess
                                    state.isChecked && isSelected && !isCorrectOption -> PpDanger
                                    isSelected -> PpGlassCyan
                                    else -> null
                                }
                                val fillBrush = when {
                                    state.isChecked && isCorrectOption ->
                                        Brush.horizontalGradient(listOf(Color(0x1F46D68F), PpCourtPanel))
                                    state.isChecked && isSelected && !isCorrectOption ->
                                        Brush.horizontalGradient(listOf(Color(0x1FFF5A5F), PpCourtPanel))
                                    isSelected ->
                                        Brush.horizontalGradient(listOf(Color(0x1A72B7F0), PpCourtPanel))
                                    else ->
                                        Brush.verticalGradient(listOf(PpCourtGlass, PpCourtPanel))
                                }
                                val edgeBrush = if (accentColor != null) {
                                    Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.35f)))
                                } else {
                                    Brush.linearGradient(listOf(PpCourtLine, PpCourtLine))
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .glassPanel(
                                            radius = PpRadius.card,
                                            fill = fillBrush,
                                            edge = edgeBrush,
                                            borderWidth = if (accentColor != null) 1.5.dp else 1.dp
                                        )
                                        .then(if (accentColor != null) Modifier.accentRail(accentColor) else Modifier)
                                        .clickable(enabled = !state.isChecked && !state.isSubmitting) {
                                            viewModel.selectOption(index)
                                        }
                                        .padding(horizontal = PpSpacing.lg, vertical = PpSpacing.md)
                                        .testTag("quiz_option_$index"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when {
                                        state.isChecked && isCorrectOption -> Icons.Rounded.CheckCircle
                                        state.isChecked && isSelected && !isCorrectOption -> Icons.Rounded.Cancel
                                        isSelected -> Icons.Rounded.RadioButtonChecked
                                        else -> Icons.Rounded.RadioButtonUnchecked
                                    }
                                    val iconTint = when {
                                        state.isChecked && isCorrectOption -> PpLime
                                        state.isChecked && isSelected && !isCorrectOption -> PpDanger
                                        isSelected -> PpGlassCyan
                                        else -> PpTextMid
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(PpSpacing.md))
                                    Text(
                                        text = optionText,
                                        style = PpTextStyles.Body,
                                        color = PpTextHigh,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (state.isChecked) {
                            val correct = state.isCurrentSelectionCorrect
                            val feedbackColor = if (correct) PpLime else PpDanger
                            PpCard(
                                accent = feedbackColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quiz_feedback_card")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)
                                ) {
                                    Icon(
                                        imageVector = if (correct) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                        contentDescription = null,
                                        tint = feedbackColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (correct) {
                                            stringResource(R.string.quiz_feedback_correct)
                                        } else {
                                            stringResource(R.string.quiz_feedback_incorrect)
                                        },
                                        style = PpTextStyles.Micro,
                                        color = feedbackColor
                                    )
                                }
                                Spacer(Modifier.height(PpSpacing.sm))
                                Text(
                                    text = stringResource(R.string.quiz_explanation_label),
                                    style = PpTextStyles.Overline,
                                    color = PpTextMid
                                )
                                Spacer(Modifier.height(PpSpacing.xs))
                                Text(
                                    text = question.explanation,
                                    style = PpTextStyles.Body,
                                    color = PpTextHigh
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirm) {
        PpConfirmDialog(
            title = stringResource(R.string.quiz_exit_title),
            body = stringResource(R.string.quiz_exit_body),
            confirmText = stringResource(R.string.quiz_exit_confirm),
            isDestructive = true,
            onConfirm = {
                showExitConfirm = false
                onExit()
            },
            onDismiss = { showExitConfirm = false }
        )
    }
}
