package com.monux.screen

import com.monux.protocol.Protocol

class ScreenSessionManager(
    private val sendMessage: (String) -> Unit,
) {
    fun start(maxSize: Int, bitrate: String) {
        sendMessage(Protocol.screenStart(maxSize, bitrate).toString())
    }

    fun stop() {
        sendMessage(Protocol.screenStop().toString())
    }
}
