package com.example.bits_helper.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD2B48C),
    onPrimary = Color(0xFF2F2B26),
    primaryContainer = Color(0xFF8B7355),
    onPrimaryContainer = Color(0xFFF5F5DC),
    secondary = Color(0xFFDEB887),
    onSecondary = Color(0xFF2F2B26),
    secondaryContainer = Color(0xFFCD853F),
    onSecondaryContainer = Color(0xFFF5F5DC),
    surface = Color(0xFF3A2F26),
    onSurface = Color(0xFFF5F5DC),
    surfaceVariant = Color(0xFF4A3F36),
    onSurfaceVariant = Color(0xFFE6D7C3),
    background = Color(0xFF2F2B26),
    onBackground = Color(0xFFF5F5DC),
    outline = Color(0xFF8B7355),
    outlineVariant = Color(0xFF6B5B4A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0078D4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E7FF),
    onPrimaryContainer = Color(0xFF001A33),
    secondary = Color(0xFF6B7280),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFF1F2937),
    surface = Color(0xFFF6F8FB),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE6EAF0),
    onSurfaceVariant = Color(0xFF374151),
    background = Color(0xFFF5F6F7),
    onBackground = Color(0xFF0F172A),
    outline = Color(0xFFD1D5DB)
)

@Composable
fun Bits_helperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val shouldUseDarkTheme = themeManager.shouldUseDarkTheme(isSystemInDarkTheme)
    
    val colorScheme = when {
        shouldUseDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    SideEffect {
        (context as? Activity)?.window?.let { window ->
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = !shouldUseDarkTheme
                isAppearanceLightNavigationBars = !shouldUseDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}