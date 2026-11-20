package com.padelpro.app.feature.academy

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.components.PpStatCard
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun CompletionScreen(
    lessonId: String,
    viewModel: CompletionViewModel,
    onContinueNext: (String) -> Unit,
    onReturnToAcademy: () -> Unit
) {
    LaunchedEffect(lessonId) {
        viewModel.loadCompletion(lessonId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val outcome = state.outcome

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
                .verticalScroll(rememberScrollState())
                .padding(PpSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = PpSpacing.xl)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(PpPrimaryTint, PpBackground)
                            )
                        )
                        .border(1.dp, PpPrimary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = PpPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(PpSpacing.lg))
                Text(
                    text = stringResource(R.string.completion_title),
                    style = PpTextStyles.H1,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(PpSpacing.xs))
                val subtitleText = when {
                    outcome == null -> stringResource(R.string.completion_empty_subtitle)
                    outcome.xpCourseBonus > 0 && outcome.nextLessonId == null ->
                        stringResource(R.string.completion_all_courses_finished_subtitle)
                    outcome.xpCourseBonus > 0 ->
                        stringResource(R.string.completion_course_finished_subtitle)
                    else ->
                        stringResource(R.string.completion_lesson_completed_subtitle, outcome.lessonTitle)
                }
                Text(
                    text = subtitleText,
                    style = PpTextStyles.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(PpSpacing.xl))

                Text(
                    text = stringResource(R.string.completion_xp_gained_format, outcome?.totalXpGained ?: 0),
                    style = PpTextStyles.DisplayNum,
                    color = PpPrimary,
                    modifier = Modifier.testTag("completion_xp_value")
                )
                Spacer(Modifier.height(PpSpacing.lg))

                PpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.xs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.completion_breakdown_lesson_format,
                                    outcome?.xpLesson ?: 0
                                ),
                                style = PpTextStyles.Label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(
                                    R.string.completion_breakdown_quiz_format,
                                    outcome?.correctAnswers ?: 0,
                                    outcome?.totalQuestions ?: 0,
                                    outcome?.xpQuiz ?: 0
                                ),
                                style = PpTextStyles.Label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if ((outcome?.xpCourseBonus ?: 0) > 0) {
                            Text(
                                text = stringResource(
                                    R.string.completion_breakdown_course_bonus_format,
                                    outcome?.xpCourseBonus ?: 0
                                ),
                                style = PpTextStyles.Label,
                                color = PpPrimary
                            )
                        }
                        if (outcome?.isReviewRepeat == true && outcome.totalXpGained == 0) {
                            Text(
                                text = stringResource(R.string.completion_review_badge),
                                style = PpTextStyles.Label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(PpSpacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    PpStatCard(
                        value = outcome?.let { "${it.scorePercent}%" } ?: stringResource(R.string.dash_placeholder),
                        label = stringResource(R.string.completion_stat_quiz_score),
                        accentValue = (outcome?.scorePercent ?: 0) == 100,
                        modifier = Modifier.weight(1f)
                    )
                    PpStatCard(
                        value = stringResource(
                            R.string.completion_stat_ratio_format,
                            outcome?.courseCompletedLessons ?: 0,
                            outcome?.courseTotalLessons ?: 4
                        ),
                        label = stringResource(R.string.completion_stat_course_lessons),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (outcome?.newlyUnlockedAchievements?.isNotEmpty() == true) {
                    Spacer(Modifier.height(PpSpacing.md))
                    Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
                        outcome.newlyUnlockedAchievements.forEach { achievement ->
                            PpCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("completion_achievement_${achievement.id}"),
                                borderColor = PpPrimary.copy(alpha = 0.7f),
                                containerColor = PpPrimaryTint.copy(alpha = 0.35f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(PpPrimaryTint)
                                            .border(1.dp, PpPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = PpPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(PpSpacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.completion_achievement_label),
                                            style = PpTextStyles.Micro,
                                            color = PpPrimary
                                        )
                                        Text(
                                            text = achievement.title,
                                            style = PpTextStyles.Title,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        if (achievement.description.isNotBlank()) {
                                            Text(
                                                text = achievement.description,
                                                style = PpTextStyles.Label,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PpSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
            ) {
                val nextLessonId = outcome?.nextLessonId
                PpPrimaryButton(
                    text = stringResource(R.string.action_continue),
                    onClick = { nextLessonId?.let(onContinueNext) },
                    enabled = nextLessonId != null,
                    modifier = Modifier.testTag("completion_continue_button")
                )
                PpSecondaryButton(
                    text = stringResource(R.string.completion_btn_return_academy),
                    onClick = onReturnToAcademy,
                    modifier = Modifier.testTag("completion_return_academy_button")
                )
            }
        }
    }
}
