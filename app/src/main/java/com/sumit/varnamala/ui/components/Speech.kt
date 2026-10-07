package com.sumit.varnamala.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sumit.varnamala.R
import com.sumit.varnamala.data.preferences.UserPreferencesRepository
import com.sumit.varnamala.tts.OnDeviceSpeaker
import com.sumit.varnamala.tts.SpeechListener
import com.sumit.varnamala.tts.SpeechPart
import com.sumit.varnamala.tts.SpeechSettings

/**
 * Shared by every screen that reads text aloud. Tracks which button is playing and which word is
 * being spoken so [HighlightedText] can follow along.
 */
@Stable
class SpeechController internal constructor(private val speaker: OnDeviceSpeaker, private val context: android.content.Context) {
    var settings: SpeechSettings = SpeechSettings()
    var enabled: Boolean = true

    var activeKey by mutableStateOf<String?>(null)
        private set
    var activePart by mutableIntStateOf(-1)
        private set
    var activeWord by mutableStateOf<IntRange?>(null)
        private set

    private val listener = object : SpeechListener {
        override fun onWord(partIndex: Int, range: IntRange) {
            activePart = partIndex
            activeWord = range
        }

        override fun onDone() = clear()
    }

    /** Speaks [parts]; pressing the same [key] again stops. */
    fun toggle(key: String, parts: List<SpeechPart>) {
        if (activeKey == key) return stop()
        start(key, parts)
    }

    /**
     * Speaks [converted]; when the "speak original first" setting is on, [original] is read
     * before it in its own language.
     */
    fun toggleConverted(key: String, original: SpeechPart, converted: SpeechPart) =
        toggle(key, if (settings.speakOriginal) listOf(original, converted) else listOf(converted))

    fun start(key: String, parts: List<SpeechPart>) {
        activeKey = key
        activePart = -1
        activeWord = null
        val engine = speaker.speakAll(parts, settings, listener)
        if (engine == OnDeviceSpeaker.Engine.NONE) {
            clear()
            Toast.makeText(context, context.getString(R.string.tts_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    fun stop() {
        speaker.stop()
        clear()
    }

    private fun clear() {
        activeKey = null
        activePart = -1
        activeWord = null
    }

    internal fun release() = speaker.release()
}

@Composable
fun rememberSpeechController(): SpeechController {
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context) }
    val settings by prefs.speechSettings.collectAsState(initial = SpeechSettings())
    val enabled by prefs.ttsEnabled.collectAsState(initial = true)
    val controller = remember { SpeechController(OnDeviceSpeaker(context), context) }
    controller.settings = settings
    controller.enabled = enabled
    DisposableEffect(controller) { onDispose { controller.release() } }
    return controller
}

/** The language code to read [detected] in: the main language of a mixed result, English when unknown. */
fun speechLanguage(detected: String?): String {
    val code = detected.orEmpty().removePrefix("mixed_").substringBefore('_')
    return if (code.isBlank() || code == "unknown" || code == "multi") "en" else code
}

/** Speak / stop button. Hidden when speech is turned off in settings. */
@Composable
fun SpeakButton(
    controller: SpeechController,
    key: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!controller.enabled) return
    val speaking = controller.activeKey == key
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(if (speaking) R.drawable.ic_stop else R.drawable.ic_volume_up),
            contentDescription = if (speaking) stringResource(R.string.tts_stop) else description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Text with the word currently being spoken highlighted. */
@Composable
fun HighlightedText(
    text: String,
    controller: SpeechController,
    key: String,
    partIndex: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    val word = controller.activeWord.takeIf { controller.activeKey == key && controller.activePart == partIndex }
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val annotated: AnnotatedString = if (word == null || word.last >= text.length) {
        AnnotatedString(text)
    } else buildAnnotatedString {
        append(text.substring(0, word.first))
        withStyle(SpanStyle(background = highlight)) { append(text.substring(word.first, word.last + 1)) }
        append(text.substring(word.last + 1))
    }
    Text(text = annotated, style = style, color = color, modifier = modifier, maxLines = maxLines)
}

/** For editable fields: highlights the spoken word without changing the text or cursor offsets. */
@Composable
fun speakableHighlight(controller: SpeechController, key: String, partIndex: Int, color: Color): androidx.compose.ui.text.input.VisualTransformation {
    val word = controller.activeWord.takeIf { controller.activeKey == key && controller.activePart == partIndex }
    return androidx.compose.ui.text.input.VisualTransformation { text ->
        val styled = if (word == null || word.last >= text.length) text else buildAnnotatedString {
            append(text)
            addStyle(SpanStyle(background = color), word.first, word.last + 1)
        }
        androidx.compose.ui.text.input.TransformedText(styled, androidx.compose.ui.text.input.OffsetMapping.Identity)
    }
}
