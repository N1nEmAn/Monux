package com.monux

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.monux.clipboard.ClipboardMonitor
import com.monux.clipboard.ClipboardSyncManager
import android.net.Uri
import com.monux.file.FileTransferManager
import com.monux.network.WebSocketServer
import com.monux.protocol.Protocol
import com.monux.screen.ScreenSessionManager
import com.monux.sms.SmsReplyExecutor
import com.monux.ui.state.ConnectionState
import com.monux.ui.state.FeatureFlags
import com.monux.ui.state.FileTransferState
import com.monux.ui.state.ScreenState
import com.monux.ui.state.UiPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class MainService : Service() {
    private var webSocketServer: WebSocketServer? = null
    private var clipboardManager: ClipboardManager? = null
    private var clipboardMonitor: ClipboardMonitor? = null
    private var clipboardSyncManager: ClipboardSyncManager? = null
    private var fileTransferManager: FileTransferManager? = null
    private var screenSessionManager: ScreenSessionManager? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            broadcastToLinux(Protocol.ping().toString(), "heartbeat ping")
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardSyncManager = ClipboardSyncManager(
            clipboardManager = clipboardManager!!,
            sendMessage = { payload -> broadcastToLinux(payload, "clipboard") },
        )
        fileTransferManager = FileTransferManager(
            contentResolver = contentResolver,
            sendMessage = { payload -> broadcastToLinux(payload, "file transfer") },
            onProgress = { fileName, progress ->
                state.value = state.value.copy(fileTransfer = FileTransferState(fileName = fileName, progress = progress, active = progress < 1f))
            },
        )
        screenSessionManager = ScreenSessionManager(
            sendMessage = { payload -> broadcastToLinux(payload, "screen session") },
        )
        clipboardMonitor = ClipboardMonitor(clipboardManager!!) { text ->
            if (state.value.featureFlags.clipboard) {
                clipboardSyncManager?.syncToLinux(text)
            }
        }
        clipboardMonitor?.start()
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        createChannel()
        startForeground(NOTIFICATION_ID, foregroundNotification())
        startWebSocketServer()
        registerMdnsService()
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS)
        Log.i(TAG, "MainService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SEND_FILE) {
            val uri = intent.getParcelableExtra<Uri>(EXTRA_FILE_URI)
            if (uri != null && state.value.featureFlags.file) {
                fileTransferManager?.send(uri)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        clipboardMonitor?.stop()
        unregisterMdnsService()
        webSocketServer?.stopServer()
        webSocketServer = null
        instance = null
        state.value = state.value.copy(connectionStatus = "未连接", deviceIp = "auto-discovery")
        super.onDestroy()
        Log.i(TAG, "MainService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startWebSocketServer() {
        val deviceName = Build.MODEL ?: "Android"
        runCatching {
            val server = WebSocketServer(
                port = SERVER_PORT,
                deviceName = deviceName,
                onClientConnected = { remote ->
                    state.value = state.value.copy(connectionStatus = "已连接", deviceIp = remote)
                    refreshNotification()
                    Log.i(TAG, "linux daemon connected remote=$remote")
                },
                onClientDisconnected = { reason ->
                    state.value = state.value.copy(connectionStatus = if (webSocketServer == null) "未连接" else "等待重连")
                    refreshNotification()
                    Log.i(TAG, "linux daemon disconnected reason=$reason")
                },
                onMessageReceived = { message -> handleIncomingMessage(message) },
                onPeerHello = { peerName, _ ->
                    state.value = state.value.copy(deviceName = peerName.ifBlank { "Linux" })
                    refreshNotification()
                },
            )
            server.startServer()
            webSocketServer = server
            state.value = state.value.copy(connectionStatus = "等待连接")
        }.onFailure {
            state.value = state.value.copy(connectionStatus = "服务启动失败")
            Log.e(TAG, "websocket server failed", it)
        }
    }

    private fun handleIncomingMessage(message: JSONObject) {
        Log.i(TAG, "message=$message")
        when (message.optString("type")) {
            Protocol.TYPE_CLIPBOARD -> {
                val payload = message.optJSONObject("payload") ?: return
                val text = payload.optString("text")
                val hash = payload.optString("content_hash")
                if (text.isNotBlank() && hash.isNotBlank()) {
                    clipboardSyncManager?.applyFromLinux(text, hash)
                }
            }
            Protocol.TYPE_SMS_SEND -> {
                val payload = message.optJSONObject("payload") ?: return
                val address = payload.optString("address")
                val body = payload.optString("body")
                if (address.isBlank() || body.isBlank()) {
                    return
                }
                val result = SmsReplyExecutor.send(address, body)
                val ack = result.fold(
                    onSuccess = { Protocol.smsSent(address, true) },
                    onFailure = { Protocol.smsSent(address, false, it.message.orEmpty()) },
                )
                broadcastToLinux(ack.toString(), "sms.sent ack")
            }
            Protocol.TYPE_FILE_RECEIVED -> {
                state.value = state.value.copy(fileTransfer = FileTransferState())
            }
            Protocol.TYPE_FILE_ERROR -> {
                state.value = state.value.copy(fileTransfer = FileTransferState())
            }
            Protocol.TYPE_SCREEN_STARTED -> {
                val payload = message.optJSONObject("payload") ?: return
                val success = payload.optBoolean("success", false)
                val current = state.value.screen
                state.value = state.value.copy(screen = current.copy(enabled = success))
            }
        }
    }

    private fun registerMdnsService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "Monux-${Build.MODEL ?: "Android"}"
            serviceType = SERVICE_TYPE
            port = SERVER_PORT
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "mDNS registered name=${serviceInfo.serviceName} port=${serviceInfo.port}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                state.value = state.value.copy(connectionStatus = "mDNS 注册失败")
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
        val channel = NotificationChannel(CHANNEL_ID, "Monux", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    private fun foregroundNotification(): Notification {
        val arch = Build.SUPPORTED_ABIS.firstOrNull()?.substringBefore('-') ?: "arch"
        val text = if (state.value.connectionStatus == "已连接") {
            "Monux · $arch · 已连接"
        } else {
            "Monux · $arch · ${state.value.connectionStatus}"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(text)
            .setContentText(state.value.deviceName)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun refreshNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, foregroundNotification())
    }

    private fun broadcastToLinux(payload: String, context: String): Boolean {
        val server = webSocketServer ?: return false
        val delivered = server.broadcast(payload)
        if (delivered == 0) {
            if (state.value.connectionStatus == "已连接") {
                state.value = state.value.copy(connectionStatus = "等待重连")
                refreshNotification()
            }
            Log.w(TAG, "broadcast skipped context=$context no active linux client")
            return false
        }
        return true
    }

    fun mirrorNotification(appName: String, title: String, body: String) {
        if (!state.value.featureFlags.notifications) {
            return
        }
        broadcastToLinux(Protocol.notify(appName, title, body).toString(), "notification mirror")
    }

    companion object {
        const val ACTION_SEND_FILE = "com.monux.action.SEND_FILE"
        const val EXTRA_FILE_URI = "com.monux.extra.FILE_URI"

        private const val TAG = "MonuxMainService"
        private const val CHANNEL_ID = "monux_bridge"
        private const val NOTIFICATION_ID = 1001
        private const val SERVER_PORT = 39281
        private const val SERVICE_TYPE = "_monux._tcp."
        private const val HEARTBEAT_INTERVAL_MS = 15_000L

        private var instance: MainService? = null
        private val state = MutableStateFlow(ConnectionState(featureFlags = FeatureFlags()))

        fun stateFlow(): StateFlow<ConnectionState> = state.asStateFlow()

        fun updateFeatureFlags(flags: FeatureFlags) {
            val previous = state.value.featureFlags
            state.value = state.value.copy(featureFlags = flags)
            val service = instance ?: return
            if (previous.screen != flags.screen) {
                TileService.requestListeningState(service, android.content.ComponentName(service, com.monux.screen.ScreenTileService::class.java))
            }
            if (previous.remoteInput != flags.remoteInput) {
                TileService.requestListeningState(service, android.content.ComponentName(service, com.monux.input.InputTileService::class.java))
            }
        }

        fun updateNotificationAccess(granted: Boolean) {
            state.value = state.value.copy(notificationAccessGranted = granted)
            instance?.refreshNotification()
        }

        fun forwardNotification(payload: String) {
            val service = instance ?: return
            if (!state.value.featureFlags.notifications) {
                return
            }
            service.broadcastToLinux(payload, "notification forward")
        }

        fun forwardSms(sender: String, body: String) {
            val service = instance ?: return
            if (!state.value.featureFlags.sms) {
                return
            }
            service.broadcastToLinux(Protocol.sms(sender, body).toString(), "sms forward")
        }

        fun toggleScreenMirror() {
            val service = instance ?: return
            if (!state.value.featureFlags.screen) {
                return
            }
            val current = state.value.screen
            if (current.enabled) {
                service.screenSessionManager?.stop()
                state.value = state.value.copy(screen = current.copy(enabled = false))
            } else {
                service.screenSessionManager?.start(current.maxSize, current.bitrate)
            }
        }

        fun screenEnabled(): Boolean = state.value.screen.enabled

        fun remoteInputEnabled(): Boolean = state.value.featureFlags.remoteInput

        fun sendRemoteInputText(text: String, fromVoice: Boolean = false): Boolean {
            val service = instance ?: return false
            if (!state.value.featureFlags.remoteInput || text.isBlank() || service.webSocketServer?.hasConnections() != true) {
                return false
            }
            val payload = if (fromVoice) Protocol.inputVoice(text) else Protocol.inputText(text)
            return service.broadcastToLinux(payload.toString(), "remote input text")
        }

        fun sendRemoteInputKey(key: String): Boolean {
            val service = instance ?: return false
            if (!state.value.featureFlags.remoteInput || key.isBlank() || service.webSocketServer?.hasConnections() != true) {
                return false
            }
            return service.broadcastToLinux(Protocol.inputKey(key).toString(), "remote input key")
        }

        fun updateScreenConfig(maxSize: Int, bitrate: String) {
            state.value = state.value.copy(screen = state.value.screen.copy(maxSize = maxSize, bitrate = bitrate))
        }

        fun updateUiPreferences(preferences: UiPreferences) {
            state.value = state.value.copy(uiPreferences = preferences)
        }
    }
}
