package com.neonroutine.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The three complete design personalities of NeonRoutine.
 *
 * NEON_GLOW   – Dark/AMOLED, vivid neon accents, pill shapes, glow effects.
 * SOFT_PASTEL – Warm whites, pastel accents, gentle rounded corners, cozy.
 * BRUTAL_MINIMAL – Pure white or black, thick borders, zero radius, flat bold.
 */
enum class DesignStyle {
    NEON_GLOW,
    SOFT_PASTEL,
    BRUTAL_MINIMAL,
    GLASSMORPHISM
}

data class AppShapes(
    val card: Shape = RoundedCornerShape(16.dp),
    val button: Shape = RoundedCornerShape(50.dp),
    val chip: Shape = RoundedCornerShape(8.dp),
    val bottomNav: Shape = RoundedCornerShape(0.dp),
    val cardElevation: Dp = 4.dp,
    val progressStrokeWidth: Dp = 14.dp
)

val NeonGlowShapes = AppShapes(
    card = RoundedCornerShape(20.dp),
    button = RoundedCornerShape(50.dp),
    chip = RoundedCornerShape(50.dp),
    bottomNav = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    cardElevation = 6.dp,
    progressStrokeWidth = 16.dp
)

val SoftPastelShapes = AppShapes(
    card = RoundedCornerShape(12.dp),
    button = RoundedCornerShape(12.dp),
    chip = RoundedCornerShape(8.dp),
    bottomNav = RoundedCornerShape(0.dp),
    cardElevation = 2.dp,
    progressStrokeWidth = 12.dp
)

val BrutalMinimalShapes = AppShapes(
    card = RoundedCornerShape(0.dp),
    button = RoundedCornerShape(0.dp),
    chip = RoundedCornerShape(0.dp),
    bottomNav = RoundedCornerShape(0.dp),
    cardElevation = 0.dp,
    progressStrokeWidth = 10.dp
)

val GlassmorphismShapes = AppShapes(
    card = RoundedCornerShape(24.dp),
    button = RoundedCornerShape(50.dp),
    chip = RoundedCornerShape(50.dp),
    bottomNav = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    cardElevation = 0.dp, // No elevation for pure glass
    progressStrokeWidth = 16.dp
)

val LocalDesignStyle = compositionLocalOf { DesignStyle.NEON_GLOW }
val LocalAppShapes   = compositionLocalOf { NeonGlowShapes }
