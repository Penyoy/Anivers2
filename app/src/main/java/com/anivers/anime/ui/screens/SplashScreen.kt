package com.anivers.anime.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anivers.anime.ui.theme.GlassBorder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onLoggedIn: () -> Unit,
    onNotLoggedIn: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (startAnim) 1f else 0.86f, animationSpec = tween(800, easing = EaseOutBack), label = "scale")
    val alpha by animateFloatAsState(targetValue = if (startAnim) 1f else 0f, animationSpec = tween(700), label = "alpha")

    LaunchedEffect(Unit) {
        startAnim = true
        delay(1300)
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) onLoggedIn() else onNotLoggedIn()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF030303)),
        contentAlignment = Alignment.Center
    ) {
        // blobs
        Box(modifier = Modifier.size(420.dp).align(Alignment.TopCenter).offset(y = (-80).dp).background(Brush.radialGradient(listOf(Color(0x33FFDB89), Color.Transparent), radius = 400f)).blur(40.dp))
        Box(modifier = Modifier.size(360.dp).align(Alignment.BottomCenter).offset(y = 80.dp).background(Brush.radialGradient(listOf(Color(0x1A7C3AED), Color.Transparent), radius = 380f)).blur(30.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale).alpha(alpha)) {
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)).background(Color(0x14FFDB89)).border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = Color(0xFFFFDB89), fontSize = 42.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(16.dp))
            Text(text = "ANIVERS", color = Color(0xFFFFDB89), fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text(text = "ANIME", color = Color(0xFFFFDB89), fontSize = 30.sp, fontWeight = FontWeight.Light, letterSpacing = 8.sp)
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.width(48.dp).height(3.dp).clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89).copy(alpha = 0.7f)))
            Spacer(Modifier.height(16.dp))
            Text("Premium Streaming", color = Color(0xFF8A8FA3), fontSize = 11.sp, letterSpacing = 2.sp)
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).clip(RoundedCornerShape(50)).background(Color(0x0DFFFFFF)).border(1.dp, GlassBorder, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Memuat…", color = Color(0xFF8A8FA3), fontSize = 11.sp)
        }
    }
}
