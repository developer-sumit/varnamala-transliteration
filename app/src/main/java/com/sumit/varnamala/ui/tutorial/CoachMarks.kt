package com.sumit.varnamala.ui.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class CoachMarkStep(
    val targetKey: String,
    val title: String,
    val message: String,
)

@Composable
fun CoachMarksOverlay(
    steps: List<CoachMarkStep>,
    targetRects: Map<String, Rect>,
    nextLabel: String,
    skipLabel: String,
    doneLabel: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return

    var stepIndex by remember { mutableIntStateOf(0) }
    val step = steps[stepIndex]
    val isLastStep = stepIndex == steps.lastIndex

    val targetRect = targetRects[step.targetKey]

    var tooltipSize by remember { mutableStateOf<IntSize?>(null) }
    val density = LocalDensity.current
    val tooltipMarginPx = with(density) { 16.dp.toPx() }
    val cutoutPaddingPx = with(density) { 10.dp.toPx() }
    val cutoutCornerPx = with(density) { 18.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // Needed so BlendMode.Clear works reliably
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.72f), style = Fill)

            if (targetRect != null && targetRect.width > 0f && targetRect.height > 0f) {
                val padded = Rect(
                    left = (targetRect.left - cutoutPaddingPx).coerceAtLeast(0f),
                    top = (targetRect.top - cutoutPaddingPx).coerceAtLeast(0f),
                    right = (targetRect.right + cutoutPaddingPx).coerceAtMost(size.width),
                    bottom = (targetRect.bottom + cutoutPaddingPx).coerceAtMost(size.height)
                )

                val cutout = RoundRect(padded, CornerRadius(cutoutCornerPx, cutoutCornerPx))
                val path = Path().apply { addRoundRect(cutout) }

                drawPath(
                    path = path,
                    color = Color.Transparent,
                    blendMode = BlendMode.Clear
                )
            }
        }

        val tooltipOffset by remember(targetRect, tooltipSize, maxWidthPx, maxHeightPx) {
            derivedStateOf {
                val size = tooltipSize
                if (size == null) return@derivedStateOf IntOffset(
                    (tooltipMarginPx).roundToInt(),
                    (tooltipMarginPx).roundToInt()
                )

                val tooltipWidth = size.width.toFloat()
                val tooltipHeight = size.height.toFloat()

                val rect = targetRect
                if (rect == null) {
                    val x = ((maxWidthPx - tooltipWidth) / 2f).coerceIn(tooltipMarginPx, (maxWidthPx - tooltipWidth - tooltipMarginPx).coerceAtLeast(tooltipMarginPx))
                    val y = ((maxHeightPx - tooltipHeight) / 2f).coerceIn(tooltipMarginPx, (maxHeightPx - tooltipHeight - tooltipMarginPx).coerceAtLeast(tooltipMarginPx))
                    return@derivedStateOf IntOffset(x.roundToInt(), y.roundToInt())
                }

                val preferBelow = rect.center.y < maxHeightPx / 2f
                val belowY = rect.bottom + tooltipMarginPx
                val aboveY = rect.top - tooltipHeight - tooltipMarginPx

                val y = when {
                    preferBelow && belowY + tooltipHeight <= maxHeightPx - tooltipMarginPx -> belowY
                    !preferBelow && aboveY >= tooltipMarginPx -> aboveY
                    // Fallback: clamp in view
                    else -> (belowY).coerceIn(tooltipMarginPx, (maxHeightPx - tooltipHeight - tooltipMarginPx).coerceAtLeast(tooltipMarginPx))
                }

                val desiredX = rect.center.x - tooltipWidth / 2f
                val x = desiredX.coerceIn(tooltipMarginPx, (maxWidthPx - tooltipWidth - tooltipMarginPx).coerceAtLeast(tooltipMarginPx))

                IntOffset(x.roundToInt(), y.roundToInt())
            }
        }

        Surface(
            modifier = Modifier
                .padding(0.dp)
                .widthIn(max = 360.dp)
                .onGloballyPositioned { tooltipSize = it.size }
                .graphicsLayer {
                    translationX = tooltipOffset.x.toFloat()
                    translationY = tooltipOffset.y.toFloat()
                },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = step.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(modifier = Modifier.padding(top = 12.dp)) {
                    TextButton(onClick = onFinished) {
                        Text(skipLabel)
                    }
                    Button(
                        onClick = {
                            if (isLastStep) {
                                onFinished()
                            } else {
                                stepIndex += 1
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(if (isLastStep) doneLabel else nextLabel)
                    }
                }
            }
        }

        // If steps list changes (hot reload / recomposition), keep index in bounds.
        LaunchedEffect(steps.size) {
            if (stepIndex > steps.lastIndex) stepIndex = steps.lastIndex
        }
    }
}
