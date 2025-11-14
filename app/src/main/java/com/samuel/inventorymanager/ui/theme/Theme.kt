package com.samuel.inventorymanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ========================================================================================
// COLOR DEFINITIONS
// ========================================================================================

private val PurpleLight40 = Color(0xFF6650a4)
private val PurpleGreyLight40 = Color(0xFF625b71)
private val PinkLight40 = Color(0xFF7D5260)

private val PurpleDark80 = Color(0xFFEADDF5)
private val PurpleGreyDark80 = Color(0xFFCCC7DB)
private val PinkDark80 = Color(0xFFEFB8C8)

// Additional Theme Colors
private val LightBackground = Color(0xFFFFFBFE)
private val DarkBackground = Color(0xFF1A1A1A)

private val DraculaPrimary = Color(0xFFBD93F9)
private val VampirePrimary = Color(0xFFFF1493)
private val OceanPrimary = Color(0xFF00B4D8)
private val ForestPrimary = Color(0xFF2D6A4F)
private val SunsetPrimary = Color(0xFFFF6B35)
private val CyberpunkPrimary = Color(0xFFFF006E)
private val NeonPrimary = Color(0xFF39FF14)

// ========================================================================================
// COLOR SCHEMES
// ========================================================================================

private val LightColorScheme = lightColorScheme(
    primary = PurpleLight40,
    secondary = PurpleGreyLight40,
    tertiary = PinkLight40,
    background = LightBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = PurpleDark80,
    secondary = PurpleGreyDark80,
    tertiary = PinkDark80,
    background = DarkBackground,
    surface = Color(0xFF2A2A2A),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val DraculaLightScheme = lightColorScheme(
    primary = DraculaPrimary,
    secondary = Color(0xFF8BE9FD),
    tertiary = Color(0xFFFF79C6),
    background = Color(0xFFF8F8F2),
    surface = Color.White
)

private val DraculaDarkScheme = darkColorScheme(
    primary = DraculaPrimary,
    secondary = Color(0xFF8BE9FD),
    tertiary = Color(0xFFFF79C6),
    background = Color(0xFF282A36),
    surface = Color(0xFF21222C)
)

private val VampireLightScheme = lightColorScheme(
    primary = VampirePrimary,
    secondary = Color(0xFFE91E63),
    tertiary = Color(0xFFC2185B),
    background = Color(0xFFFFF1F5),
    surface = Color.White
)

private val VampireDarkScheme = darkColorScheme(
    primary = VampirePrimary,
    secondary = Color(0xFFE91E63),
    tertiary = Color(0xFFC2185B),
    background = Color(0xFF1A0015),
    surface = Color(0xFF2A0A1F)
)

private val OceanLightScheme = lightColorScheme(
    primary = OceanPrimary,
    secondary = Color(0xFF0096C7),
    tertiary = Color(0xFF00B4D8),
    background = Color(0xFFF0F8FF),
    surface = Color.White
)

private val OceanDarkScheme = darkColorScheme(
    primary = OceanPrimary,
    secondary = Color(0xFF0096C7),
    tertiary = Color(0xFF00B4D8),
    background = Color(0xFF001F3F),
    surface = Color(0xFF003D5C)
)

private val ForestLightScheme = lightColorScheme(
    primary = ForestPrimary,
    secondary = Color(0xFF40916C),
    tertiary = Color(0xFF52B788),
    background = Color(0xFFF1FAEE),
    surface = Color.White
)

private val ForestDarkScheme = darkColorScheme(
    primary = ForestPrimary,
    secondary = Color(0xFF40916C),
    tertiary = Color(0xFF52B788),
    background = Color(0xFF1B4332),
    surface = Color(0xFF2D6A4F)
)

private val SunsetLightScheme = lightColorScheme(
    primary = SunsetPrimary,
    secondary = Color(0xFFFFA500),
    tertiary = Color(0xFFFFB703),
    background = Color(0xFFFFF8F3),
    surface = Color.White
)

private val SunsetDarkScheme = darkColorScheme(
    primary = SunsetPrimary,
    secondary = Color(0xFFFFA500),
    tertiary = Color(0xFFFFB703),
    background = Color(0xFF3D2817),
    surface = Color(0xFF5C3D2E)
)

private val CyberpunkLightScheme = lightColorScheme(
    primary = CyberpunkPrimary,
    secondary = Color(0xFF00F5FF),
    tertiary = Color(0xFFFFBE0B),
    background = Color(0xFFF8F9FF),
    surface = Color.White
)

private val CyberpunkDarkScheme = darkColorScheme(
    primary = CyberpunkPrimary,
    secondary = Color(0xFF00F5FF),
    tertiary = Color(0xFFFFBE0B),
    background = Color(0xFF0A0E27),
    surface = Color(0xFF1A1F3A)
)

private val NeonLightScheme = lightColorScheme(
    primary = NeonPrimary,
    secondary = Color(0xFFFF006E),
    tertiary = Color(0xFF00F5FF),
    background = Color(0xFFFFFFF0),
    surface = Color.White
)

private val NeonDarkScheme = darkColorScheme(
    primary = NeonPrimary,
    secondary = Color(0xFFFF006E),
    tertiary = Color(0xFF00F5FF),
    background = Color(0xFF0D0221),
    surface = Color(0xFF1A0033)
)

// ========================================================================================
// CUSTOM TYPOGRAPHY WITH FONT SCALING
// ========================================================================================

@Composable
fun getScaledTypography(scale: Float): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (57 * scale).sp
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (45 * scale).sp
        ),
        displaySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (36 * scale).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (32 * scale).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (28 * scale).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (22 * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * scale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (12 * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (11 * scale).sp
        )
    )
}

// ========================================================================================
// THEME ENUM
// ========================================================================================

enum class AppThemeType {
    LIGHT, DARK, DRACULA, VAMPIRE, OCEAN, FOREST, SUNSET, CYBERPUNK, NEON
}

// ========================================================================================
// MAIN THEME COMPOSABLE
// ========================================================================================

@Composable
fun InventoryManagerTheme(
    themeType: AppThemeType = AppThemeType.LIGHT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1.0f,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        themeType == AppThemeType.LIGHT -> LightColorScheme
        themeType == AppThemeType.DARK -> DarkColorScheme
        themeType == AppThemeType.DRACULA -> if (darkTheme) DraculaDarkScheme else DraculaLightScheme
        themeType == AppThemeType.VAMPIRE -> if (darkTheme) VampireDarkScheme else VampireLightScheme
        themeType == AppThemeType.OCEAN -> if (darkTheme) OceanDarkScheme else OceanLightScheme
        themeType == AppThemeType.FOREST -> if (darkTheme) ForestDarkScheme else ForestLightScheme
        themeType == AppThemeType.SUNSET -> if (darkTheme) SunsetDarkScheme else SunsetLightScheme
        themeType == AppThemeType.CYBERPUNK -> if (darkTheme) CyberpunkDarkScheme else CyberpunkLightScheme
        themeType == AppThemeType.NEON -> if (darkTheme) NeonDarkScheme else NeonLightScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val scaledTypography = getScaledTypography(fontScale)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content
    )
}