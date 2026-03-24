package com.monux.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monux.ui.theme.MonuxUi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectionCard(
    deviceName: String,
    deviceIp: String,
    status: String,
    modifier: Modifier = Modifier,
) {
    val connected = status == "已连接"
    val spacing = MonuxUi.spacing
    val radii = MonuxUi.radii
    val elevations = MonuxUi.elevations
    val containerColor = if (connected) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = if (connected) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.26f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
    }
    val statusLabel = if (connected) "低延迟在线" else "等待握手"
    val headline = if (connected) "已连接" else "发现中"
    val capability = if (connected) "通知 / 文件 / 剪贴板" else "等待附近设备"
    val accent = if (connected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val headlineColor = if (connected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
    val bodyColor = if (connected) {
        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.large),
        color = containerColor,
        tonalElevation = elevations.medium,
        shadowElevation = elevations.low,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(label = statusLabel, active = connected)
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = headlineColor,
                )
                Text(
                    text = "$deviceName · $deviceIp",
                    style = MaterialTheme.typography.bodyLarge,
                    color = bodyColor,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                SummaryTag(text = deviceName, active = connected)
                SummaryTag(text = deviceIp, active = connected)
                SummaryTag(text = capability, active = connected)
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    active: Boolean,
) {
    val radii = MonuxUi.radii

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.pill),
        color = if (active) {
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (active) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            },
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryTag(
    text: String,
    active: Boolean,
) {
    val radii = MonuxUi.radii

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.small),
        color = if (active) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (active) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
            },
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
