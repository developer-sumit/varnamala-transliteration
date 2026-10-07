package com.paddle.ocr.util

import android.graphics.Bitmap
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.CLAHE
import org.opencv.imgproc.Imgproc

/** Photo clean-up applied before recognition. Needs [OpenCVUtils.init] to have succeeded. */
object ImageEnhancer {
    /** Grey-level spread below which a photo counts as washed out or dim. */
    private const val LOW_CONTRAST_STDDEV = 55.0

    /**
     * Boosts local contrast (CLAHE on lightness) when the photo is flat; returns [bitmap] itself
     * when it already has enough contrast, so good photos are never degraded.
     */
    fun boostContrast(bitmap: Bitmap): Bitmap {
        val bgr = BitmapUtils.bitmapToBGRMat(bitmap)
        val gray = Mat()
        val lab = Mat()
        val channels = ArrayList<Mat>()
        try {
            Imgproc.cvtColor(bgr, gray, Imgproc.COLOR_BGR2GRAY)
            val mean = org.opencv.core.MatOfDouble()
            val std = org.opencv.core.MatOfDouble()
            Core.meanStdDev(gray, mean, std)
            val spread = std.toArray().firstOrNull() ?: 255.0
            mean.release(); std.release()
            if (spread >= LOW_CONTRAST_STDDEV) return bitmap

            Imgproc.cvtColor(bgr, lab, Imgproc.COLOR_BGR2Lab)
            Core.split(lab, channels)
            val clahe: CLAHE = Imgproc.createCLAHE(3.0, org.opencv.core.Size(8.0, 8.0))
            clahe.apply(channels[0], channels[0])
            Core.merge(channels, lab)
            Imgproc.cvtColor(lab, bgr, Imgproc.COLOR_Lab2BGR)
            return BitmapUtils.bgrMatToBitmap(bgr)
        } finally {
            bgr.release(); gray.release(); lab.release(); channels.forEach(Mat::release)
        }
    }
}
