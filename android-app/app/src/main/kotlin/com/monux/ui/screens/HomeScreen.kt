package com.monux.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
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
        columns = GridCells.Fixed(2),
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
            ConnectionCard(
                deviceName = state.deviceName,
                deviceIp = state.deviceIp,
                status = state.connectionStatus,
            )
        }
        item {
            OverviewCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Sync,
                title = "已启用",
                value = "$enabledCount/5",
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
                subtitle = "像系统设置一样快速访问通知、剪贴板、短信、文件与投屏",
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
                        else -> current.copy(screen = enabled)
                    }
                )
            }
        }
        if (state.fileTransfer.active) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HighlightCard(
                    overline = "实时传输",
                    title = "文件传输中",
                    subtitle = "${state.fileTransfer.fileName} · ${(state.fileTransfer.progress * 100).toInt()}%",
                    icon = Icons.Rounded.Description,
                    badgeText = "进行中",
                    tone = HighlightCardTone.File,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            HighlightCard(
                overline = "屏幕桥接",
                title = if (state.screen.enabled) "投屏已启动" else "投屏待命",
                subtitle = "分辨率上限 ${state.screen.maxSize} · 码率 ${state.screen.bitrate}",
                icon = Icons.Rounded.Wallpaper,
                badgeText = if (state.screen.enabled) "scrcpy" else "可启动",
                tone = HighlightCardTone.Brand,
                onClick = {
                    if (!state.screen.enabled) {
                        MainService.toggleScreenMirror()
                    }
                },
            )
        }
    }
}
