package com.mohamedabdelazeim.zekr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    secondary = DarkGreen,
    tertiary = LightGreen,
    background = Color.Black,
    surface = Color(0xFF1A1A1A),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Gold,
    secondary = DarkGreen,
    tertiary = LightGreen,
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// الألوان المخصصة
val Gold = Color(0xFFFFD700)
val DarkGreen = Color(0xFF1B2E1C)
val LightGreen = Color(0xFF2EAE30)
val GoldGradient = listOf(Color(0xFFFFD700), Color(0xFFFFBC00))
val GreenGradient = listOf(Color(0xFF2EAE30), Color(0xFF1B2E1C))

@Composable
fun SalaatiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
