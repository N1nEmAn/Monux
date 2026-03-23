package com.minux

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.minux.ui.theme.MinuxTheme

data class FeatureToggle(val key: String, val name: String, val enabled: Boolean, val hint: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.startForegroundService(this, Intent(this, MainService::class.java))
        setContent {
            MinuxTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val state by MainService.stateFlow().collectAsState()
    val toggles = listOf(
        FeatureToggle("notifications", "通知镜像", state.featureFlags.notifications, if (state.notificationAccessGranted) "通知权限已就绪" else "需授予通知访问权限"),
        FeatureToggle("clipboard", "剪贴板", state.featureFlags.clipboard, "通信链路已接入"),
        FeatureToggle("sms", "短信", state.featureFlags.sms, "短信镜像/回复已接入"),
        FeatureToggle("file", "文件快传", state.featureFlags.file, "支持 Share Sheet 与进度显示"),
        FeatureToggle("screen", "投屏", state.featureFlags.screen, "Phase 6"),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            StatusCard(
                linuxName = state.deviceName,
                linuxIp = state.deviceIp,
                status = state.connectionStatus,
            )
        }
        if (state.fileTransfer.active) {
            item {
                TransferCard(state.fileTransfer.fileName, state.fileTransfer.progress)
            }
        }
        items(toggles) { item ->
            FeatureCard(item) { enabled ->
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
private fun StatusCard(linuxName: String, linuxIp: String, status: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Minux", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Linux 端：$linuxName")
            Text("地址：$linuxIp")
            Text("状态：$status", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FeatureCard(feature: FeatureToggle, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(feature.name, style = MaterialTheme.typography.titleMedium)
                Text(feature.hint)
                Text(if (feature.enabled) "已启用" else "未启用")
            }
            Switch(checked = feature.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun TransferCard(fileName: String, progress: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("文件传输", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(fileName)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Text("${(progress * 100).toInt()}%")
        }
    }
}
