package com.sumit.varnamala.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: Painter? = null,
    size: Dp = 50.dp,
    iconSize: Dp = 24.dp,
    textColor: Color? = null,
    background: Color? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background ?: MaterialTheme.colorScheme.surface)
                .clip(CircleShape)
                .blur(radius = 40.dp)
        )
        if (icon !== null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
            )
        }

        if (text !== null && text.isNotBlank()) {
            Text(
                text,
                color = textColor ?: MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight(500)
            )
        }
    }
}
