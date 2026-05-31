package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SimulationChart(
    modifier: Modifier = Modifier,
    popHistory: List<Double>,
    fisherWageHistory: List<Double>,
    adminWageHistory: List<Double>,
    carryingCapacity: Double = 450000.0
) {
    val textMeasurer = rememberTextMeasurer()
    val axisLabelColor = Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegendItem(color = Color(0xFF2563EB), label = "Bio-Populasi (Ton)", isDashed = false)
            ChartLegendItem(color = Color(0xFFF97316), label = "Upah Nelayan (Rp)", isDashed = true)
            ChartLegendItem(color = Color(0xFF8B5CF6), label = "Upah Admin (Rp)", isDashed = true)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val paddingLeft = 50.dp.toPx()
            val paddingRight = 50.dp.toPx()
            val paddingTop = 15.dp.toPx()
            val paddingBottom = 25.dp.toPx()

            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

            // Scale configuration
            val maxPop = carryingCapacity.coerceAtLeast(450000.0)
            val maxWage = 10000000.0 // 10 Million Rp top of scale

            // 1. Draw Grid lines and horizontal axis marks
            val gridCount = 5
            for (i in 0..gridCount) {
                val ratio = i.toFloat() / gridCount
                val y = paddingTop + chartHeight * (1f - ratio)

                // Grid line
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(paddingLeft, y),
                    end = Offset(paddingLeft + chartWidth, y),
                    strokeWidth = 1f
                )

                // Left scale text: Tonnage (e.g. 500k, 250k)
                val popValueLabel = "${((maxPop * ratio) / 1000).toInt()}k t"
                drawText(
                    textMeasurer = textMeasurer,
                    text = popValueLabel,
                    topLeft = Offset(5.dp.toPx(), y - 8.dp.toPx()),
                    style = TextStyle(fontSize = 9.sp, color = Color(0xFF2563EB))
                )

                // Right scale text: Wage (e.g. 10jt, 5jt)
                val wageValueLabel = "${(maxWage * ratio / 1000000.0).toInt()}Jt"
                drawText(
                    textMeasurer = textMeasurer,
                    text = wageValueLabel,
                    topLeft = Offset(width - paddingRight + 5.dp.toPx(), y - 8.dp.toPx()),
                    style = TextStyle(fontSize = 9.sp, color = Color(0xFFF97316))
                )
            }

            // Draw X-axis year markings
            val yearMarkings = listOf(0, 2, 4, 6, 8, 10)
            yearMarkings.forEach { year ->
                val monthIdx = year * 12
                val x = paddingLeft + (monthIdx / 120f) * chartWidth
                
                drawLine(
                    color = Color(0xFF94A3B8),
                    start = Offset(x, paddingTop + chartHeight),
                    end = Offset(x, paddingTop + chartHeight + 4.dp.toPx()),
                    strokeWidth = 1.5f
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = "Th $year",
                    topLeft = Offset(x - 12.dp.toPx(), paddingTop + chartHeight + 6.dp.toPx()),
                    style = TextStyle(fontSize = 9.sp, color = axisLabelColor)
                )
            }

            // 2. Draw Population Series (Filled + Outline)
            if (popHistory.isNotEmpty()) {
                val popPath = Path()
                val popAreaPath = Path()

                popHistory.forEachIndexed { index, value ->
                    val x = paddingLeft + (index.toFloat() / 120f).coerceAtMost(1f) * chartWidth
                    val y = paddingTop + chartHeight * (1f - (value / maxPop).coerceIn(0.0, 1.0).toFloat())

                    if (index == 0) {
                        popPath.moveTo(x, y)
                        popAreaPath.moveTo(x, paddingTop + chartHeight)
                        popAreaPath.lineTo(x, y)
                    } else {
                        popPath.lineTo(x, y)
                        popAreaPath.lineTo(x, y)
                    }

                    if (index == popHistory.lastIndex) {
                        popAreaPath.lineTo(x, paddingTop + chartHeight)
                        popAreaPath.close()
                    }
                }

                // Draw translucent filled area under population line
                drawPath(
                    path = popAreaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2563EB).copy(alpha = 0.15f), Color.Transparent),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )

                // Draw population line outline
                drawPath(
                    path = popPath,
                    color = Color(0xFF2563EB),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Draw Fisher Wage Series (Dashed outline)
            if (fisherWageHistory.isNotEmpty()) {
                val fisherPath = Path()
                fisherWageHistory.forEachIndexed { index, value ->
                    val x = paddingLeft + (index.toFloat() / 120f).coerceAtMost(1f) * chartWidth
                    val y = paddingTop + chartHeight * (1f - (value / maxWage).coerceIn(0.0, 1.0).toFloat())
                    if (index == 0) {
                        fisherPath.moveTo(x, y)
                    } else {
                        fisherPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = fisherPath,
                    color = Color(0xFFF97316),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f),
                        cap = StrokeCap.Round
                    )
                )
            }

            // 4. Draw Admin Wage Series (Dashed outline)
            if (adminWageHistory.isNotEmpty()) {
                val adminPath = Path()
                adminWageHistory.forEachIndexed { index, value ->
                    val x = paddingLeft + (index.toFloat() / 120f).coerceAtMost(1f) * chartWidth
                    val y = paddingTop + chartHeight * (1f - (value / maxWage).coerceIn(0.0, 1.0).toFloat())
                    if (index == 0) {
                        adminPath.moveTo(x, y)
                    } else {
                        adminPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = adminPath,
                    color = Color(0xFF8B5CF6),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

@Composable
fun ChartLegendItem(color: Color, label: String, isDashed: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(16.dp, 4.dp)) {
            if (isDashed) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )
            } else {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Text(text = label, fontSize = 10.sp, color = Color(0xFF475569))
    }
}
