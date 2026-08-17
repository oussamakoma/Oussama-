package com.example.ui.theme

import androidx.compose.ui.geometry.Offset
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

fun Modifier.liquidGlass(
    cornerRadius: Dp = 22.dp,
    backgroundAlpha: Float = 0.25f
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color.White.copy(alpha = backgroundAlpha))
    .then(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                renderEffect = RenderEffect
                    .createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
        } else Modifier.blur(20.dp)
    )
    .border(
        width = 1.2.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.75f),  // top specular
                Color.White.copy(alpha = 0.20f)   // bottom depth
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
    .drawWithContent {
        drawContent()
        // diagonal specular overlay — the glass shine
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.06f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )
    }

@Composable
fun GlassmorphismContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.2.dp,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .liquidGlass(cornerRadius = cornerRadius, backgroundAlpha = 0.25f)
            .padding(16.dp),
        content = content
    )
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
