package com.sumit.varnamala.models.modeldownload

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object ModelDownloadManager {
    fun enqueueDownload(context: Context, tesseractLanguageCode: String) {
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
            .enqueueUniqueWork(uniqueWorkName(tesseractLanguageCode), ExistingWorkPolicy.KEEP, request)
    }

    fun uniqueWorkName(tesseractLanguageCode: String): String = "download_model_$tesseractLanguageCode"

    fun tagForLanguage(tesseractLanguageCode: String): String = "model_$tesseractLanguageCode"
}
