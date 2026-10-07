package com.sumit.varnamala.ui.components

import com.sumit.varnamala.R
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextElement

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.max
import kotlin.math.min

private data class OverlayTransform(val scale: Float, val offsetX: Float, val offsetY: Float)

private fun overlayTransform(
    result: OCRResult,
    containerSize: IntSize,
    cropToFill: Boolean,
): OverlayTransform {
    val xScale = containerSize.width.toFloat() / result.imageWidth
    val yScale = containerSize.height.toFloat() / result.imageHeight
    val scale = if (cropToFill) max(xScale, yScale) else min(xScale, yScale)
    return OverlayTransform(
        scale = scale,
        offsetX = (containerSize.width - result.imageWidth * scale) / 2f,
        offsetY = (containerSize.height - result.imageHeight * scale) / 2f,
    )
}


@Composable
fun OCROverlayCanvas(
    result: OCRResult?,
    textOverride: Map<android.graphics.Rect, String>? = null,
    containerSize: IntSize,
    cropToFill: Boolean = false,
    sourceBitmap: Bitmap? = null,
    modifier: Modifier = Modifier,
    // Display settings
    showBoundingBoxes: Boolean = true,
    showTextOverlay: Boolean = true,
    showWordBoundingBoxes: Boolean = false,
    showLineBoundingBoxes: Boolean = true,
    showBlockBoundingBoxes: Boolean = true,
    textBackgroundOpacity: Float = 0.8f,
    boundingBoxStrokeWidth: Float = 2f,
    // Colors
    blockBoundingBoxColor: Color = Color.Blue,
    lineBoundingBoxColor: Color = Color.Green,
    wordBoundingBoxColor: Color = Color.Red,
    textColor: Color = Color.White,
    textBackgroundColor: Color = Color.Black.copy(alpha = 0.85f), // More solid background
    // Interaction
    enableTextCopy: Boolean = true,
    /** Called with the tapped word (or whole line when it is a single word). Long-press still copies. */
    onWordTap: ((String) -> Unit)? = null,
    /** Black-and-yellow rendering with solid backgrounds, for low vision and bright sunlight. */
    highContrast: Boolean = false,
) {
    if (result == null || containerSize == IntSize.Zero) return

    Box(modifier = modifier) {
        // Render text first. Its background fills the detected area, so bounding-box
        // strokes must be drawn afterward or they are completely covered.
        if (showTextOverlay) {
            EnhancedOCRTextOverlay(
                result = result,
                textOverride = textOverride,
                containerSize = containerSize,
                cropToFill = cropToFill,
                sourceBitmap = sourceBitmap,
                textColor = if (highContrast) Color.Yellow else textColor,
                backgroundColor = if (highContrast) Color.Black else textBackgroundColor.copy(alpha = textBackgroundOpacity),
                enableTextCopy = enableTextCopy,
                onWordTap = onWordTap,
                highContrast = highContrast,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showBoundingBoxes) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
            ) {
                drawOCRBoundingBoxes(
                    result = result,
                    containerSize = containerSize,
                    cropToFill = cropToFill,
                    drawScope = this,
                    showBlocks = showBlockBoundingBoxes,
                    showLines = showLineBoundingBoxes,
                    showWords = showWordBoundingBoxes,
                    strokeWidth = if (highContrast) boundingBoxStrokeWidth * 2f else boundingBoxStrokeWidth,
                    textBackgroundAlpha = textBackgroundOpacity,
                    blockColor = if (highContrast) Color.Yellow else blockBoundingBoxColor,
                    lineColor = if (highContrast) Color.Yellow else lineBoundingBoxColor,
                    wordColor = wordBoundingBoxColor
                )
            }
        }
    }
}

/**
 * Draw OCR bounding boxes on the canvas
 */
