package com.sumit.varnamala.models.modeldownload

import com.sumit.varnamala.R
import com.sumit.varnamala.models.ModelRegistry
import com.sumit.varnamala.models.TesseractModelStore

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.workDataOf
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val tessLang = inputData.getString(KEY_TESS_LANG) ?: return Result.failure()
        val model = ModelRegistry.forAppLanguageCode(tessLang) ?: run {
            // Worker input is expected to be tesseract code already.
            val fileName = "$tessLang.traineddata"
            ModelRegistry.Model(
                tesseractLanguageCode = tessLang,
                fileName = fileName,
                downloadUrl = "https://github.com/tesseract-ocr/tessdata_best/raw/main/$fileName"
            )
        }

        val outFile = TesseractModelStore.modelFile(applicationContext, model.tesseractLanguageCode)
        val tmpFile = File(outFile.parentFile, outFile.name + ".download")
        val notificationId =
            NOTIFICATION_ID_BASE + (model.tesseractLanguageCode.hashCode().absoluteValue % 10000)

        return try {
            ensureChannel()
            postProgressNotification(model.tesseractLanguageCode, 0, notificationId)
            setProgress(
                workDataOf(
                    KEY_PROGRESS to 0,
                    KEY_TOTAL_BYTES to -1L,
                    KEY_DOWNLOADED_BYTES to 0L
                )
            )

            downloadWithProgress(
                model.downloadUrl,
                tmpFile
            ) { percent, downloadedBytes, totalBytes ->
                postProgressNotification(model.tesseractLanguageCode, percent, notificationId)
                setProgress(
                    workDataOf(
                        KEY_PROGRESS to percent,
                        KEY_TOTAL_BYTES to (totalBytes ?: -1L),
                        KEY_DOWNLOADED_BYTES to downloadedBytes
                    )
                )
            }
            if (outFile.exists()) outFile.delete()
            tmpFile.renameTo(outFile)
            cancelNotification(notificationId)
            setProgress(
                workDataOf(
                    KEY_PROGRESS to 100,
                    KEY_TOTAL_BYTES to outFile.length(),
                    KEY_DOWNLOADED_BYTES to outFile.length()
                )
            )
            Result.success()
        } catch (_: Exception) {
            tmpFile.delete()
            cancelNotification(notificationId)
            Result.retry()
        }
    }

    private suspend fun downloadWithProgress(
        url: String,
        destination: File,
        onProgress: suspend (percent: Int, downloadedBytes: Long, totalBytes: Long?) -> Unit
    ) = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")

            val body = response.body ?: throw IllegalStateException("Empty body")
            val totalBytes = body.contentLength().takeIf { it > 0L }

            destination.parentFile?.mkdirs()
            FileOutputStream(destination).use { fos ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L
                var lastPercent = -1

                body.byteStream().use { input ->
                    while (true) {
                        if (isStopped) throw IllegalStateException("Stopped")
                        val read = input.read(buffer)
                        if (read <= 0) break
                        fos.write(buffer, 0, read)
                        downloaded += read

                        if (totalBytes != null) {
                            val percent = ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent, downloaded, totalBytes)
                            }
                        }
                    }
                }

                if (totalBytes != null && lastPercent < 100) {
                    onProgress(100, downloaded, totalBytes)
                } else if (totalBytes == null) {
                    // Unknown size, still emit occasional progress updates.
                    onProgress(lastPercent.coerceAtLeast(0), downloaded, null)
                }
                fos.flush()
            }
        }
    }

    private fun postProgressNotification(tessLang: String, percent: Int, notificationId: Int) {
        val channelId = CHANNEL_ID
        val title = applicationContext.getString(R.string.model_download_notification_title)
        val content = applicationContext.getString(
            R.string.model_download_notification_content,
            tessLang,
            percent
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_database)
            .setContentTitle(title)
            .setContentText(content)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, percent, false)
            .build()

        val mgr =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(notificationId, notification)
    }

    private fun cancelNotification(notificationId: Int) {
        val mgr =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.cancel(notificationId)
    }

    private fun ensureChannel() {
        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = mgr.getNotificationChannel(channelId)
            if (existing == null) {
                val channel = NotificationChannel(
                    channelId,
                    applicationContext.getString(R.string.model_download_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                )
                mgr.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val KEY_TESS_LANG = "tessLang"
        const val KEY_PROGRESS = "progressPercent"
        const val KEY_TOTAL_BYTES = "totalBytes"
        const val KEY_DOWNLOADED_BYTES = "downloadedBytes"
        private const val CHANNEL_ID = "model_downloads"
        private const val NOTIFICATION_ID_BASE = 7000
    }
}

private val Int.absoluteValue: Int
    get() = if (this < 0) -this else this
