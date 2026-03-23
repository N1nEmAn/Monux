package com.monux.ui.state

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

data class UiPreferences(
    val useDynamicColor: Boolean = true,
    val customSeedColor: Long = 0xFFFF7A1A,
)

data class ConnectionState(
    val deviceName: String = "未发现",
    val deviceIp: String = "auto-discovery",
    val connectionStatus: String = "等待连接",
    val notificationAccessGranted: Boolean = false,
    val featureFlags: FeatureFlags = FeatureFlags(),
    val fileTransfer: FileTransferState = FileTransferState(),
    val screen: ScreenState = ScreenState(),
    val uiPreferences: UiPreferences = UiPreferences(),
)

