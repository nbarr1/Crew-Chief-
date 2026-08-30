package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = FlagGold,
    onPrimary = OnFlagGold,
    primaryContainer = FlagGoldContainer,
    onPrimaryContainer = OnFlagGoldContainer,
    secondary = TurfGreenLight,
    onSecondary = Color(0xFF003912),
    secondaryContainer = TurfContainer,
    onSecondaryContainer = OnTurfContainer,
    tertiary = WhistleChrome,
    onTertiary = Color(0xFF191D1E),
    tertiaryContainer = Color(0xFF2C3539),
    onTertiaryContainer = WhistleChromeLight,
    background = StadiumNightBg,
    onBackground = StripeWhite,
    surface = StadiumSurface,
    onSurface = StripeWhite,
    surfaceVariant = StadiumSurfaceVariant,
    onSurfaceVariant = StripeGray,
    outline = StadiumBorder,
    outlineVariant = Color(0xFF1E2823),
    error = GradeIncorrectRed,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme(
    // Enforcing high-contrast dark theme by default since officiating simulation is dark-first
    primary = FlagGoldDark,
    onPrimary = OnFlagGold,
    primaryContainer = FlagGoldContainer,
    onPrimaryContainer = OnFlagGoldContainer,
    secondary = TurfGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF003912),
    tertiary = WhistleChromeDark,
    onTertiary = Color.White,
    background = StadiumNightBg,
    onBackground = StripeWhite,
    surface = StadiumSurface,
    onSurface = StripeWhite,
    surfaceVariant = StadiumSurfaceVariant,
    onSurfaceVariant = StripeGray,
    outline = StadiumBorder,
    error = GradeIncorrectRed,
    onError = Color.White
)

@Composable
fun CrewChiefTheme(
    darkTheme: Boolean = true, // Dark mode first for low light & high contrast officiating feel
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
