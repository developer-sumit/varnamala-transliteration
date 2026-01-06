package com.sumit.varnamala.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

object ModelSizeFetcher {
    private val cache = ConcurrentHashMap<String, Long?>()
    private val client by lazy { OkHttpClient.Builder().build() }

    suspend fun getRemoteSizeBytes(url: String): Long? {
        cache[url]?.let { return it }

        val size = withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).head().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.header("Content-Length")?.toLongOrNull()
                }
            } catch (_: Exception) {
                null
            }
        }

        cache[url] = size
        return size
    }
}
