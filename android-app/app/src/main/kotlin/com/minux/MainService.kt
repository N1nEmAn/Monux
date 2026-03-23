package com.minux

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import fi.iki.elonen.NanoWSD
import org.json.JSONObject

class MainService : Service() {
    private var webSocketServer: MinuxSocketServer? = null
    private var clipboardManager: ClipboardManager? = null

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        createChannel()
        startForeground(NOTIFICATION_ID, foregroundNotification())
        webSocketServer = MinuxSocketServer(8765)
        webSocketServer?.start()
        Log.i(TAG, "MainService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        webSocketServer?.stop()
        webSocketServer = null
        super.onDestroy()
        Log.i(TAG, "MainService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Minux", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    private fun foregroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Minux connected")
            .setContentText("Phone bridge is ready")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private inner class MinuxSocketServer(port: Int) : NanoWSD(port) {
        override fun openWebSocket(handshake: IHTTPSession?): WebSocket {
            return object : WebSocket(handshake) {
                override fun onOpen() {
                    Log.i(TAG, "linux daemon connected")
                }

                override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
                    Log.i(TAG, "linux daemon disconnected reason=$reason")
                }

                override fun onMessage(message: WebSocketFrame) {
                    val payload = message.textPayload ?: return
                    Log.i(TAG, "message=$payload")
                }

                override fun onPong(pong: WebSocketFrame?) = Unit

                override fun onException(exception: java.lang.Exception?) {
                    Log.e(TAG, "socket error", exception)
                }
            }
        }
    }

    fun mirrorNotification(appName: String, title: String, body: String) {
        val payload = JSONObject()
            .put("type", "notify")
            .put("app", appName)
            .put("title", title)
            .put("body", body)
        webSocketServer?.broadcast(payload.toString())
    }

    fun syncClipboard(text: String) {
        clipboardManager?.setPrimaryClip(ClipData.newPlainText("minux", text))
        val payload = JSONObject()
            .put("type", "clipboard")
            .put("text", text)
        webSocketServer?.broadcast(payload.toString())
    }

    companion object {
        private const val TAG = "MinuxMainService"
        private const val CHANNEL_ID = "minux_bridge"
        private const val NOTIFICATION_ID = 1001
    }
}
