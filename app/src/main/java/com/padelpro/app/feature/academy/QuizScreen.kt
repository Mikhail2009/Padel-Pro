package com.padelpro.app.feature.academy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Quiz
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpConfirmDialog
import com.padelpro.app.core.designsystem.components.PpEmptyState
import com.padelpro.app.core.designsystem.components.PpIconButton
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpProgressBar
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun QuizScreen(
    lessonId: String,
    onExit: () -> Unit,
    onCompleteQuiz: (String) -> Unit
) {
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
                PpPrimaryButton(
                    text = stringResource(R.string.quiz_btn_check),
                    onClick = { onCompleteQuiz(lessonId) },
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
                    PpProgressBar(
                        progress = 0f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(PpSpacing.sm))
                    Text(
                        text = stringResource(R.string.quiz_question_counter_format, 0, 0),
                        style = PpTextStyles.Micro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.quiz_xp_hint),
                    style = PpTextStyles.Label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PpEmptyState(
                    icon = Icons.Rounded.Quiz,
                    title = stringResource(R.string.quiz_empty_title),
                    subtitle = stringResource(R.string.quiz_empty_body)
                )
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
