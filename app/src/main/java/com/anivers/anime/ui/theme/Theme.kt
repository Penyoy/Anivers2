package com.anivers.anime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.anivers.anime.R

// === DARK PREMIUM PALETTE ===
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFDB89),
    onPrimary = Color(0xFF030303),
    primaryContainer = Color(0xFFD8B76D),
    onPrimaryContainer = Color(0xFF030303),
    secondary = Color(0xFFD8B76D),
    onSecondary = Color(0xFF030303),
    background = Color(0xFF030303),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121214),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFFB8B8B8),
    surfaceContainer = Color(0xFF1A1A1E),
    outline = Color(0x1AFFFFFF),
    outlineVariant = Color(0x0DFFFFFF),
    error = Color(0xFFFF5F5F),
    onError = Color(0xFFFFFFFF),
    scrim = Color(0x99000000)
)

@Composable
fun AniVerseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(
            titleLarge = Typography().titleLarge.copy(
                fontFamily = BestyFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = Color(0xFFFFDB89)
            ),
            titleMedium = Typography().titleMedium.copy(
                fontFamily = BestyFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFDB89)
            ),
            titleSmall = Typography().titleSmall.copy(
                fontFamily = BestyFontFamily,
                color = Color(0xFFF8FAFC)
            ),
            bodyMedium = Typography().bodyMedium.copy(
                color = Color(0xFFAEB2C7),
                lineHeight = 18.sp
            ),
            labelSmall = Typography().labelSmall.copy(
                color = Color(0xFF8A8FA3),
                letterSpacing = 0.3.sp
            ),
            labelMedium = Typography().labelMedium.copy(
                fontFamily = BestyFontFamily,
                color = Color(0xFFFFDB89)
            )
        ),
        content = content
    )
}

// === CORE PALETTE ===
val GoldPrimary = Color(0xFFFFDB89)
val GoldVariant = Color(0xFFD8B76D)
val GoldGlow = Color(0x33FFDB89)
val BgBlack = Color(0xFF030303)
val BgNavy = Color(0xFF0A0F1E)
val BgDeep = Color(0xFF080A12)
val SurfaceGray = Color(0xFF1A1A1E)
val SurfaceDark = Color(0xFF121214)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB8B8B8)
val TextMuted = Color(0xFF8A8FA3)
val DividerGray = Color(0xFF3A3A3C)
val ErrorRed = Color(0xFFFF5F5F)
val SuccessGreen = Color(0xFF8FD694)

// === GLASS TOKENS ===
val GlassBg = Color(0x14FFFFFF)              // translucent white 8%
val GlassBgStrong = Color(0x1FFFFFFF)        // 12% white
val GlassBorder = Color(0x1AFFFFFF)          // 10% white border
val GlassBorderStrong = Color(0x26FFFFFF)    // 15% white
val GlassHighlight = Color(0x0FFFFFFF)       // subtle highlight top
val GlassDark = Color(0xCC030303)            // 80% black for sheets
val GlassBlurOverlay = Color(0x99121414)     // for fallback blur

// gradients
val BgGradient = Brush.verticalGradient(
    listOf(Color(0xFF080A14), Color(0xFF030303), Color(0xFF0D0D12))
)
val BgGradientRadial = Brush.radialGradient(
    colors = listOf(Color(0x1AFFDB89), Color.Transparent, Color(0x0A7C3AED)),
    radius = 800f
)
val GlassGradient = Brush.linearGradient(
    listOf(Color(0x14FFFFFF), Color(0x08FFFFFF))
)
val CardShimmer = Color(0xFF1E1E22)
val CardShimmerHighlight = Color(0xFF2A2A32)

// glow
val GoldShadow = Color(0x40000000)
val GlassShadow = Color(0x66000000)

val BestyFontFamily = FontFamily(Font(R.font.besty))
