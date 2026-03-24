package com.monux.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.monux.ui.state.UiPreferences

private val FallbackLightColors = lightColorScheme(
    primary = Color(0xFFFF7A1A),
    secondary = Color(0xFFFFA35C),
    tertiary = Color(0xFF6E5B4A),
    surfaceTint = Color(0xFFFF7A1A),
)

private val FallbackDarkColors = darkColorScheme(
    primary = Color(0xFFFFB37D),
    secondary = Color(0xFFFFCBA6),
    tertiary = Color(0xFFE2B28B),
    surfaceTint = Color(0xFFFFB37D),
)

@Immutable
data class MonuxSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val pageHorizontal: Dp = 20.dp,
    val pageTop: Dp = 12.dp,
    val bottomBarInset: Dp = 104.dp,
)

@Immutable
data class MonuxRadii(
    val small: Dp = 16.dp,
    val medium: Dp = 22.dp,
    val large: Dp = 26.dp,
    val pill: Dp = 999.dp,
)

@Immutable
data class MonuxElevations(
    val low: Dp = 0.dp,
    val medium: Dp = 2.dp,
    val high: Dp = 4.dp,
)

private val LocalMonuxSpacing = staticCompositionLocalOf { MonuxSpacing() }
private val LocalMonuxRadii = staticCompositionLocalOf { MonuxRadii() }
private val LocalMonuxElevations = staticCompositionLocalOf { MonuxElevations() }

object MonuxUi {
    val spacing: MonuxSpacing
        @Composable get() = LocalMonuxSpacing.current

    val radii: MonuxRadii
        @Composable get() = LocalMonuxRadii.current

    val elevations: MonuxElevations
        @Composable get() = LocalMonuxElevations.current
}

@Composable
fun MonuxTheme(
    uiPreferences: UiPreferences,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = when {
        uiPreferences.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val seed = Color(uiPreferences.customSeedColor)
            if (darkTheme) {
                FallbackDarkColors.copy(
                    primary = seed.lighten(0.24f),
                    secondary = seed.lighten(0.38f),
                    tertiary = seed.lighten(0.46f),
                    surfaceTint = seed.lighten(0.24f),
                )
            } else {
                FallbackLightColors.copy(
                    primary = seed,
                    secondary = seed.lighten(0.18f),
                    tertiary = seed.darken(0.42f),
                    surfaceTint = seed,
                )
            }
        }
    }

    CompositionLocalProvider(
        LocalMonuxSpacing provides MonuxSpacing(),
        LocalMonuxRadii provides MonuxRadii(),
        LocalMonuxElevations provides MonuxElevations(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

private fun Color.lighten(amount: Float): Color {
    val blended = ColorUtils.blendARGB(this.toArgb(), android.graphics.Color.WHITE, amount.coerceIn(0f, 1f))
    return Color(blended)
}

private fun Color.darken(amount: Float): Color {
    val blended = ColorUtils.blendARGB(this.toArgb(), android.graphics.Color.BLACK, amount.coerceIn(0f, 1f))
    return Color(blended)
}
