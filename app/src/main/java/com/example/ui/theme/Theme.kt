package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = JalaaGold,
    secondary = JalaaGold,
    tertiary = JalaaGold,
    background = JalaaDarkBg,
    surface = JalaaDarkSurface,
    onBackground = JalaaOnDark,
    onSurface = JalaaOnDark,
    primaryContainer = JalaaDarkSurface,
    onPrimaryContainer = JalaaGold
)

private val LightColorScheme = lightColorScheme(
    primary = JalaaGold,
    secondary = JalaaBlack,
    tertiary = JalaaGold,
    background = JalaaLightBg,
    surface = JalaaLightSurface,
    onBackground = JalaaOnLight,
    onSurface = JalaaOnLight,
    primaryContainer = JalaaBlack,
    onPrimaryContainer = JalaaWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Enforce Jalaa gym visual brand identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
