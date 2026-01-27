package com.sumit.varnamala.models.modeldownload

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object ModelDownloadManager {
    fun enqueueDownload(context: Context, tesseractLanguageCode: String): Boolean {
        if (!isNetworkAvailable(context)) return false

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(ModelDownloadWorker.KEY_TESS_LANG to tesseractLanguageCode))
            .addTag(tagForLanguage(tesseractLanguageCode))
            .build()

        // One download per language at a time.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uniqueWorkName(tesseractLanguageCode),
                ExistingWorkPolicy.KEEP,
                request
            )

        return true
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: SecurityException) {
            false
        }
    }

    fun uniqueWorkName(tesseractLanguageCode: String): String =
        "download_model_$tesseractLanguageCode"

    fun tagForLanguage(tesseractLanguageCode: String): String = "model_$tesseractLanguageCode"
}
