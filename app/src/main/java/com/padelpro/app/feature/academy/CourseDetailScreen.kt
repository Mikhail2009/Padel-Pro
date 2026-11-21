package com.padelpro.app.feature.academy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import com.padelpro.app.core.designsystem.theme.PpBallTrail
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpScrimVertical
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CourseDetailScreen(
    courseId: String,
    viewModel: CourseDetailViewModel,
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit
) {
    LaunchedEffect(courseId) {
        viewModel.loadCourse(courseId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = state.detail
    val course = detail?.course
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lockedLessonHint = stringResource(R.string.course_locked_lesson_hint)

    val order = course?.order ?: 1
    val lessonCount = course?.lessonCount ?: 4
    val completedCount = course?.completedLessons ?: 0
    val progressPercent = ((course?.progressFraction ?: 0f) * 100f).roundToInt()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            val nextLessonId = detail?.nextActionLessonId
            val isCourseFinished = course?.isCompleted == true
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PpScrimVertical)
                    .navigationBarsPadding()
                    .padding(horizontal = PpSpacing.lg, vertical = PpSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                PpPrimaryButton(
                    text = if (isCourseFinished) {
                        stringResource(R.string.course_cta_review)
                    } else {
                        stringResource(R.string.course_cta_continue)
                    },
                    onClick = { nextLessonId?.let(onOpenLesson) },
                    enabled = nextLessonId != null && course?.isUnlocked == true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .testTag("course_detail_continue_button")
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
                        onClick = onBack,
                        modifier = Modifier.testTag("course_detail_back_button")
                    )
                }

                item {
                    PpCard(
                        accent = PpGlassCyan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PpCourtGraphic(
                            diagramId = when (order) {
                                2 -> "court_overview"
                                4 -> "diagonal_serve"
                                6 -> "net_zone"
                                8 -> "glass_rebound"
                                else -> null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(128.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = course?.title?.ifBlank { courseTitle(order) } ?: courseTitle(order),
                        style = PpTextStyles.H1,
                        color = PpTextHigh
                    )
                    Spacer(Modifier.height(PpSpacing.xs))
                    Text(
                        text = course?.description?.ifBlank { courseDescription(order) } ?: courseDescription(order),
                        style = PpTextStyles.Body,
                        color = PpTextMid
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
                                style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
                                color = PpLime
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.course_lessons_count_format,
                                    lessonCount,
                                    completedCount,
                                    lessonCount
                                ),
                                style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                                color = PpTextMid
                            )
                        }
                        Spacer(Modifier.height(PpSpacing.sm))
                        PpProgressBar(
                            progress = course?.progressFraction ?: 0f,
                            brush = PpBallTrail,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    PpSectionHeader(title = stringResource(R.string.course_section_lessons))
                }

                items(detail?.lessons.orEmpty(), key = { it.id }) { lesson ->
                    val bestPercent = lesson.bestQuizScore?.let { (it * 100f).roundToInt() }
                    val metaText = buildString {
                        append(
                            pluralStringResource(
                                R.plurals.course_lesson_meta_format,
                                lesson.quizQuestionCount,
                                lesson.estimatedMinutes,
                                lesson.quizQuestionCount
                            )
                        )
                        if (lesson.isCompleted && bestPercent != null) {
                            append(" · ")
                            append(stringResource(R.string.course_lesson_best_score_format, bestPercent))
                        }
                    }
                    val xpBadge = if (lesson.isCompleted) {
                        stringResource(R.string.course_lesson_done_badge)
                    } else {
                        stringResource(R.string.course_lesson_xp_format, lesson.xpReward)
                    }
                    PpLessonRow(
                        index = lesson.order,
                        title = lesson.title,
                        meta = metaText,
                        xpLabel = xpBadge,
                        isCompleted = lesson.isCompleted,
                        isCurrent = lesson.isCurrent,
                        isUnlocked = lesson.isUnlocked,
                        enabled = true,
                        onClick = {
                            if (lesson.isUnlocked) {
                                onOpenLesson(lesson.id)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(lockedLessonHint)
                                }
                            }
                        },
                        modifier = Modifier.testTag("lesson_row_${lesson.id}")
                    )
                }
            }
        }
    }
}
