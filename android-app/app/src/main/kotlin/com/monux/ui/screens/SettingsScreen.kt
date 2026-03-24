package com.monux.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SettingsSection(
                title = "连接设置",
                subtitle = "保证后台链路稳定，尽量减少重连打断",
            ) {
                PreferenceRow(
                    title = "自动发现设备",
                    subtitle = "通过 mDNS 搜索 Linux daemon，靠近系统级体验",
                    trailing = { SettingValueChip("已启用") },
                )
                PreferenceDivider()
                PreferenceRow(
                    title = "连接策略",
                    subtitle = "前台服务常驻，断线后自动重连并恢复状态",
                    trailing = { SettingValueChip("智能重连") },
                )
            }
        }
        item {
            SettingsSection(
                title = "外观",
                subtitle = "Dynamic Color 与自定义主色自由切换",
            ) {
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
                PreferenceDivider()
                ColorPreferenceRow(
                    selected = preferences.customSeedColor,
                    dynamicColorEnabled = preferences.useDynamicColor,
                    onSelect = {
                        onPreferencesChange(preferences.copy(useDynamicColor = false, customSeedColor = it))
                    },
                    onCustom = { showColorPicker = true },
                )
            }
        }
        item {
            SettingsSection(
                title = "关于",
                subtitle = "当前版本的视觉语言与体验目标",
            ) {
                PreferenceRow(
                    title = "设计语言",
                    subtitle = "Material 3 / Reply 风格卡片 / 超椭圆图标",
                    trailing = { SettingValueChip("v0.1.3") },
                )
                PreferenceDivider()
                PreferenceRow(
                    title = "体验目标",
                    subtitle = "接近 iOS / HyperOS 顶级层级与动效",
                    trailing = { SettingValueChip("旗舰级") },
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
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                            )
                        )
                    )
                    .padding(vertical = 6.dp),
            ) {
                content()
            }
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun PreferenceDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    )
}

@Composable
private fun SettingValueChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ColorPreferenceRow(
    selected: Long,
    dynamicColorEnabled: Boolean,
    onSelect: (Long) -> Unit,
    onCustom: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("主题主色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            if (dynamicColorEnabled) "当前跟随系统取色，选择下方色板后会切换为自定义模式" else "已切换为手动主色，可继续微调品牌氛围",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(presetColors) { color ->
                val selectedColor = selected == color && !dynamicColorEnabled
                PaletteCell(
                    color = Color(color),
                    selected = selectedColor,
                    onClick = { onSelect(color) },
                )
            }
            item {
                CustomPaletteCell(onClick = onCustom)
            }
        }
    }
}

@Composable
private fun PaletteCell(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        shadowElevation = if (selected) 8.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 72.dp)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CustomPaletteCell(
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .size(width = 72.dp, height = 72.dp)
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "自定义",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
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
