package com.padelpro.app.feature.academy

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
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
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpError
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
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
        containerColor = PpBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PpBackground)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
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
                        style = PpTextStyles.Micro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (state.lesson?.isCompleted == true) {
                        stringResource(R.string.quiz_review_mode_hint)
                    } else {
                        stringResource(R.string.quiz_xp_hint)
                    },
                    style = PpTextStyles.Label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.md)) {
                        question.options.forEachIndexed { index, optionText ->
                            val isSelected = state.selectedOptionIndex == index
                            val isCorrectOption = index == question.correctIndex
                            val borderColor = when {
                                state.isChecked && isCorrectOption -> PpPrimary
                                state.isChecked && isSelected && !isCorrectOption -> PpError
                                isSelected -> PpPrimary
                                else -> PpOutline
                            }
                            val containerColor = when {
                                state.isChecked && isCorrectOption -> PpPrimaryTint.copy(alpha = 0.45f)
                                state.isChecked && isSelected && !isCorrectOption -> PpError.copy(alpha = 0.14f)
                                isSelected -> PpPrimaryTint.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val shape = RoundedCornerShape(PpRadius.card)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .clip(shape)
                                    .background(containerColor)
                                    .border(
                                        width = if (isSelected || (state.isChecked && isCorrectOption)) 1.5.dp else 1.dp,
                                        color = borderColor,
                                        shape = shape
                                    )
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
                                    state.isChecked && isCorrectOption -> PpPrimary
                                    state.isChecked && isSelected && !isCorrectOption -> PpError
                                    isSelected -> PpPrimary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (state.isChecked) {
                        val correct = state.isCurrentSelectionCorrect
                        val feedbackColor = if (correct) PpPrimary else PpError
                        PpCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_feedback_card"),
                            borderColor = feedbackColor.copy(alpha = 0.7f),
                            containerColor = feedbackColor.copy(alpha = 0.12f)
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
                                style = PpTextStyles.Micro,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(PpSpacing.xs))
                            Text(
                                text = question.explanation,
                                style = PpTextStyles.Body,
                                color = MaterialTheme.colorScheme.onBackground
                            )
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
