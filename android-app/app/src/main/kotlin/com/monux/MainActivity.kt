package com.monux

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.monux.ui.screens.buildFeatureToggles
import com.monux.ui.screens.FilesScreen
import com.monux.ui.screens.HomeScreen
import com.monux.ui.screens.SettingsTab
import com.monux.ui.state.ConnectionState
import com.monux.ui.theme.MonuxTheme
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun MainScreen() {
    val state by MainService.stateFlow().collectAsState()
    val enabledCount = buildFeatureToggles(state).count { it.enabled }
    val currentTab = rememberMainDestination()
    val background = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainerLowest,
            MaterialTheme.colorScheme.surfaceContainerLow,
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            MainTopBar(
                destination = currentTab.value,
                state = state,
                enabledCount = enabledCount,
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                shadowElevation = 14.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
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
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
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
                .background(background),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                                Color.Transparent,
                            ),
                            radius = 1400f,
                        )
                    ),
            )
            AnimatedContent(
                targetState = currentTab.value,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    slideInHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> fullWidth / 4 * direction },
                    ) + fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> -fullWidth / 4 * direction },
                        ) + fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                },
                label = "main_tab_transition",
            ) { destination ->
                when (destination) {
                    MainDestination.Home -> HomeScreen(state = state, padding = padding)
                    MainDestination.Files -> FilesScreen(state = state, padding = padding)
                    MainDestination.Settings -> SettingsTab(state = state, padding = padding)
                }
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
            "系统级接力查看当前传输与跨端落盘状态"
        }
        MainDestination.Settings -> if (state.uiPreferences.useDynamicColor) {
            "Dynamic Color 已启用，整机配色自动同步"
        } else {
            "当前使用自定义主色，可继续微调视觉语言"
        }
    }

    LargeTopAppBar(
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
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
