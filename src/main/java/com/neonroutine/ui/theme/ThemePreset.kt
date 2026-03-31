package com.neonroutine.ui.theme

enum class ThemePreset(
    val title: String,
    val description: String,
    val primaryHex: String,
    val surfaceHex: String,
    val backgroundHex: String,
    val designStyle: DesignStyle,
    val isDark: Boolean = true
) {
    // ── NEON GLOW family ──────────────────────────────────────────────────────
    DEFAULT(
        title = "Midnight Violet",
        description = "Dark AMOLED + neon purple glow",
        primaryHex = "#7F77DD",
        surfaceHex = "#151515",
        backgroundHex = "#000000",
        designStyle = DesignStyle.NEON_GLOW
    ),
    OCEAN(
        title = "Ocean Blue",
        description = "Deep sea dark + cyan neon",
        primaryHex = "#4ECDC4",
        surfaceHex = "#0D1F2D",
        backgroundHex = "#040D14",
        designStyle = DesignStyle.NEON_GLOW
    ),
    CYBERPUNK(
        title = "Cyber Neon",
        description = "Futuristic dark + electric yellow",
        primaryHex = "#F7DC6F",
        surfaceHex = "#1E1A29",
        backgroundHex = "#0B090F",
        designStyle = DesignStyle.NEON_GLOW
    ),
    // ── SOFT PASTEL family ────────────────────────────────────────────────────
    PASTEL_MINT(
        title = "Mint Fresh",
        description = "Warm whites + soft mint green",
        primaryHex = "#4DB6AC",
        surfaceHex = "#F5FAFA",
        backgroundHex = "#EDFAF8",
        designStyle = DesignStyle.SOFT_PASTEL,
        isDark = false
    ),
    PASTEL_ROSE(
        title = "Rose Garden",
        description = "Creamy whites + blush pink",
        primaryHex = "#E57373",
        surfaceHex = "#FFF5F5",
        backgroundHex = "#FFF0F0",
        designStyle = DesignStyle.SOFT_PASTEL,
        isDark = false
    ),
    PASTEL_LAVENDER(
        title = "Lavender Dream",
        description = "Soft lilac + warm white canvas",
        primaryHex = "#9575CD",
        surfaceHex = "#F5F0FF",
        backgroundHex = "#EFE9FF",
        designStyle = DesignStyle.SOFT_PASTEL,
        isDark = false
    ),
    // ── BRUTAL MINIMAL family ─────────────────────────────────────────────────
    BRUTAL_LIGHT(
        title = "Brutal Light",
        description = "Pure white + bold black borders",
        primaryHex = "#000000",
        surfaceHex = "#F5F5F5",
        backgroundHex = "#FFFFFF",
        designStyle = DesignStyle.BRUTAL_MINIMAL,
        isDark = false
    ),
    BRUTAL_DARK(
        title = "Brutal Dark",
        description = "Pure black + stark white type",
        primaryHex = "#FFFFFF",
        surfaceHex = "#111111",
        backgroundHex = "#000000",
        designStyle = DesignStyle.BRUTAL_MINIMAL
    ),
    BRUTAL_RED(
        title = "Brutal Red",
        description = "White canvas + raw red accent",
        primaryHex = "#E53935",
        surfaceHex = "#F5F5F5",
        backgroundHex = "#FFFFFF",
        designStyle = DesignStyle.BRUTAL_MINIMAL,
        isDark = false
    ),
    // ── GLASSMORPHISM family ──────────────────────────────────────────────────
    FROSTED_MIDNIGHT(
        title = "Frosted Midnight",
        description = "iOS-style deep dark glass + purple",
        primaryHex = "#A288E3",
        surfaceHex = "#1A1A1A",
        backgroundHex = "#050505",
        designStyle = DesignStyle.GLASSMORPHISM
    ),
    CRYSTAL_AURORA(
        title = "Crystal Aurora",
        description = "Translucent cyan glass + northern glow",
        primaryHex = "#48CAE4",
        surfaceHex = "#0A192F",
        backgroundHex = "#020617",
        designStyle = DesignStyle.GLASSMORPHISM
    ),
    AERO_GLASS(
        title = "Aero Glass",
        description = "Light airy glass with translucent white",
        primaryHex = "#0077B6",
        surfaceHex = "#FFFFFF",
        backgroundHex = "#F0F9FF",
        designStyle = DesignStyle.GLASSMORPHISM,
        isDark = false
    )
}

