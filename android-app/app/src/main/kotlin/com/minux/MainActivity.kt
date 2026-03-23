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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.minux.ui.theme.MinuxTheme

data class FeatureToggle(val name: String, val enabled: Boolean)

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
    val toggles = remember {
        mutableStateListOf(
            FeatureToggle("通知镜像", true),
            FeatureToggle("剪贴板", true),
            FeatureToggle("短信", false),
            FeatureToggle("文件快传", false),
            FeatureToggle("投屏", false),
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            StatusCard(
                linuxName = "arch",
                linuxIp = "auto-discovery",
                status = "等待连接",
            )
        }
        items(toggles.size) { index ->
            val item = toggles[index]
            FeatureCard(item)
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
private fun FeatureCard(feature: FeatureToggle) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(feature.name, style = MaterialTheme.typography.titleMedium)
                Text(if (feature.enabled) "已启用" else "未启用")
            }
            Switch(checked = feature.enabled, onCheckedChange = {})
        }
    }
}
