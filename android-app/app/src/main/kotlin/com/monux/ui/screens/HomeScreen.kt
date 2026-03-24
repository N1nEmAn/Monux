package com.monux.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monux.MainService
import com.monux.ui.components.ConnectionCard
import com.monux.ui.components.FeatureToggle
import com.monux.ui.components.FeatureToggleCard
import com.monux.ui.components.HighlightCard
import com.monux.ui.components.HighlightCardTone
import com.monux.ui.components.OverviewCard
import com.monux.ui.components.SectionHeader
import com.monux.ui.state.ConnectionState

fun buildFeatureToggles(state: ConnectionState): List<FeatureToggle> = listOf(
    FeatureToggle(
        key = "notifications",
        name = "通知镜像",
        subtitle = if (state.notificationAccessGranted) "桌面提醒实时同步" else "需授予通知访问权限",
        icon = Icons.Rounded.Notifications,
        enabled = state.featureFlags.notifications,
    ),
    FeatureToggle(
        key = "clipboard",
        name = "剪贴板同步",
        subtitle = "跨端复制粘贴，秒级接力",
        icon = Icons.Rounded.Sync,
        enabled = state.featureFlags.clipboard,
    ),
    FeatureToggle(
        key = "sms",
        name = "短信镜像",
        subtitle = "接收通知并支持桌面回复",
        icon = Icons.Rounded.PhoneAndroid,
        enabled = state.featureFlags.sms,
    ),
    FeatureToggle(
        key = "file",
        name = "文件快传",
        subtitle = if (state.fileTransfer.active) "${state.fileTransfer.fileName} 传输中" else "系统分享直达 Linux",
        icon = Icons.Rounded.Folder,
        enabled = state.featureFlags.file,
    ),
    FeatureToggle(
        key = "screen",
        name = "投屏控制",
        subtitle = if (state.screen.enabled) "scrcpy 已启动" else "Quick Settings / UI 控制",
        icon = Icons.Rounded.Wallpaper,
        enabled = state.featureFlags.screen,
    ),
    FeatureToggle(
        key = "remoteInput",
        name = "远程输入",
        subtitle = "文本 / 语音输入到 Linux 焦点窗口",
        icon = Icons.Rounded.Edit,
        enabled = state.featureFlags.remoteInput,
    ),
)

@Composable
fun HomeScreen(
    state: ConnectionState,
    padding: PaddingValues,
) {
    val toggles = buildFeatureToggles(state)
    val enabledCount = toggles.count { it.enabled }
    val connected = state.connectionStatus == "已连接"

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 10.dp + padding.calculateTopPadding(),
            bottom = 108.dp + padding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "连接概览",
                subtitle = "先看连接状态与跨端关键提示",
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            HighlightCard(
                overline = if (state.fileTransfer.active) "实时传输" else "屏幕桥接",
                title = if (state.fileTransfer.active) "${state.fileTransfer.fileName} 传输中" else if (state.screen.enabled) "投屏已启动" else "投屏待命",
                subtitle = if (state.fileTransfer.active) {
                    "同步到 Linux · ${(state.fileTransfer.progress * 100).toInt()}%"
                } else {
                    "分辨率上限 ${state.screen.maxSize} · 码率 ${state.screen.bitrate}"
                },
                icon = if (state.fileTransfer.active) Icons.Rounded.Description else Icons.Rounded.Wallpaper,
                badgeText = if (state.fileTransfer.active) "进行中" else if (state.screen.enabled) "scrcpy" else "可启动",
                tone = if (state.fileTransfer.active) HighlightCardTone.File else HighlightCardTone.Brand,
                onClick = if (!state.fileTransfer.active && !state.screen.enabled) {
                    { MainService.toggleScreenMirror() }
                } else {
                    null
                },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ConnectionCard(
                deviceName = state.deviceName,
                deviceIp = state.deviceIp,
                status = state.connectionStatus,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "重点状态",
                subtitle = "保留最关键的两张概览卡，减少首屏竞争注意力",
            )
        }
        item {
            OverviewCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Sync,
                title = "已启用",
                value = "$enabledCount/6",
                subtitle = "核心能力常驻",
            )
        }
        item {
            OverviewCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.PhoneAndroid,
                title = "链路状态",
                value = if (connected) "在线" else "搜索",
                subtitle = state.deviceName,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "能力矩阵",
                subtitle = "像系统设置一样快速访问通知、剪贴板、短信、文件、投屏与输入",
            )
        }
        items(toggles, key = { it.key }) { feature ->
            FeatureToggleCard(
                modifier = Modifier.fillMaxWidth(),
                feature = feature,
            ) { enabled ->
                val current = state.featureFlags
                MainService.updateFeatureFlags(
                    when (feature.key) {
                        "notifications" -> current.copy(notifications = enabled)
                        "clipboard" -> current.copy(clipboard = enabled)
                        "sms" -> current.copy(sms = enabled)
                        "file" -> current.copy(file = enabled)
                        "screen" -> current.copy(screen = enabled)
                        else -> current.copy(remoteInput = enabled)
                    }
                )
            }
        }
    }
}
