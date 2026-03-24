package com.monux.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.monux.ui.theme.MonuxUi

data class FeatureToggle(
    val key: String,
    val name: String,
    val subtitle: String,
    val icon: ImageVector,
    val enabled: Boolean,
)

enum class HighlightCardTone {
    Brand,
    File,
    Settings,
    Neutral,
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
) {
    val spacing = MonuxUi.spacing

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
}

@Composable
fun OverviewCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
) {
    val spacing = MonuxUi.spacing
    val radii = MonuxUi.radii
    val elevations = MonuxUi.elevations

    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.medium),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = elevations.medium,
        shadowElevation = elevations.low,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.64f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun HighlightCard(
    overline: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String? = null,
    tone: HighlightCardTone = HighlightCardTone.Brand,
    onClick: (() -> Unit)? = null,
) {
    val spacing = MonuxUi.spacing
    val radii = MonuxUi.radii
    val elevations = MonuxUi.elevations
    val containerColor = when (tone) {
        HighlightCardTone.Brand -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
        HighlightCardTone.File -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        HighlightCardTone.Settings -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
        HighlightCardTone.Neutral -> MaterialTheme.colorScheme.surfaceContainer
    }
    val accentColor = when (tone) {
        HighlightCardTone.Brand -> MaterialTheme.colorScheme.primary
        HighlightCardTone.File -> MaterialTheme.colorScheme.secondary
        HighlightCardTone.Settings -> MaterialTheme.colorScheme.tertiary
        HighlightCardTone.Neutral -> MaterialTheme.colorScheme.onSurface
    }
    val badgeColor = if (tone == HighlightCardTone.Neutral) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.medium),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevations.low),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = overline,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                )
                if (badgeText != null) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.pill),
                        color = badgeColor,
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(radii.small))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor)
                }
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
            }
        }
    }
}

@Composable
fun FeatureToggleCard(
    modifier: Modifier = Modifier,
    feature: FeatureToggle,
    onToggle: (Boolean) -> Unit,
) {
    val spacing = MonuxUi.spacing
    val radii = MonuxUi.radii
    val containerColor by animateColorAsState(
        targetValue = if (feature.enabled) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.46f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "featureCardColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (feature.enabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "featureCardBorder",
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (feature.enabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "featureIconContainer",
    )

    Surface(
        modifier = modifier
            .height(168.dp)
            .clickable { onToggle(!feature.enabled) },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.medium),
        color = containerColor,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(radii.small))
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Switch(checked = feature.enabled, onCheckedChange = onToggle)
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = feature.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedVisibility(feature.enabled) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(radii.pill),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                ) {
                    Text(
                        text = "已启用",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
