package com.padelpro.app.feature.academy

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MenuBook
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
import com.padelpro.app.core.designsystem.components.PpCourtGraphic
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpPrimaryTint
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.academy.LessonBlock
import com.padelpro.app.domain.academy.LessonBlockType

@Composable
fun LessonScreen(
    lessonId: String,
    viewModel: LessonViewModel,
    onExit: () -> Unit,
    onStartQuiz: (String) -> Unit
) {
    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lesson = state.lesson
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler {
        showExitConfirm = true
    }

    Scaffold(
        containerColor = PpBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PpBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = PpSpacing.lg, vertical = PpSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
            ) {
                PpSecondaryButton(
                    text = stringResource(R.string.lesson_btn_previous),
                    onClick = { viewModel.previousPage() },
                    enabled = lesson != null && !state.isFirstPage,
                    modifier = Modifier
                        .weight(0.4f)
                        .testTag("lesson_previous_button")
                )
                if (state.isLastPage) {
                    PpPrimaryButton(
                        text = stringResource(R.string.lesson_btn_take_quiz),
                        onClick = { onStartQuiz(lessonId) },
                        enabled = lesson != null && lesson.quiz.isNotEmpty(),
                        modifier = Modifier
                            .weight(0.6f)
                            .testTag("lesson_take_quiz_button")
                    )
                } else {
                    PpPrimaryButton(
                        text = stringResource(R.string.lesson_btn_next),
                        onClick = { viewModel.nextPage() },
                        enabled = lesson != null && state.totalPages > 0,
                        modifier = Modifier
                            .weight(0.6f)
                            .testTag("lesson_next_button")
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
                        modifier = Modifier.testTag("lesson_close_button")
                    )
                    Spacer(Modifier.width(PpSpacing.sm))
                    val totalSegments = state.totalPages.coerceAtLeast(1)
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(PpSpacing.xs)
                    ) {
                        for (i in 0 until totalSegments) {
                            val active = i <= state.currentPageIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(PpRadius.pill)
                                    .background(if (active) PpPrimary else PpOutline)
                                    .clickable(enabled = lesson != null) {
                                        viewModel.selectPage(i)
                                    }
                            )
                        }
                    }
                    Spacer(Modifier.width(PpSpacing.sm))
                    Text(
                        text = if (state.totalPages > 0) {
                            stringResource(
                                R.string.lesson_page_counter_format,
                                state.currentPageIndex + 1,
                                state.totalPages
                            )
                        } else {
                            stringResource(R.string.lesson_page_counter_format, 0, 0)
                        },
                        style = PpTextStyles.Label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (lesson == null) {
                    PpEmptyState(
                        icon = Icons.Rounded.MenuBook,
                        title = if (state.isLoading) {
                            stringResource(R.string.lesson_loading_title)
                        } else {
                            stringResource(R.string.lesson_empty_title)
                        },
                        subtitle = stringResource(R.string.lesson_empty_body)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.xs)) {
                        Text(
                            text = stringResource(
                                R.string.lesson_header_meta_format,
                                lesson.courseTitle.uppercase(),
                                lesson.order
                            ),
                            style = PpTextStyles.Micro,
                            color = PpPrimary
                        )
                        Text(
                            text = lesson.title,
                            style = PpTextStyles.H1,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    AnimatedContent(
                        targetState = state.currentPageIndex,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith
                                fadeOut(animationSpec = tween(180))
                        },
                        label = "LessonBlockTransition"
                    ) { pageIdx ->
                        val block = lesson.blocks.getOrNull(pageIdx)
                        if (block != null) {
                            LessonBlockContentCard(block = block)
                        }
                    }

                    if (lesson.keyFacts.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(PpRadius.card))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, PpOutline, RoundedCornerShape(PpRadius.card))
                                .padding(PpSpacing.lg)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(PpSpacing.sm)) {
                                Text(
                                    text = stringResource(R.string.lesson_key_facts),
                                    style = PpTextStyles.Micro,
                                    color = PpPrimary
                                )
                                lesson.keyFacts.forEach { fact ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 7.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(PpPrimary)
                                        )
                                        Spacer(Modifier.width(PpSpacing.sm))
                                        Text(
                                            text = fact,
                                            style = PpTextStyles.Body,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirm) {
        PpConfirmDialog(
            title = stringResource(R.string.lesson_exit_title),
            body = stringResource(R.string.lesson_exit_body),
            confirmText = stringResource(R.string.lesson_exit_confirm),
            onConfirm = {
                showExitConfirm = false
                onExit()
            },
            onDismiss = { showExitConfirm = false }
        )
    }
}

@Composable
private fun LessonBlockContentCard(block: LessonBlock) {
    when (block.type) {
        LessonBlockType.TEXT -> {
            PpCard(modifier = Modifier.fillMaxWidth()) {
                if (!block.title.isNullOrBlank()) {
                    Text(
                        text = block.title,
                        style = PpTextStyles.H2,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(PpSpacing.sm))
                }
                Text(
                    text = block.body,
                    style = PpTextStyles.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LessonBlockType.FACT -> {
            PpCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = PpPrimary.copy(alpha = 0.65f),
                containerColor = PpPrimaryTint.copy(alpha = 0.35f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = PpPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.lesson_block_fact_badge),
                        style = PpTextStyles.Micro,
                        color = PpPrimary
                    )
                }
                if (!block.title.isNullOrBlank()) {
                    Spacer(Modifier.height(PpSpacing.sm))
                    Text(
                        text = block.title,
                        style = PpTextStyles.Title,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.height(PpSpacing.sm))
                Text(
                    text = block.body,
                    style = PpTextStyles.Body,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        LessonBlockType.DIAGRAM -> {
            PpCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.lesson_block_diagram_badge),
                    style = PpTextStyles.Micro,
                    color = PpPrimary
                )
                if (!block.title.isNullOrBlank()) {
                    Spacer(Modifier.height(PpSpacing.xs))
                    Text(
                        text = block.title,
                        style = PpTextStyles.H2,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.height(PpSpacing.md))
                PpCourtGraphic(
                    diagramId = block.diagramId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                )
                val captionRes = diagramCaptionRes(block.diagramId)
                if (captionRes != null) {
                    Spacer(Modifier.height(PpSpacing.sm))
                    Text(
                        text = stringResource(captionRes),
                        style = PpTextStyles.Label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(PpSpacing.md))
                Text(
                    text = block.body,
                    style = PpTextStyles.Body,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

private fun diagramCaptionRes(diagramId: String?): Int? = when (diagramId) {
    "court_overview" -> R.string.lesson_diagram_caption_overview
    "service_boxes" -> R.string.lesson_diagram_caption_service_boxes
    "diagonal_serve" -> R.string.lesson_diagram_caption_diagonal_serve
    "glass_rebound" -> R.string.lesson_diagram_caption_glass_rebound
    "net_zone" -> R.string.lesson_diagram_caption_net_zone
    else -> null
}
