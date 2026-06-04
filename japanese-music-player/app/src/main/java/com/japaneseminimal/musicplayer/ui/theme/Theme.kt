package com.japaneseminimal.musicplayer.ui.theme

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

// Light color scheme - Japanese minimal aesthetic
private val LightColorScheme = lightColorScheme(
    primary = SumiBlack,
    onPrimary = WashiWhite,
    primaryContainer = KinariBeige,
    onPrimaryContainer = SumiBlackDark,
    
    secondary = SakuraPink,
    onSecondary = SumiBlack,
    secondaryContainer = SakuraPinkLight,
    onSecondaryContainer = SumiBlackDark,
    
    tertiary = MatchaGreen,
    onTertiary = WashiWhite,
    tertiaryContainer = MatchaGreenLight,
    onTertiaryContainer = SumiBlackDark,
    
    background = WashiWhite,
    onBackground = SumiBlack,
    
    surface = WashiWhiteLight,
    onSurface = SumiBlack,
    surfaceVariant = KinariBeigeLight,
    onSurfaceVariant = SumiBlackLight,
    
    outline = Grey300,
    outlineVariant = Grey200,
    
    error = Color(0xFFBA1A1A),
    onError = WashiWhite
)

// Dark color scheme - Japanese minimal aesthetic with dark tones
private val DarkColorScheme = darkColorScheme(
    primary = WashiWhite,
    onPrimary = SumiBlack,
    primaryContainer = SumiBlackLight,
    onPrimaryContainer = WashiWhiteLight,
    
    secondary = SakuraPinkDark,
    onSecondary = WashiWhite,
    secondaryContainer = AiIndigoDark,
    onSecondaryContainer = SakuraPinkLight,
    
    tertiary = MatchaGreenDark,
    onTertiary = WashiWhite,
    tertiaryContainer = AiIndigoDark,
    onTertiaryContainer = MatchaGreenLight,
    
    background = SumiBlackDark,
    onBackground = WashiWhite,
    
    surface = SumiBlack,
    onSurface = WashiWhite,
    surfaceVariant = SumiBlackLight,
    onSurfaceVariant = Grey300,
    
    outline = Grey600,
    outlineVariant = Grey700,
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun JapaneseMusicPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
