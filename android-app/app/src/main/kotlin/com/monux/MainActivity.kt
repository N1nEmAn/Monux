package com.monux

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.monux.ui.components.ConnectionCard
import com.monux.ui.screens.SettingsScreen
import com.monux.ui.theme.MonuxTheme

private data class FeatureToggle(
    val key: String,
    val name: String,
    val subtitle: String,
    val icon: ImageVector,
    val enabled: Boolean,
)

private enum class MainDestination(
    val label: String,
    val icon: ImageVector,
) {
    Home("Home", Icons.Rounded.Home),
    Files("文件", Icons.Rounded.Folder),
    Settings("设置", Icons.Rounded.Settings),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.startForegroundService(this, Intent(this, MainService::class.java))
        setContent {
            val state by MainService.stateFlow().collectAsState()
            MonuxTheme(uiPreferences = state.uiPreferences) {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val state by MainService.stateFlow().collectAsState()
    val toggles = listOf(
        FeatureToggle(
            key = "notifications",
            name = "通知镜像",
            subtitle = if (state.notificationAccessGranted) "通知权限已就绪，桌面提醒实时同步" else "需授予通知访问权限",
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
            subtitle = if (state.fileTransfer.active) "${state.fileTransfer.fileName} 正在传输" else "支持 Share Sheet 与进度显示",
            icon = Icons.Rounded.Folder,
            enabled = state.featureFlags.file,
        ),
        FeatureToggle(
            key = "screen",
            name = "投屏控制",
            subtitle = if (state.screen.enabled) "scrcpy 已启动，可继续调参" else "Quick Settings Tile / UI 控制",
            icon = Icons.Rounded.Wallpaper,
            enabled = state.featureFlags.screen,
        ),
    )

    val background = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainerLowest,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        )
    )
    var currentTab = rememberMainDestination()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
                MainDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentTab.value == destination,
                        onClick = { currentTab.value = destination },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background),
        ) {
            when (currentTab.value) {
                MainDestination.Home -> HomeScreen(state, toggles, padding)
                MainDestination.Files -> FilesScreen(state, padding)
                MainDestination.Settings -> SettingsTab(state, padding)
            }
        }
    }
}

@Composable
private fun rememberMainDestination() = androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(MainDestination.Home)
}

@Composable
private fun HomeScreen(
    state: com.monux.ui.state.ConnectionState,
    toggles: List<FeatureToggle>,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 18.dp + padding.calculateTopPadding(),
            bottom = 24.dp + padding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Monux",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "旗舰级 Android ↔ Linux 桥接体验",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            ConnectionCard(
                deviceName = state.deviceName,
                deviceIp = state.deviceIp,
                status = state.connectionStatus,
            )
        }
        if (state.fileTransfer.active) {
            item {
                HighlightCard(
                    title = "文件传输中",
                    subtitle = "${state.fileTransfer.fileName} · ${(state.fileTransfer.progress * 100).toInt()}%",
                    icon = Icons.Rounded.Description,
                )
            }
        }
        item {
            HighlightCard(
                title = if (state.screen.enabled) "投屏已启动" else "投屏待命",
                subtitle = "分辨率上限 ${state.screen.maxSize} · 码率 ${state.screen.bitrate}",
                icon = Icons.Rounded.Wallpaper,
                onClick = {
                    if (!state.screen.enabled) {
                        MainService.toggleScreenMirror()
                    }
                },
            )
        }
        items(toggles) { item ->
            FeatureToggleCard(item) { enabled ->
                val current = state.featureFlags
                MainService.updateFeatureFlags(
                    when (item.key) {
                        "notifications" -> current.copy(notifications = enabled)
                        "clipboard" -> current.copy(clipboard = enabled)
                        "sms" -> current.copy(sms = enabled)
                        "file" -> current.copy(file = enabled)
                        else -> current.copy(screen = enabled)
                    }
                )
            }
        }
    }
}

@Composable
private fun FilesScreen(
    state: com.monux.ui.state.ConnectionState,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("文件", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        HighlightCard(
            title = if (state.fileTransfer.active) state.fileTransfer.fileName else "等待分享文件",
            subtitle = if (state.fileTransfer.active) "正在同步到 Linux · ${(state.fileTransfer.progress * 100).toInt()}%" else "通过系统分享面板发送到 Linux daemon",
            icon = Icons.Rounded.Folder,
        )
        HighlightCard(
            title = "投屏与传输一体化",
            subtitle = "延续 Reply 风格卡片层级，统一文件 / 屏幕 / 连接反馈",
            icon = Icons.Rounded.Description,
        )
    }
}

@Composable
private fun SettingsTab(
    state: com.monux.ui.state.ConnectionState,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        SettingsScreen(
            preferences = state.uiPreferences,
            onPreferencesChange = MainService::updateUiPreferences,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun HighlightCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.surface,
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(colors))
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FeatureToggleCard(
    feature: FeatureToggle,
    onToggle: (Boolean) -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (feature.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "featureCardColor",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = containerColor,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(feature.name, style = MaterialTheme.typography.titleMedium)
                    Text(feature.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedVisibility(feature.enabled) {
                        Text("已启用", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Switch(checked = feature.enabled, onCheckedChange = onToggle)
        }
    }
}
