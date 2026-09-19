package com.sumit.varnamala.ui.components

import com.sumit.varnamala.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun DisplayOptionsDialog(
    onDismiss: () -> Unit,
    showBoundingBoxes: Boolean = true,
    onShowBoundingBoxesChange: (Boolean) -> Unit = {},
    showTextOverlay: Boolean = true,
    onShowTextOverlayChange: (Boolean) -> Unit = {},
    showWordBoundingBoxes: Boolean = false,
    onShowWordBoundingBoxesChange: (Boolean) -> Unit = {},
    showLineBoundingBoxes: Boolean = true,
    onShowLineBoundingBoxesChange: (Boolean) -> Unit = {},
    showBlockBoundingBoxes: Boolean = true,
    onShowBlockBoundingBoxesChange: (Boolean) -> Unit = {},
    textBackgroundOpacity: Float = 0.1f,
    onTextBackgroundOpacityChange: (Float) -> Unit = {},
    boundingBoxStrokeWidth: Float = 2f,
    onBoundingBoxStrokeWidthChange: (Float) -> Unit = {}
) {
    AppModalBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Text(
                    text = stringResource(R.string.display_options),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Bounding Boxes Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = showBoundingBoxes,
                            onCheckedChange = onShowBoundingBoxesChange
                        )
                        Text(
                            stringResource(R.string.show_bounding_boxes),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (showBoundingBoxes) {
                        Column(
                            modifier = Modifier.padding(start = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = showBlockBoundingBoxes,
                                    onCheckedChange = onShowBlockBoundingBoxesChange
                                )
                                Text(
                                    stringResource(R.string.text_blocks),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = showLineBoundingBoxes,
                                    onCheckedChange = onShowLineBoundingBoxesChange
                                )
                                Text(
                                    stringResource(R.string.text_lines),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = showWordBoundingBoxes,
                                    onCheckedChange = onShowWordBoundingBoxesChange
                                )
                                Text(
                                    stringResource(R.string.words),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Stroke width slider
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(
                                R.string.bounding_box_width,
                                boundingBoxStrokeWidth.toInt()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = boundingBoxStrokeWidth,
                            onValueChange = onBoundingBoxStrokeWidthChange,
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    HorizontalDivider()

                    // Text Overlay Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = showTextOverlay,
                            onCheckedChange = onShowTextOverlayChange
                        )
                        Text(
                            stringResource(R.string.show_text_overlay),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (showTextOverlay) {
                        // Text background opacity slider
                        Text(
                            stringResource(
                                R.string.text_background_opacity,
                                (textBackgroundOpacity * 100).toInt()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = textBackgroundOpacity,
                            onValueChange = onTextBackgroundOpacityChange,
                            valueRange = 0f..1f,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
        }
    }
}
