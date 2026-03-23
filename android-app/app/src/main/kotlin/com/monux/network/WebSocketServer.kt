package com.monux.network

import android.util.Log
import com.monux.protocol.Protocol
import fi.iki.elonen.NanoWSD
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class WebSocketServer(
    private val port: Int,
    private val deviceName: String,
    private val onClientConnected: (String) -> Unit,
    private val onClientDisconnected: (String?) -> Unit,
    private val onMessageReceived: (JSONObject) -> Unit,
    private val onPeerHello: (String, String) -> Unit,
) : NanoWSD(port) {
    private val sockets = ConcurrentHashMap.newKeySet<ClientSocket>()

    @Throws(IOException::class)
    fun startServer() {
        start(SOCKET_READ_TIMEOUT, false)
        Log.i(TAG, "WebSocket server started on port=$port")
    }

    fun stopServer() {
        sockets.forEach { socket ->
            runCatching { socket.close(WebSocketFrame.CloseCode.NormalClosure, "service stopping", false) }
        }
        sockets.clear()
        stop()
        Log.i(TAG, "WebSocket server stopped")
    }

    fun hasConnections(): Boolean = sockets.isNotEmpty()

    fun broadcast(text: String) {
        sockets.forEach { socket ->
            runCatching { socket.send(text) }
                .onFailure { Log.w(TAG, "broadcast failed", it) }
        }
    }

    override fun openWebSocket(handshake: IHTTPSession?): WebSocket {
        return ClientSocket(handshake)
    }

    private inner class ClientSocket(private val session: IHTTPSession?) : WebSocket(session) {
        override fun onOpen() {
            sockets += this
            onClientConnected(session?.remoteIpAddress ?: "unknown")
            send(Protocol.hello(deviceName, PLATFORM_ANDROID).toString())
        }

        override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
            sockets -= this
            onClientDisconnected(reason)
        }

        override fun onMessage(message: WebSocketFrame) {
            val raw = message.textPayload ?: return
            val payload = runCatching { JSONObject(raw) }
                .onFailure { Log.w(TAG, "invalid payload=$raw", it) }
                .getOrNull() ?: return

            when (payload.optString("type")) {
                Protocol.TYPE_HELLO -> {
                    val inner = payload.optJSONObject("payload")
                    val peerName = inner?.optString("device_name").orEmpty()
                    val peerPlatform = inner?.optString("platform").orEmpty()
                    onPeerHello(peerName, peerPlatform)
                    send(Protocol.helloAck(deviceName, PLATFORM_ANDROID).toString())
                }
                Protocol.TYPE_PING -> send(Protocol.pong().toString())
            }
            onMessageReceived(payload)
        }

        override fun onPong(pong: WebSocketFrame?) {
            Log.d(TAG, "pong received")
        }

        override fun onException(exception: IOException?) {
            Log.e(TAG, "socket error", exception)
        }
    }

    companion object {
        private const val TAG = "MonuxWebSocketServer"
        private const val PLATFORM_ANDROID = "android"
    }
}
