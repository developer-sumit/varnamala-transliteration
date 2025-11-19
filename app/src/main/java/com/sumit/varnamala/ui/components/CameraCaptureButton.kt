package com.sumit.varnamala.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt


@Composable
fun CameraCaptureButton(
    text: String,
    size: Dp = 80.dp,
    textColor: Color = Color.White,
    backgroundColor: Color = Color(0xFF2196F3),
    longPressBackgroundColor: Color = Color(0xFFf44336),
    progressColor: Color = Color.White,
    isRealTimeMode: Boolean = false,
    onSingleCapture: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var hasStartedRealTime by remember { mutableStateOf(false) }
    var pressStartTime by remember { mutableLongStateOf(0L) }
    val longPressThreshold = 300L // ms before we treat as long press

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            pressStartTime = System.currentTimeMillis()
            // Wait to confirm long press
            while (isPressed && System.currentTimeMillis() - pressStartTime < longPressThreshold) {
                kotlinx.coroutines.delay(16)
            }
            if (isPressed && !hasStartedRealTime) {
                // Now considered long press
                hasStartedRealTime = true
                onLongPressStart()
                // Start progress animation (full circle to indicate active streaming)
                progress = 1f
            }
        } else {
            // Released
            if (hasStartedRealTime) {
                hasStartedRealTime = false
                onLongPressEnd()
            }
            progress = 0f
        }
    }

    val currentBackgroundColor = if (isRealTimeMode) longPressBackgroundColor else backgroundColor
    val strokeWidth = 4.dp
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .size(size)
            .scale(animatedScale)
            .pointerInput(Unit) {
                val buttonRadius = with(density) { size.toPx() / 2f }
                val buttonCenter = Offset(buttonRadius, buttonRadius)

                detectTapGestures(
                    onPress = { offset ->
                        // Check if the initial touch is within the button
                        val distanceFromCenter = sqrt(
                            (offset.x - buttonCenter.x) * (offset.x - buttonCenter.x) +
                                    (offset.y - buttonCenter.y) * (offset.y - buttonCenter.y)
                        )

                        if (distanceFromCenter <= buttonRadius) {
                            isPressed = true
                            val downTime = System.currentTimeMillis()
                            val wasTap: Boolean = awaitPointerEventScope {
                                var currentPosition = offset
                                var finished = false
                                var result = false
                                while (!finished) {
                                    val event = awaitPointerEvent()
                                    var stillPressed = false
                                    for (change in event.changes) {
                                        if (change.pressed) {
                                            stillPressed = true
                                            currentPosition = change.position
                                            val currentDistance = sqrt(
                                                (currentPosition.x - buttonCenter.x) * (currentPosition.x - buttonCenter.x) +
                                                        (currentPosition.y - buttonCenter.y) * (currentPosition.y - buttonCenter.y)
                                            )
                                            if (currentDistance > buttonRadius * 1.8f) {
                                                result = false
                                                finished = true
                                                break
                                            }
                                        }
                                    }
                                    if (!stillPressed) {
                                        val pressTime = System.currentTimeMillis() - downTime
                                        result = pressTime < longPressThreshold
                                        finished = true
                                    }
                                }
                                result
                            }
                            isPressed = false
                            if (wasTap && !isRealTimeMode && !hasStartedRealTime) {
                                onSingleCapture()
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Glass effect background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(currentBackgroundColor.copy(alpha = 0.3f))
                .blur(radius = 40.dp)
        )

        // Main background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(currentBackgroundColor)
        )

        // Progress ring when pressing
        if (hasStartedRealTime) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawProgressRing(
                    progress = 1f,
                    color = progressColor,
                    strokeWidth = strokeWidth.toPx(),
                    size = size.toPx()
                )
            }
        }

        // Text content
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun DrawScope.drawProgressRing(
    progress: Float,
    color: Color,
    strokeWidth: Float,
    size: Float
) {
    val radius = (size - strokeWidth) / 2f
    val center = Offset(size / 2f, size / 2f)

    // Draw progress arc
    drawArc(
        color = color,
        startAngle = -90f, // Start from top
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(
            center.x - radius,
            center.y - radius
        ),
        size = Size(radius * 2, radius * 2),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}