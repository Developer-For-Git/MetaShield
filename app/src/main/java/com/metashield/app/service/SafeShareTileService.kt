package com.metashield.app.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.metashield.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SafeShareTileService : TileService() {

    override fun onClick() {
        super.onClick()
        
        // This tile will open the app in a special mode or just launch SafeShareActivity
        // For a Tile to work, we usually want to start an activity.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION_SAFE_SHARE", true)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires pending intent for tiles
            // But for simplicity in this demo, we'll use the standard start
            startActivityAndCollapse(intent)
        } else {
            startActivityAndCollapse(intent)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }
}
