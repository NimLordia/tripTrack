package com.triptrack.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = lightColorScheme(
    primary = Color(0xFF146C49),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F1DF),
    onPrimaryContainer = Color(0xFF143E2C),
    secondaryContainer = Color(0xFFE4E9E3),
    background = Color(0xFFF6F8F4),
    surface = Color(0xFFF6F8F4),
    surfaceVariant = Color(0xFFE8EDE5),
    onSurface = Color(0xFF18231D),
)

@Composable
fun TripTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
