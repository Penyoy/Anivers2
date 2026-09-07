package com.anivers.anime.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5B5BD6), // natural muted indigo (was harsh #3730A3)
    onPrimary = Color.White,
    primaryContainer = Color(0xFF242457),
    onPrimaryContainer = Color(0xFFC7C5FF),
    secondary = Color(0xFF8A7DFF),
    background = Color(0xFF10131C), // softer charcoal, not pure black-blue
    onBackground = Color(0xFFF1F1F3),
    surface = Color(0xFF1A1E2E),
    onSurface = Color(0xFFF1F1F3),
    surfaceVariant = Color(0xFF252A3D),
    onSurfaceVariant = Color(0xFFAEB2C7),
    outline = Color(0xFF2E3448)
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

// Glass card colors - natural muted
val GlassBg = Color(0x0FFFFFFF).copy(alpha = 0.06f)
val GlassBorder = Color(0x1AFFFFFF)
val GlassStrong = Color(0xE61A1E2E) // solid for bottom bar
