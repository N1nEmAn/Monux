package com.monux.input

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.monux.MainService

class InputTileService : TileService() {
    override fun onClick() {
        super.onClick()
        if (!MainService.remoteInputEnabled()) {
            updateTileState(Tile.STATE_UNAVAILABLE)
            return
        }
        val intent = Intent(this, RemoteInputActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
        updateTileState(Tile.STATE_ACTIVE)
    }

    override fun onStartListening() {
        super.onStartListening()
        val state = if (MainService.remoteInputEnabled()) Tile.STATE_ACTIVE else Tile.STATE_UNAVAILABLE
        updateTileState(state)
    }

    private fun updateTileState(state: Int) {
        qsTile?.state = state
        qsTile?.updateTile()
    }
}
