package com.minux.ui.state

data class FeatureFlags(
    val notifications: Boolean = true,
    val clipboard: Boolean = true,
    val sms: Boolean = false,
    val file: Boolean = false,
    val screen: Boolean = false,
)

data class ConnectionState(
    val deviceName: String = "未发现",
    val deviceIp: String = "auto-discovery",
    val connectionStatus: String = "等待连接",
    val notificationAccessGranted: Boolean = false,
    val featureFlags: FeatureFlags = FeatureFlags(),
)
