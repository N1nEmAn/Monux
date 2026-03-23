package com.minux

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.minux.network.WebSocketServer
import org.json.JSONObject

class MainService : Service() {
    private var webSocketServer: WebSocketServer? = null
    private var clipboardManager: ClipboardManager? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            webSocketServer?.broadcast(JSONObject().put("type", "ping").put("payload", JSONObject()).toString())
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        createChannel()
        startForeground(NOTIFICATION_ID, foregroundNotification())
        startWebSocketServer()
        registerMdnsService()
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS)
        Log.i(TAG, "MainService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        unregisterMdnsService()
        webSocketServer?.stopServer()
        webSocketServer = null
        super.onDestroy()
        Log.i(TAG, "MainService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startWebSocketServer() {
        val deviceName = Build.MODEL ?: "Android"
        val server = WebSocketServer(
            port = SERVER_PORT,
            deviceName = deviceName,
            onClientConnected = { remote -> Log.i(TAG, "linux daemon connected remote=$remote") },
            onClientDisconnected = { reason -> Log.i(TAG, "linux daemon disconnected reason=$reason") },
            onMessageReceived = { message -> Log.i(TAG, "message=$message") },
        )
        server.startServer()
        webSocketServer = server
    }

    private fun registerMdnsService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "Minux-${Build.MODEL ?: "Android"}"
            serviceType = SERVICE_TYPE
            port = SERVER_PORT
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "mDNS registered name=${serviceInfo.serviceName} port=${serviceInfo.port}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "mDNS registration failed code=$errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "mDNS unregistered name=${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "mDNS unregistration failed code=$errorCode")
            }
        }
        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        registrationListener = listener
    }

    private fun unregisterMdnsService() {
        val listener = registrationListener ?: return
        runCatching { nsdManager?.unregisterService(listener) }
            .onFailure { Log.w(TAG, "mDNS unregister failed", it) }
        registrationListener = null
    }

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
        private const val SERVER_PORT = 39281
        private const val SERVICE_TYPE = "_minux._tcp."
        private const val HEARTBEAT_INTERVAL_MS = 15_000L
    }
}
