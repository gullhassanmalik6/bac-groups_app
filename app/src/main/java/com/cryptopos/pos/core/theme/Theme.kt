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

val Navy = Color(0xFF0B1F3A)
val Royal = Color(0xFF2563EB)
val Emerald = Color(0xFF10B981)
val NearBlack = Color(0xFF09090B)
val LightGray = Color(0xFFF4F6F8)

// Roboto / system sans — Inter can be bundled under res/font when font assets are provisioned.
private val PosFont = FontFamily.SansSerif

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Royal,
    onSecondary = Color.White,
    tertiary = Emerald,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Navy,
    surface = LightGray,
    onSurface = Navy,
    error = Color(0xFFDC2626),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Royal,
    onPrimary = Color.White,
    secondary = Royal,
    onSecondary = Color.White,
    tertiary = Emerald,
    onTertiary = Color.White,
    background = NearBlack,
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111113),
    onSurface = Color(0xFFF8FAFC),
    error = Color(0xFFF87171),
    onError = NearBlack,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = PosFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

@Composable
fun CryptoPosTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
