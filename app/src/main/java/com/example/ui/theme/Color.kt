package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Colors
val EmeraldPrimary = Color(0xFF00796B)
val EmeraldSecondary = Color(0xFF004D40)
val CrimsonCost = Color(0xFFD32F2F)
val GoldAccent = Color(0xFFFFB300)

val LightBackground = Color(0xFFF5F7F6)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFE0F2F1)

// Dark Colors
val DarkBackground = Color(0xFF0F1513)
val DarkSurface = Color(0xFF161F1C)
val DarkCard = Color(0xFF232E2A)

// Legacy / Fallback Status colors to prevent compilation errors in unchanged files
val SoftwarePurple = Color(0xFF9C27B0)
val GeneralBlue = Color(0xFF2196F3)
val AccessoryOrange = Color(0xFFFF9800)

// Glass surfaces
val GlassBg         = Color(0x40FFFFFF)  // 25% white
val GlassBgThin     = Color(0x2DFFFFFF)  // 18% white — for small cards
val GlassBorder     = Color(0xBFFFFFFF)  // top specular border
val GlassBorderBot  = Color(0x40FFFFFF)  // bottom depth border

// Text on glass (dark text — glass is light)
val TextPrimary     = Color(0xE61C1C1E)  // 90% black
val TextSecondary   = Color(0x8C1C1C1E)  // 55% black
val TextTertiary    = Color(0x521C1C1E)  // 32% black

// Semantic colors
val ProfitGreen     = Color(0xFF059669)
val ExpenseRed      = Color(0xFFE11D48)
val NeutralBlue     = Color(0xFF1D4ED8)

// Category accent colors (icon bg + amount text ONLY)
val AccentScreen    = Color(0xFF60A5FA)  // soft blue
val AccentParts     = Color(0xFFFB923C)  // warm orange
val AccentService   = Color(0xFFA78BFA)  // soft purple
val AccentAccessory = Color(0xFFF472B6)  // pink
val AccentExpense   = Color(0xFFF87171)  // soft red
val AccentRefurb    = Color(0xFF34D399)  // mint green
val AccentInventory = Color(0xFFD97706)  // amber
val AccentOther     = Color(0xFF6366F1)  // indigo
