package com.sumit.varnamala.ocr.paddle

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Downloads and verifies the two PP-OCRv6 ONNX model tiers required by the app.
 *
 * Files come from the official PaddlePaddle Hugging Face repositories (pinned commits,
 * SHA-256 verified, resumable). The previous Baidu BOS source served the ~138 MB medium
 * tier at a few dozen KB/s, and file-count progress made that look like a hang at 50%.
 */
class PaddleModelProvisioner(context: Context) {
    data class State(
        val downloadedByTier: Map<Tier, Long> = emptyMap(),
        val stage: String = "OCR resources are required",
        val isDownloading: Boolean = false,
        val error: String? = null,
    ) {
        val downloadedBytes: Long get() = downloadedByTier.values.sum()
        val totalBytes: Long get() = TOTAL_BYTES
        val progress: Float get() = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
        val isComplete: Boolean get() = downloadedBytes >= totalBytes && error == null

        fun downloaded(tier: Tier): Long = downloadedByTier[tier] ?: 0L
        fun progress(tier: Tier): Float =
            (downloaded(tier).toFloat() / totalBytes(tier)).coerceIn(0f, 1f)
    }

    enum class Tier(val folder: String) { TINY("tiny"), MEDIUM("medium") }
    data class ModelPaths(val det: File, val rec: File, val config: File)
    private enum class Kind(val suffix: String) { DETECTION("det"), RECOGNITION("rec") }

    /** One file of a Hugging Face model repository, pinned to a commit and checksum. */
    private data class Resource(
        val tier: Tier,
        val kind: Kind,
        val fileName: String,
        val revision: String,
        val sizeBytes: Long,
        val sha256: String,
    ) {
        val repo get() = "PaddlePaddle/PP-OCRv6_${tier.folder}_${kind.suffix}_onnx"
        val url get() = "https://huggingface.co/$repo/resolve/$revision/$fileName"
    }

    private val root = File(context.applicationContext.filesDir, "paddle-ocr-v6")
    private val client = ModelDownloader.newClient()
    private val mutableState = MutableStateFlow(initialState())
    val state: StateFlow<State> = mutableState

    fun paths(tier: Tier): ModelPaths {
        val directory = File(root, tier.folder)
        return ModelPaths(
            det = File(directory, "det/inference.onnx"),
            rec = File(directory, "rec/inference.onnx"),
            config = File(directory, "rec/inference.yml"),
        )
    }

    fun isReady(): Boolean = RESOURCES.all { destination(it).isInstalled(it) }

    /** True when one tier's detector, recognizer and dictionary are all installed. */
    fun isReady(tier: Tier): Boolean = RESOURCES.filter { it.tier == tier }.all { destination(it).isInstalled(it) }

    /** Removes one tier's files. The tiny tier is the minimum the app needs to run OCR. */
    fun delete(tier: Tier) {
        File(root, tier.folder).deleteRecursively()
        mutableState.value = initialState()
    }

    /** Starts the download in the app's own scope so it keeps going when the screen is closed. */
    fun start(tiers: Set<Tier> = Tier.entries.toSet()) {
        if (mutableState.value.isDownloading) return
        mutableState.value = mutableState.value.copy(isDownloading = true, error = null, stage = "Preparing download…")
        appScope.launch { provision(tiers) }
    }

    /** Downloads the given tiers (both by default); tiers that are already installed are skipped. */
    suspend fun provision(tiers: Set<Tier> = Tier.entries.toSet()) = withContext(Dispatchers.IO) {
        try {
            mutableState.value = State(installedByTier(), isDownloading = true, stage = "Preparing download…")
            for (resource in RESOURCES.filter { it.tier in tiers }) {
                val target = destination(resource)
                if (target.isInstalled(resource)) continue
                val label = when (resource.kind) {
                    Kind.DETECTION -> "Downloading text detector"
                    Kind.RECOGNITION -> "Downloading text recognizer"
                }
                val installed = installedByTier()
                downloadVerified(resource, target) { fileBytes ->
                    mutableState.value = State(
                        downloadedByTier = installed + (resource.tier to (installed[resource.tier] ?: 0L) + fileBytes),
                        isDownloading = true,
                        stage = "$label…",
                    )
                }
            }
            check(tiers.all(::isReady)) { "Downloaded OCR resources failed verification." }
            mutableState.value = State(installedByTier(), stage = "OCR resources ready")
        } catch (error: Throwable) {
            mutableState.value = State(
                downloadedByTier = installedByTier(),
                stage = "Download interrupted",
                error = error.message ?: "Unable to download OCR resources.",
            )
        }
    }

