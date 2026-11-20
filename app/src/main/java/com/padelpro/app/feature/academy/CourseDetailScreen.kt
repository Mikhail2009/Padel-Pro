package com.padelpro.app.feature.academy

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpCourtGraphic
import com.padelpro.app.core.designsystem.components.PpFilterChip
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpLessonRow
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpProgressBar
import com.padelpro.app.core.designsystem.components.PpSectionHeader
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun CourseDetailScreen(
    courseId: String,
    viewModel: AcademyViewModel,
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val course = state.courses.firstOrNull { it.id == courseId } ?: state.courses.firstOrNull()
    val order = course?.order ?: 1
    val lessonCount = course?.lessonCount ?: 4
    val completedCount = course?.completedLessons ?: 0
    val progressPercent = ((course?.progressFraction ?: 0f) * 100f).toInt()

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
                PpPrimaryButton(
                    text = stringResource(R.string.course_cta_continue),
                    onClick = { onOpenLesson("${courseId}_l1") },
                    enabled = false,
                    modifier = Modifier.widthIn(max = 600.dp)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(PpSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
            ) {
                item {
                    PpIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack
                    )
                }

                item {
                    PpCard(modifier = Modifier.fillMaxWidth()) {
                        PpCourtGraphic(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = courseTitle(order),
                        style = PpTextStyles.H1,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(PpSpacing.xs))
                    Text(
                        text = courseDescription(order),
                        style = PpTextStyles.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(PpSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
                        PpFilterChip(
                            label = pluralStringResource(R.plurals.course_meta_lessons_format, lessonCount, lessonCount),
                            selected = false,
                            onClick = {}
                        )
                        PpFilterChip(
                            label = stringResource(R.string.course_meta_min_format, course?.estimatedMinutes ?: 20),
                            selected = false,
                            onClick = {}
                        )
                        PpFilterChip(
                            label = stringResource(R.string.course_meta_xp_format, course?.xpReward ?: 180),
                            selected = true,
                            onClick = {}
                        )
                    }
                }

                item {
                    PpCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.course_progress_complete_format, progressPercent),
                                style = PpTextStyles.Micro,
                                color = PpPrimary
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.course_lessons_count_format,
                                    lessonCount,
                                    completedCount,
                                    lessonCount
                                ),
                                style = PpTextStyles.Label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(PpSpacing.sm))
                        PpProgressBar(
                            progress = course?.progressFraction ?: 0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(PpSpacing.sm))
                        Text(
                            text = stringResource(R.string.course_content_pending_note),
                            style = PpTextStyles.Label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    PpSectionHeader(title = stringResource(R.string.course_section_lessons))
                }

                items(lessonCount) { idx ->
                    val lessonNumber = idx + 1
                    PpLessonRow(
                        index = lessonNumber,
                        title = stringResource(R.string.course_lesson_placeholder_title, lessonNumber),
                        meta = stringResource(R.string.course_lesson_placeholder_meta),
                        xpLabel = stringResource(R.string.course_lesson_xp_format, 20),
                        isCompleted = false,
                        isCurrent = lessonNumber == 1,
                        isUnlocked = lessonNumber == 1,
                        enabled = false,
                        onClick = { onOpenLesson("${courseId}_l$lessonNumber") }
                    )
                }
            }
        }
    }
}
