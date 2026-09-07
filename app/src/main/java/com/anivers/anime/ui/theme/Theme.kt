package com.anivers.anime.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3730A3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E1B4B),
    onPrimaryContainer = Color(0xFFA5B4FC),
    secondary = Color(0xFF6366F1),
    background = Color(0xFF05070E),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0A0F1E),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF141A2E),
    onSurfaceVariant = Color(0xFFAEB2C7),
    outline = Color(0xFF1E293B)
)

private val LightColorScheme = DarkColorScheme // force dark like hanime

@Composable
fun AniVerseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(
            titleLarge = Typography().titleLarge.copy(fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
            titleMedium = Typography().titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color(0xFFF8FAFC)),
            bodyMedium = Typography().bodyMedium.copy(color = Color(0xFFAEB2C7), lineHeight = 18.sp),
            labelSmall = Typography().labelSmall.copy(color = Color(0xFF8A8FA3), letterSpacing = 0.3.sp)
        ),
        content = content
    )
}

// Glass card colors matching hanime-main
val GlassBg = Color(0x0DFFFFFF) // rgba 255 5%
val GlassBorder = Color(0x12FFFFFF) // rgba 7%
val GlassStrong = Color(0x59050E14) // slightly opaque
