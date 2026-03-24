package com.monux.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monux.ui.components.HighlightCard
import com.monux.ui.components.HighlightCardTone
import com.monux.ui.components.OverviewCard
import com.monux.ui.state.ConnectionState

@Composable
fun FilesScreen(
    state: ConnectionState,
    padding: PaddingValues,
) {
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
            HighlightCard(
                overline = "跨端传输",
                title = if (state.fileTransfer.active) state.fileTransfer.fileName else "等待分享文件",
                subtitle = if (state.fileTransfer.active) {
                    "正在同步到 Linux · ${(state.fileTransfer.progress * 100).toInt()}%"
                } else {
                    "通过系统分享面板发送到 Linux daemon"
                },
                icon = Icons.Rounded.Folder,
                badgeText = if (state.fileTransfer.active) "同步中" else "Share Sheet",
                tone = HighlightCardTone.File,
            )
        }
        item {
            OverviewCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Home,
                title = "目标设备",
                value = if (state.connectionStatus == "已连接") "已连接" else "等待",
                subtitle = state.deviceName,
            )
        }
        item {
            OverviewCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Sync,
                title = "传输状态",
                value = if (state.fileTransfer.active) "忙碌" else "空闲",
                subtitle = state.deviceIp,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            HighlightCard(
                overline = "视觉一致性",
                title = "投屏与传输一体化",
                subtitle = "延续系统级卡片层次，统一文件 / 屏幕 / 连接反馈",
                icon = Icons.Rounded.Description,
                badgeText = "高质感",
                tone = HighlightCardTone.Neutral,
            )
        }
    }
}
