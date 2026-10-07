package com.sumit.varnamala.tts

import android.content.Context
import com.sumit.varnamala.ocr.paddle.ModelDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Downloads the AI4Bharat Rasa multilingual Indic VITS voice (ONNX, CC-BY-4.0): one 118 MB model
 * covering Assamese, Bengali, Bodo, Gujarati, Hindi, Kannada, Malayalam, Manipuri, Marathi,
 * Odia, Punjabi, Tamil and Telugu. Pinned to a commit and SHA-256 verified.
 */
class RasaModelProvisioner(context: Context) {
    data class State(
        val isInstalled: Boolean,
        val isDownloading: Boolean = false,
        val progress: Float = 0f,
        val error: String? = null,
    )

    private val root = File(context.applicationContext.filesDir, "rasa-tts")
    private val client = ModelDownloader.newClient()
    private val mutableState = MutableStateFlow(State(isInstalled = isReady()))
    val state: StateFlow<State> = mutableState

    val modelFile get() = File(root, "model.onnx")
    val tokensFile get() = File(root, "tokens.txt")

    fun isReady(): Boolean =
        modelFile.length() == MODEL_BYTES && tokensFile.length() == TOKENS_BYTES

    /** Starts the download in the app's own scope so it keeps going when the screen is closed. */
    fun start() {
        if (mutableState.value.isDownloading || isReady()) return
        mutableState.value = State(isInstalled = false, isDownloading = true)
        appScope.launch { provision() }
    }

    suspend fun provision() = withContext(Dispatchers.IO) {
        mutableState.value = State(isInstalled = false, isDownloading = true)
        try {
            if (tokensFile.length() != TOKENS_BYTES) {
                ModelDownloader.downloadVerified(client, url("tokens.txt"), TOKENS_BYTES, TOKENS_SHA256, "rasa tokens", tokensFile) {}
            }
            if (modelFile.length() != MODEL_BYTES) {
                ModelDownloader.downloadVerified(client, url("model.onnx"), MODEL_BYTES, MODEL_SHA256, "rasa model", modelFile) {
                    mutableState.value = State(isInstalled = false, isDownloading = true, progress = it.toFloat() / MODEL_BYTES)
                }
            }
            mutableState.value = State(isInstalled = isReady())
        } catch (error: Throwable) {
            mutableState.value = State(isInstalled = false, error = error.message ?: "Unable to download the voice.")
        }
    }

    fun delete() {
        root.deleteRecursively()
        mutableState.value = State(isInstalled = false)
    }

    private fun url(file: String) = "https://huggingface.co/MatiasLin/sherpa-onnx-vits-rasa-13/resolve/$REVISION/$file"

    companion object {
        private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        @Volatile private var instance: RasaModelProvisioner? = null

        /** One shared instance, so every screen sees the same download progress. */
        fun get(context: Context): RasaModelProvisioner =
            instance ?: synchronized(this) { instance ?: RasaModelProvisioner(context).also { instance = it } }

        private const val REVISION = "b5afd4e991e3bdd6195528de650e2ed219d47506"
        const val MODEL_BYTES = 123_338_635L
        private const val TOKENS_BYTES = 9_556L
        private const val MODEL_SHA256 = "4473f8d0b6f738196c1626bbf337cda99b34749b638bb7234c4cb8e1992bf804"
        private const val TOKENS_SHA256 = "32ad56b2533484ccd61f8c783cc32b1f90ae06e65469476548c2f832eb6d3a9c"
    }
}
