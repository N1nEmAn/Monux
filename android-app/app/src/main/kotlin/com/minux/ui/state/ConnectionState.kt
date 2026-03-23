package com.minux.ui.state

data class FeatureFlags(
    val notifications: Boolean = true,
    val clipboard: Boolean = true,
    val sms: Boolean = false,
    val file: Boolean = false,
    val screen: Boolean = false,
)

data class FileTransferState(
    val fileName: String = "",
    val progress: Float = 0f,
    val active: Boolean = false,
)

data class ScreenState(
    val enabled: Boolean = false,
    val maxSize: Int = 1600,
    val bitrate: String = "8M",
)

data class ConnectionState(
    val deviceName: String = "未发现",
    val deviceIp: String = "auto-discovery",
    val connectionStatus: String = "等待连接",
    val notificationAccessGranted: Boolean = false,
    val featureFlags: FeatureFlags = FeatureFlags(),
    val fileTransfer: FileTransferState = FileTransferState(),
    val screen: ScreenState = ScreenState(),
)

fun FeatureFlags.copy(
    notifications: Boolean = this.notifications,
    clipboard: Boolean = this.clipboard,
    sms: Boolean = this.sms,
    file: Boolean = this.file,
    screen: Boolean = this.screen,
): FeatureFlags = FeatureFlags(notifications, clipboard, sms, file, screen)
