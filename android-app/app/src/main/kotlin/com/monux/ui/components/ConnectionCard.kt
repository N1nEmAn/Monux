package com.monux.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionCard(
    deviceName: String,
    deviceIp: String,
    status: String,
    modifier: Modifier = Modifier,
) {
    val connected = status == "已连接"
    val pulse = rememberInfiniteTransition(label = "connectionPulse").animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val shimmer = rememberInfiniteTransition(label = "connectionShimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerShift",
    )
    val connectedBrush = Brush.linearGradient(
        listOf(
            Color(0xFF0E8F63),
            Color(0xFF3DBB82),
            Color(0xFF79E5B1),
        )
    )
    val disconnectedBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(900f * shimmer.value, 500f),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .background(if (connected) connectedBrush else disconnectedBrush)
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "设备连接状态",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (connected) Color.White.copy(alpha = 0.84f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (connected) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = deviceIp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (connected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StatusPill(
                        label = if (connected) "已连接 · 低延迟稳定传输" else "发现中 · 等待握手",
                        active = connected,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (connected) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp * pulse.value)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f)),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (connected) Color.White else MaterialTheme.colorScheme.outline),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (connected) "链路稳定，可直接同步通知 / 文件 / 剪贴板" else "正在搜索可用设备…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (connected) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                        AnimatedVisibility(!connected) {
                            ShimmerLine()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    active: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (active) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShimmerLine() {
    val progress by rememberInfiniteTransition(label = "shimmerLine").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lineProgress",
    )
    val shimmerColors = listOf(
        Color.Transparent,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
        Color.Transparent,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .alpha(0.85f),
    ) {
        drawLine(
            brush = Brush.horizontalGradient(
                colors = shimmerColors,
                startX = size.width * (progress - 1f),
                endX = size.width * progress,
            ),
            start = androidx.compose.ui.geometry.Offset.Zero.copy(y = size.height / 2),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
        )
    }
}
