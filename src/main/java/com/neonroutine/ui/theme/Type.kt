package com.neonroutine.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Default / Neon Glow ───────────────────────────────────────────────────────
val Typography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Black,  fontSize = 57.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,   fontSize = 45.sp, letterSpacing = 0.sp),
    displaySmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,   fontSize = 36.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,   fontSize = 32.sp, letterSpacing = 0.sp),
    headlineMedium= TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize=28.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize=24.sp, letterSpacing = 0.sp),
    titleLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,   fontSize = 22.sp, letterSpacing = 0.sp),
    titleMedium   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize=16.sp, letterSpacing = 0.15.sp),
    titleSmall    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.25.sp),
    bodySmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp)
)

// ── Brutal Minimal — large, heavy, geometric ────────────────────────────────
val BrutalTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,  fontSize = 64.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,  fontSize = 50.sp, letterSpacing = (-0.5).sp),
    displaySmall  = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,  fontSize = 40.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,  fontSize = 34.sp, letterSpacing = 0.sp),
    headlineMedium= TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,  fontSize = 28.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 0.sp),
    titleLarge    = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,   fontSize = 22.sp, letterSpacing = 0.sp),
    titleMedium   = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,   fontSize = 16.sp, letterSpacing = 0.sp),
    titleSmall    = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,   fontSize = 14.sp, letterSpacing = 0.sp),
    bodyLarge     = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.sp),
    bodyMedium    = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.sp),
    bodySmall     = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.sp),
    labelLarge    = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,   fontSize = 14.sp, letterSpacing = 1.sp),
    labelMedium   = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,   fontSize = 12.sp, letterSpacing = 1.5.sp),
    labelSmall    = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,   fontSize = 10.sp, letterSpacing = 2.sp)
)

// ── Soft Pastel — compact, rounded, friendly ───────────────────────────────
val PastelTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,   fontSize = 52.sp, letterSpacing = 0.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,   fontSize = 42.sp, letterSpacing = 0.sp),
    displaySmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,   fontSize = 34.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 30.sp, letterSpacing = 0.sp),
    headlineMedium= TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 26.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 22.sp, letterSpacing = 0.sp),
    titleLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 20.sp, letterSpacing = 0.sp),
    titleMedium   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 15.sp, letterSpacing = 0.1.sp),
    titleSmall    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, letterSpacing = 0.25.sp),
    bodyMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = 0.25.sp),
    bodySmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.4.sp),
    labelLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.5.sp),
    labelMedium   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.5.sp)
)