private fun drawOCRBoundingBoxes(
    result: OCRResult,
    containerSize: IntSize,
    cropToFill: Boolean,
    drawScope: DrawScope,
    showBlocks: Boolean,
    showLines: Boolean,
    showWords: Boolean,
    strokeWidth: Float,
    textBackgroundAlpha: Float,
    blockColor: Color,
    lineColor: Color,
    wordColor: Color
) {
    if (result.imageWidth == 0 || result.imageHeight == 0) return

    val transform = overlayTransform(result, containerSize, cropToFill)
    // The public setting is expressed like the rest of the Compose UI (dp-like),
    // while DrawScope expects physical pixels. A raw 1-2 px stroke was effectively
    // invisible on modern high-density screens.
    val blockStrokePx = max(2f, strokeWidth * drawScope.density)

    result.boundingBoxes.forEach { block ->
        // Lines the recognizer is unsure about are drawn grey, not in the normal colour.
        val unsure = block.isLowConfidence
        val blockColor = if (unsure) Color.Gray else blockColor
        val lineColor = if (unsure) Color.Gray else lineColor
        // Draw block bounding boxes
        if (showBlocks) {
            val scaledRect = android.graphics.RectF(
                block.boundingBox.left * transform.scale + transform.offsetX,
                block.boundingBox.top * transform.scale + transform.offsetY,
                block.boundingBox.right * transform.scale + transform.offsetX,
                block.boundingBox.bottom * transform.scale + transform.offsetY
            )

            if (textBackgroundAlpha > 0f) {
                drawScope.drawRect(
                    color = blockColor.copy(alpha = textBackgroundAlpha * 0.3f),
                    topLeft = Offset(scaledRect.left, scaledRect.top),
                    size = Size(scaledRect.width(), scaledRect.height())
                )
            }
            drawScope.drawRect(
                color = blockColor,
                topLeft = Offset(scaledRect.left, scaledRect.top),
                size = Size(scaledRect.width(), scaledRect.height()),
                style = Stroke(width = blockStrokePx)
            )
        }

        // Draw line bounding boxes
        if (showLines) {
            block.lines.forEach { line ->
                val scaledRect = android.graphics.RectF(
                    line.boundingBox.left * transform.scale + transform.offsetX,
                    line.boundingBox.top * transform.scale + transform.offsetY,
                    line.boundingBox.right * transform.scale + transform.offsetX,
                    line.boundingBox.bottom * transform.scale + transform.offsetY
                )

                if (textBackgroundAlpha > 0f) {
                    drawScope.drawRect(
                        color = lineColor.copy(alpha = textBackgroundAlpha * 0.2f),
                        topLeft = Offset(scaledRect.left, scaledRect.top),
                        size = Size(scaledRect.width(), scaledRect.height())
                    )
                }
                drawScope.drawRect(
                    color = lineColor,
                    topLeft = Offset(scaledRect.left, scaledRect.top),
                    size = Size(scaledRect.width(), scaledRect.height()),
                    style = Stroke(width = blockStrokePx)
                )
            }
        }

        // Draw word bounding boxes
        if (showWords) {
            block.lines.forEach { line ->
                line.elements.forEach { element ->
                    val scaledRect = android.graphics.RectF(
                        element.boundingBox.left * transform.scale + transform.offsetX,
                        element.boundingBox.top * transform.scale + transform.offsetY,
                        element.boundingBox.right * transform.scale + transform.offsetX,
                        element.boundingBox.bottom * transform.scale + transform.offsetY
                    )

                if (textBackgroundAlpha > 0f) {
                        drawScope.drawRect(
                            color = wordColor.copy(alpha = textBackgroundAlpha * 0.1f),
                            topLeft = Offset(scaledRect.left, scaledRect.top),
                            size = Size(scaledRect.width(), scaledRect.height())
                    )
                }
                drawScope.drawRect(
                    color = wordColor,
                    topLeft = Offset(scaledRect.left, scaledRect.top),
                    size = Size(scaledRect.width(), scaledRect.height()),
                    style = Stroke(width = blockStrokePx)
                )
                }
            }
        }
    }
}

