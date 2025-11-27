package de.zemki.metagcompose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// MetaG-Analyze Brand Colors from backend design system
object MetagColors {
    // Primary Brand Colors
    val Primary = Color(0xFF046cbe)        // Main brand blue
    val PrimaryVariant = Color(0xFF2563eb)  // blue-600
    val PrimaryDark = Color(0xFF1d4ed8)     // blue-700
    val PrimaryLight = Color(0xFF60a5fa)    // blue-400
    val PrimaryContainer = Color(0xFFdbeafe) // blue-100
    val PrimaryContainerLight = Color(0xFFeff6ff) // blue-50

    // Background Colors
    val Background = Color(0xFFfdfbf7)      // Body background
    val Surface = Color(0xFFffffff)         // Card surfaces
    val SurfaceVariant = Color(0xFFffffff)  // Input field backgrounds - pure white

    // Status Colors
    val Error = Color(0xFFd92442)           // Danger/error red
    val Success = Color(0xFF38c172)         // Success green
    val Warning = Color(0xFFf6993f)         // Warning orange
    val WarningLight = Color(0xFFffed4a)    // Warning yellow

    // Navigation
    val NavigationBackground = Color(0xFF3b82f6) // bg-blue-500
    val UserMenuBackground = Color(0xFF0ea5e9)   // bg-sky-500
    
    // Text Colors
    val OnPrimary = Color.White
    val OnBackground = Color(0xFF1f2937)     // Dark gray
    val OnSurface = Color(0xFF374151)        // Medium gray
    val OnSurfaceVariant = Color(0xFF6b7280)  // Light gray
    
    // Dark theme variations
    val BackgroundDark = Color(0xFF0f172a)
    val SurfaceDark = Color(0xFF1e293b)
    val OnBackgroundDark = Color(0xFFf1f5f9)
    val OnSurfaceDark = Color(0xFFe2e8f0)
}

private val LightColorScheme = lightColorScheme(
    primary = MetagColors.Primary,
    onPrimary = MetagColors.OnPrimary,
    primaryContainer = MetagColors.PrimaryContainer,
    onPrimaryContainer = MetagColors.Primary,
    secondary = MetagColors.PrimaryVariant,
    onSecondary = MetagColors.OnPrimary,
    tertiary = MetagColors.Success,
    onTertiary = Color.White,
    background = MetagColors.Background,
    onBackground = MetagColors.OnBackground,
    surface = MetagColors.Surface,
    onSurface = MetagColors.OnSurface,
    surfaceVariant = MetagColors.SurfaceVariant,
    onSurfaceVariant = MetagColors.OnSurfaceVariant,
    error = MetagColors.Error,
    onError = Color.White,
    outline = MetagColors.OnSurfaceVariant,
    outlineVariant = MetagColors.PrimaryContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = MetagColors.PrimaryLight,
    onPrimary = MetagColors.Primary,
    primaryContainer = MetagColors.Primary,
    onPrimaryContainer = MetagColors.PrimaryContainer,
    secondary = MetagColors.PrimaryLight,
    onSecondary = MetagColors.Primary,
    tertiary = MetagColors.Success,
    onTertiary = Color.White,
    background = MetagColors.BackgroundDark,
    onBackground = MetagColors.OnBackgroundDark,
    surface = MetagColors.SurfaceDark,
    onSurface = MetagColors.OnSurfaceDark,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94a3b8),
    error = MetagColors.Error,
    onError = Color.White,
    outline = Color(0xFF64748b),
    outlineVariant = Color(0xFF475569),
)

@Composable
fun MetagTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MetagTypography,
        content = content
    )
}