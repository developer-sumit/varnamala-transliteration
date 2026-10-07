package com.sumit.varnamala.tts

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.text.Normalizer

/**
 * Runs `ai4bharat/vits_rasa_13` (ONNX export with an `emotion_id` input) directly on ONNX
 * Runtime. Input is native-script text: the model's frontend is characters, so Latin
 * transliteration cannot be spoken by it. Thread-safe; synthesis calls are serialized.
 */
class RasaTtsEngine(modelFile: File, tokensFile: File, threads: Int = 4) : AutoCloseable {
    class Audio(val samples: FloatArray, val sampleRate: Int)

    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(
        modelFile.absolutePath,
        OrtSession.SessionOptions().apply { setIntraOpNumThreads(threads) },
    )
    private val tokens: Map<Char, Long> = loadTokens(tokensFile)
    val sampleRate: Int = session.metadata.customMetadata["sample_rate"]?.toIntOrNull() ?: DEFAULT_SAMPLE_RATE

    /**
     * Synthesizes the whole [text] sentence by sentence.
     * @param speakerId voice, 0 until [SPEAKERS]. The speaker-to-language table of the upstream
     *   model is gated, so any speaker is used for any language.
     * @param emotionId style, 0 until [EMOTIONS]; 0 is neutral narration.
     * @param speed 1.0 is normal; higher is faster.
     */
    fun synthesize(text: String, speakerId: Int = 0, emotionId: Int = 0, speed: Float = 1f): Audio {
        val pause = FloatArray((sampleRate * SENTENCE_PAUSE_SECONDS).toInt())
        val out = ArrayList<FloatArray>()
        for (range in sentenceRanges(text)) {
            val samples = synthesizeSentence(text.substring(range), speakerId, emotionId, speed) ?: continue
            if (out.isNotEmpty()) out += pause
            out += samples
        }
        val samples = FloatArray(out.sumOf { it.size })
        var at = 0
        for (part in out) { part.copyInto(samples, at); at += part.size }
        return Audio(samples, sampleRate)
    }

    /** One sentence of audio, or null when none of its characters are in the vocabulary. */
    @Synchronized
    fun synthesizeSentence(sentence: String, speakerId: Int, emotionId: Int, speed: Float): FloatArray? {
        val ids = encode(sentence)
        if (ids.isEmpty()) return null
        return run(ids, speakerId.coerceIn(0, SPEAKERS - 1), emotionId.coerceIn(0, EMOTIONS - 1), speed)
    }

    /** Character ranges of the sentences in [text], so callers can follow along while it plays. */
    fun sentenceRanges(text: String): List<IntRange> {
        val ranges = ArrayList<IntRange>()
        var start = 0
        fun flush(endExclusive: Int) {
            var from = start
            var to = endExclusive
            while (from < to && text[from].isWhitespace()) from++
            while (to > from && text[to - 1].isWhitespace()) to--
            if (to > from) ranges += from until to
            start = endExclusive
        }
        for (i in text.indices) {
            val c = text[i]
            if (c in ".?!।॥\n" || (i + 1 - start >= MAX_SENTENCE_CHARS && c == ' ')) flush(i + 1)
        }
        flush(text.length)
        return ranges
    }

    /** True when at least one character of [text] is in the model's vocabulary. */
    fun canSpeak(text: String): Boolean = Normalizer.normalize(text, Normalizer.Form.NFC).any { it in tokens && !it.isWhitespace() }

    private fun run(ids: LongArray, speakerId: Int, emotionId: Int, speed: Float): FloatArray {
        // add_blank: the model expects a blank (0) between and around every token.
        val x = LongArray(ids.size * 2 + 1)
        ids.forEachIndexed { i, id -> x[i * 2 + 1] = id }
        val tensors = mutableListOf<OnnxTensor>()
        try {
            fun tensor(t: OnnxTensor) = t.also(tensors::add)
            val inputs = mapOf(
                "x" to tensor(OnnxTensor.createTensor(env, LongBuffer.wrap(x), longArrayOf(1, x.size.toLong()))),
                "x_length" to tensor(OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(x.size.toLong())), longArrayOf(1))),
                "noise_scale" to tensor(OnnxTensor.createTensor(env, FloatBuffer.wrap(floatArrayOf(NOISE_SCALE)), longArrayOf())),
                "length_scale" to tensor(OnnxTensor.createTensor(env, FloatBuffer.wrap(floatArrayOf(1f / speed.coerceIn(0.5f, 2f))), longArrayOf())),
                "noise_scale_w" to tensor(OnnxTensor.createTensor(env, FloatBuffer.wrap(floatArrayOf(NOISE_SCALE_W)), longArrayOf())),
                "sid" to tensor(OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(speakerId.toLong())), longArrayOf(1))),
                "emotion_id" to tensor(OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(emotionId.toLong())), longArrayOf(1))),
            )
            session.run(inputs).use { result ->
                val y = result[0] as OnnxTensor
                val buffer = y.floatBuffer
                return FloatArray(buffer.remaining()).also { buffer.get(it) }
            }
        } finally {
            tensors.forEach(OnnxTensor::close)
        }
    }

    private fun encode(sentence: String): LongArray {
        val normalized = Normalizer.normalize(sentence, Normalizer.Form.NFC)
            .replace('“', '"').replace('”', '"').replace('‘', '\'').replace('’', '\'')
            .replace('…', '.').replace('\n', ' ')
        return normalized.mapNotNull { tokens[it] }.toLongArray()
    }

    override fun close() {
        session.close()
    }

    companion object {
        const val SPEAKERS = 1024
        const val EMOTIONS = 32
        private const val DEFAULT_SAMPLE_RATE = 24_000
        private const val NOISE_SCALE = 0.667f
        private const val NOISE_SCALE_W = 0.8f
        private const val SENTENCE_PAUSE_SECONDS = 0.15f
        private const val MAX_SENTENCE_CHARS = 160

        /** One `<char> <id>` per line. The space character is stored as a line starting with a space. */
        private fun loadTokens(file: File): Map<Char, Long> = buildMap {
            file.forEachLine(Charsets.UTF_8) { line ->
                val split = line.lastIndexOf(' ')
                if (split < 0) return@forEachLine
                val id = line.substring(split + 1).toLongOrNull() ?: return@forEachLine
                val symbol = line.substring(0, split)
                if (symbol.length == 1) put(symbol[0], id) else if (symbol.isEmpty()) put(' ', id)
            }
        }
    }
}
