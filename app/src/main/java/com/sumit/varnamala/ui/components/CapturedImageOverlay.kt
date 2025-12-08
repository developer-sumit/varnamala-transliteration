package com.sumit.varnamala.ui.components

import com.sumit.varnamala.R
import com.sumit.varnamala.data.ocr.OCRResult

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CapturedImageOverlay(
    capturedBitmap: Bitmap?,
    ocrResult: OCRResult?,
    isProcessing: Boolean = false,
    errorMessage: String? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (capturedBitmap == null) return
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Header with close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.captured_image),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            // Close button
            GlassButton(
                icon = painterResource(R.drawable.ic_delete_outline),
                size = 40.dp,
                onClick = onClose
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Captured image
        Image(
            bitmap = capturedBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.captured_image),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(capturedBitmap.width.toFloat() / capturedBitmap.height.toFloat())
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // OCR Results section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = stringResource(R.string.ocr_results),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                when {
                    isProcessing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Text(
                                text = "Processing image...",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    errorMessage != null -> {
                        Text(
                            text = "Error: $errorMessage",
                            color = Color.Red.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                    
                    ocrResult != null && ocrResult.text.isNotBlank() -> {
                        // OCR text
                        Text(
                            text = ocrResult.text,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // OCR metadata
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (ocrResult.language != null) {
                                Text(
                                    text = "Language: ${ocrResult.language}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                            
                            Text(
                                text = "Confidence: ${(ocrResult.confidence * 100).toInt()}%",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            
                            if (ocrResult.processingTimeMs > 0) {
                                Text(
                                    text = "Processing time: ${ocrResult.processingTimeMs}ms",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    
                    else -> {
                        Text(
                            text = stringResource(R.string.no_text_detected),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons at bottom
        if (ocrResult != null && ocrResult.text.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Copy text to clipboard
                        // This will be handled by the parent component
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.copy))
                }
                
                OutlinedButton(
                    onClick = {
                        // Share text
                        // This will be handled by the parent component
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.share))
                }
            }
        }
    }
}