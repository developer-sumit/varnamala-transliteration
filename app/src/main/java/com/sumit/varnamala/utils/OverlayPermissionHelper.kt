package com.sumit.varnamala.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

object OverlayPermissionHelper {
    /**
     * Request overlay permission by opening system settings
     */
    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
