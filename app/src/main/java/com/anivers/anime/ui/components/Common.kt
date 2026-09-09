package com.anivers.anime.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anivers.anime.ui.theme.*

// ===== GLASS BACKGROUND =====
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030303))
    ) {
        // subtle gradient blob top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1FFFFDB89), Color.Transparent),
                        center = Offset(0.5f * 1200f, 0f),
                        radius = 700f
                    )
                )
        )
        // bottom blob
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x0A7C3AED), Color.Transparent),
                        center = Offset(0.5f * 1200f, 1200f),
                        radius = 600f
                    )
                )
        )
        // dark vertical gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF080A14).copy(alpha = 0.6f), Color.Transparent, Color(0xFF030303).copy(alpha = 0.9f))))
        )
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    borderColor: Color = GlassBorder,
    bgColor: Color = GlassBg,
    blurEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(bgColor, bgColor.copy(alpha = bgColor.alpha * 0.7f)))
            )
            .border(BorderStroke(1.dp, borderColor), shape)
            .then(if (blurEnabled) Modifier.blur(12.dp) else Modifier)
            .padding(14.dp),
        content = content
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    bgColor: Color = GlassBg,
    borderColor: Color = GlassBorder,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), shape),
        content = content
    )
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    fullWidth: Boolean = false
) {
    val shape = RoundedCornerShape(50.dp)
    val bg = if (isPrimary) Color(0xFFFFDB89) else GlassBg
    val contentColor = if (isPrimary) Color(0xFF030303) else Color.White
    val border = if (isPrimary) null else BorderStroke(1.dp, GlassBorder)
    Button(
        onClick = onClick,
        modifier = modifier.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = contentColor, disabledContainerColor = bg.copy(alpha = 0.5f)),
        border = border,
        shape = shape,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun GlassOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(1.dp, GlassBorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ===== BACK BUTTON GLASS =====
@Composable
fun GlassBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x66000000))
            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
            .clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            if (onBack != null) GlassBackButton(onBack = onBack)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

// ===== SHIMMER =====
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, corner: Dp = 12.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = EaseInOut), repeatMode = RepeatMode.Reverse),
        label = "shimmerAlpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(CardShimmer.copy(alpha = alpha))
    )
}

@Composable
fun ShimmerGridPlaceholder(modifier: Modifier = Modifier, columns: Int = 3) {
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(columns) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f), corner = 16.dp)
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp), corner = 6.dp)
                        ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(10.dp), corner = 6.dp)
                    }
                }
            }
        }
        // hero shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(160.dp), corner = 20.dp)
    }
}

@Composable
fun ShimmerListPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(5) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(modifier = Modifier.size(64.dp, 80.dp), corner = 12.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(11.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(10.dp))
                }
            }
        }
    }
}

// ===== STATES =====
@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            Text("Memuat...", color = TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, debugDetail: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0x1AFF5F5F)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color(0xFFFF5F5F), modifier = Modifier.size(28.dp)) }
        Text("Gagal memuat", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(message, color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
        if (!debugDetail.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x1AFF0000)).border(1.dp, Color(0x33FF0000), RoundedCornerShape(12.dp)).padding(10.dp)
            ) {
                Column {
                    Text("Detail:", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(debugDetail.take(500), color = Color(0xFFFCA5A5), fontSize = 10.sp, lineHeight = 12.sp)
                }
            }
        }
        GlassButton(text = "Coba Lagi", onClick = onRetry, icon = { Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) })
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit = { Icon(Icons.Filled.SearchOff, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp)) },
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(20.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0x14FFDB89)), contentAlignment = Alignment.Center) { icon() }
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(subtitle, color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            GlassButton(text = actionText, onClick = onAction)
        }
    }
}

@Composable
fun SectionHeaderGlass(
    title: String,
    subtitle: String? = null,
    onMore: (() -> Unit)? = null,
    moreText: String = "Lihat Semua"
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(50)).background(GoldPrimary))
                    Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 11.dp))
            }
            if (onMore != null) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(50)).clickable { onMore() }.padding(horizontal = 12.dp, vertical = 6.dp)
                ) { Text(moreText, color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
