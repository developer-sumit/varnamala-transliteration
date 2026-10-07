package com.sumit.varnamala.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumit.varnamala.R
import com.sumit.varnamala.tts.SpeechPart

/** The recognized text and its converted form, each with a speak button that highlights the spoken word. */
@Composable
fun ScanResultCard(
    original: String,
    converted: String,
    originalLanguage: String,
    convertedLanguage: String,
    speech: SpeechController,
    textScale: Float,
) {
    val originalPart = SpeechPart(original, originalLanguage)
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(12.dp)
            .heightIn(max = 220.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HighlightedText(
                text = original, controller = speech, key = "original", partIndex = 0,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textScale).sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            SpeakButton(speech, "original", stringResource(R.string.tts_speak_original_text),
                onClick = { speech.toggle("original", listOf(originalPart)) })
        }
        if (converted.isNotBlank() && converted != original) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HighlightedText(
                    text = converted, controller = speech, key = "converted",
                    partIndex = if (speech.settings.speakOriginal) 1 else 0,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = (17 * textScale).sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
                SpeakButton(speech, "converted", stringResource(R.string.tts_speak_converted_text),
                    onClick = { speech.toggleConverted("converted", originalPart, SpeechPart(converted, convertedLanguage)) })
            }
        }
    }
}

