package com.dominiqueherbrigpersonalteam.lademonitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Blue-forward palette echoing the iOS app's accent (systemBlue).
val Blue = Color(0xFF0A84FF)
val Green = Color(0xFF34C759)
val Orange = Color(0xFFFF9500)
val Teal = Color(0xFF30B0C7)

private val LightColors = lightColorScheme(
    primary = Blue,
    secondary = Teal,
    tertiary = Green,
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9E9EF)
)

private val DarkColors = darkColorScheme(
    primary = Blue,
    secondary = Teal,
    tertiary = Green,
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E)
)

@Composable
fun LademonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
