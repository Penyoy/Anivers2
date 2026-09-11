package com.anivers.anime.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anivers.anime.ui.theme.GlassBorder

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF030303))) {
        Box(modifier = Modifier.size(500.dp).align(Alignment.TopCenter).offset(y = (-120).dp).background(Brush.radialGradient(listOf(Color(0x33FFDB89), Color.Transparent), radius = 500f)).blur(40.dp))
        Box(modifier = Modifier.size(420.dp).align(Alignment.BottomEnd).offset(x = 80.dp, y = 80.dp).background(Brush.radialGradient(listOf(Color(0x1A7C3AED), Color.Transparent), radius = 420f)).blur(30.dp))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 64.dp, bottom = 32.dp).statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(28.dp)).background(Color(0x0DFFFFFF)).border(1.dp, GlassBorder, RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x14FFDB89), Color.Transparent))))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = com.anivers.anime.R.drawable.ic_launcher),
                            contentDescription = "Anivers Logo",
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Anivers", color = Color(0xFFFFDB89), fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.width(32.dp).height(2.dp).clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89)))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Streaming Anime\nElegant & HD", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 28.sp)
                Spacer(Modifier.height(12.dp))
                Text("Nonton ribuan anime subtitle Indonesia\nkualitas HD, pengalaman glass modern.", color = Color(0xFFB8B8B8), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89), contentColor = Color(0xFF030303)),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Masuk / Daftar", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                OutlinedButton(
                    onClick = onGuestClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Masuk sebagai Tamu", fontSize = 14.sp, color = Color(0xFFE6E8EE)) }
                Text("Dengan melanjutkan, kamu setuju dengan Syarat & Privasi.", color = Color(0xFF5A5A6A), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }
}
