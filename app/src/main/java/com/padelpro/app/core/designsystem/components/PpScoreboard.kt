package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpCard
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun PpCourtGraphic(
    modifier: Modifier = Modifier,
    highlightServiceBoxes: Boolean = false,
    showTrajectory: Boolean = true
) {
    val cd = stringResource(R.string.cd_court_diagram)
    Canvas(
        modifier = modifier
            .semantics { contentDescription = cd }
    ) {
        val w = size.width
        val h = size.height
        val padX = w * 0.08f
        val padY = h * 0.10f
        val cw = w - padX * 2
        val ch = h - padY * 2

        // Outer glass court rectangle
        drawRect(
            color = PpOutline,
            topLeft = Offset(padX, padY),
            size = Size(cw, ch),
            style = Stroke(width = 2.dp.toPx())
        )

        // Optional service box highlight
        if (highlightServiceBoxes) {
            drawRect(
                color = PpPrimary.copy(alpha = 0.10f),
                topLeft = Offset(padX, padY + ch * 0.18f),
                size = Size(cw, ch * 0.64f)
            )
        }

        // Service lines
        val leftServiceX = padX + cw * 0.18f
        val rightServiceX = padX + cw * 0.82f
        drawLine(
            color = PpOutline,
            start = Offset(leftServiceX, padY),
            end = Offset(leftServiceX, padY + ch),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = PpOutline,
            start = Offset(rightServiceX, padY),
            end = Offset(rightServiceX, padY + ch),
            strokeWidth = 1.5.dp.toPx()
        )

        // Center service line
        drawLine(
            color = PpOutline,
            start = Offset(leftServiceX, padY + ch * 0.5f),
            end = Offset(rightServiceX, padY + ch * 0.5f),
            strokeWidth = 1.5.dp.toPx()
        )

        // Net line
        drawLine(
            color = PpPrimary.copy(alpha = 0.65f),
            start = Offset(padX + cw * 0.5f, padY - 4.dp.toPx()),
            end = Offset(padX + cw * 0.5f, padY + ch + 4.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        // Subtle lime ball trajectory arc
        if (showTrajectory) {
            val arc = Path().apply {
                moveTo(padX + cw * 0.14f, padY + ch * 0.72f)
                quadraticTo(
                    padX + cw * 0.52f,
                    padY + ch * 0.22f,
                    padX + cw * 0.84f,
                    padY + ch * 0.34f
                )
            }
            drawPath(
                path = arc,
                color = PpPrimary,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawCircle(
                color = PpPrimary,
                radius = 4.5.dp.toPx(),
                center = Offset(padX + cw * 0.84f, padY + ch * 0.34f)
            )
        }
    }
}

@Composable
fun PpScoreboard(
    teamALabel: String,
    teamBLabel: String,
    scoreA: String,
    scoreB: String,
    servingTeamA: Boolean,
    servingTeamB: Boolean,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    contextChipText: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
    ) {
        ScoreHalfCard(
            teamLabel = teamALabel,
            score = scoreA,
            isServing = servingTeamA,
            accentBorder = true,
            enabled = enabled,
            onClick = onPointA
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
            Text(
                text = contextChipText ?: stringResource(R.string.label_vs),
                style = PpTextStyles.Micro,
                color = if (contextChipText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = PpSpacing.md)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
        }

        ScoreHalfCard(
            teamLabel = teamBLabel,
            score = scoreB,
            isServing = servingTeamB,
            accentBorder = false,
            enabled = enabled,
            onClick = onPointB
        )
    }
}

@Composable
private fun ScoreHalfCard(
    teamLabel: String,
    score: String,
    isServing: Boolean,
    accentBorder: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(PpRadius.card)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 144.dp)
            .alpha(if (enabled) 1f else 0.72f)
            .clip(shape)
            .background(PpCard)
            .border(
                width = 1.dp,
                color = if (accentBorder) PpPrimary.copy(alpha = 0.45f) else PpOutline,
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(PpSpacing.xl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = teamLabel.uppercase(),
                        style = PpTextStyles.Micro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isServing) {
                        Spacer(Modifier.size(PpSpacing.sm))
                        PpServeIndicator(isServing = true)
                    }
                }
                Spacer(Modifier.height(PpSpacing.sm))
                Text(
                    text = score,
                    style = PpTextStyles.ScoreDisplay,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(PpRadius.pill)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, PpRadius.pill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
