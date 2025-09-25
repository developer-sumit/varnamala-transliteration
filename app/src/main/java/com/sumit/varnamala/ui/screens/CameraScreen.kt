package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.ui.components.ResultCard
import com.sumit.varnamala.ui.components.CameraPermissionHandler
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.OCRCameraPreview
import com.sumit.varnamala.data.ocr.OCRResult

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

@Composable
fun CameraScreen(navController: NavController) {
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    var flashEnabled by remember { mutableStateOf(false) }
    var detectionRunning by remember { mutableStateOf(false) }
    var lastOCRResult by remember { mutableStateOf<OCRResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detectionCount by remember { mutableIntStateOf(0) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }
    var preferredLanguage by remember { mutableStateOf<String?>(null) }
    var manualLanguageRequired by remember { mutableStateOf(false) }
    var detectionIntervalMs by remember { mutableLongStateOf(1000L) }
    var readingMsPerWord by remember { mutableIntStateOf(300) }
    var maxExtraDelayMs by remember { mutableIntStateOf(5000) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Scanner Settings") },
            text = {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Detection interval controls
                    Text("Detection Interval", fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${detectionIntervalMs}ms", fontSize = 14.sp)
                        Row {
                            OutlinedIconButton(
                                onClick = {
                                    detectionIntervalMs = max(250L, detectionIntervalMs - 250L)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-")
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedIconButton(
                                onClick = {
                                    detectionIntervalMs = min(5000L, detectionIntervalMs + 250L)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+")
                            }
                        }
                    }
                    Slider(
                        value = detectionIntervalMs.toFloat(),
                        onValueChange = { detectionIntervalMs = it.toLong() },
                        valueRange = 250f..5000f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // Reading speed controls
                    Text("Reading Time per Word", fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${readingMsPerWord}ms", fontSize = 14.sp)
                        Row {
                            OutlinedIconButton(
                                onClick = { readingMsPerWord = max(100, readingMsPerWord - 50) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-")
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedIconButton(
                                onClick = { readingMsPerWord = min(1000, readingMsPerWord + 50) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+")
                            }
                        }
                    }
                    Slider(
                        value = readingMsPerWord.toFloat(),
                        onValueChange = { readingMsPerWord = it.toInt() },
                        valueRange = 100f..1000f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, topInset + 10.dp, 16.dp, 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings button
            GlassButton(
                icon = painterResource(R.drawable.ic_settings_outline),
                size = 44.dp,
                onClick = { showSettingsDialog = true }
            )
        }

        CameraPermissionHandler(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface),
            onGranted = {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    OCRCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        isDetectionRunning = detectionRunning,
                        flashEnabled = flashEnabled,
                        preferredLanguage = preferredLanguage,
                        detectionIntervalMs = detectionIntervalMs,
                        readingMsPerWord = readingMsPerWord,
                        maxExtraDelayMs = maxExtraDelayMs,
                        skipFrames = 5,
                        onOCRResult = { result ->
                            lastOCRResult = result
                            errorMessage = null
                            detectionCount += 1
                            lastUpdateTime = System.currentTimeMillis()
                        },
                        onManualLanguageRequired = { manualLanguageRequired = true },
                        onError = { error ->
                            errorMessage = error
                        }
                    )

                    // Status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (detectionRunning) Color.Green.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(
                                    alpha = 0.2f
                                )
                            )
                            .padding(14.dp, 10.dp)
                    ) {
                        if (detectionRunning) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Green
                            ) {}
                        }
                        Text(
                            text = if (detectionRunning) "Scanning" else "Ready",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (detectionRunning) Color.Green else MaterialTheme.colorScheme.outline
                        )
                        if (detectionRunning && detectionCount > 0) {
                            Text(
                                text = "($detectionCount)",
                                fontSize = 10.sp,
                                color = Color.Green.copy(alpha = 0.8f)
                            )
                        }
                    }

                    ResultCard(
                        lastOCRResult,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    )
                }
            })

        // Bottom bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Error message display
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 8.dp),
                    color = Color(0xFFf44336).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFf44336),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 16.dp, 20.dp, topInset),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash toggle
                GlassButton(
                    icon = if (flashEnabled) painterResource(R.drawable.ic_flash_on_outline) else painterResource(
                        R.drawable.ic_flash_off_outline
                    ), onClick = { flashEnabled = !flashEnabled }
                )

                // Start/Stop button
                GlassButton(
                    text = if (detectionRunning) stringResource(R.string.stop) else stringResource(
                        R.string.start
                    ),
                    size = 80.dp,
                    textColor = Color.White,
                    background = if (detectionRunning) Color(0xFFf44336) else Color(0xFF2196F3),
                    onClick = {
                        detectionRunning = !detectionRunning
                        if (!detectionRunning) {
                            errorMessage = null
                            detectionCount = 0
                        } else {
                            detectionCount = 0
                            lastUpdateTime = System.currentTimeMillis()
                        }
                    }
                )

                // Gallery button
                GlassButton(
                    icon = painterResource(R.drawable.ic_image_outline),
                    onClick = { navController.navigate(Screen.Gallery.route) }
                )
            }
        }
    }
}
