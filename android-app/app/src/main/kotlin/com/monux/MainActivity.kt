package com.monux

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
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

private enum class HighlightCardTone {
    Brand,
    File,
    Settings,
    Neutral,
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
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        )
    )
    val currentTab = rememberMainDestination()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(30.dp)),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 0.dp,
            ) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                Color.Transparent,
                            ),
                            radius = 1200f,
                        )
                    ),
            )
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
    val enabledCount = listOf(
        state.featureFlags.notifications,
        state.featureFlags.clipboard,
        state.featureFlags.sms,
        state.featureFlags.file,
        state.featureFlags.screen,
    ).count { it }
    val connected = state.connectionStatus == "已连接"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp + padding.calculateTopPadding(),
            bottom = 112.dp + padding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            PageHeader(
                title = "Monux",
                subtitle = if (connected) "旗舰级 Android ↔ Linux 桥接体验已就绪" else "等待发现附近设备并建立高质量连接",
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OverviewCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Sync,
                    title = "已启用",
                    value = "$enabledCount / 5",
                    subtitle = "核心能力常驻",
                )
                OverviewCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.PhoneAndroid,
                    title = "连接状态",
                    value = if (connected) "在线" else "搜索中",
                    subtitle = state.deviceName,
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
        item {
            SectionHeader(
                title = "高频能力",
                subtitle = "统一管理通知、短信、文件、剪贴板与投屏",
            )
        }
        if (state.fileTransfer.active) {
            item {
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
        item {
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp + padding.calculateTopPadding(),
            bottom = 112.dp + padding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            PageHeader(
                title = "文件",
                subtitle = "像系统级接力一样查看当前传输与跨端落盘状态",
            )
        }
        item {
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
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OverviewCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Home,
                    title = "目标设备",
                    value = state.deviceName,
                    subtitle = state.deviceIp,
                )
                OverviewCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Sync,
                    title = "传输状态",
                    value = if (state.fileTransfer.active) "忙碌" else "空闲",
                    subtitle = if (state.connectionStatus == "已连接") "连接稳定" else "等待连接",
                )
            }
        }
        item {
            HighlightCard(
                overline = "视觉一致性",
                title = "投屏与传输一体化",
                subtitle = "延续 Reply 风格卡片层级，统一文件 / 屏幕 / 连接反馈",
                icon = Icons.Rounded.Description,
                badgeText = "高质感",
                tone = HighlightCardTone.Neutral,
            )
        }
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            title = "设置",
            subtitle = "动态配色、主色偏好与连接策略集中在一页完成",
        )
        HighlightCard(
            overline = "外观速览",
            title = if (state.uiPreferences.useDynamicColor) "Dynamic Color 已启用" else "正在使用自定义主色",
            subtitle = if (state.uiPreferences.useDynamicColor) "跟随系统壁纸实时取色，保持整机一致性" else "当前主色可在下方卡片中继续微调",
            icon = Icons.Rounded.Settings,
            badgeText = if (state.uiPreferences.useDynamicColor) "Material You" else "自定义",
            tone = HighlightCardTone.Settings,
        )
        SettingsScreen(
            preferences = state.uiPreferences,
            onPreferencesChange = MainService::updateUiPreferences,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PageHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OverviewCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HighlightCard(
    overline: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String? = null,
    tone: HighlightCardTone = HighlightCardTone.Brand,
    onClick: (() -> Unit)? = null,
) {
    val colors = when (tone) {
        HighlightCardTone.Brand -> listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface,
        )
        HighlightCardTone.File -> listOf(
            Color(0xFF5AA9FF).copy(alpha = 0.18f),
            Color(0xFF9AD7FF).copy(alpha = 0.12f),
            MaterialTheme.colorScheme.surface,
        )
        HighlightCardTone.Settings -> listOf(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface,
        )
        HighlightCardTone.Neutral -> listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            MaterialTheme.colorScheme.surface,
        )
    }
    val accentColor = when (tone) {
        HighlightCardTone.Brand -> MaterialTheme.colorScheme.primary
        HighlightCardTone.File -> Color(0xFF317AF7)
        HighlightCardTone.Settings -> MaterialTheme.colorScheme.tertiary
        HighlightCardTone.Neutral -> MaterialTheme.colorScheme.onSurface
    }
    val badgeColor = when (tone) {
        HighlightCardTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(colors))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = overline,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(badgeColor)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        targetValue = if (feature.enabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "featureCardColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (feature.enabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "featureCardBorder",
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (feature.enabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "featureIconContainer",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = containerColor,
        tonalElevation = if (feature.enabled) 8.dp else 4.dp,
        shadowElevation = if (feature.enabled) 14.dp else 10.dp,
        border = BorderStroke(1.dp, borderColor),
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
                        .size(54.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(feature.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(feature.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedVisibility(feature.enabled) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                        ) {
                            Text(
                                "已启用",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
            Switch(checked = feature.enabled, onCheckedChange = onToggle)
        }
    }
}
