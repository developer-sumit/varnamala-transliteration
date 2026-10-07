package com.sumit.varnamala

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner
import com.sumit.varnamala.ocr.paddle.PaddleOcrRepository
import com.sumit.varnamala.tts.RasaModelProvisioner
import com.sumit.varnamala.tts.RasaTtsEngine
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real pipelines on the phone with pictures pushed to /data/local/tmp. Logs under tag OCRTEST. */
@RunWith(AndroidJUnit4::class)
class OcrDeviceTest {
    @Test
    fun ocrOnDevice() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (name in listOf("ocrtest", "t_rot90", "t_columns", "t_flat", "t_tilt")) {
            val bitmap = BitmapFactory.decodeFile("/data/local/tmp/$name.png") ?: continue
            for ((tier, enhance) in listOf(PaddleModelProvisioner.Tier.TINY to false, PaddleModelProvisioner.Tier.MEDIUM to true)) {
                val repo = PaddleOcrRepository(context)
                try {
                    val t = System.currentTimeMillis()
                    val result = repo.processBitmap(bitmap, "eng", tier, enhance)
                    Log.i("OCRTEST", "$name $tier enhance=$enhance ${System.currentTimeMillis() - t}ms lines=${result.boundingBoxes.size} text=${result.text.replace("\n", " | ")}")
                    result.boundingBoxes.forEach { Log.i("OCRTEST", "    conf=${"%.2f".format(it.confidence)} ${it.boundingBox} '${it.text}'") }
                } catch (e: Throwable) {
                    Log.e("OCRTEST", "$name $tier failed", e)
                } finally { repo.release() }
            }
        }
    }

    @Test
    fun rasaOnDevice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val models = RasaModelProvisioner(context)
        if (!models.isReady()) { Log.i("OCRTEST", "rasa model not installed"); return }
        val engine = RasaTtsEngine(models.modelFile, models.tokensFile)
        for ((text, sid) in listOf("नमस्ते, आप कैसे हैं" to 0, "வணக்கம், நீங்கள் எப்படி இருக்கிறீர்கள்" to 8, "আপনি কেমন আছেন" to 1)) {
            val t = System.currentTimeMillis()
            val audio = engine.synthesize(text, sid, 0, 1f)
            val ms = System.currentTimeMillis() - t
            val seconds = audio.samples.size.toFloat() / audio.sampleRate
            val peak = audio.samples.maxOf { kotlin.math.abs(it) }
            Log.i("OCRTEST", "rasa sid=$sid '$text' -> ${"%.2f".format(seconds)}s audio in ${ms}ms (rtf ${"%.2f".format(ms / 1000f / seconds)}) peak=${"%.2f".format(peak)} rate=${audio.sampleRate}")
        }
        engine.close()
    }
}
