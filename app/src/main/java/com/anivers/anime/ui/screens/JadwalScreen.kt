package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.anivers.anime.ui.components.*
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import kotlinx.coroutines.launch

@Composable
fun JadwalScreen(onAnimeClick: (String) -> Unit) {
    var days by remember { mutableStateOf<List<JadwalDay>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var active by remember { mutableIntStateOf(0) }
    val repo = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                days = repo.getJadwal().data ?: emptyList()
                if (days.isEmpty()) error = "Jadwal belum tersedia"
            } catch (e: Exception) {
                error = e.message ?: "Gagal memuat jadwal"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
            GlassTopBar(title = "Jadwal Rilis", subtitle = "Update anime mingguan • ${days.size} hari")

            if (loading) {
                Spacer(Modifier.height(12.dp))
                ShimmerListPlaceholder()
                return@Column
            }

            if (error != null && days.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                ErrorState(message = error!!, onRetry = { load() })
                return@Column
            }

            Spacer(Modifier.height(8.dp))

            // Day pills glass
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days.size) { idx ->
                    val d = days[idx]
                    val sel = idx == active
                    val label = d.day?.take(3) ?: "Day"
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) GoldPrimary else GlassBg)
                            .border(1.dp, if (sel) GoldPrimary else GlassBorder, RoundedCornerShape(16.dp))
                            .clickable { active = idx }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            label.replaceFirstChar { it.uppercase() },
                            color = if (sel) Color(0xFF030303) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(d.date?.take(5) ?: "", color = if (sel) Color(0xFF030303).copy(alpha = 0.7f) else Color(0xFF8A8FA3), fontSize = 10.sp)
                        if (sel) {
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF030303)))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val list = days.getOrNull(active)?.animeList ?: emptyList()
            val dayName = days.getOrNull(active)?.day ?: ""

            Row(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(dayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${list.size} anime", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))

            if (list.isEmpty()) {
                EmptyState(title = "Tidak ada jadwal", subtitle = "Tidak ada anime untuk hari $dayName", actionText = "Muat Ulang", onAction = { load() })
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(list) { a ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassBg)
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .clickable { a.link?.let { onAnimeClick(it) } }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(64.dp, 84.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF14141A))) {
                                AsyncImage(model = a.cover, contentDescription = a.animeName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(a.animeName ?: "", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 15.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x14FFDB89)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text(dayName.take(3), color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Update terbaru", color = Color(0xFF8A8FA3), fontSize = 11.sp)
                                }
                            }
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(GoldPrimary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color(0xFF030303), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
