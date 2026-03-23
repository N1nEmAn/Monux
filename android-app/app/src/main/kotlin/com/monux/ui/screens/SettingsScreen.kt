package com.monux.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.monux.ui.state.UiPreferences

private val presetColors = listOf(
    0xFFFF7A1AL,
    0xFF4E7BFFL,
    0xFF32B7A6L,
    0xFFD95CE6L,
    0xFFFFC247L,
    0xFF67A853L,
)

@Composable
fun SettingsScreen(
    preferences: UiPreferences,
    onPreferencesChange: (UiPreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showColorPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SettingsSection(title = "连接设置") {
                PreferenceRow(
                    title = "自动发现设备",
                    subtitle = "通过 mDNS 搜索 Linux daemon",
                    trailing = { Text("已启用", color = MaterialTheme.colorScheme.primary) },
                )
                PreferenceRow(
                    title = "连接策略",
                    subtitle = "前台服务常驻，断线后自动重连",
                    trailing = { Text("智能重连") },
                )
            }
        }
        item {
            SettingsSection(title = "外观") {
                PreferenceRow(
                    title = "Dynamic Color",
                    subtitle = "跟随系统壁纸取色，兼容 Material You",
                    trailing = {
                        Switch(
                            checked = preferences.useDynamicColor,
                            onCheckedChange = { onPreferencesChange(preferences.copy(useDynamicColor = it)) },
                        )
                    },
                )
                ColorPreferenceRow(
                    selected = preferences.customSeedColor,
                    onSelect = {
                        onPreferencesChange(preferences.copy(useDynamicColor = false, customSeedColor = it))
                    },
                    onCustom = { showColorPicker = true },
                )
            }
        }
        item {
            SettingsSection(title = "关于") {
                PreferenceRow(
                    title = "设计语言",
                    subtitle = "Material 3 / Reply 风格卡片 / 超椭圆图标",
                    trailing = { Text("v0.1.1") },
                )
                PreferenceRow(
                    title = "体验目标",
                    subtitle = "接近 iOS / HyperOS 顶级层级与动效",
                    trailing = { Text("旗舰级") },
                )
            }
        }
    }

    if (showColorPicker) {
        CustomColorDialog(
            initialColor = preferences.customSeedColor,
            onDismiss = { showColorPicker = false },
            onConfirm = {
                showColorPicker = false
                onPreferencesChange(preferences.copy(useDynamicColor = false, customSeedColor = it))
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun ColorPreferenceRow(
    selected: Long,
    onSelect: (Long) -> Unit,
    onCustom: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("主题主色", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            presetColors.forEach { color ->
                val selectedColor = selected == color
                Box(
                    modifier = Modifier
                        .size(if (selectedColor) 38.dp else 34.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .clickable { onSelect(color) },
                )
            }
            Text(
                text = "自定义",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onCustom),
            )
        }
    }
}

@Composable
private fun CustomColorDialog(
    initialColor: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var selected by remember(initialColor) { mutableLongStateOf(initialColor) }
    val customChoices = listOf(0xFFEC5B5BL, 0xFF5C8DFFL, 0xFF15B392L, 0xFFAA6BFFL)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义颜色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                customChoices.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(option)),
                        )
                        Text("#${option.toString(16).uppercase().removePrefix("FF")}", modifier = Modifier.weight(1f))
                        RadioButton(selected = selected == option, onClick = { selected = option })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
