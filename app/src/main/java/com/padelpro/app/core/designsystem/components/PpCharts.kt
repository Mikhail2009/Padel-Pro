package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpCard
import com.padelpro.app.core.designsystem.theme.PpOutline
import com.padelpro.app.core.designsystem.theme.PpPrimary
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.MonthlyMatchCount

@Composable
fun PpChartDonut(
    winRatePercent: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            if (winRatePercent == null) {
                drawArc(
                    color = PpOutline,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
            } else {
                drawArc(
                    color = PpOutline,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                val sweep = (winRatePercent.coerceIn(0, 100) / 100f) * 360f
                drawArc(
                    color = PpPrimary,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (winRatePercent != null) "$winRatePercent%" else stringResource(R.string.dash_placeholder),
                style = PpTextStyles.H1,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.performance_win_rate_label),
                style = PpTextStyles.Micro,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PpChartBar(
    items: List<MonthlyMatchCount>,
    modifier: Modifier = Modifier
) {
    val maxCount = (items.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.count.toString(),
                        style = PpTextStyles.Label,
                        color = if (item.count > 0 && item.isCurrentMonth) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(PpSpacing.xs))
                    val fraction = if (item.count == 0) 0.06f else (item.count.toFloat() / maxCount.toFloat()).coerceIn(0.12f, 1f)
                    Canvas(
                        modifier = Modifier
                            .size(width = 22.dp, height = 74.dp)
                    ) {
                        val barH = size.height * fraction
                        val barColor = when {
                            item.count == 0 -> PpOutline
                            item.isCurrentMonth -> PpPrimary
                            else -> PpCard
                        }
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(0f, size.height - barH),
                            size = Size(size.width, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(PpSpacing.xs))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            drawLine(
                color = PpOutline,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
        Spacer(Modifier.height(PpSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEach { item ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.label,
                        style = PpTextStyles.Micro,
                        color = if (item.isCurrentMonth) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PpChartLine(
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        drawLine(
            color = PpOutline,
            start = Offset(0f, size.height - 1.dp.toPx()),
            end = Offset(size.width, size.height - 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        if (points.size >= 2) {
            val path = Path()
            val stepX = size.width / (points.size - 1)
            points.forEachIndexed { idx, value ->
                val x = idx * stepX
                val y = size.height - (value.coerceIn(0f, 1f) * (size.height * 0.8f) + size.height * 0.1f)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = PpPrimary,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
