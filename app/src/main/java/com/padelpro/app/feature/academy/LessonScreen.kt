package com.padelpro.app.feature.academy

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun LessonScreen(
    lessonId: String,
    onExit: () -> Unit,
    onStartQuiz: (String) -> Unit
) {
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
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(0.4f)
                )
                PpPrimaryButton(
                    text = stringResource(R.string.lesson_btn_take_quiz),
                    onClick = { onStartQuiz(lessonId) },
                    enabled = false,
                    modifier = Modifier.weight(0.6f)
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
                        onClick = { showExitConfirm = true }
                    )
                    Spacer(Modifier.width(PpSpacing.sm))
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(PpSpacing.xs)
                    ) {
                        for (i in 0 until 5) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(PpRadius.pill)
                                    .background(if (i == 0) PpPrimary else PpOutline)
                            )
                        }
                    }
                    Spacer(Modifier.width(PpSpacing.sm))
                    Text(
                        text = stringResource(R.string.lesson_page_counter_format, 0, 0),
                        style = PpTextStyles.Label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.lesson_empty_title),
                    style = PpTextStyles.H1,
                    color = MaterialTheme.colorScheme.onBackground
                )

                PpCard(modifier = Modifier.fillMaxWidth()) {
                    PpCourtGraphic(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        highlightServiceBoxes = true
                    )
                }

                PpEmptyState(
                    icon = Icons.Rounded.MenuBook,
                    title = stringResource(R.string.lesson_empty_title),
                    subtitle = stringResource(R.string.lesson_empty_body)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(PpRadius.card))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, PpOutline, RoundedCornerShape(PpRadius.card))
                        .padding(PpSpacing.lg)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.lesson_key_facts),
                            style = PpTextStyles.Micro,
                            color = PpPrimary
                        )
                        Spacer(Modifier.height(PpSpacing.sm))
                        Text(
                            text = stringResource(R.string.label_next_iteration_badge),
                            style = PpTextStyles.Body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
