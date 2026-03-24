package com.monux

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.monux.ui.screens.FilesScreen
import com.monux.ui.screens.HomeScreen
import com.monux.ui.screens.SettingsTab
import com.monux.ui.screens.buildFeatureToggles
import com.monux.ui.state.ConnectionState
import com.monux.ui.theme.MonuxTheme
import com.monux.ui.theme.MonuxUi
import java.time.LocalTime

private enum class MainDestination(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Home("主页", Icons.Rounded.Home),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
    val state by MainService.stateFlow().collectAsState()
    val enabledCount = buildFeatureToggles(state).count { it.enabled }
    val currentTab = rememberMainDestination()
    val spacing = MonuxUi.spacing
    val radii = MonuxUi.radii
    val elevations = MonuxUi.elevations

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            MainTopBar(
                destination = currentTab.value,
                state = state,
                enabledCount = enabledCount,
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.large),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = elevations.medium,
                shadowElevation = elevations.low,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                ),
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    MainDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentTab.value == destination,
                            onClick = { currentTab.value = destination },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            ),
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (currentTab.value) {
                MainDestination.Home -> HomeScreen(state = state, padding = padding)
                MainDestination.Files -> FilesScreen(state = state, padding = padding)
                MainDestination.Settings -> SettingsTab(state = state, padding = padding)
            }
        }
    }
}

@Composable
private fun rememberMainDestination() = remember {
    mutableStateOf(MainDestination.Home)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    destination: MainDestination,
    state: ConnectionState,
    enabledCount: Int,
) {
    val connected = state.connectionStatus == "已连接"
    val spacing = MonuxUi.spacing
    val title = when (destination) {
        MainDestination.Home -> greetingText()
        MainDestination.Files -> "文件中心"
        MainDestination.Settings -> "设置"
    }
    val subtitle = when (destination) {
        MainDestination.Home -> if (connected) {
            "${state.deviceName} 已连接 · $enabledCount 项能力在线"
        } else {
            "正在发现设备 · $enabledCount 项能力已就绪"
        }
        MainDestination.Files -> if (state.fileTransfer.active) {
            "${state.fileTransfer.fileName} 正在同步到 Linux"
        } else {
            "查看当前传输状态与目标设备"
        }
        MainDestination.Settings -> if (state.uiPreferences.useDynamicColor) {
            "Dynamic Color 已启用"
        } else {
            "当前使用自定义主色"
        }
    }

    TopAppBar(
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

private fun greetingText(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 6 -> "凌晨好"
        hour < 12 -> "早上好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }
}
