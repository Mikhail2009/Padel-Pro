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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    onContinueNext: () -> Unit,
    onReturnToAcademy: () -> Unit
) {
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
                Text(
                    text = stringResource(R.string.completion_empty_subtitle),
                    style = PpTextStyles.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(PpSpacing.xl))

                Text(
                    text = stringResource(R.string.completion_xp_gained_format, 0),
                    style = PpTextStyles.DisplayNum,
                    color = PpPrimary
                )
                Spacer(Modifier.height(PpSpacing.lg))

                PpCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.completion_breakdown_lesson_format, 0),
                            style = PpTextStyles.Label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.completion_breakdown_quiz_format, 0, 0, 0),
                            style = PpTextStyles.Label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(PpSpacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.md)
                ) {
                    PpStatCard(
                        value = stringResource(R.string.dash_placeholder),
                        label = stringResource(R.string.completion_stat_quiz_score),
                        modifier = Modifier.weight(1f)
                    )
                    PpStatCard(
                        value = stringResource(R.string.completion_stat_ratio_format, 0, 4),
                        label = stringResource(R.string.completion_stat_course_lessons),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PpSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
            ) {
                PpPrimaryButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinueNext,
                    enabled = false
                )
                PpSecondaryButton(
                    text = stringResource(R.string.completion_btn_return_academy),
                    onClick = onReturnToAcademy
                )
            }
        }
    }
}
