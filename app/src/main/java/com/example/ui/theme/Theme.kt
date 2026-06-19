package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// =================== EMERALD COLOR PALETTE ===================
private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF00796B),
    secondary = Color(0xFF004D40),
    tertiary = Color(0xFFFFB300),
    background = Color(0xFFF5F7F6),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White
)
private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF10B981),
    secondary = Color(0xFF059669),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onPrimary = Color.White,
    onSecondary = Color.White
)

// =================== OCEAN BLUE COLOR PALETTE ===================
private val OceanLight = lightColorScheme(
    primary = Color(0xFF0288D1),
    secondary = Color(0xFF005691),
    tertiary = Color(0xFF26A69A),
    background = Color(0xFFF0F4F8),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White
)
private val OceanDark = darkColorScheme(
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFF0EA5E9),
    tertiary = Color(0xFF2DD4BF),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onPrimary = Color.Black,
    onSecondary = Color.White
)

// =================== ROYAL AMBER COLOR PALETTE ===================
private val AmberLight = lightColorScheme(
    primary = Color(0xFFE65100),
    secondary = Color(0xFFF57C00),
    tertiary = Color(0xFF00796B),
    background = Color(0xFFFFFDF9),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White
)
private val AmberDark = darkColorScheme(
    primary = Color(0xFFFFB300),
    secondary = Color(0xFFFFA000),
    tertiary = Color(0xFF4DB6AC),
    background = Color(0xFF1B120C),
    surface = Color(0xFF2A1C12),
    onPrimary = Color.Black,
    onSecondary = Color.Black
)

// =================== CREATIVE PURPLE COLOR PALETTE ===================
private val PurpleLight = lightColorScheme(
    primary = Color(0xFF7B1FA2),
    secondary = Color(0xFF4A148C),
    tertiary = Color(0xFFFF4081),
    background = Color(0xFFFAF5FF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White
)
private val PurpleDark = darkColorScheme(
    primary = Color(0xFFCE93D8),
    secondary = Color(0xFF8E24AA),
    tertiary = Color(0xFFF50057),
    background = Color(0xFF140D1E),
    surface = Color(0xFF221630),
    onPrimary = Color.Black,
    onSecondary = Color.White
)

// =================== SLATE STEEL COLOR PALETTE ===================
private val SlateLight = lightColorScheme(
    primary = Color(0xFF37474F),
    secondary = Color(0xFF21272A),
    tertiary = Color(0xFF78909C),
    background = Color(0xFFF2F4F5),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White
)
private val SlateDark = darkColorScheme(
    primary = Color(0xFF90A4AE),
    secondary = Color(0xFF455A64),
    tertiary = Color(0xFF37474F),
    background = Color(0xFF161A1D),
    surface = Color(0xFF22262B),
    onPrimary = Color.Black,
    onSecondary = Color.White
)

// =================== LIQUID GLASS COLOR PALETTE ===================
private val LiquidGlassLight = lightColorScheme(
    primary = Color(0xFF007AFF), // iOS Vivid Blue
    secondary = Color(0xFF5856D6), // iOS Purple
    tertiary = Color(0xFFFF2D55), // iOS Pink
    background = Color(0x15F2F2F7), // Highly transparent base
    surface = Color(0x33FFFFFF), // Elegant translucent Apple-style light frosted glass
    surfaceVariant = Color(0x22FFFFFF), // Very thin glass accent surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    outlineVariant = Color(0x55FFFFFF) // Distinct fine light specular grid edge
)
private val LiquidGlassDark = darkColorScheme(
    primary = Color(0xFF0A84FF), // iOS Vivid Blue
    secondary = Color(0xFF5E5CE6), // iOS Purple
    tertiary = Color(0xFFFF375F), // iOS Pink
    background = Color(0x10000000), // Highly transparent dark base
    surface = Color(0x3012121A), // Pure translucent premium dark obsidian glass
    surfaceVariant = Color(0x2012121A), // Subtle dark glass highlighting
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF2F2F7),
    onSurface = Color(0xFFF2F2F7),
    outlineVariant = Color(0x33FFFFFF) // Discrete fine glowing dark specular boundary
)

val LocalIsLiquidTheme = staticCompositionLocalOf { false }

@Composable
fun MyApplicationTheme(
    themeKey: String = "EMERALD",
    darkModeVal: String = "SYSTEM",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (darkModeVal) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when (themeKey) {
        "OCEAN" -> if (darkTheme) OceanDark else OceanLight
        "AMBER" -> if (darkTheme) AmberDark else AmberLight
        "PURPLE" -> if (darkTheme) PurpleDark else PurpleLight
        "SLATE" -> if (darkTheme) SlateDark else SlateLight
        "LIQUID_GLASS" -> if (darkTheme) LiquidGlassDark else LiquidGlassLight
        else -> if (darkTheme) EmeraldDark else EmeraldLight
    }

    CompositionLocalProvider(LocalIsLiquidTheme provides (themeKey == "LIQUID_GLASS")) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
