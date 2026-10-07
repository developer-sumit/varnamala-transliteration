package com.sumit.varnamala.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sumit.varnamala.R
import com.sumit.varnamala.data.preferences.UserPreferencesRepository
import com.sumit.varnamala.tts.OnDeviceSpeaker
import com.sumit.varnamala.tts.RasaModelProvisioner
import com.sumit.varnamala.tts.RasaVoices
import com.sumit.varnamala.tts.SpeechSettings
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.ModelCard
import com.sumit.varnamala.utils.rememberDownloadGate
import kotlinx.coroutines.launch

private const val PREVIEW_TEXT = "नमस्ते, मैं आपकी आवाज़ हूँ।"

/**
 * Offline voice: the download status and progress on top; the voices, style, speed and pitch
 * appear underneath only once the model is installed.
 */
@Composable
fun VoiceScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val prefs = remember { UserPreferencesRepository(context) }
    val provisioner = remember { RasaModelProvisioner.get(context) }
    val model by provisioner.state.collectAsState()
    val gate = rememberDownloadGate()
    val settings by prefs.speechSettings.collectAsState(initial = SpeechSettings())
    val autoSpeak by prefs.ttsAutoSpeak.collectAsState(initial = false)
    val speaker = remember { OnDeviceSpeaker(context) }
    DisposableEffect(Unit) { onDispose { speaker.release() } }

    fun preview(changed: SpeechSettings = settings) {
        speaker.speak(PREVIEW_TEXT, "hi", changed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp, topInset + 5.dp, 16.dp, 0.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            GlassButton(description = stringResource(R.string.back), icon = painterResource(R.drawable.ic_chevron_left), onClick = { navController.popBackStack() })
            Text(stringResource(R.string.tts_voice_screen_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(45.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ModelCard(
                title = stringResource(R.string.offline_voice),
                description = stringResource(R.string.offline_voice_description),
                sizeBytes = RasaModelProvisioner.MODEL_BYTES,
                installed = model.isInstalled,
                downloading = model.isDownloading,
                progress = model.progress,
                error = model.error,
                onDownload = { gate.request(RasaModelProvisioner.MODEL_BYTES) { provisioner.start() } },
                onDelete = { speaker.stop(); provisioner.delete() },
            )

            Text(
                stringResource(R.string.offline_voice_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (model.isInstalled) {
                SectionTitle(stringResource(R.string.tts_voice))
                RasaVoices.all.forEach { voice ->
                    val name = stringResource(
                        if (voice.gender == RasaVoices.Gender.FEMALE) R.string.voice_female else R.string.voice_male,
                        voice.number,
                    )
                    val selected = voice.speakerId == settings.speakerId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.RadioButton) {
                                scope.launch { prefs.setTtsVoiceId(voice.speakerId) }
                                preview(settings.copy(speakerId = voice.speakerId))
                            }
                            .semantics(mergeDescendants = true) { contentDescription = name }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                SectionTitle(stringResource(R.string.tts_style))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll2()) {
                    RasaVoices.Style.entries.forEach { style ->
                        FilterChip(
                            selected = settings.emotionId == style.emotionId,
                            onClick = {
                                scope.launch { prefs.setTtsStyleId(style.emotionId) }
                                preview(settings.copy(emotionId = style.emotionId))
                            },
                            label = { Text(stringResource(styleLabel(style))) },
                        )
                    }
                }

                LabelledSlider(
                    label = stringResource(R.string.tts_speed),
                    value = settings.speed,
                    range = 0.6f..1.6f,
                    display = "%.1fx".format(settings.speed),
                    onFinished = { v -> scope.launch { prefs.setTtsSpeed(v) }; preview(settings.copy(speed = v)) },
                )
                LabelledSlider(
                    label = stringResource(R.string.tts_pitch),
                    value = settings.pitch,
                    range = 0.8f..1.25f,
                    display = "%.2fx".format(settings.pitch),
                    onFinished = { v -> scope.launch { prefs.setTtsPitch(v) }; preview(settings.copy(pitch = v)) },
                )
            }

            SectionTitle(stringResource(R.string.tts_behaviour))
            SwitchRow(
                title = stringResource(R.string.tts_speak_original),
                subtitle = stringResource(R.string.tts_speak_original_hint),
                checked = settings.speakOriginal,
                onChange = { scope.launch { prefs.setTtsSpeakOriginal(it) } },
            )
            SwitchRow(
                title = stringResource(R.string.tts_auto_speak),
                subtitle = stringResource(R.string.tts_auto_speak_hint),
                checked = autoSpeak,
                onChange = { scope.launch { prefs.setTtsAutoSpeak(it) } },
            )
        }
    }
    gate.Dialogs()
}

private fun styleLabel(style: RasaVoices.Style) = when (style) {
    RasaVoices.Style.NEUTRAL -> R.string.style_neutral
    RasaVoices.Style.NEWS -> R.string.style_news
    RasaVoices.Style.CONVERSATION -> R.string.style_conversation
    RasaVoices.Style.STORY -> R.string.style_story
    RasaVoices.Style.HAPPY -> R.string.style_happy
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun LabelledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onFinished: (Float) -> Unit) {
    var dragging by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(if (dragging == value) display else "%.2f".format(dragging), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = dragging,
            onValueChange = { dragging = it },
            onValueChangeFinished = { onFinished(dragging) },
            valueRange = range,
            modifier = Modifier.semantics { contentDescription = "$label $display" },
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Modifier.horizontalScroll2(): Modifier =
    this.then(Modifier.horizontalScroll(rememberScrollState()))
