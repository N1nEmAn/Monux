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
import com.monux.ui.components.HighlightCard
import com.monux.ui.components.HighlightCardTone
import com.monux.ui.components.OverviewCard
import com.monux.ui.components.SectionHeader
import com.monux.ui.state.ConnectionState
import com.monux.ui.theme.MonuxUi

@Composable
fun FilesScreen(
    state: ConnectionState,
    padding: PaddingValues,
) {
    val spacing = MonuxUi.spacing

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 172.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.pageHorizontal,
            end = spacing.pageHorizontal,
            top = spacing.pageTop + padding.calculateTopPadding(),
            bottom = spacing.bottomBarInset + padding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
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
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "传输概览",
                subtitle = "优先展示目标设备与当前传输状态，保持手机端单列阅读节奏",
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
                overline = "使用方式",
                title = "从系统分享面板发到桌面",
                subtitle = "统一连接、传输与投屏反馈，不再堆叠额外装饰卡片",
                icon = Icons.Rounded.Description,
                badgeText = if (state.fileTransfer.active) "处理中" else "就绪",
                tone = HighlightCardTone.Neutral,
            )
        }
    }
}
