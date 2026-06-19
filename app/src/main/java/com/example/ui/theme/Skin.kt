package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class GHSkin(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val accentRed: Color = Color(0xFFEF2B2D), // Ghana Red
    val accentGold: Color = Color(0xFFFCD116), // Ghana Gold
    val accentGreen: Color = Color(0xFF006B3F) // Ghana Green
)

val SkinsList = listOf(
    GHSkin(
        name = "Ghana Pride",
        primary = Color(0xFFFCD116), // Gold
        secondary = Color(0xFF006B3F), // Green
        background = Color(0xFF0C0D0C), // Dark Charcoal/Black
        surface = Color(0xFF161A16), // Deep Forest Green Tint
        onBackground = Color(0xFFFFFFF2), // Ivory/light
        onSurface = Color(0xFFE2EBE2)
    ),
    GHSkin(
        name = "Kente Gold",
        primary = Color(0xFFFCD116), // Gold
        secondary = Color(0xFFE65100), // Vibrant Orange
        background = Color(0xFF151009), // Warm Dark Chocolate
        surface = Color(0xFF2B2013), // Deep Brown
        onBackground = Color(0xFFFFF3E0),
        onSurface = Color(0xFFFFE0B2)
    ),
    GHSkin(
        name = "Forest Green",
        primary = Color(0xFF006B3F), // Green
        secondary = Color(0xFFFCD116), // Gold
        background = Color(0xFF080F0B), // Very dark green-black
        surface = Color(0xFF132018), // Deep Forest Canopy
        onBackground = Color(0xFFE8F5E9),
        onSurface = Color(0xFFC8E6C9)
    ),
    GHSkin(
        name = "Cape Coast Crimson",
        primary = Color(0xFFEF2B2D), // Red
        secondary = Color(0xFF006B3F), // Green
        background = Color(0xFF120B0B), // Deep Reddish Black
        surface = Color(0xFF241515), // Muted Maroon
        onBackground = Color(0xFFFFECEC),
        onSurface = Color(0xFFFFD1D1)
    ),
    GHSkin(
        name = "Black Star AMOLED",
        primary = Color(0xFFFFFFFF), // White
        secondary = Color(0xFFFCD116), // Gold Accent
        background = Color(0xFF000000), // Pure Black
        surface = Color(0xFF121212), // Dark Gray
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFE0E0E0)
    ),
    GHSkin(
        name = "NDC Green",
        primary = Color(0xFF008000),                
        secondary = Color(0xFFFF0000),              
        background = Color(0xFF101010),
        surface = Color(0xFF202020),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFCCCCCC)
    ),
    GHSkin(
        name = "NPP Red",
        primary = Color(0xFFFF0000),
        secondary = Color(0xFF0000FF),
        background = Color(0xFF101010),
        surface = Color(0xFF202020),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFCCCCCC)
    ),
    GHSkin(
        name = "CPP Red",
        primary = Color(0xFFFF0000),
        secondary = Color(0xFF008000),
        background = Color(0xFFF0F0F0),
        surface = Color(0xFFE0E0E0),
        onBackground = Color(0xFF000000),
        onSurface = Color(0xFF333333)
    ),
    GHSkin(
        name = "Kotoko Red",
        primary = Color(0xFFD32F2F),
        secondary = Color(0xFF388E3C),
        background = Color(0xFF1B1B1B),
        surface = Color(0xFF2C2C2C),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFDDDDDD)
    ),
    GHSkin(
        name = "Hearts Rainbow",
        primary = Color(0xFFD32F2F),
        secondary = Color(0xFF1976D2),
        background = Color(0xFF1B1B1B),
        surface = Color(0xFF2C2C2C),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFDDDDDD)
    )
)
