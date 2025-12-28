package com.sumit.varnamala.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.provider.Settings

object OverlayManager {

    private var isServiceRunning = false
    private var hasScreenCapturePermission = false

    /**
     * Check if overlay permission is granted
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Request screen capture permission
     * Returns true if permission request was launched
     */
    fun requestScreenCapturePermission(activity: Activity, requestCode: Int): Boolean {
        if (!canDrawOverlays(activity)) {
            return false
        }

        val mediaProjectionManager =
            activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        activity.startActivityForResult(captureIntent, requestCode)
        return true
    }

    /**
     * Start overlay service with screen capture capability
     * Must be called after screen capture permission is granted
     */
    fun startOverlay(context: Context, resultCode: Int, data: Intent?): Boolean {
        if (!canDrawOverlays(context)) {
            return false
        }

        if (!isServiceRunning) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = OverlayService.ACTION_INIT_SCREEN_CAPTURE
                putExtra(OverlayService.EXTRA_RESULT_CODE, resultCode)
                putExtra(OverlayService.EXTRA_DATA, data)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            isServiceRunning = true
            hasScreenCapturePermission = true
        }
        return true
    }

    fun stopOverlay(context: Context) {
        if (isServiceRunning) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
            isServiceRunning = false
            hasScreenCapturePermission = false
        }
    }

    /**
     * Mark service as stopped (called from service onDestroy)
     */
    fun markServiceStopped() {
        isServiceRunning = false
        hasScreenCapturePermission = false
    }

    fun isOverlayRunning(): Boolean {
        return isServiceRunning
    }

    fun hasScreenCapturePermission(): Boolean {
        return hasScreenCapturePermission
    }
}
