package com.sumit.varnamala.widget

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sumit.varnamala.MainActivity
import com.sumit.varnamala.R

/** Quick Settings tile: swipe down, tap, and the camera opens. */
class CameraTileService : TileService() {
    override fun onStartListening() {
        qsTile?.apply {
            icon = Icon.createWithResource(this@CameraTileService, R.drawable.ic_tile_camera)
            label = getString(R.string.widget_label)
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
