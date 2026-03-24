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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monux.ui.state.UiPreferences
import com.monux.ui.theme.MonuxUi

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
    val spacing = MonuxUi.spacing
    var showColorPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
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
                    subtitle = "更克制的 Material 3 扁平与色阶层级",
                    trailing = { SettingValueChip("v0.1.4") },
                )
                PreferenceDivider()
                PreferenceRow(
                    title = "体验目标",
                    subtitle = "让连接、文件与设置像系统应用一样稳定清晰",
                    trailing = { SettingValueChip("手机优先") },
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
    val spacing = MonuxUi.spacing
    val radii = MonuxUi.radii
    val elevations = MonuxUi.elevations

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
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
            shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.medium),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = elevations.low),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
        ) {
            Column(
                modifier = Modifier.padding(vertical = spacing.xs),
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
    val spacing = MonuxUi.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.xl, vertical = spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun PreferenceDivider() {
    val spacing = MonuxUi.spacing

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.xl)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
    )
}

@Composable
private fun SettingValueChip(text: String) {
    val radii = MonuxUi.radii

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.pill),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
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
    val spacing = MonuxUi.spacing

    Column(
        modifier = Modifier.padding(start = spacing.xl, end = spacing.xl, top = spacing.lg, bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text("主题主色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            if (dynamicColorEnabled) "当前跟随系统取色，选择下方色板后会切换为自定义模式" else "已切换为手动主色，可继续微调品牌氛围",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
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
    val radii = MonuxUi.radii

    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.small),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        shadowElevation = if (selected) 4.dp else 0.dp,
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
    val radii = MonuxUi.radii

    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.small),
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
    val spacing = MonuxUi.spacing
    var selected by remember(initialColor) { mutableLongStateOf(initialColor) }
    val customChoices = listOf(0xFFEC5B5BL, 0xFF5C8DFFL, 0xFF15B392L, 0xFFAA6BFFL)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义颜色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                customChoices.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
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
