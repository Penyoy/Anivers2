package com.anivers.anime.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0DFFFFFF))
            .padding(12.dp),
        content = content
    )
}

@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Color(0xFFFFDB89), modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        Text("Memuat...", color = Color(0xFF8A8FA3), fontSize = 13.sp)
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1E).copy(alpha = alpha))
    )
}

@Composable
fun ShimmerGridPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 3 columns grid shimmer - 2 rows
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f))
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp))
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, debugDetail: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Gagal memuat", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Color(0xFF8A8FA3), fontSize = 12.sp)
        if (!debugDetail.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1AFF0000))
                    .padding(10.dp)
            ) {
                Column {
                    Text("Detail:", color = Color(0xFFFCA5A5), fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(debugDetail.take(600), color = Color(0xFFFCA5A5), fontSize = 10.sp, lineHeight = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89))) {
            Text("Coba Lagi", color = Color(0xFF030303))
        }
    }
}
