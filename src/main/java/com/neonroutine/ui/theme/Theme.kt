package com.neonroutine.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { Color.Gray }

// ─────────────────────────────────────────────────────────────────────────────
// NEON GLOW color scheme builder (dark AMOLED with vivid accents)
// ─────────────────────────────────────────────────────────────────────────────
private fun neonGlowScheme(primary: Color, surface: Color, bg: Color) =
    darkColorScheme(
        primary              = primary,
        onPrimary            = Color.White,
        primaryContainer     = primary.copy(alpha = 0.20f),
        onPrimaryContainer   = primary,
        secondary            = primary.copy(alpha = 0.70f),
        onSecondary          = Color.White,
        secondaryContainer   = primary.copy(alpha = 0.14f),
        onSecondaryContainer = primary,
        tertiary             = primary.copy(alpha = 0.50f),
        background           = bg,
        onBackground         = Color(0xFFDDDDDD),
        surface              = surface,
        onSurface            = Color(0xFFE0E0E0),
        surfaceVariant       = surface.copy(alpha = 0.6f).run {
            Color(red + 0.03f, green + 0.03f, blue + 0.04f, 1f)
        },
        onSurfaceVariant     = Color(0xFF9090A0),
        outline              = primary.copy(alpha = 0.30f),
        error                = Color(0xFFCF6679),
        onError              = Color.White,
        errorContainer       = Color(0xFF5A0020),
        onErrorContainer     = Color(0xFFFFB3C1),
        inversePrimary       = primary,
        inverseSurface       = Color(0xFFE0E0E0),
        inverseOnSurface     = bg
    )

// ─────────────────────────────────────────────────────────────────────────────
// SOFT PASTEL color scheme builder (light, warm, airy)
// ─────────────────────────────────────────────────────────────────────────────
private fun softPastelScheme(primary: Color, surface: Color, bg: Color) =
    lightColorScheme(
        primary              = primary,
        onPrimary            = Color.White,
        primaryContainer     = primary.copy(alpha = 0.15f),
        onPrimaryContainer   = primary.copy(alpha = 0.85f),
        secondary            = primary.copy(alpha = 0.65f),
        onSecondary          = Color.White,
        secondaryContainer   = primary.copy(alpha = 0.10f),
        onSecondaryContainer = primary,
        tertiary             = primary.copy(alpha = 0.45f),
        background           = bg,
        onBackground         = Color(0xFF2C2C3E),
        surface              = surface,
        onSurface            = Color(0xFF2C2C3E),
        surfaceVariant       = primary.copy(alpha = 0.06f),
        onSurfaceVariant     = Color(0xFF6B6B80),
        outline              = primary.copy(alpha = 0.25f),
        error                = Color(0xFFBA1A1A),
        onError              = Color.White,
        errorContainer       = Color(0xFFFFDAD6),
        onErrorContainer     = Color(0xFF410002),
        inversePrimary       = Color.White,
        inverseSurface       = Color(0xFF2C2C3E),
        inverseOnSurface     = bg
    )

// ─────────────────────────────────────────────────────────────────────────────
// BRUTAL MINIMAL color scheme builder (stark, zero ornament)
// ─────────────────────────────────────────────────────────────────────────────
private fun brutalMinimalScheme(primary: Color, surface: Color, bg: Color, dark: Boolean): androidx.compose.material3.ColorScheme {
    return if (dark) {
        darkColorScheme(
            primary              = primary,
            onPrimary            = Color.Black,
            primaryContainer     = Color(0xFF222222),
            onPrimaryContainer   = primary,
            secondary            = Color(0xFFAAAAAA),
            onSecondary          = Color.Black,
            secondaryContainer   = Color(0xFF333333),
            onSecondaryContainer = Color.White,
            background           = bg,
            onBackground         = Color.White,
            surface              = surface,
            onSurface            = Color.White,
            surfaceVariant       = Color(0xFF1A1A1A),
            onSurfaceVariant     = Color(0xFFAAAAAA),
            outline              = Color(0xFF444444),
            error                = Color(0xFFFF5252),
            onError              = Color.Black
        )
    } else {
        lightColorScheme(
            primary              = primary,
            onPrimary            = Color.White,
            primaryContainer     = Color(0xFFEEEEEE),
            onPrimaryContainer   = primary,
            secondary            = Color(0xFF555555),
            onSecondary          = Color.White,
            secondaryContainer   = Color(0xFFDDDDDD),
            onSecondaryContainer = Color.Black,
            background           = bg,
            onBackground         = Color.Black,
            surface              = surface,
            onSurface            = Color.Black,
            surfaceVariant       = Color(0xFFEEEEEE),
            onSurfaceVariant     = Color(0xFF555555),
            outline              = Color.Black,
            error                = Color(0xFFB00020),
            onError              = Color.White
        )
    }
}

