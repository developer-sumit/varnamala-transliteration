package com.sumit.varnamala.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.nio.ByteBuffer

/**
 * Helper class to capture screen using MediaProjection API
 */
class ScreenCaptureHelper(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val displayMetrics: DisplayMetrics by lazy {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        DisplayMetrics().apply {
            windowManager.defaultDisplay.getMetrics(this)
        }
    }

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1001
    }

    /**
     * Initialize MediaProjection with result from permission request
     */
    fun initMediaProjection(resultCode: Int, data: Intent?) {
        val mediaProjectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
    }

    /**
     * Capture screen and return as Bitmap
     */
    fun captureScreen(onCapture: (Bitmap?) -> Unit) {
        if (mediaProjection == null) {
            onCapture(null)
            return
        }

        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi

        // Create ImageReader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        // Create VirtualDisplay
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        // Wait longer for the virtual display to be ready and render frames
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val image = imageReader?.acquireLatestImage()
                if (image != null) {
                    val bitmap = imageToBitmap(image)
                    image.close()
                    onCapture(bitmap)
                } else {
                    onCapture(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onCapture(null)
            } finally {
                // Clean up
                virtualDisplay?.release()
                imageReader?.close()
                virtualDisplay = null
                imageReader = null
            }
        }, 300) // Increased delay to allow virtual display to render frames
    }

    /**
     * Convert Image to Bitmap
     */
    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        return if (rowPadding == 0) {
            bitmap
        } else {
            // Crop to actual screen size if there's padding
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        }
    }

    /**
     * Release resources
     */
    fun release() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()

        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    /**
     * Check if MediaProjection is active
     */
    fun isActive(): Boolean {
        return mediaProjection != null
    }
}
