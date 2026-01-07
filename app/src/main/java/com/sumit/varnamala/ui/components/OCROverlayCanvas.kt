package com.sumit.varnamala.ui.components

import com.sumit.varnamala.R
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.data.ocr.TextElement

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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


@Composable
fun OCROverlayCanvas(
    result: OCRResult?,
    textOverride: Map<android.graphics.Rect, String>? = null,
    containerSize: IntSize,
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
    enableTextCopy: Boolean = true
) {
    if (result == null || containerSize == IntSize.Zero) return
    
    val context = LocalContext.current
    val density = LocalDensity.current
    
    Box(modifier = modifier) {
        // Bounding boxes overlay
        if (showBoundingBoxes) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawOCRBoundingBoxes(
                    result = result,
                    containerSize = containerSize,
                    drawScope = this,
                    showBlocks = showBlockBoundingBoxes,
                    showLines = showLineBoundingBoxes,
                    showWords = showWordBoundingBoxes,
                    strokeWidth = boundingBoxStrokeWidth,
                    textBackgroundAlpha = textBackgroundOpacity,
                    blockColor = blockBoundingBoxColor,
                    lineColor = lineBoundingBoxColor,
                    wordColor = wordBoundingBoxColor
                )
            }
        }
        
        // Enhanced Text overlay with copy functionality
        if (showTextOverlay) {
            EnhancedOCRTextOverlay(
                result = result,
                textOverride = textOverride,
                containerSize = containerSize,
                textColor = textColor,
                backgroundColor = textBackgroundColor.copy(alpha = textBackgroundOpacity),
                enableTextCopy = enableTextCopy,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Draw OCR bounding boxes on the canvas
 */
private fun drawOCRBoundingBoxes(
    result: OCRResult,
    containerSize: IntSize,
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
    
    val scaleX = containerSize.width.toFloat() / result.imageWidth.toFloat()
    val scaleY = containerSize.height.toFloat() / result.imageHeight.toFloat()

    result.boundingBoxes.forEach { block ->
        // Draw block bounding boxes
        if (showBlocks) {
            val scaledRect = android.graphics.RectF(
                block.boundingBox.left * scaleX,
                block.boundingBox.top * scaleY,
                block.boundingBox.right * scaleX,
                block.boundingBox.bottom * scaleY
            )

            drawScope.drawRect(
                color = blockColor,
                topLeft = Offset(scaledRect.left, scaledRect.top),
                size = Size(scaledRect.width(), scaledRect.height()),
                style = Stroke(width = strokeWidth)
            )

            // Draw text background if enabled
            if (textBackgroundAlpha > 0f) {
                drawScope.drawRect(
                    color = blockColor.copy(alpha = textBackgroundAlpha * 0.3f),
                    topLeft = Offset(scaledRect.left, scaledRect.top),
                    size = Size(scaledRect.width(), scaledRect.height())
                )
            }
        }

        // Draw line bounding boxes
        if (showLines) {
            block.lines.forEach { line ->
                val scaledRect = android.graphics.RectF(
                    line.boundingBox.left * scaleX,
                    line.boundingBox.top * scaleY,
                    line.boundingBox.right * scaleX,
                    line.boundingBox.bottom * scaleY
                )

                drawScope.drawRect(
                    color = lineColor,
                    topLeft = Offset(scaledRect.left, scaledRect.top),
                    size = Size(scaledRect.width(), scaledRect.height()),
                    style = Stroke(width = strokeWidth * 0.8f)
                )

                if (textBackgroundAlpha > 0f) {
                    drawScope.drawRect(
                        color = lineColor.copy(alpha = textBackgroundAlpha * 0.2f),
                        topLeft = Offset(scaledRect.left, scaledRect.top),
                        size = Size(scaledRect.width(), scaledRect.height())
                    )
                }
            }
        }

        // Draw word bounding boxes
        if (showWords) {
            block.lines.forEach { line ->
                line.elements.forEach { element ->
                    val scaledRect = android.graphics.RectF(
                        element.boundingBox.left * scaleX,
                        element.boundingBox.top * scaleY,
                        element.boundingBox.right * scaleX,
                        element.boundingBox.bottom * scaleY
                    )

                    drawScope.drawRect(
                        color = wordColor,
                        topLeft = Offset(scaledRect.left, scaledRect.top),
                        size = Size(scaledRect.width(), scaledRect.height()),
                        style = Stroke(width = strokeWidth * 0.6f)
                    )

                    if (textBackgroundAlpha > 0f) {
                        drawScope.drawRect(
                            color = wordColor.copy(alpha = textBackgroundAlpha * 0.1f),
                            topLeft = Offset(scaledRect.left, scaledRect.top),
                            size = Size(scaledRect.width(), scaledRect.height())
                        )
                    }
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
    textColor: Color,
    backgroundColor: Color,
    enableTextCopy: Boolean,
    modifier: Modifier = Modifier
) {
    if (result.imageWidth == 0 || result.imageHeight == 0) return
    
    val context = LocalContext.current
    val density = LocalDensity.current
    val scaleX = containerSize.width.toFloat() / result.imageWidth.toFloat()
    val scaleY = containerSize.height.toFloat() / result.imageHeight.toFloat()
    
    var selectedElement by remember { mutableStateOf<TextElement?>(null) }
    
    Box(modifier = modifier) {
        result.boundingBoxes.forEach { block ->
            block.lines.forEach { line ->
                line.elements.forEach { element ->
                    val displayText = textOverride?.get(element.boundingBox) ?: element.text
                    if (displayText.isNotBlank()) {
                        val scaledRect = android.graphics.RectF(
                            element.boundingBox.left * scaleX,
                            element.boundingBox.top * scaleY,
                            element.boundingBox.right * scaleX,
                            element.boundingBox.bottom * scaleY
                        )
                        
                        val boundingHeightPx = max(1f, scaledRect.height())
                        val boundingWidthPx = max(1f, scaledRect.width())

                        // Fit text to both width and height. Allow up to 2 lines.
                        val paddingPx = 2f
                        val targetWidthPx = max(1f, boundingWidthPx - paddingPx * 2)
                        val targetHeightPx = max(1f, boundingHeightPx - paddingPx * 2)

                        val minTextPx = 8f
                        val maxTextPx = max(12f, targetHeightPx * 0.9f)
                        val maxLines = 2

                        fun fits(textSizePx: Float): Pair<Boolean, Int> {
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                textSize = textSizePx
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }

                            // Approx line height. Paint.FontMetrics gives better but this is stable.
                            val fm = paint.fontMetrics
                            val lineHeight = (fm.descent - fm.ascent) * 1.05f

                            val words = displayText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                            if (words.isEmpty()) return false to 1

                            var lines = 1
                            var current = words.first()
                            for (w in words.drop(1)) {
                                val candidate = "$current $w"
                                if (paint.measureText(candidate) <= targetWidthPx) {
                                    current = candidate
                                } else {
                                    lines++
                                    current = w
                                    if (lines > maxLines) break
                                }
                            }

                            val totalHeight = lineHeight * lines
                            val ok = lines <= maxLines && totalHeight <= targetHeightPx

                            // Also ensure single long word doesn't exceed width massively
                            val longestWordWidth = words.maxOf { paint.measureText(it) }
                            return (ok && longestWordWidth <= targetWidthPx) to lines
                        }

                        // Binary search largest text size that fits.
                        var lo = minTextPx
                        var hi = maxTextPx
                        var best = minTextPx
                        var bestLines = 1
                        repeat(16) {
                            val mid = (lo + hi) / 2f
                            val (ok, usedLines) = fits(mid)
                            if (ok) {
                                best = mid
                                bestLines = usedLines
                                lo = mid
                            } else {
                                hi = mid
                            }
                        }
                        val optimalFontSizePx = best
                        val optimalMaxLines = bestLines
                        
                        // Convert pixels to dp for proper positioning
                        val leftDp = with(density) { scaledRect.left.toDp() }
                        val topDp = with(density) { scaledRect.top.toDp() }
                        val widthDp = with(density) { scaledRect.width().toDp() }
                        val heightDp = with(density) { scaledRect.height().toDp() }
                        
                        // Background box that stays within bounding box
                        Box(
                            modifier = Modifier
                                .offset(x = leftDp, y = topDp)
                                .size(width = widthDp, height = heightDp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(backgroundColor)
                                .zIndex(1f)
                        )
                        
                        // Text that can extend beyond the background
                        Box(
                            modifier = Modifier
                                .offset(x = leftDp, y = topDp)
                                .size(width = widthDp, height = heightDp)
                                .then(
                                    if (enableTextCopy) {
                                        Modifier.clickable {
                                            selectedElement = element
                                            copyTextToClipboard(context, displayText)
                                        }
                                    } else Modifier
                                )
                                .zIndex(2f), // Higher z-index to appear above background
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayText,
                                color = textColor,
                                fontSize = with(density) { optimalFontSizePx.toSp() },
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = optimalMaxLines,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.8f),
                                        offset = Offset(1f, 1f),
                                        blurRadius = 2f
                                    )
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(1.dp)
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

/**
 * Copy text to clipboard with feedback
 */
private fun copyTextToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clipData = ClipData.newPlainText(context.getString(R.string.ocr_text), text)
    clipboardManager?.setPrimaryClip(clipData)
}