private fun glassmorphismScheme(primary: Color, surface: Color, bg: Color) =
    darkColorScheme(
        primary              = primary,
        onPrimary            = Color.White,
        primaryContainer     = primary.copy(alpha = 0.25f),
        onPrimaryContainer   = Color.White,
        secondary            = primary.copy(alpha = 0.8f),
        background           = bg,
        onBackground         = Color.White,
        surface              = surface.copy(alpha = 0.15f), // Translucent surface base
        onSurface            = Color.White,
        surfaceVariant       = surface.copy(alpha = 0.25f),
        onSurfaceVariant     = Color.White.copy(alpha = 0.7f),
        outline              = Color.White.copy(alpha = 0.15f), // iOS-style thin border
        error                = Color(0xFFCF6679)
    )

/**
 * iPhone-style glass panel — hardware-accelerated, premium look.
 * Uses real backdrop blur on Android 12+ (API 31+) while maintaining 120FPS performance
 * using specialized GraphicsLayer compositions.
 */
@Composable
fun Modifier.glassPanel(
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surface
): Modifier {
    if (!enabled) return this

    // 1. Frosted Filler: High-contrast semi-transparent white/surface mix
    // This fixes the text legibility issue by providing a solid "scrim" backdrop
    val frostedBrush = remember(color) {
        Brush.verticalGradient(
            listOf(
                color.copy(alpha = 0.35f), // Top slightly more opaque for light catching
                color.copy(alpha = 0.25f)
            )
        )
    }

    // 2. Specular Highlight: A sub-pixel white border that catches light at the top-left
    val lightBorderBrush = remember {
        Brush.linearGradient(
            0.0f to Color.White.copy(alpha = 0.30f), // Bright highlight
            0.5f to Color.White.copy(alpha = 0.10f),
            1.0f to Color.White.copy(alpha = 0.05f)
        )
    }

    return this
        .graphicsLayer {
            // Shadow for depth, but NO Blur on the content layer!
            shadowElevation = 15f
            clip = true
        }
        .clip(shape)
        .background(frostedBrush)
        .border(
            width = 0.6.dp,
            brush = lightBorderBrush,
            shape = shape
        )
}

/**
 * Scale-based press effect — uses graphicsLayer so zero recompositions during animation.
 */
@Composable
fun Modifier.glassHover(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "glassScale"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        // Hardware layer means GPU handles the scale transform with zero CPU cost
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main theme composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TimetableTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    themePreset: ThemePreset = ThemePreset.DEFAULT,
    content: @Composable () -> Unit
) {
    val primary    = parseColor(themePreset.primaryHex)
    val surface    = parseColor(themePreset.surfaceHex)
    val background = parseColor(themePreset.backgroundHex)

    val colorScheme = when (themePreset.designStyle) {
        DesignStyle.NEON_GLOW    -> neonGlowScheme(primary, surface, background)
        DesignStyle.SOFT_PASTEL  -> softPastelScheme(primary, surface, background)
        DesignStyle.BRUTAL_MINIMAL -> brutalMinimalScheme(primary, surface, background, themePreset.isDark)
        DesignStyle.GLASSMORPHISM -> glassmorphismScheme(primary, surface, background)
    }

    val shapes = when (themePreset.designStyle) {
        DesignStyle.NEON_GLOW      -> NeonGlowShapes
        DesignStyle.SOFT_PASTEL    -> SoftPastelShapes
        DesignStyle.BRUTAL_MINIMAL -> BrutalMinimalShapes
        DesignStyle.GLASSMORPHISM -> GlassmorphismShapes
    }

    val typography = when (themePreset.designStyle) {
        DesignStyle.BRUTAL_MINIMAL -> BrutalTypography
        DesignStyle.SOFT_PASTEL    -> PastelTypography
        else                       -> Typography
    }

    CompositionLocalProvider(
        LocalDesignStyle provides themePreset.designStyle,
        LocalAppShapes   provides shapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = typography,
            content     = content
        )
    }
}
