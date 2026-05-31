package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun OceanScene(
    modifier: Modifier = Modifier,
    fleetCount: Int,
    populationRatio: Float,
    isProsperous: Boolean
) {
    // Infinite animation to simulate wave bobbing
    val infiniteTransition = rememberInfiniteTransition(label = "Waves")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE0F2FE))
    ) {
        val width = size.width
        val height = size.height

        // 1. Draw Sky (Gradient from 0 to 70dp)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFBAE6FD), Color(0xFF7DD3FC)),
                startY = 0f,
                endY = 70.dp.toPx()
            ),
            size = Size(width, 70.dp.toPx())
        )

        // 2. Draw Sea (y = 70dp to bottom)
        drawRect(
            color = Color(0xFF3B82F6),
            topLeft = Offset(0f, 70.dp.toPx()),
            size = Size(width, height - 70.dp.toPx())
        )

        // 3. Draw Shore Land (Green area on the left)
        val landPath = Path().apply {
            moveTo(0f, 50.dp.toPx())
            quadraticTo(
                120.dp.toPx(), 60.dp.toPx(),
                180.dp.toPx(), 110.dp.toPx()
            )
            lineTo(180.dp.toPx(), height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = landPath,
            color = Color(0xFF10B981)
        )

        // 4. Draw Pier/Dock (Grey rectangle representing harbor)
        drawRect(
            color = Color(0xFF64748B),
            topLeft = Offset(160.dp.toPx(), 105.dp.toPx()),
            size = Size(40.dp.toPx(), 8.dp.toPx())
        )

        // 5. Draw Houses on the Shore Land
        val housePos = listOf(
            Offset(15f, 60f), Offset(40f, 65f), Offset(25f, 90f), Offset(60f, 80f), Offset(80f, 85f),
            Offset(30f, 115f), Offset(65f, 110f), Offset(95f, 105f), Offset(45f, 140f), Offset(80f, 135f),
            Offset(15f, 155f), Offset(110f, 130f), Offset(120f, 100f), Offset(140f, 120f), Offset(115f, 150f)
        )

        val houseColor = if (isProsperous) Color(0xFFFACC15) else Color(0xFFCBD5E1)
        housePos.forEach { pos ->
            val scalePos = Offset(pos.x.dp.toPx(), pos.y.dp.toPx())
            drawHouse(scalePos, houseColor, isProsperous)
        }

        // 6. Draw Wave Accent
        val wavePath = Path().apply {
            moveTo(180.dp.toPx(), 125.dp.toPx())
            quadraticTo(
                310.dp.toPx() + sin(waveOffset) * 10f, 115.dp.toPx() + sin(waveOffset + 1f) * 6f,
                width, 125.dp.toPx()
            )
            lineTo(width, height)
            lineTo(180.dp.toPx(), height)
            close()
        }
        drawPath(
            path = wavePath,
            color = Color(0xFF2563EB).copy(alpha = 0.3f)
        )

        // 7. Draw Fish (Little bubbles swimming in deep water)
        val fishCount = (populationRatio * 15).toInt().coerceIn(0, 30)
        repeat(fishCount) { i ->
            val seedX = (220f + (i * 21) % 180f).dp.toPx()
            val seedY = (115f + (i * 11) % 60f).dp.toPx()
            val fishX = seedX + sin(waveOffset + i) * 4.dp.toPx()
            val fishY = seedY + sin(waveOffset * 1.5f + i) * 2.dp.toPx()
            
            drawOval(
                color = Color(0xFF1E40AF).copy(alpha = 0.5f),
                topLeft = Offset(fishX, fishY),
                size = Size(6.dp.toPx(), 3.dp.toPx())
            )
        }

        // 8. Draw Small Floating Boats
        val COLS = 12
        repeat(fleetCount) { i ->
            val row = i / COLS
            val col = i % COLS
            val baseX = (210f + col * 15f + (row % 2) * 7f).dp.toPx()
            val baseY = (80f + row * 11f).dp.toPx()
            
            // Limit drawing inside coordinates to prevent visual clutter
            if (baseX < width) {
                val animatedY = baseY + sin(waveOffset + i * 0.3f) * 1.5f.dp.toPx()
                drawBoat(Offset(baseX, animatedY))
            }
        }
    }
}

private fun DrawScope.drawHouse(offset: Offset, color: Color, glow: Boolean) {
    val sizePx = 12.dp.toPx()
    // House base path
    val housePath = Path().apply {
        moveTo(offset.x, offset.y + sizePx * 0.4f)
        lineTo(offset.x + sizePx * 0.5f, offset.y)
        lineTo(offset.x + sizePx, offset.y + sizePx * 0.4f)
        lineTo(offset.x + sizePx, offset.y + sizePx)
        lineTo(offset.x, offset.y + sizePx)
        close()
    }
    
    if (glow) {
        drawCircle(
            color = Color(0xFFFACC15).copy(alpha = 0.25f),
            radius = sizePx * 0.9f,
            center = Offset(offset.x + sizePx / 2f, offset.y + sizePx / 2f)
        )
    }
    
    drawPath(path = housePath, color = color)
    drawRect(
        color = Color(0xFF475569),
        topLeft = Offset(offset.x + sizePx * 0.4f, offset.y + sizePx * 0.6f),
        size = Size(sizePx * 0.25f, sizePx * 0.4f)
    )
}

private fun DrawScope.drawBoat(offset: Offset) {
    val width = 9.dp.toPx()
    val height = 6.dp.toPx()
    
    val hullPath = Path().apply {
        moveTo(offset.x, offset.y + height * 0.3f)
        lineTo(offset.x + width, offset.y + height * 0.3f)
        lineTo(offset.x + width * 0.85f, offset.y + height)
        lineTo(offset.x + width * 0.15f, offset.y + height)
        close()
    }
    drawPath(path = hullPath, color = Color(0xFF92400E))
    
    val sailPath = Path().apply {
        moveTo(offset.x + width * 0.5f, offset.y + height * 0.3f)
        lineTo(offset.x + width * 0.5f, offset.y)
        lineTo(offset.x + width * 0.8f, offset.y + height * 0.2f)
        close()
    }
    drawPath(path = sailPath, color = Color(0xFF1E3A8A))
}
