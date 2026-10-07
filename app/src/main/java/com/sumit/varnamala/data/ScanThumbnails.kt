package com.sumit.varnamala.data

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.util.UUID

/** Small JPEG previews of scans, kept in app storage so history lists can show the picture. */
object ScanThumbnails {
    private const val MAX_SIDE = 256
    private const val QUALITY = 80

    /** Saves a downscaled copy of [bitmap]; returns the file path to store in the history entry. */
    fun save(context: Context, bitmap: Bitmap): String {
        val directory = File(context.filesDir, "thumbs").apply { mkdirs() }
        val scale = MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val small = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        } else bitmap
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        return file.absolutePath
    }

    /** Removes a thumbnail file; content URIs and empty references are left alone. */
    fun delete(imageRef: String) {
        if (imageRef.startsWith("/")) runCatching { File(imageRef).delete() }
    }
}
