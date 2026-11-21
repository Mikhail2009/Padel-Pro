package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpCourtBlue
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpCourtNight
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpLimeGlow
import com.padelpro.app.core.designsystem.theme.PpTextHigh

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

        // Subtle blue night-court surface inside court boundaries
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(PpCourtNight, PpCourtBlue.copy(alpha = 0.22f), PpCourtNight)
            ),
            topLeft = Offset(padX, padY),
            size = Size(cw, ch),
            cornerRadius = CornerRadius(4.dp.toPx())
        )

        // Outer glass court rectangle
        drawRect(
            color = if (diagramId == "glass_rebound") PpGlassCyan else PpCourtLineStrong,
            topLeft = Offset(padX, padY),
            size = Size(cw, ch),
            style = Stroke(width = if (diagramId == "glass_rebound") 2.5.dp.toPx() else 1.8.dp.toPx())
        )

        when {
            diagramId == "service_boxes" || highlightServiceBoxes -> {
                drawRect(
                    color = PpGlassCyan.copy(alpha = 0.14f),
                    topLeft = Offset(leftServiceX, padY),
                    size = Size(rightServiceX - leftServiceX, ch)
                )
            }
            diagramId == "diagonal_serve" -> {
                drawRect(
                    color = PpGlassCyan.copy(alpha = 0.18f),
                    topLeft = Offset(netX, padY),
                    size = Size(rightServiceX - netX, ch * 0.5f)
                )
            }
            diagramId == "net_zone" -> {
                drawRect(
                    color = PpGlassCyan.copy(alpha = 0.16f),
                    topLeft = Offset(padX + cw * 0.36f, padY),
                    size = Size(cw * 0.28f, ch)
                )
            }
            diagramId == "glass_rebound" -> {
                drawRect(
                    color = PpGlassCyan.copy(alpha = 0.16f),
                    topLeft = Offset(rightServiceX, padY),
                    size = Size(padX + cw - rightServiceX, ch)
                )
            }
        }

        // Service lines
        val serviceLineColor = if (diagramId == "service_boxes") PpGlassCyan else PpCourtLineStrong
        drawLine(
            color = serviceLineColor,
            start = Offset(leftServiceX, padY),
            end = Offset(leftServiceX, padY + ch),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = serviceLineColor,
            start = Offset(rightServiceX, padY),
            end = Offset(rightServiceX, padY + ch),
            strokeWidth = 1.5.dp.toPx()
        )

        // Center service line
        drawLine(
            color = serviceLineColor,
            start = Offset(leftServiceX, centerY),
            end = Offset(rightServiceX, centerY),
            strokeWidth = 1.5.dp.toPx()
        )

        // Net line
        drawLine(
            color = PpTextHigh.copy(alpha = 0.55f),
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
                    brush = Brush.linearGradient(listOf(PpGlassCyan, PpLime), serveStart, serveEnd),
                    start = serveStart,
                    end = serveEnd,
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = PpGlassCyan, radius = 4.dp.toPx(), center = serveStart)
                drawCircle(color = PpLimeGlow.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = serveEnd)
                drawCircle(color = PpLime, radius = 5.dp.toPx(), center = serveEnd)
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
                    color = PpLime,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(color = PpGlassCyan, radius = 4.dp.toPx(), center = wall)
                drawCircle(color = PpLimeGlow.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = rebound)
                drawCircle(color = PpLime, radius = 4.5.dp.toPx(), center = rebound)
            }
            "net_zone" -> {
                val player1 = Offset(padX + cw * 0.41f, padY + ch * 0.28f)
                val player2 = Offset(padX + cw * 0.41f, padY + ch * 0.72f)
                drawCircle(color = PpGlassCyan, radius = 5.5.dp.toPx(), center = player1)
                drawCircle(color = PpGlassCyan, radius = 5.5.dp.toPx(), center = player2)
                if (showTrajectory) {
                    drawLine(
                        color = PpLime,
                        start = player1,
                        end = Offset(padX + cw * 0.86f, padY + ch * 0.68f),
                        strokeWidth = 2.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                    )
                    drawCircle(color = PpLime, radius = 4.dp.toPx(), center = Offset(padX + cw * 0.86f, padY + ch * 0.68f))
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
                    brush = Brush.horizontalGradient(listOf(PpGlassCyan, PpLime)),
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
                val endPt = Offset(padX + cw * 0.84f, padY + ch * 0.34f)
                drawCircle(color = PpLimeGlow.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = endPt)
                drawCircle(color = PpLime, radius = 4.5.dp.toPx(), center = endPt)
            }
        }
    }
}
