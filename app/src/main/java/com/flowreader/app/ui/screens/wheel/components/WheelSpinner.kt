package com.flowreader.app.ui.screens.wheel.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flowreader.app.domain.model.WheelItem
import kotlin.math.cos
import kotlin.math.sin

/**
 * 转盘绘制组件
 */
@Composable
fun WheelSpinner(
    items: List<WheelItem>,
    rotationAngle: Float,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp
) {
    if (items.isEmpty()) return

    val sliceAngle = 360f / items.size

    Canvas(
        modifier = modifier.size(size)
    ) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height
        val center = Offset(canvasWidth / 2, canvasHeight / 2)
        val radius = kotlin.math.min(canvasWidth, canvasHeight) / 2

        items.forEachIndexed { index, item ->
            val startAngle = index * sliceAngle - 90f + rotationAngle

            // 绘制扇形
            val path = Path().apply {
                moveTo(center.x, center.y)
                val startRad = Math.toRadians(startAngle.toDouble())
                
                lineTo(
                    (center.x + radius * cos(startRad)).toFloat(),
                    (center.y + radius * sin(startRad)).toFloat()
                )

                val sweepAngle = sliceAngle
                val steps = 20
                for (i in 1..steps) {
                    val angle = startAngle + (sweepAngle * i / steps)
                    val rad = Math.toRadians(angle.toDouble())
                    lineTo(
                        (center.x + radius * cos(rad)).toFloat(),
                        (center.y + radius * sin(rad)).toFloat()
                    )
                }

                close()
            }

            drawPath(
                path = path,
                color = item.color.copy(alpha = 0.8f)
            )
            
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.3f),
                style = Stroke(width = 2f)
            )

            // 绘制文字
            val textAngle = startAngle + sliceAngle / 2
            val textRad = Math.toRadians(textAngle.toDouble())
            val textRadius = radius * 0.65f
            val textX = center.x + textRadius * cos(textRad).toFloat()
            val textY = center.y + textRadius * sin(textRad).toFloat()

            drawContext.canvas.nativeCanvas.apply {
                save()
                translate(textX, textY)
                rotate(textAngle + 90f)

                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 28f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val displayText = if (item.label.length > 8) {
                    item.label.take(7) + "…"
                } else {
                    item.label
                }
                drawText(displayText, 0f, 0f, textPaint)
                restore()
            }
        }

        // 绘制中心圆
        drawCircle(
            color = Color.White,
            radius = 20f
        )
        drawCircle(
            color = Color(0xFF666666),
            radius = 15f
        )

        // 绘制边框
        drawCircle(
            color = Color(0xFF333333),
            radius = radius - 2f,
            style = Stroke(width = 4f)
        )
    }
}

/**
 * 转盘指针
 */
@Composable
fun WheelPointer(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(40.dp)
    ) {
        val path = Path().apply {
            moveTo(size.width / 2, size.height)
            lineTo(0f, 0f)
            lineTo(size.width, 0f)
            close()
        }

        drawPath(
            path = path,
            color = Color(0xFFFF0000)
        )
        drawPath(
            path = path,
            color = Color(0xFFCC0000),
            style = Stroke(width = 2f)
        )
    }
}
