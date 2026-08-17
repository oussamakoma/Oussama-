package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

@Composable
fun LiquidGlassBlobBackground(modifier: Modifier = Modifier, isDark: Boolean = true) {
    val animX1 = remember { Animatable(0.2f) }
    val animY1 = remember { Animatable(0.1f) }
    val animX2 = remember { Animatable(0.8f) }
    val animY2 = remember { Animatable(0.8f) }
    val animX3 = remember { Animatable(0.5f) }
    val animY3 = remember { Animatable(0.4f) }

    val pulse1 = remember { Animatable(0.75f) }
    val pulse2 = remember { Animatable(0.70f) }
    val pulse3 = remember { Animatable(0.65f) }

    LaunchedEffect(Unit) {
        launch {
            while (true) {
                animX1.animateTo(0.8f, animationSpec = tween(18000, easing = FastOutSlowInEasing))
                animX1.animateTo(0.2f, animationSpec = tween(18000, easing = FastOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                animY1.animateTo(0.7f, animationSpec = tween(22000, easing = FastOutSlowInEasing))
                animY1.animateTo(0.1f, animationSpec = tween(22000, easing = FastOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                animX2.animateTo(0.1f, animationSpec = tween(25000, easing = FastOutSlowInEasing))
                animX2.animateTo(0.8f, animationSpec = tween(25000, easing = FastOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                animY2.animateTo(0.2f, animationSpec = tween(20000, easing = FastOutSlowInEasing))
                animY2.animateTo(0.8f, animationSpec = tween(20000, easing = FastOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                animX3.animateTo(0.9f, animationSpec = tween(28000, easing = FastOutSlowInEasing))
                animX3.animateTo(0.3f, animationSpec = tween(28000, easing = FastOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                animY3.animateTo(0.1f, animationSpec = tween(24000, easing = FastOutSlowInEasing))
                animY3.animateTo(0.9f, animationSpec = tween(24000, easing = FastOutSlowInEasing))
            }
        }
        // Organic biological fluid pulse animations
        launch {
            while (true) {
                pulse1.animateTo(0.95f, animationSpec = tween(12000, easing = LinearOutSlowInEasing))
                pulse1.animateTo(0.75f, animationSpec = tween(12000, easing = LinearOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                pulse2.animateTo(0.88f, animationSpec = tween(14000, easing = LinearOutSlowInEasing))
                pulse2.animateTo(0.68f, animationSpec = tween(14000, easing = LinearOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                pulse3.animateTo(0.82f, animationSpec = tween(16000, easing = LinearOutSlowInEasing))
                pulse3.animateTo(0.58f, animationSpec = tween(16000, easing = LinearOutSlowInEasing))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF141727), Color(0xFF060811))
                    } else {
                        listOf(Color(0xFFEFF5FB), Color(0xFFD3E2F2))
                    },
                    radius = 2200f
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isDark) {
                // Blob 1: Electric Sky Blue
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF007AFF).copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(width * animX1.value, height * animY1.value),
                        radius = width * pulse1.value
                    ),
                    center = Offset(width * animX1.value, height * animY1.value),
                    radius = width * pulse1.value
                )
                // Blob 2: Vibrant Violet
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFD355F5).copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(width * animX2.value, height * animY2.value),
                        radius = width * pulse2.value
                    ),
                    center = Offset(width * animX2.value, height * animY2.value),
                    radius = width * pulse2.value
                )
                // Blob 3: Premium Crimson Rose
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF2D55).copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(width * animX3.value, height * animY3.value),
                        radius = width * pulse3.value
                    ),
                    center = Offset(width * animX3.value, height * animY3.value),
                    radius = width * pulse3.value
                )
            } else {
                // Blob 1: Vibrant Aqua Blue
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF007AFF).copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(width * animX1.value, height * animY1.value),
                        radius = width * pulse1.value
                    ),
                    center = Offset(width * animX1.value, height * animY1.value),
                    radius = width * pulse1.value
                )
                // Blob 2: Soft Purple Pearl
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFD355F5).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(width * animX2.value, height * animY2.value),
                        radius = width * pulse2.value
                    ),
                    center = Offset(width * animX2.value, height * animY2.value),
                    radius = width * pulse2.value
                )
            }
        }
    }
}
