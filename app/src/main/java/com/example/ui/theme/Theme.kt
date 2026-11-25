package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class MsRsExtendedColors(
    val emeraldGradientStart: Color,
    val emeraldGradientEnd: Color,
    val solverBlueStart: Color,
    val solverBlueEnd: Color,
    val solverBlueContainer: Color,
    val onSolverBlueContainer: Color,
    val gold: Color,
    val goldBright: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
    val passGreen: Color,
    val passContainer: Color,
    val failRed: Color,
    val failContainer: Color,
    val khataPaper: Color,
    val khataLine: Color,
    val khataMargin: Color
)

private val LightExtendedColors = MsRsExtendedColors(
    emeraldGradientStart = EmeraldDark,
    emeraldGradientEnd = EmeraldPrimary,
    solverBlueStart = SolverBlue,
    solverBlueEnd = SolverBlueLight,
    solverBlueContainer = SolverBlueContainer,
    onSolverBlueContainer = OnSolverBlueContainer,
    gold = GoldAccent,
    goldBright = GoldBright,
    goldContainer = GoldContainer,
    onGoldContainer = OnGoldContainer,
    passGreen = PassGreen,
    passContainer = PassGreenContainer,
    failRed = FailRed,
    failContainer = FailRedContainer,
    khataPaper = Color(0xFFFFFDF7),
    khataLine = Color(0xFFD5E6DC),
    khataMargin = Color(0xFFE57373)
)

private val DarkExtendedColors = MsRsExtendedColors(
    emeraldGradientStart = Color(0xFF073B1E),
    emeraldGradientEnd = Color(0xFF0E6835),
    solverBlueStart = Color(0xFF104A91),
    solverBlueEnd = Color(0xFF1967C4),
    solverBlueContainer = Color(0xFF132C4B),
    onSolverBlueContainer = Color(0xFFD0E4FF),
    gold = GoldBright,
    goldBright = GoldAccent,
    goldContainer = Color(0xFF3E3004),
    onGoldContainer = Color(0xFFFFE599),
    passGreen = Color(0xFF4CD984),
    passContainer = Color(0xFF0C3B21),
    failRed = Color(0xFFFF6B6B),
    failContainer = Color(0xFF4A1313),
    khataPaper = Color(0xFF14241B),
    khataLine = Color(0xFF234030),
    khataMargin = Color(0xFF9E4747)
)

val LocalMsRsColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhite,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = SolverBlue,
    onSecondary = PureWhite,
    secondaryContainer = SolverBlueContainer,
    onSecondaryContainer = OnSolverBlueContainer,
    tertiary = GoldAccent,
    onTertiary = InkDark,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = MintSurface,
    onBackground = InkDark,
    surface = PureWhite,
    onSurface = InkDark,
    surfaceVariant = SoftCardSurface,
    onSurfaceVariant = InkMuted,
    outline = OutlineGreen,
    error = FailRed,
    onError = PureWhite,
    errorContainer = FailRedContainer,
    onErrorContainer = Color(0xFF5F0B0B)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = Color(0xFF032110),
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = Color(0xFFBDF5D3),
    secondary = Color(0xFF7CB8FF),
    onSecondary = Color(0xFF04264E),
    secondaryContainer = Color(0xFF133660),
    onSecondaryContainer = Color(0xFFD6E8FF),
    tertiary = GoldBright,
    onTertiary = Color(0xFF2B1F00),
    tertiaryContainer = Color(0xFF453404),
    onTertiaryContainer = Color(0xFFFFE7A1),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF2E523E),
    error = Color(0xFFFF7A7A),
    onError = Color(0xFF450707),
    errorContainer = Color(0xFF5C1515),
    onErrorContainer = Color(0xFFFFDAD6)
)

val MsRsShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MsRsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalMsRsColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MsRsTypography,
            shapes = MsRsShapes,
            content = content
        )
    }
}
