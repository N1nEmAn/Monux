package com.monux.network

import android.util.Log
import com.monux.protocol.Protocol
import fi.iki.elonen.NanoWSD
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

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
        sockets.toList().forEach { socket ->
            socket.shutdown("service stopping")
        }
        stop()
        Log.i(TAG, "WebSocket server stopped")
    }

    fun hasConnections(): Boolean = sockets.isNotEmpty()

    fun broadcast(text: String): Int {
        var delivered = 0
        sockets.toList().forEach { socket ->
            if (socket.sendSafely(text, "broadcast")) {
                delivered += 1
            }
        }
        return delivered
    }

    override fun openWebSocket(handshake: IHTTPSession?): WebSocket {
        return ClientSocket(handshake)
    }

    private inner class ClientSocket(private val session: IHTTPSession?) : WebSocket(session) {
        private val cleanedUp = AtomicBoolean(false)

        override fun onOpen() {
            sockets += this
            onClientConnected(session?.remoteIpAddress ?: "unknown")
            sendSafely(Protocol.hello(deviceName, PLATFORM_ANDROID).toString(), "hello")
        }

        override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
            cleanupSocket(reason ?: code?.name ?: "socket closed")
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
                    if (!sendSafely(Protocol.helloAck(deviceName, PLATFORM_ANDROID).toString(), "hello_ack")) {
                        return
                    }
                }
                Protocol.TYPE_PING -> {
                    if (!sendSafely(Protocol.pong().toString(), "pong")) {
                        return
                    }
                }
            }
            onMessageReceived(payload)
        }

        override fun onPong(pong: WebSocketFrame?) {
            Log.d(TAG, "pong received")
        }

        override fun onException(exception: IOException?) {
            cleanupSocket(exception?.message ?: "socket error", exception)
        }

        fun sendSafely(text: String, context: String): Boolean {
            return runCatching { send(text) }
                .onFailure { cleanupSocket("$context failed: ${it.message ?: it.javaClass.simpleName}", it) }
                .isSuccess
        }

        fun shutdown(reason: String) {
            runCatching { close(WebSocketFrame.CloseCode.NormalClosure, reason, false) }
                .onFailure { cleanupSocket(reason, it) }
            cleanupSocket(reason)
        }

        private fun cleanupSocket(reason: String?, exception: Throwable? = null) {
            if (!cleanedUp.compareAndSet(false, true)) {
                return
            }
            sockets -= this
            if (exception != null) {
                Log.w(TAG, "socket cleanup reason=$reason", exception)
            }
            onClientDisconnected(reason)
        }
    }

    companion object {
        private const val TAG = "MonuxWebSocketServer"
        private const val PLATFORM_ANDROID = "android"
    }
}
