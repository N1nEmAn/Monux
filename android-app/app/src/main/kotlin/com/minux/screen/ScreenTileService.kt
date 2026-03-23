package com.minux.screen

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.minux.MainService

class ScreenTileService : TileService() {
    override fun onClick() {
        super.onClick()
        MainService.toggleScreenMirror()
        qsTile?.state = if (MainService.screenEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.state = if (MainService.screenEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }
}
