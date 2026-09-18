package com.cryptopos.pos.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cryptopos.pos.domain.model.ThemeMode

/** CryptoPos mockup palette — dark terminal UI with neon green CTAs. */
val NeonGreen = Color(0xFF22C55E)
val CryptoBg = Color(0xFF0B1220)
val CryptoSurface = Color(0xFF151C2C)
val CryptoSurfaceAlt = Color(0xFF1B2436)
val CryptoMuted = Color(0xFF94A3B8)
val CryptoOutline = Color(0xFF334155)
val ReceiptPaper = Color(0xFFF8FAFC)
val ReceiptInk = Color(0xFF0F172A)

private val PosFont = FontFamily.SansSerif

private val LightColors = lightColorScheme(
    primary = NeonGreen,
    onPrimary = Color.White,
    secondary = NeonGreen,
    onSecondary = Color.White,
    tertiary = NeonGreen,
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = ReceiptInk,
    surface = Color.White,
    onSurface = ReceiptInk,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF052E16),
    secondary = NeonGreen,
    onSecondary = Color(0xFF052E16),
    tertiary = NeonGreen,
    onTertiary = Color(0xFF052E16),
    background = CryptoBg,
    onBackground = Color(0xFFF8FAFC),
    surface = CryptoSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = CryptoSurfaceAlt,
    onSurfaceVariant = CryptoMuted,
    outline = CryptoOutline,
    error = Color(0xFFF87171),
    onError = CryptoBg,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
)

@Composable
fun CryptoPosTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> true // POS product UI is dark-first (matches CryptoPos mockups)
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
