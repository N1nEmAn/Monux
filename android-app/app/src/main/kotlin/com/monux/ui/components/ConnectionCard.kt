package com.monux.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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

    val containerBrush = if (connected) {
        Brush.linearGradient(listOf(Color(0xFF0E8F63), Color(0xFF47C98E), Color(0xFF79E5B1)))
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            ),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(900f * shimmer.value, 500f),
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .background(containerBrush)
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "设备连接状态",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (connected) Color.White.copy(alpha = 0.86f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (connected) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = deviceIp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (connected) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Badge(
                        containerColor = if (connected) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceBright,
                        contentColor = if (connected) Color.White else MaterialTheme.colorScheme.onSurface,
                    ) {
                        Text(if (connected) "已连接" else "未连接")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (connected) {
                            Box(
                                modifier = Modifier
                                    .size((42.dp * pulse.value))
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
                    Column {
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
