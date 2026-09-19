package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anivers.anime.data.local.ProgressEntity
import com.anivers.anime.data.repository.FirestoreRepository
import com.anivers.anime.ui.components.EmptyState
import com.anivers.anime.ui.components.GlassBackground
import com.anivers.anime.ui.components.GlassTopBar
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(onWatchClick: (String, String) -> Unit) {
    val context = LocalContext.current
    val fsRepo = remember { FirestoreRepository(context) }
    val scope = rememberCoroutineScope()

    // Firestore primary: watchProgress per episode, grouped to 1 per anime
    val rawProgress by fsRepo.watchProgressFlow().collectAsState(initial = emptyList())

    // Group by seriesUrl → 1 row per anime with latest episode
    val groupedAnime = remember(rawProgress) {
        rawProgress
            .filter { it.seriesUrl.isNotBlank() }
            .groupBy { it.seriesUrl }
            .map { (_, episodes) ->
                episodes.maxByOrNull { it.updatedAt } ?: episodes.first()
            }
            .sortedByDescending { it.updatedAt }
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
            GlassTopBar(
                title = "Recent",
                subtitle = "${groupedAnime.size} anime ditonton"
            )

            if (groupedAnime.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("${groupedAnime.size} anime", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            fsRepo.clearAllProgress()
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hapus Semua", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (groupedAnime.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EmptyState(
                        title = "Belum ada riwayat",
                        subtitle = "Tonton anime hingga progress tersimpan, akan muncul di sini. Login untuk sinkronisasi cloud.",
                        icon = { Icon(Icons.Filled.History, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp)) }
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(groupedAnime) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(GlassBg)
                                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                                .clickable { onWatchClick(p.seriesUrl, p.episode) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(78.dp, 104.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF14141A))
                            ) {
                                AsyncImage(model = p.cover, contentDescription = p.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(Color(0x66000000))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((p.progress / 100f).coerceIn(0f, 1f)).background(GoldPrimary))
                                }
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(50)).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                    Text("${p.progress}%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                if (p.progress >= 90) Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(8.dp).clip(CircleShape).background(Color(0xFF22C55E)).border(1.dp, Color.White, CircleShape))
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(p.judul.ifBlank { p.seriesUrl }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 15.sp)
                                Text("Episode ${p.episode}", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("${fmtHistory(p.currentTime)} / ${fmtHistory(p.duration)}", color = Color(0xFFB8B8B8), fontSize = 11.sp)
                                Text(formatDateHistory(p.updatedAt), color = Color(0xFF6B7280), fontSize = 10.sp)
                                if (p.progress >= 90) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x1422C55E)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text("Selesai", color = Color(0xFF22C55E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    LinearProgressIndicator(
                                        progress = { p.progress / 100f },
                                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)),
                                        color = GoldPrimary,
                                        trackColor = Color(0x33FFFFFF)
                                    )
                                }
                            }
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(GoldPrimary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF030303), modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

private fun fmtHistory(s: Long): String {
    if (s <= 0) return "00:00"
    val sec = (s % 60).toString().padStart(2, '0')
    val min = ((s / 60) % 60).toString().padStart(2, '0')
    val hr = s / 3600
    return if (hr > 0) "$hr:$min:$sec" else "$min:$sec"
}

private fun formatDateHistory(ts: Long): String {
    return try {
        java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(ts))
    } catch (_: Exception) { "" }
}
