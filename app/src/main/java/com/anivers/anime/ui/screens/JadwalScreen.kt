package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anivers.anime.data.model.JadwalDay
import com.anivers.anime.data.repository.AnimeRepository
import kotlinx.coroutines.launch

@Composable
fun JadwalScreen(onAnimeClick: (String) -> Unit) {
    var days by remember { mutableStateOf<List<JadwalDay>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var active by remember { mutableStateOf(0) }
    val repo = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            loading = true
            try { days = repo.getJadwal().data ?: emptyList() } catch (_: Exception) {}
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF030303)).verticalScroll(rememberScrollState()).padding(bottom = 100.dp).padding(top = 48.dp)) {
        Text("Jadwal Rilis", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        Text("Update mingguan", color = Color(0xFF8A8FA3), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
        if (loading) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFDB89)) }
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days.size) { idx ->
                    val d = days[idx]
                    val sel = idx == active
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) Color(0xFF242457) else Color(0x0DFFFFFF)).clickable { active = idx }.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text(d.day?.take(3) ?: "", color = if (sel) Color(0xFFA5B4FC) else Color(0xFF8A8FA3), fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
                }
            }
            Spacer(Modifier.height(12.dp))
            val list = days.getOrNull(active)?.animeList ?: emptyList()
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (a in list) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0DFFFFFF)).clickable { a.link?.let { onAnimeClick(it) } }.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = a.cover, contentDescription = a.animeName, modifier = Modifier.size(56.dp, 72.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(a.animeName ?: "", color = Color(0xFFE6E8EE), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(days[active].day ?: "", color = Color(0xFF8A8FA3), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
