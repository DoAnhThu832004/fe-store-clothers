package org.example.project.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ─── Bảng màu Dark Mode ────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor2,          // Màu chính: Tím xanh (#6C72FF)
    onPrimary = Neutral100,
    primaryContainer = PrimaryColor1, // Tím hồng
    onPrimaryContainer = Neutral100,

    secondary = SecondaryColor2,      // Tím sáng
    onSecondary = Neutral100,
    secondaryContainer = SecondaryColor3, // Xanh cyan
    onSecondaryContainer = Neutral800,

    tertiary = SecondaryColor4,       // Xanh dương
    onTertiary = Neutral100,
    tertiaryContainer = SecondaryColor5, // Vàng cam
    onTertiaryContainer = Neutral800,

    background = Neutral800,          // Nền app đen/xanh navy (#080F25)
    onBackground = Neutral100,
    surface = SecondaryColor1,        // Nền thẻ/card (#101935)
    onSurface = Neutral100,
    surfaceVariant = Neutral700,
    onSurfaceVariant = Neutral400,

    error = ErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    outline = Neutral600
)

// ─── Bảng màu Light Mode ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor2,
    onPrimary = Neutral100,
    primaryContainer = Neutral300,
    onPrimaryContainer = Neutral800,

    secondary = SecondaryColor2,
    onSecondary = Neutral100,
    secondaryContainer = SecondaryColor3,
    onSecondaryContainer = Neutral800,

    tertiary = SecondaryColor4,
    onTertiary = Neutral100,
    tertiaryContainer = SecondaryColor5,
    onTertiaryContainer = Neutral800,

    background = Neutral100,          // Nền app trắng (#FFFFFF)
    onBackground = Neutral800,
    surface = Neutral200,             // Nền thẻ xám nhạt (#D9E1FA)
    onSurface = Neutral800,
    surfaceVariant = Neutral300,
    onSurfaceVariant = Neutral700,

    error = ErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    outline = Neutral500
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
