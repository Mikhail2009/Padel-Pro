package com.padelpro.app.feature.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLine
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpCourtPanel
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.MomentumPoint
import com.padelpro.app.domain.model.TeamSide
import kotlin.math.abs

data class MomentumChartSample(
    val point: MomentumPoint,
    val xFraction: Float
)

data class MomentumSetBoundary(
    val setIndex: Int,
    val xFraction: Float
)

/**
 * Подготавливает точки графика из реальных [MomentumPoint] без сглаживания и выдуманных данных.
 * Для длинных матчей (> [maxSamples]) сохраняет локальные минимумы/максимумы каждого интервала
 * и границы смены сетов, чтобы пики преимущества оставались точными на узком экране.
 */
fun prepareMomentumSamples(
    points: List<MomentumPoint>,
    maxSamples: Int = 96
): List<MomentumChartSample> {
    if (points.isEmpty()) return emptyList()
    if (points.size == 1) {
        return listOf(MomentumChartSample(point = points.first(), xFraction = 0.5f))
    }

    val firstRally = points.first().rallyIndex
    val rallySpan = (points.last().rallyIndex - firstRally).coerceAtLeast(1)

    fun toSample(point: MomentumPoint): MomentumChartSample {
        val fraction = ((point.rallyIndex - firstRally).toFloat() / rallySpan.toFloat()).coerceIn(0f, 1f)
        return MomentumChartSample(point = point, xFraction = fraction)
    }

    if (points.size <= maxSamples) {
        return points.map(::toSample)
    }

    val bucketCount = (maxSamples / 2).coerceAtLeast(16)
    val selected = LinkedHashMap<Int, MomentumPoint>()
    selected[points.first().rallyIndex] = points.first()

    for (i in 1 until points.size) {
        if (points[i].setIndex != points[i - 1].setIndex) {
            selected[points[i].rallyIndex] = points[i]
        }
    }

    for (bucket in 0 until bucketCount) {
        val start = (bucket * points.size) / bucketCount
        val end = ((bucket + 1) * points.size) / bucketCount
        if (start >= end) continue

        var minPoint = points[start]
        var maxPoint = points[start]
        for (idx in start until end) {
            val candidate = points[idx]
            if (candidate.pointDifference < minPoint.pointDifference) {
                minPoint = candidate
            }
            if (candidate.pointDifference > maxPoint.pointDifference) {
                maxPoint = candidate
            }
        }
        selected[minPoint.rallyIndex] = minPoint
        selected[maxPoint.rallyIndex] = maxPoint
    }

    selected[points.last().rallyIndex] = points.last()

    return selected.values
        .sortedBy { it.rallyIndex }
        .map(::toSample)
}

fun extractMomentumSetBoundaries(points: List<MomentumPoint>): List<MomentumSetBoundary> {
    if (points.size < 2) return emptyList()
    val firstRally = points.first().rallyIndex
    val rallySpan = (points.last().rallyIndex - firstRally).coerceAtLeast(1)
    val boundaries = mutableListOf<MomentumSetBoundary>()
    for (i in 1 until points.size) {
        if (points[i].setIndex != points[i - 1].setIndex) {
            val fraction = ((points[i].rallyIndex - firstRally).toFloat() / rallySpan.toFloat())
                .coerceIn(0f, 1f)
            boundaries += MomentumSetBoundary(setIndex = points[i].setIndex, xFraction = fraction)
        }
    }
    return boundaries
}

fun findNearestMomentumSample(
    samples: List<MomentumChartSample>,
    touchXFraction: Float
): MomentumChartSample? {
    if (samples.isEmpty()) return null
    val target = touchXFraction.coerceIn(0f, 1f)
    return samples.minByOrNull { abs(it.xFraction - target) }
}

