package com.sumit.varnamala.ocr.paddle

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Resumable, checksum-verified model download shared by the OCR and speech model stores. */
internal object ModelDownloader {
    private const val BUFFER = 64 * 1024
    private const val REPORT_EVERY_BYTES = 256 * 1024L

    fun newClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Streams into `<name>.partial`, resuming with an HTTP Range request when a partial file
     * exists, then verifies SHA-256 before atomically moving it into place.
     */
    suspend fun downloadVerified(
        client: OkHttpClient,
        url: String,
        sizeBytes: Long,
        sha256: String,
        label: String,
        target: File,
        onProgress: (Long) -> Unit,
    ) {
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, "${target.name}.partial")
        if (partial.length() > sizeBytes) partial.delete()
        val offset = partial.length()

        // A complete partial from an interrupted verify goes straight to checksum; asking the
        // server for "bytes=size-" would be answered with 416.
        if (offset < sizeBytes) client.newCall(
            Request.Builder().url(url).apply {
                if (offset > 0) header("Range", "bytes=$offset-")
            }.build()
        ).execute().use { response ->
            check(response.isSuccessful) { "Model download failed (${response.code})." }
            val append = offset > 0 && response.code == 206
            var written = if (append) offset else 0L
            val body = checkNotNull(response.body) { "Model download was empty." }
            FileOutputStream(partial, append).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(BUFFER)
                    var lastReport = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        written += read
                        if (written - lastReport >= REPORT_EVERY_BYTES) {
                            lastReport = written
                            onProgress(written)
                        }
                    }
                }
            }
            onProgress(written)
        }

        if (!partial.sha256().equals(sha256, ignoreCase = true)) {
            partial.delete()
            error("Checksum mismatch for $label.")
        }
        if (target.exists()) target.delete()
        check(partial.renameTo(target)) { "Unable to install model file." }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
