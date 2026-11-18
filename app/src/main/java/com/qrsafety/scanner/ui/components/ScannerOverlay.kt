package com.qrsafety.scanner.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qrsafety.scanner.ui.theme.PrimaryBlue
import com.qrsafety.scanner.ui.theme.TextPrimary

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val boxSize = canvasWidth * 0.72f
            val left = (canvasWidth - boxSize) / 2f
            val top = (canvasHeight - boxSize) / 2.3f
            val right = left + boxSize
            val bottom = top + boxSize

            // Dark semi-transparent cutout overlay
            val overlayPath = Path().apply {
                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
            }
            val cutoutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                )
            }

            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(color = Color(0x99000000))
            }

            // Draw modern corner brackets
            val cornerLength = 48f
            val strokeWidth = 8f
            val cornerRadius = 24f

            // Top-Left
            drawLine(
                color = PrimaryBlue,
                start = Offset(left + cornerRadius, top),
                end = Offset(left + cornerLength, top),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = PrimaryBlue,
                start = Offset(left, top + cornerRadius),
                end = Offset(left, top + cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Top-Right
            drawLine(
                color = PrimaryBlue,
                start = Offset(right - cornerLength, top),
                end = Offset(right - cornerRadius, top),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = PrimaryBlue,
                start = Offset(right, top + cornerRadius),
                end = Offset(right, top + cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-Left
            drawLine(
                color = PrimaryBlue,
                start = Offset(left + cornerRadius, bottom),
                end = Offset(left + cornerLength, bottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = PrimaryBlue,
                start = Offset(left, bottom - cornerLength),
                end = Offset(left, bottom - cornerRadius),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-Right
            drawLine(
                color = PrimaryBlue,
                start = Offset(right - cornerLength, bottom),
                end = Offset(right - cornerRadius, bottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = PrimaryBlue,
                start = Offset(right, bottom - cornerLength),
                end = Offset(right, bottom - cornerRadius),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Animated scanning laser line
            val currentLaserY = top + (bottom - top) * laserPosition
            drawLine(
                color = PrimaryBlue.copy(alpha = 0.85f),
                start = Offset(left + 16f, currentLaserY),
                end = Offset(right - 16f, currentLaserY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }

        // Instructional Text Box
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Align QR code within the frame",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
