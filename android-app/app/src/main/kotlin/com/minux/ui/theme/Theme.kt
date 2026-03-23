package com.minux.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF6900),
    secondary = Color(0xFFFF8A3D),
    tertiary = Color(0xFF5C5F62),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A3D),
    secondary = Color(0xFFFFB07A),
    tertiary = Color(0xFFC7C7C7),
)

@Composable
fun MinuxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
