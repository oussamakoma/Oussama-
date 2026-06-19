package com.example.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.SolidColor

/**
 * Modern Liquid Glass (Glassmorphism + iOS Frosted Specular styling) containers
 * and fluid interactive components for the 'Warshaty' app.
 */

@Composable
fun GlassmorphismContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.2.dp,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable ColumnScope.() -> Unit
) {
    // Beautifully translucent glass color base with proper alphas
    val glassColor = if (isDark) {
        Color(0x551E1E2E) // Dark slate frosted glass translucent backing
    } else {
        Color(0x66FFFFFF) // Ultra bright reflective pure frosted glass base
    }

    // Dynamic, high-fidelity specular borders mimicking live ambient reflections
    val specularGradient = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.04f),
                Color(0xFF007AFF).copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.08f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.65f),
                Color.White.copy(alpha = 0.10f),
                Color(0xFF007AFF).copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.30f)
            )
        }
    )

    Box(
        modifier = modifier
    ) {
        // LAYER 1: The isolated fully hardware-blurred frosted glass backplate
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(cornerRadius)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect.createBlurEffect(
                            30f, 30f, Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
                .background(glassColor, RoundedCornerShape(cornerRadius))
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        val paint = androidx.compose.ui.graphics.Paint()
                        val frameworkPaint = paint.asFrameworkPaint()
                        frameworkPaint.color = if (isDark) {
                            0x2D000000.toInt()
                        } else {
                            0x14007AFF.toInt()
                        }
                        frameworkPaint.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
                        canvas.drawRoundRect(
                            left = -4f,
                            top = -4f,
                            right = size.width + 4f,
                            bottom = size.height + 4f,
                            radiusX = cornerRadius.toPx(),
                            radiusY = cornerRadius.toPx(),
                            paint = paint
                        )
                    }
                }
        )

        // LAYER 2: The actual crisp text & icons content layer with reflective specular border
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = borderWidth,
                    brush = specularGradient,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .drawWithContent {
                    drawContent()
                    // Mirror reflection specular overlay
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.04f else 0.12f),
                                Color.Transparent,
                                Color.White.copy(alpha = if (isDark) 0.02f else 0.06f)
                            )
                        )
                    )
                }
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun FluidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var pressed by remember { mutableStateOf(false) }

    // Fluid liquid transition for press events - mimicking a squishy water or jelly bounce
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fluid_button_bounce"
    )

    val buttonColors = if (isDark) {
        listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6)) // iOS-like vibrant electric gradient
    } else {
        listOf(Color(0xFF007AFF), Color(0xFFD355F5)) // iOS-like light liquid pinkish blue gradient
    }

    Box(
        modifier = modifier
            .scale(buttonScale)
            .graphicsLayer {
                // Subtle fluid squeeze on tap
                scaleY = if (pressed) 0.96f else 1.0f
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(colors = buttonColors)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                onClick()
            }
            // Simple gesture listener approximation for fluid scaling inside clickable
            .drawWithContent {
                drawContent()
                // A dynamic liquid highlight overlaying the interactive button
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }

        // Handle press state manually for extra fluid animations on down gesture
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is androidx.compose.foundation.interaction.PressInteraction.Press -> pressed = true
                    is androidx.compose.foundation.interaction.PressInteraction.Release -> pressed = false
                    is androidx.compose.foundation.interaction.PressInteraction.Cancel -> pressed = false
                }
            }
        }
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isLiquidTheme = LocalIsLiquidTheme.current
    val isDark = isSystemInDarkTheme()
    
    if (isLiquidTheme) {
        GlassmorphismContainer(
            modifier = modifier,
            cornerRadius = cornerRadius,
            borderWidth = borderWidth,
            isDark = isDark,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Powerful extension modifier that instantly adds a sleek physical glass border, specular highlights
 * and a mirror-like sheen overlay to any standard Jetpack Compose element when the Liquid Glass theme is active.
 */
fun Modifier.glassmorphicSheen(
    isLiquidTheme: Boolean,
    isDark: Boolean = false,
    cornerRadius: Dp = 16.dp
): Modifier = this.then(
    if (isLiquidTheme) {
        Modifier
            .background(
                color = if (isDark) Color(0x751E1E2E) else Color(0xC5FFFFFF),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.04f),
                            Color(0xFF007AFF).copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.12f),
                            Color(0xFF007AFF).copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.35f)
                        )
                    }
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    } else Modifier
)
