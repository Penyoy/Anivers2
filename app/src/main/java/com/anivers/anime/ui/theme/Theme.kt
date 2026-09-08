package com.anivers.anime.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.anivers.anime.R

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFDB89),
    onPrimary = Color(0xFF030303),
    primaryContainer = Color(0xFFD8B76D),
    onPrimaryContainer = Color(0xFF030303),
    secondary = Color(0xFFD8B76D),
    onSecondary = Color(0xFF030303),
    background = Color(0xFF030303),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF2C2C2E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFFB8B8B8),
    surfaceContainer = Color(0xFF2C2C2E),
    outline = Color(0xFF3A3A3C),
    error = Color(0xFFFF5F5F),
    onError = Color(0xFFFFFFFF)
)

private val LightColorScheme = DarkColorScheme // force dark like hanime

@Composable
fun AniVerseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(
            titleLarge = Typography().titleLarge.copy(fontFamily = BestyFontFamily, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = (-0.3).sp, color = Color(0xFFFFDB89)),
            titleMedium = Typography().titleMedium.copy(fontFamily = BestyFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color(0xFFFFDB89)),
            titleSmall = Typography().titleSmall.copy(fontFamily = BestyFontFamily, color = Color(0xFFF8FAFC)),
            bodyMedium = Typography().bodyMedium.copy(color = Color(0xFFAEB2C7), lineHeight = 18.sp),
            labelSmall = Typography().labelSmall.copy(color = Color(0xFF8A8FA3), letterSpacing = 0.3.sp),
            labelMedium = Typography().labelMedium.copy(fontFamily = BestyFontFamily, color = Color(0xFFFFDB89))
        ),
        content = content
    )
}

// Spec palette extras
val GoldPrimary = Color(0xFFFFDB89)
val GoldVariant = Color(0xFFD8B76D)
val BgBlack = Color(0xFF030303)
val SurfaceGray = Color(0xFF2C2C2E)
val SurfaceDark = Color(0xFF1C1C1E)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB8B8B8)
val DividerGray = Color(0xFF3A3A3C)
val ErrorRed = Color(0xFFFF5F5F)
val SuccessGreen = Color(0xFF8FD694)

// Glass luxury
val GlassBg = Color(0x14FFDB89).copy(alpha = 0.08f)
val GlassBorder = Color(0x33FFDB89)
val GlassStrong = Color(0xCC030303)
val BestyFontFamily = FontFamily(Font(R.font.besty))
