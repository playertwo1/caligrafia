package com.scribe.caligrafia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ScribePrimary,
    secondary = ScribeAccent,
    background = ScribeBackground,
    surface = ScribeSurface,
    onPrimary = ScribeSurface,
    onSecondary = ScribeSurface,
    onBackground = InkPrimary,
    onSurface = InkPrimary,
)

@Composable
fun ScribeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
