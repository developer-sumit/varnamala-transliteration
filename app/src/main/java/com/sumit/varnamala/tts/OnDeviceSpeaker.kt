package com.sumit.varnamala.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/** How the voice sounds. [speed] and [pitch] are multipliers around 1.0. */
data class SpeechSettings(
    val speakerId: Int = RasaVoices.default.speakerId,
    val emotionId: Int = 0,
    val speed: Float = 1f,
    val pitch: Float = 1f,
    val speakOriginal: Boolean = false,
)

/** One piece of text and the language it is written in. */
data class SpeechPart(val text: String, val languageCode: String)

/** Progress callbacks, always delivered on the main thread. */
interface SpeechListener {
    /** [partIndex] is the index into the parts being spoken; [range] is the word within that part's text. */
    fun onWord(partIndex: Int, range: IntRange) {}
    fun onDone() {}
}

/**
 * Speaks text fully on device. Indic languages use the Rasa ONNX voice once it is downloaded;
 * everything else (English, Urdu, or Indic before the download) uses the system voice.
 */
class OnDeviceSpeaker(context: Context) {
    enum class Engine { RASA, SYSTEM, NONE }

    private val appContext = context.applicationContext
    private val provisioner = RasaModelProvisioner(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val main = Handler(Looper.getMainLooper())
    private var rasa: RasaTtsEngine? = null
    private var job: Job? = null
    @Volatile private var track: AudioTrack? = null
    @Volatile private var systemReady = false
    private val systemUtterance = java.util.concurrent.atomic.AtomicReference<CompletableDeferred<Unit>?>(null)
    @Volatile private var systemWordListener: ((IntRange) -> Unit)? = null

    private val system = TextToSpeech(appContext) { systemReady = it == TextToSpeech.SUCCESS }.also { tts ->
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { systemUtterance.getAndSet(null)?.complete(Unit) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { systemUtterance.getAndSet(null)?.complete(Unit) }
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                systemWordListener?.invoke(start until end)
            }
        })
    }

    /** Language codes the Rasa voice can read (ISO 639-1 as used by the app's locale codes). */
    private val rasaLanguages = setOf("as", "bn", "gu", "hi", "kn", "ml", "mr", "or", "pa", "ta", "te", "ne", "sa")

    /** True when the downloaded Rasa voice will be used for [languageCode]. */
    fun usesRasa(languageCode: String) = languageCode.lowercase().take(2) in rasaLanguages && provisioner.isReady()

    fun isAvailableFor(languageCode: String) = usesRasa(languageCode) || systemReady

    /** Starts speaking [text], which must be written in the script of [languageCode]. */
    fun speak(text: String, languageCode: String, settings: SpeechSettings = SpeechSettings(), listener: SpeechListener? = null): Engine =
        speakAll(listOf(SpeechPart(text, languageCode)), settings, listener)

    /** Speaks each part in turn (for example the original sign, then its conversion). */
    fun speakAll(parts: List<SpeechPart>, settings: SpeechSettings = SpeechSettings(), listener: SpeechListener? = null): Engine {
        stop()
        val usable = parts.filter { it.text.isNotBlank() }
        if (usable.isEmpty()) return Engine.NONE
        val engine = when {
            usable.any { usesRasa(it.languageCode) } -> Engine.RASA
            systemReady -> Engine.SYSTEM
            else -> Engine.NONE
        }
        if (engine == Engine.NONE) return engine
        job = scope.launch {
            try {
                usable.forEachIndexed { index, part ->
                    val onWord: (IntRange) -> Unit = { range -> listener?.let { main.post { it.onWord(index, range) } } }
                    if (usesRasa(part.languageCode)) speakWithRasa(part, settings, onWord) else speakWithSystem(part, settings, onWord)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // A failed part must not leave the UI waiting; fall through to done.
            } finally {
                if (isActive) listener?.let { main.post { it.onDone() } }
            }
        }
        return engine
    }

    fun stop() {
        job?.cancel()
        track?.let { runCatching { it.pause(); it.flush(); it.release() } }
        track = null
        systemUtterance.getAndSet(null)?.complete(Unit)
        if (systemReady) system.stop()
    }

    fun release() {
        stop()
        scope.cancel()
        rasa?.close()
        rasa = null
        system.shutdown()
    }

    // ---- Rasa ----

    private suspend fun speakWithRasa(part: SpeechPart, settings: SpeechSettings, onWord: (IntRange) -> Unit) = coroutineScope2 {
        val engine = rasa ?: RasaTtsEngine(provisioner.modelFile, provisioner.tokensFile).also { rasa = it }
        val text = part.text
        // Pitch is applied by playing the audio at a different rate, which also changes speed;
        // the synthesis speed compensates so overall speed stays what the user chose.
        val pitch = settings.pitch.coerceIn(0.7f, 1.4f)
        val synthSpeed = (settings.speed / pitch).coerceIn(0.4f, 2.5f)
        val sentences = engine.sentenceRanges(text)
        // Synthesize the next sentence while the current one plays, so there is no gap between them.
        val audio: List<Deferred<FloatArray?>> = sentences.map { range ->
            async(Dispatchers.Default) {
                engine.synthesizeSentence(text.substring(range), settings.speakerId, settings.emotionId, synthSpeed)
            }
        }
        // Only keep one sentence ahead of playback: later ones start when the earlier one ends.
        for ((i, range) in sentences.withIndex()) {
            val samples = audio[i].await() ?: continue
            play(samples, (engine.sampleRate * pitch).toInt(), text, range, onWord)
        }
    }

    private suspend fun <T> coroutineScope2(block: suspend CoroutineScope.() -> T): T =
        kotlinx.coroutines.coroutineScope(block)

    /** Plays one sentence and reports each word as the playback position reaches it. */
    private suspend fun play(samples: FloatArray, rate: Int, fullText: String, range: IntRange, onWord: (IntRange) -> Unit) {
        val pcm = ShortArray(samples.size) { (samples[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
        val newTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track = newTrack
        try {
            newTrack.write(pcm, 0, pcm.size)
            newTrack.play()
            val words = wordSpans(fullText, range)
            val totalWeight = words.sumOf { it.second }.toFloat().coerceAtLeast(1f)
            var cumulative = 0f
            val starts = words.map { (_, weight) -> (cumulative / totalWeight).also { cumulative += weight } }
            var reported = -1
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                val head = newTrack.playbackHeadPosition
                if (head >= pcm.size) break
                val progress = head.toFloat() / pcm.size
                val index = starts.indexOfLast { it <= progress }
                if (index > reported && index >= 0) {
                    reported = index
                    onWord(words[index].first)
                }
                delay(30)
            }
        } finally {
            runCatching { newTrack.stop() }
            runCatching { newTrack.release() }
            if (track === newTrack) track = null
        }
    }

    /** Words inside [range] with a weight (letters plus a little for the gap) used to estimate when each is spoken. */
    private fun wordSpans(text: String, range: IntRange): List<Pair<IntRange, Int>> =
        Regex("\\S+").findAll(text.substring(range)).map {
            val start = range.first + it.range.first
            (start..range.first + it.range.last) to (it.value.length + 1)
        }.toList()

    // ---- System voice ----

    private suspend fun speakWithSystem(part: SpeechPart, settings: SpeechSettings, onWord: (IntRange) -> Unit) {
        if (!systemReady) return
        val code = part.languageCode.lowercase().take(2)
        system.language = Locale(code, if (code == "en") "US" else "IN")
        system.setSpeechRate(settings.speed.coerceIn(0.5f, 2f))
        system.setPitch(settings.pitch.coerceIn(0.5f, 2f))
        val done = CompletableDeferred<Unit>()
        systemUtterance.set(done)
        systemWordListener = if (Build.VERSION.SDK_INT >= 26) onWord else null
        if (system.speak(part.text, TextToSpeech.QUEUE_FLUSH, null, "varnamala") != TextToSpeech.SUCCESS) {
            systemUtterance.set(null)
            return
        }
        try {
            done.await()
        } finally {
            systemWordListener = null
        }
    }
}