@Composable
fun MomentumChartCardContent(
    momentum: List<MomentumPoint>,
    teamALabel: String,
    teamBLabel: String,
    modifier: Modifier = Modifier
) {
    val samples = remember(momentum) { prepareMomentumSamples(momentum) }
    val setBoundaries = remember(momentum) { extractMomentumSetBoundaries(momentum) }
    val maxAbs = remember(momentum) {
        momentum.maxOfOrNull { abs(it.pointDifference) }?.coerceAtLeast(1) ?: 1
    }
    var selectedRallyIndex by remember(momentum) { mutableStateOf<Int?>(null) }
    val selectedSample = remember(samples, selectedRallyIndex) {
        selectedRallyIndex?.let { target ->
            samples.firstOrNull { it.point.rallyIndex == target }
        } ?: samples.lastOrNull()
    }
    val hasExplicitSelection = selectedRallyIndex != null

    val motion = LocalPpMotion.current
    val reveal = remember(momentum) { Animatable(if (motion.enabled) 0f else 1f) }
    LaunchedEffect(momentum, motion.enabled) {
        if (motion.enabled) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, motion.glide())
        } else {
            reveal.snapTo(1f)
        }
    }

    val chartCd = stringResource(R.string.insights_momentum_chart_cd)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.insights_momentum_subtitle),
            style = PpTextStyles.Label,
            color = PpTextMid
        )
        Spacer(Modifier.height(PpSpacing.md))

        // Легенда команд
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PpSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(
                color = PpLime,
                text = stringResource(R.string.insights_momentum_legend_a, teamALabel),
                modifier = Modifier.weight(1f)
            )
            LegendItem(
                color = PpGlassCyan,
                text = stringResource(R.string.insights_momentum_legend_b, teamBLabel),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(PpSpacing.md))

        // Область графика с Y-метками и интерактивным Canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(34.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "+$maxAbs",
                    style = PpTextStyles.Overline.copy(fontFeatureSettings = "tnum"),
                    color = PpLime,
                    textAlign = TextAlign.End
                )
                Text(
                    text = stringResource(R.string.insights_momentum_zero_label),
                    style = PpTextStyles.Overline.copy(fontFeatureSettings = "tnum"),
                    color = PpTextMid,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "-$maxAbs",
                    style = PpTextStyles.Overline.copy(fontFeatureSettings = "tnum"),
                    color = PpGlassCyan,
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.width(PpSpacing.sm))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(PpRadius.inputShape)
                    .background(PpCourtGlass.copy(alpha = 0.55f))
                    .border(1.dp, PpCourtLine, PpRadius.inputShape)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = chartCd }
                        .testTag("insights_momentum_canvas")
                        .pointerInput(samples) {
                            detectTapGestures { offset ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val pad = 8.dp.toPx()
                                val usableW = (w - pad * 2f).coerceAtLeast(1f)
                                val fraction = ((offset.x - pad) / usableW).coerceIn(0f, 1f)
                                selectedRallyIndex = findNearestMomentumSample(samples, fraction)?.point?.rallyIndex
                            }
                        }
                        .pointerInput(samples) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val pad = 8.dp.toPx()
                                val usableW = (w - pad * 2f).coerceAtLeast(1f)
                                val fraction = ((change.position.x - pad) / usableW).coerceIn(0f, 1f)
                                selectedRallyIndex = findNearestMomentumSample(samples, fraction)?.point?.rallyIndex
                            }
                        }
                ) {
                    val hPad = 8.dp.toPx()
                    val vPad = 12.dp.toPx()
                    val plotW = (size.width - hPad * 2f).coerceAtLeast(1f)
                    val zeroY = size.height / 2f
                    val halfH = (size.height / 2f - vPad).coerceAtLeast(1f)

                    fun xFor(fraction: Float): Float = hPad + plotW * fraction.coerceIn(0f, 1f)
                    fun yFor(diff: Int): Float = zeroY - (diff.toFloat() / maxAbs.toFloat()) * halfH

                    // Верхняя и нижняя направляющие
                    drawLine(
                        color = PpCourtLine.copy(alpha = 0.7f),
                        start = Offset(hPad, yFor(maxAbs)),
                        end = Offset(size.width - hPad, yFor(maxAbs)),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = PpCourtLine.copy(alpha = 0.7f),
                        start = Offset(hPad, yFor(-maxAbs)),
                        end = Offset(size.width - hPad, yFor(-maxAbs)),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Разделители сетов
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    setBoundaries.forEach { boundary ->
                        val bx = xFor(boundary.xFraction)
                        drawLine(
                            color = PpCourtLineStrong.copy(alpha = 0.75f),
                            start = Offset(bx, vPad),
                            end = Offset(bx, size.height - vPad),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    // Нулевая линия равенства
                    drawLine(
                        color = PpCourtLineStrong,
                        start = Offset(0f, zeroY),
                        end = Offset(size.width, zeroY),
                        strokeWidth = 1.4.dp.toPx()
                    )

                    val revealRight = size.width * reveal.value.coerceIn(0f, 1f)

                    if (samples.size == 1) {
                        val single = samples.first()
                        val sx = xFor(single.xFraction)
                        val sy = yFor(single.point.pointDifference)
                        val singleColor = when {
                            single.point.pointDifference > 0 -> PpLime
                            single.point.pointDifference < 0 -> PpGlassCyan
                            else -> PpTextHigh
                        }
                        if (revealRight >= sx) {
                            drawLine(
                                color = singleColor,
                                start = Offset(sx, zeroY),
                                end = Offset(sx, sy),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = PpCourtPanel, radius = 6.dp.toPx(), center = Offset(sx, sy))
                            drawCircle(color = singleColor, radius = 4.dp.toPx(), center = Offset(sx, sy))
                        }
                    } else if (samples.size > 1) {
                        val linePath = Path()
                        val areaPath = Path()

                        val first = samples.first()
                        val startX = xFor(first.xFraction)
                        val startY = yFor(first.point.pointDifference)
                        linePath.moveTo(startX, startY)
                        areaPath.moveTo(startX, zeroY)
                        areaPath.lineTo(startX, startY)

                        for (i in 1 until samples.size) {
                            val s = samples[i]
                            val px = xFor(s.xFraction)
                            val py = yFor(s.point.pointDifference)
                            linePath.lineTo(px, py)
                            areaPath.lineTo(px, py)
                        }

                        val lastX = xFor(samples.last().xFraction)
                        areaPath.lineTo(lastX, zeroY)
                        areaPath.close()

                        val positiveBrush = Brush.verticalGradient(
                            colors = listOf(
                                PpLime.copy(alpha = 0.28f),
                                PpLime.copy(alpha = 0.04f)
                            ),
                            startY = yFor(maxAbs),
                            endY = zeroY
                        )
                        val negativeBrush = Brush.verticalGradient(
                            colors = listOf(
                                PpGlassCyan.copy(alpha = 0.04f),
                                PpGlassCyan.copy(alpha = 0.28f)
                            ),
                            startY = zeroY,
                            endY = yFor(-maxAbs)
                        )
                        val stroke = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )

                        // Верхняя полуплоскость (преимущество команды A)
                        clipRect(left = 0f, top = 0f, right = revealRight, bottom = zeroY) {
                            drawPath(path = areaPath, brush = positiveBrush)
                            drawPath(path = linePath, color = PpLime, style = stroke)
                        }
                        // Нижняя полуплоскость (преимущество команды B)
                        clipRect(left = 0f, top = zeroY, right = revealRight, bottom = size.height) {
                            drawPath(path = areaPath, brush = negativeBrush)
                            drawPath(path = linePath, color = PpGlassCyan, style = stroke)
                        }

                        // Точки только при небольшом количестве розыгрышей, чтобы длинные матчи оставались чистыми
                        if (samples.size <= 32) {
                            samples.forEach { sample ->
                                val cx = xFor(sample.xFraction)
                                if (cx <= revealRight) {
                                    val cy = yFor(sample.point.pointDifference)
                                    val dotColor = when {
                                        sample.point.pointDifference > 0 -> PpLime
                                        sample.point.pointDifference < 0 -> PpGlassCyan
                                        else -> PpTextMid
                                    }
                                    drawCircle(
                                        color = dotColor,
                                        radius = 2.2.dp.toPx(),
                                        center = Offset(cx, cy)
                                    )
                                }
                            }
                        }
                    }

                    // Активный маркер выбранного/последнего розыгрыша
                    selectedSample?.let { selected ->
                        val sx = xFor(selected.xFraction)
                        val sy = yFor(selected.point.pointDifference)
                        if (sx <= revealRight) {
                            val markerColor = when {
                                selected.point.pointDifference > 0 -> PpLime
                                selected.point.pointDifference < 0 -> PpGlassCyan
                                else -> PpTextHigh
                            }
                            drawLine(
                                color = PpTextMid.copy(alpha = 0.45f),
                                start = Offset(sx, vPad),
                                end = Offset(sx, size.height - vPad),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawCircle(
                                color = PpCourtPanel,
                                radius = 6.dp.toPx(),
                                center = Offset(sx, sy)
                            )
                            drawCircle(
                                color = markerColor,
                                radius = 4.dp.toPx(),
                                center = Offset(sx, sy)
                            )
                        }
                    }
                }
            }
        }

        // Подпись начала/конца розыгрышей + сеты
        if (momentum.isNotEmpty()) {
            Spacer(Modifier.height(PpSpacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#1",
                    style = PpTextStyles.Overline.copy(fontFeatureSettings = "tnum"),
                    color = PpTextLow
                )
                if (setBoundaries.isNotEmpty()) {
                    Text(
                        text = buildString {
                            append(stringResource(R.string.live_match_set_chip_format, momentum.first().setIndex))
                            setBoundaries.forEach { boundary ->
                                append(" · ")
                                append(stringResource(R.string.live_match_set_chip_format, boundary.setIndex))
                            }
                        },
                        style = PpTextStyles.Overline.copy(fontFeatureSettings = "tnum"),
                        color = PpTextMid
                    )
                }
                Text(
                    text = "#${momentum.last().rallyIndex}",
                    style = PpTextStyles.Overline.copy(fontFeatureSettings = "tnum"),
                    color = PpTextLow
                )
            }
        }

        // Панель выбранного розыгрыша
        selectedSample?.point?.let { pt ->
            Spacer(Modifier.height(PpSpacing.md))
            val pointWinnerName = if (pt.winner == TeamSide.A) teamALabel else teamBLabel
            val pointWinnerColor = if (pt.winner == TeamSide.A) PpLime else PpGlassCyan
            val diffText = when {
                pt.pointDifference > 0 -> stringResource(
                    R.string.insights_momentum_point_diff_a_format,
                    teamALabel,
                    pt.pointDifference
                )
                pt.pointDifference < 0 -> stringResource(
                    R.string.insights_momentum_point_diff_b_format,
                    teamBLabel,
                    abs(pt.pointDifference)
                )
                else -> stringResource(R.string.insights_momentum_point_diff_even)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PpRadius.inputShape)
                    .background(PpCourtGlass)
                    .border(1.dp, PpCourtLineStrong, PpRadius.inputShape)
                    .padding(horizontal = PpSpacing.md, vertical = PpSpacing.sm + 2.dp)
                    .testTag("insights_momentum_selected_box")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.insights_momentum_point_header_format,
                            pt.rallyIndex,
                            pt.setIndex
                        ),
                        style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
                        color = PpTextHigh
                    )
                    Text(
                        text = stringResource(
                            R.string.insights_momentum_point_games_format,
                            pt.gamesA,
                            pt.gamesB
                        ),
                        style = PpTextStyles.Micro.copy(fontFeatureSettings = "tnum"),
                        color = PpTextMid
                    )
                }
                Spacer(Modifier.height(PpSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.insights_momentum_point_winner_format, pointWinnerName),
                        style = PpTextStyles.Label,
                        color = pointWinnerColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(PpSpacing.sm))
                    Text(
                        text = diffText,
                        style = PpTextStyles.Label.copy(fontFeatureSettings = "tnum"),
                        color = PpTextHigh,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!hasExplicitSelection) {
                    Spacer(Modifier.height(PpSpacing.xs))
                    Text(
                        text = stringResource(R.string.insights_momentum_tap_hint),
                        style = PpTextStyles.Overline,
                        color = PpTextLow
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(PpSpacing.xs + 2.dp))
        Text(
            text = text,
            style = PpTextStyles.Micro,
            color = PpTextMid,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
