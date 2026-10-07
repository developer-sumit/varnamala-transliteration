package com.sumit.varnamala.ui.components

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Language
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LanguageSelectionRow(
    sourceLanguage: Language?,
    targetLanguage: Language?,
    onSourceLanguageClick: () -> Unit,
    onTargetLanguageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LanguageButton(
            language = sourceLanguage,
            label = stringResource(R.string.detect),
            onClick = onSourceLanguageClick,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(100.dp, 0.dp, 0.dp, 100.dp)),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = stringResource(R.string.to),
            modifier = Modifier.padding(horizontal = 8.dp).size(30.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        LanguageButton(
            language = targetLanguage,
            label = stringResource(R.string.transliterate),
            onClick = onTargetLanguageClick,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(0.dp, 100.dp, 100.dp, 0.dp)),
        )
    }
}

@Composable
private fun LanguageButton(
    language: Language?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "languageButtonScale",
    )
    Row(
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (language?.flagEmoji?.isNotBlank() == true) {
            Text(language.flagEmoji, fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
        }
        Text(
            text = language?.name ?: label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (language == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