/**
 * Enhanced text overlay with Google Lens-like copy functionality
 */
@Composable
private fun EnhancedOCRTextOverlay(
    result: OCRResult,
    textOverride: Map<android.graphics.Rect, String>?,
    containerSize: IntSize,
    cropToFill: Boolean,
    sourceBitmap: Bitmap?,
    textColor: Color,
    backgroundColor: Color,
    enableTextCopy: Boolean,
    onWordTap: ((String) -> Unit)?,
    highContrast: Boolean,
    modifier: Modifier = Modifier
) {
    if (result.imageWidth == 0 || result.imageHeight == 0) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val transform = overlayTransform(result, containerSize, cropToFill)

    var selectedElement by remember { mutableStateOf<TextElement?>(null) }

    Box(modifier = modifier) {
        result.boundingBoxes.forEach { block ->
            block.lines.forEach { line ->
                line.elements.forEach { element ->
                    val unsure = element.isLowConfidence
                    // Show the converted text in place of the original when there is one, so the box
                    // reads like the sign itself; the result card below shows both.
                    val converted = if (unsure) null else textOverride?.get(element.boundingBox)
                    val displayText = if (!converted.isNullOrBlank() && converted != element.text) converted else element.text
                    if (displayText.isNotBlank()) {
                        val scaledRect = android.graphics.RectF(
                            element.boundingBox.left * transform.scale + transform.offsetX,
                            element.boundingBox.top * transform.scale + transform.offsetY,
                            element.boundingBox.right * transform.scale + transform.offsetX,
                            element.boundingBox.bottom * transform.scale + transform.offsetY
                        )

                        // Match the text in the picture: the letters are as tall as the detected box
                        // allows, shrunk only as far as needed to stay on one line inside it. Tall,
                        // narrow boxes hold sideways text, which is drawn turned to match.
                        val sideways = scaledRect.height() > scaledRect.width() * 1.5f && displayText.length > 2
                        val boxLength = if (sideways) scaledRect.height() else scaledRect.width()
                        val boxThickness = if (sideways) scaledRect.width() else scaledRect.height()
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        // Ink of a text line is about 0.75 of its font size tall, so scale from the box height.
                        val heightLimitPx = max(6f, boxThickness / 0.95f)
                        paint.textSize = heightLimitPx
                        val widthAtLimit = paint.measureText(displayText.trim())
                        val optimalFontSizePx = if (widthAtLimit <= boxLength) heightLimitPx
                        else max(6f, heightLimitPx * boxLength / widthAtLimit)

                        // Convert pixels to dp for proper positioning
                        val leftDp = with(density) { scaledRect.left.toDp() }
                        val topDp = with(density) { scaledRect.top.toDp() }
                        val widthDp = with(density) { scaledRect.width().toDp() }
                        val heightDp = with(density) { scaledRect.height().toDp() }

                        val replacementColor = when {
                            highContrast -> Color.Black
                            unsure -> Color.DarkGray.copy(alpha = 0.7f)
                            else -> sourceBitmap?.sampleBackgroundColor(element.boundingBox) ?: backgroundColor
                        }
                        val lowConfidenceLabel = stringResource(R.string.low_confidence)
                        val spokenLabel = if (unsure) "$displayText, $lowConfidenceLabel" else displayText

                        // Replacement patch that stays within the detected text box.
                        // OpenCV inpainting is reserved for still-image/high-quality mode; when
                        // OpenCV is unavailable this falls back to the same sampled patch.
                        Box(
                            modifier = Modifier
                                .offset(x = leftDp, y = topDp)
                                .size(width = widthDp, height = heightDp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(replacementColor)
                                .zIndex(1f)
                        )

                        // Text that can extend beyond the background
                        Box(
                            modifier = Modifier
                                .offset(x = leftDp, y = topDp)
                                .size(width = widthDp, height = heightDp)
                                .semantics(mergeDescendants = true) { contentDescription = spokenLabel }
                                .then(
                                    if (enableTextCopy || onWordTap != null) {
                                        Modifier.pointerInput(element, onWordTap) {
                                            detectTapGestures(
                                                onTap = { tap ->
                                                    if (onWordTap != null) {
                                                        onWordTap(wordAt(element.text, tap.x / size.width.toFloat()))
                                                    } else {
                                                        selectedElement = element
                                                        copyTextToClipboard(context, displayText)
                                                    }
                                                },
                                                onLongPress = {
                                                    selectedElement = element
                                                    copyTextToClipboard(context, displayText)
                                                },
                                            )
                                        }
                                    } else Modifier
                                )
                                .zIndex(2f), // Higher z-index to appear above background
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayText,
                                color = if (unsure) Color.LightGray.copy(alpha = 0.8f) else textColor,
                                fontSize = with(density) { optimalFontSizePx.toSp() },
                                fontWeight = if (highContrast) FontWeight.Bold else FontWeight.SemiBold,
                                fontStyle = if (unsure) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.8f),
                                        offset = Offset(1f, 1f),
                                        blurRadius = 2f
                                    )
                                ),
                                modifier = if (sideways) {
                                    Modifier
                                        .requiredSize(width = heightDp, height = widthDp)
                                        .rotate(90f)
                                } else {
                                    Modifier
                                        .fillMaxSize()
                                        .padding(1.dp)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Show copy confirmation when text is selected
        selectedElement?.let { element ->
            LaunchedEffect(element) {
                kotlinx.coroutines.delay(1000) // Show for 1 second
                selectedElement = null
            }

            // Copy confirmation overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = stringResource(R.string.copied),
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "\"${element.text}\" copied!",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/** The word under a tap at [fraction] (0..1) across a line, estimating word widths by letter count. */
internal fun wordAt(line: String, fraction: Float): String {
    val words = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.size <= 1) return line.trim()
    val total = words.sumOf { it.length + 1 }.toFloat()
    var edge = 0f
    for (word in words) {
        edge += (word.length + 1) / total
        if (fraction <= edge) return word
    }
    return words.last()
}

/**
 * Copy text to clipboard with feedback
 */
private fun copyTextToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clipData = ClipData.newPlainText(context.getString(R.string.ocr_text), text)
    clipboardManager?.setPrimaryClip(clipData)
}

private fun Bitmap.sampleBackgroundColor(rect: android.graphics.Rect): Color {
    val left = rect.left.coerceIn(0, width - 1)
    val top = rect.top.coerceIn(0, height - 1)
    val right = rect.right.coerceIn(left + 1, width)
    val bottom = rect.bottom.coerceIn(top + 1, height)
    val insetX = max(1, (right - left) / 8)
    val insetY = max(1, (bottom - top) / 8)
    val samples = mutableListOf<Int>()

    fun addSample(x: Int, y: Int) {
        samples.add(getPixel(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1)))
    }

    val sampleXs = listOf(left, left + insetX, right - insetX, right - 1)
    val sampleYs = listOf(top, top + insetY, bottom - insetY, bottom - 1)
    sampleXs.forEach { x ->
        addSample(x, top)
        addSample(x, bottom - 1)
    }
    sampleYs.forEach { y ->
        addSample(left, y)
        addSample(right - 1, y)
    }

    if (samples.isEmpty()) return Color.Black.copy(alpha = 0.72f)

    var red = 0
    var green = 0
    var blue = 0
    samples.forEach { color ->
        red += android.graphics.Color.red(color)
        green += android.graphics.Color.green(color)
        blue += android.graphics.Color.blue(color)
    }
    val count = samples.size
    return Color(red / count, green / count, blue / count).copy(alpha = 0.94f)
}
