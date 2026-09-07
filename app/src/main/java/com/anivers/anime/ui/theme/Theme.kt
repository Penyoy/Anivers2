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
    primary = Color(0xFFFFDB89), // luxury gold #ffdb89 from palette
    onPrimary = Color(0xFF030303),
    primaryContainer = Color(0xFF2C2C2E), // charcoal #2c2c2e
    onPrimaryContainer = Color(0xFFFFDB89),
    secondary = Color(0xFFFFDB89),
    background = Color(0xFF030303), // pure black #030303
    onBackground = Color(0xFFF1F1F1),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF1F1F1),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEB2C7),
    outline = Color(0xFF3A3A3A)
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

// Glass luxury
val GlassBg = Color(0x14FFDB89).copy(alpha = 0.08f) // gold translucent
val GlassBorder = Color(0x33FFDB89) // gold border
val GlassStrong = Color(0xCC030303) // black 80%
val BestyFontFamily = FontFamily(Font(R.font.besty))
