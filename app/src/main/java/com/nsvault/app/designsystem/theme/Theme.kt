package com.nsvault.app.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nsvault.app.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = VaultColors.AuroraViolet,
    onPrimary = VaultColors.OnAccent,
    primaryContainer = VaultColors.SurfaceHigh,
    onPrimaryContainer = VaultColors.TextPrimary,
    secondary = VaultColors.AuroraCyan,
    onSecondary = VaultColors.OnAccent,
    secondaryContainer = VaultColors.SurfaceRaised,
    onSecondaryContainer = VaultColors.TextPrimary,
    tertiary = VaultColors.Gold,
    onTertiary = VaultColors.OnAccent,
    background = VaultColors.Obsidian,
    onBackground = VaultColors.TextPrimary,
    surface = VaultColors.Surface,
    onSurface = VaultColors.TextPrimary,
    surfaceVariant = VaultColors.SurfaceRaised,
    onSurfaceVariant = VaultColors.TextSecondary,
    surfaceContainerLowest = VaultColors.Obsidian,
    surfaceContainerLow = VaultColors.Surface,
    surfaceContainer = VaultColors.SurfaceRaised,
    surfaceContainerHigh = VaultColors.SurfaceHigh,
    error = VaultColors.Error,
    onError = VaultColors.OnAccent,
    outline = VaultColors.Hairline,
    outlineVariant = VaultColors.Hairline,
)

private val LightColorScheme = lightColorScheme(
    primary = VaultColors.AuroraViolet,
    onPrimary = VaultColors.OnAccent,
    primaryContainer = VaultColors.SurfaceHigh,
    onPrimaryContainer = VaultColors.TextPrimary,
    secondary = VaultColors.AuroraCyan,
    onSecondary = VaultColors.OnAccent,
    secondaryContainer = VaultColors.SurfaceRaised,
    onSecondaryContainer = VaultColors.TextPrimary,
    tertiary = VaultColors.Gold,
    background = VaultColors.TextPrimary,
    onBackground = VaultColors.Obsidian,
    surface = VaultColors.Surface,
    onSurface = VaultColors.TextPrimary,
    surfaceVariant = VaultColors.SurfaceRaised,
    onSurfaceVariant = VaultColors.TextSecondary,
    error = VaultColors.Error,
    onError = VaultColors.OnAccent,
    outline = VaultColors.Hairline,
)

@Composable
fun NSVaultTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val useDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        useDynamicColors && useDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
        useDynamicColors && !useDarkTheme -> dynamicLightColorScheme(LocalContext.current)
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VaultTypography,
        shapes = VaultShapes,
        content = content,
    )
}