    private fun initialState(): State = State(
        installedByTier(),
        stage = if (isReady()) "OCR resources ready" else "OCR resources are required",
    )

    private fun installedByTier(): Map<Tier, Long> = RESOURCES
        .filter { destination(it).isInstalled(it) }
        .groupBy { it.tier }
        .mapValues { (_, files) -> files.sumOf { it.sizeBytes } }

    private fun destination(resource: Resource): File =
        File(root, "${resource.tier.folder}/${resource.kind.suffix}/${resource.fileName}")

    /** Size check only: files reach their final path solely after checksum verification. */
    private fun File.isInstalled(resource: Resource) = isFile && when (resource.fileName) {
        "inference.onnx" -> length() > 1_000_000L
        else -> length() > 100L
    }

    private suspend fun downloadVerified(resource: Resource, target: File, onProgress: (Long) -> Unit) =
        ModelDownloader.downloadVerified(
            client, resource.url, resource.sizeBytes, resource.sha256, "${resource.repo}/${resource.fileName}",
            target, onProgress,
        )

    companion object {
        private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        @Volatile private var instance: PaddleModelProvisioner? = null

        /** One shared instance, so every screen sees the same download progress. */
        fun get(context: Context): PaddleModelProvisioner =
            instance ?: synchronized(this) { instance ?: PaddleModelProvisioner(context).also { instance = it } }

        // Sizes and SHA-256 values from the Hugging Face API at the pinned revisions.
        private val RESOURCES = listOf(
            Resource(Tier.TINY, Kind.DETECTION, "inference.onnx",
                "2ba1506c0380b8f0b03dd142459aac66d4421f6c", 1_780_590L,
                "193bab7a04fca699a6c82e6abb5b81bdb28177f0abd4062552b04908dafb19f8"),
            Resource(Tier.TINY, Kind.RECOGNITION, "inference.onnx",
                "2612ab37152ae0a677521bae4e1e3d4fb4cf7c30", 4_462_639L,
                "9ef676d6ed3c88256a2d92c640c44f25b0c40947e111b14b8be8f594091563e6"),
            Resource(Tier.TINY, Kind.RECOGNITION, "inference.yml",
                "2612ab37152ae0a677521bae4e1e3d4fb4cf7c30", 55_571L,
                "66170210bad538e83fff3c4a3867e547d6bf20b50d64b20347c4b913f3034ea1"),
            Resource(Tier.MEDIUM, Kind.DETECTION, "inference.onnx",
                "61323801669c338b7891481ec7bac61ce31b576a", 62_032_837L,
                "eb13b44b25bb36f89528b68720af8a61d9cf381176107f465db1757b65d086e1"),
            Resource(Tier.MEDIUM, Kind.RECOGNITION, "inference.onnx",
                "50c7eacafc52fa7bcf4194e8cd08e46f8558504b", 76_554_979L,
                "9c09abf0957f7968c7586464b7397b84ad2387a0497a351af40e9acc71b673ba"),
            Resource(Tier.MEDIUM, Kind.RECOGNITION, "inference.yml",
                "50c7eacafc52fa7bcf4194e8cd08e46f8558504b", 150_580L,
                "991b700facf5b50a7de193468207d5f4255b538dde0d312ae3b7c7a9b6873129"),
        )
        private val TOTAL_BYTES = RESOURCES.sumOf { it.sizeBytes }

        /** Download size of one tier's detector, recognizer and dictionary. */
        fun totalBytes(tier: Tier): Long = RESOURCES.filter { it.tier == tier }.sumOf { it.sizeBytes }
    }
}
