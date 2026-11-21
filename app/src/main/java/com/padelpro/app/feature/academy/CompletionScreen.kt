package com.padelpro.app.feature.academy

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpScrollColumn
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.components.PpStatCard
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.core.designsystem.theme.PpTrophyMagenta
import com.padelpro.app.core.designsystem.theme.PpTrophySweep

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
            .windowInsetsPadding(WindowInsets.safeDrawing),
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
            ) {
                Spacer(Modifier.height(PpSpacing.md))

                // Trophy medal with PpTrophySweep
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(96.dp)) {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Color(0x4DFF3D8F), Color(0x26FFB43D), Color.Transparent)
                            )
                        )
                        drawCircle(
                            brush = PpTrophySweep,
                            radius = size.minDimension / 2f - 4.dp.toPx(),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = PpAmberLed,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.completion_title),
                    style = PpTextStyles.H1,
                    color = PpTextHigh,
                    textAlign = TextAlign.Center
                )
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
                    color = PpTextMid,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(PpSpacing.sm))
                Text(
                    text = stringResource(R.string.completion_xp_gained_format, outcome?.totalXpGained ?: 0),
                    style = PpTextStyles.DisplayNum,
                    color = PpAmberLed,
                    modifier = Modifier.testTag("completion_xp_value")
                )
                Spacer(Modifier.height(PpSpacing.sm))

                PpCard(
                    accent = PpAmberLed,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                                style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                                color = PpTextMid
                            )
                            Text(
                                text = stringResource(
                                    R.string.completion_breakdown_quiz_format,
                                    outcome?.correctAnswers ?: 0,
                                    outcome?.totalQuestions ?: 0,
                                    outcome?.xpQuiz ?: 0
                                ),
                                style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                                color = PpTextMid
                            )
                        }
                        if ((outcome?.xpCourseBonus ?: 0) > 0) {
                            Text(
                                text = stringResource(
                                    R.string.completion_breakdown_course_bonus_format,
                                    outcome?.xpCourseBonus ?: 0
                                ),
                                style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                                color = PpAmberLed
                            )
                        }
                        if (outcome?.isReviewRepeat == true && outcome.totalXpGained == 0) {
                            Text(
                                text = stringResource(R.string.completion_review_badge),
                                style = PpTextStyles.Label,
                                color = PpTextMid
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    PpStatCard(
                        value = outcome?.let { "${it.scorePercent}%" } ?: stringResource(R.string.dash_placeholder),
                        label = stringResource(R.string.completion_stat_quiz_score),
                        accent = if ((outcome?.scorePercent ?: 0) == 100) PpLime else PpGlassCyan,
                        modifier = Modifier.weight(1f)
                    )
                    PpStatCard(
                        value = stringResource(
                            R.string.completion_stat_ratio_format,
                            outcome?.courseCompletedLessons ?: 0,
                            outcome?.courseTotalLessons ?: 4
                        ),
                        label = stringResource(R.string.completion_stat_course_lessons),
                        accent = PpGlassCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (outcome?.newlyUnlockedAchievements?.isNotEmpty() == true) {
                    Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
                        outcome.newlyUnlockedAchievements.forEach { achievement ->
                            PpCard(
                                accent = PpTrophyMagenta,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("completion_achievement_${achievement.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(Modifier.size(44.dp)) {
                                            drawCircle(
                                                Brush.radialGradient(
                                                    listOf(Color(0x4DFF3D8F), Color(0x1AFFB43D), Color.Transparent)
                                                )
                                            )
                                            drawCircle(
                                                brush = PpTrophySweep,
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = PpAmberLed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(PpSpacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.completion_achievement_label),
                                            style = PpTextStyles.Micro,
                                            color = PpTrophyMagenta
                                        )
                                        Text(
                                            text = achievement.title,
                                            style = PpTextStyles.Title,
                                            color = PpTextHigh
                                        )
                                        if (achievement.description.isNotBlank()) {
                                            Text(
                                                text = achievement.description,
                                                style = PpTextStyles.Label,
                                                color = PpTextMid
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(PpSpacing.md))
                val nextLessonId = outcome?.nextLessonId
                PpPrimaryButton(
                    text = stringResource(R.string.action_continue),
                    onClick = { nextLessonId?.let(onContinueNext) },
                    enabled = nextLessonId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("completion_continue_button")
                )
                PpSecondaryButton(
                    text = stringResource(R.string.completion_btn_return_academy),
                    onClick = onReturnToAcademy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("completion_return_academy_button")
                )
            }
        }
    }
}
