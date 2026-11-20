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
    showTrajectory: Boolean = true,
    diagramId: String? = null
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

        val leftServiceX = padX + cw * 0.18f
        val rightServiceX = padX + cw * 0.82f
        val netX = padX + cw * 0.5f
        val centerY = padY + ch * 0.5f

        // Outer glass court rectangle
        drawRect(
            color = if (diagramId == "glass_rebound") PpPrimary.copy(alpha = 0.7f) else PpOutline,
            topLeft = Offset(padX, padY),
            size = Size(cw, ch),
            style = Stroke(width = if (diagramId == "glass_rebound") 2.5.dp.toPx() else 2.dp.toPx())
        )

        when {
            diagramId == "service_boxes" || highlightServiceBoxes -> {
                drawRect(
                    color = PpPrimary.copy(alpha = 0.12f),
                    topLeft = Offset(leftServiceX, padY),
                    size = Size(rightServiceX - leftServiceX, ch)
                )
            }
            diagramId == "diagonal_serve" -> {
                // Highlight opposite receiving box (top-right quadrant between net and right service line)
                drawRect(
                    color = PpPrimary.copy(alpha = 0.14f),
                    topLeft = Offset(netX, padY),
                    size = Size(rightServiceX - netX, ch * 0.5f)
                )
            }
            diagramId == "net_zone" -> {
                // Highlight attacking zone near net
                drawRect(
                    color = PpPrimary.copy(alpha = 0.14f),
                    topLeft = Offset(padX + cw * 0.36f, padY),
                    size = Size(cw * 0.28f, ch)
                )
            }
            diagramId == "glass_rebound" -> {
                // Highlight back glass bounce zone on right
                drawRect(
                    color = PpPrimary.copy(alpha = 0.12f),
                    topLeft = Offset(rightServiceX, padY),
                    size = Size(padX + cw - rightServiceX, ch)
                )
            }
        }

        // Service lines
        drawLine(
            color = if (diagramId == "service_boxes") PpPrimary.copy(alpha = 0.7f) else PpOutline,
            start = Offset(leftServiceX, padY),
            end = Offset(leftServiceX, padY + ch),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = if (diagramId == "service_boxes") PpPrimary.copy(alpha = 0.7f) else PpOutline,
            start = Offset(rightServiceX, padY),
            end = Offset(rightServiceX, padY + ch),
            strokeWidth = 1.5.dp.toPx()
        )

        // Center service line
        drawLine(
            color = if (diagramId == "service_boxes") PpPrimary.copy(alpha = 0.7f) else PpOutline,
            start = Offset(leftServiceX, centerY),
            end = Offset(rightServiceX, centerY),
            strokeWidth = 1.5.dp.toPx()
        )

        // Net line
        drawLine(
            color = PpPrimary.copy(alpha = 0.65f),
            start = Offset(netX, padY - 4.dp.toPx()),
            end = Offset(netX, padY + ch + 4.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        when (diagramId) {
            "diagonal_serve" -> {
                val serveStart = Offset(leftServiceX - cw * 0.08f, padY + ch * 0.75f)
                val serveEnd = Offset(netX + (rightServiceX - netX) * 0.58f, padY + ch * 0.24f)
                drawLine(
                    color = PpPrimary,
                    start = serveStart,
                    end = serveEnd,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = PpPrimary, radius = 4.dp.toPx(), center = serveStart)
                drawCircle(color = PpPrimary, radius = 5.dp.toPx(), center = serveEnd)
            }
            "glass_rebound" -> {
                val p1 = Offset(padX + cw * 0.22f, padY + ch * 0.30f)
                val bounce = Offset(padX + cw * 0.87f, padY + ch * 0.66f)
                val wall = Offset(padX + cw, padY + ch * 0.56f)
                val rebound = Offset(padX + cw * 0.78f, padY + ch * 0.42f)
                val path = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(bounce.x, bounce.y)
                    lineTo(wall.x, wall.y)
                    lineTo(rebound.x, rebound.y)
                }
                drawPath(
                    path = path,
                    color = PpPrimary,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(color = PpPrimary, radius = 4.5.dp.toPx(), center = rebound)
            }
            "net_zone" -> {
                val player1 = Offset(padX + cw * 0.41f, padY + ch * 0.28f)
                val player2 = Offset(padX + cw * 0.41f, padY + ch * 0.72f)
                drawCircle(color = PpPrimary, radius = 5.dp.toPx(), center = player1)
                drawCircle(color = PpPrimary, radius = 5.dp.toPx(), center = player2)
                if (showTrajectory) {
                    drawLine(
                        color = PpPrimary.copy(alpha = 0.75f),
                        start = player1,
                        end = Offset(padX + cw * 0.86f, padY + ch * 0.68f),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                    )
                }
            }
            else -> if (showTrajectory) {
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
