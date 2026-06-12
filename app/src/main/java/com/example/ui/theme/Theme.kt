package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun GHPlayerTheme(
    skin: GHSkin,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = skin.primary,
        secondary = skin.secondary,
        tertiary = skin.accentRed,
        background = skin.background,
        surface = skin.surface,
        onBackground = skin.onBackground,
        onSurface = skin.onSurface,
        onPrimary = skin.background, // text on primary (e.g. black text on yellow/gold button)
        onSecondary = skin.background
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

