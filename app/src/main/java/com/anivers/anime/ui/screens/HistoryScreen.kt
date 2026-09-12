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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.repository.BookmarkRepository
import com.anivers.anime.ui.components.EmptyState
import com.anivers.anime.ui.components.GlassBackground
import com.anivers.anime.ui.components.GlassTopBar
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun HistoryScreen(onWatchClick: (String, String) -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).historyDao() }
    val repo = remember { BookmarkRepository(context) }
    var history by remember { mutableStateOf(emptyList<com.anivers.anime.data.local.HistoryEntity>()) }
    // Recent seharusnya 1 row per slug (series+episode) dari watch_progress, bukan 50 row history per menit
    var progressList by remember { mutableStateOf(emptyList<com.anivers.anime.data.local.ProgressEntity>()) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            // Jika login, pull Firestore dulu biar recent dari cloud ke local
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    // Pull watchProgress (dedup) dengan cover/judul agar recent muncul dengan cover
                    try {
                        val snap = FirebaseFirestore.getInstance().collection("users").document(uid).collection("watchProgress").get().await()
                        for (doc in snap.documents) {
                            val data = doc.data ?: continue
                            val seriesUrl = (data["seriesUrl"] ?: data["animeId"] ?: "").toString()
                            val episode = (data["episode"] ?: data["episodeId"] ?: "").toString()
                            val judul = (data["title"] ?: data["judul"] ?: seriesUrl).toString()
                            val cover = (data["poster"] ?: data["cover"] ?: "").toString()
                            val pos = (data["position"] as? Number)?.toLong() ?: 0L
                            val dur = (data["duration"] as? Number)?.toLong() ?: 0L
                            val prog = (data["progress"] as? Number)?.toInt() ?: if (dur>0) ((pos.toDouble()/dur)*100).toInt() else 0
                            if (seriesUrl.isNotBlank() && episode.isNotBlank()) {
                                dao.upsertProgress(com.anivers.anime.data.local.ProgressEntity(key="$seriesUrl|$episode", seriesUrl=seriesUrl, episode=episode, currentTime=pos, duration=dur, progress=prog, updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(), judul=judul, cover=cover))
                            }
                        }
                    } catch (_: Exception) {}
                    // Pull history juga untuk cover/judul (tapi akan di-group jadi tidak 50 duplikat tampil)
                    try {
                        val hSnap = FirebaseFirestore.getInstance().collection("users").document(uid).collection("history").orderBy("watchedAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(50).get().await()
                        for (doc in hSnap.documents) {
                            val data = doc.data ?: continue
                            val seriesUrl = (data["seriesUrl"] ?: data["animeId"] ?: "").toString()
                            val episode = (data["episode"] ?: data["episodeId"] ?: "").toString()
                            val judul = (data["judul"] ?: data["title"] ?: seriesUrl).toString()
                            val cover = (data["cover"] ?: data["poster"] ?: "").toString()
                            val pos = (data["position"] as? Number)?.toLong() ?: (data["currentTime"] as? Number)?.toLong() ?: 0L
                            val dur = (data["duration"] as? Number)?.toLong() ?: 0L
                            val prog = if (dur>0) ((pos.toDouble()/dur)*100).toInt().coerceIn(0,100) else 0
                            val ts = (data["watchedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
                            if (seriesUrl.isNotBlank() && episode.isNotBlank()) {
                                // Cek existing biar tidak duplikat autoId terus
                                val existing = dao.getAll().find { it.seriesUrl==seriesUrl && it.episode==episode && kotlin.math.abs(it.timestamp - ts) < 60000 }
                                if (existing == null) dao.insert(com.anivers.anime.data.local.HistoryEntity(seriesUrl=seriesUrl, episode=episode, judul=judul, cover=cover, currentTime=pos, duration=dur, progress=prog, timestamp=ts))
                            }
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
            // Recent fix: pakai watch_progress (1 per slug) agar tidak 50 duplikat, dengan cover dari ProgressEntity
            val progresses = try { dao.getAllProgress().sortedByDescending { it.updatedAt } } catch (_: Exception) { emptyList() }
            if (progresses.isNotEmpty()) {
                progressList = progresses
                // Map progress -> HistoryEntity untuk UI (cover/judul dari ProgressEntity, terbaru di atas)
                history = progresses.map { p ->
                    com.anivers.anime.data.local.HistoryEntity(
                        seriesUrl = p.seriesUrl, episode = p.episode,
                        judul = p.judul.ifBlank { p.seriesUrl }, cover = p.cover,
                        currentTime = p.currentTime, duration = p.duration, progress = p.progress,
                        timestamp = p.updatedAt, completed = p.progress >= 90
                    )
                }
            } else {
                // Fallback: group history by slug biar tidak 50 duplikat untuk 1 anime, terbaru di atas
                val all = dao.getAll()
                val grouped = all.groupBy { it.seriesUrl to it.episode }.mapNotNull { (_, list) -> list.maxByOrNull { it.timestamp } }.sortedByDescending { it.timestamp }
                history = grouped
                progressList = emptyList()
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }
    // Auto-refresh saat data berubah - pakai progress dedup
    LaunchedEffect(dao) {
        // Observe progress jika ada, else history
        try {
            // Jika progress ada, history derived dari progress sudah di refresh
        } catch (_: Exception) {}
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
            GlassTopBar(
                title = "Recent",
                subtitle = "Continue watching & riwayat tontonan"
            )

            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("${history.size} tontonan", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            dao.clearAll()
                            history = emptyList()
                            repo.clearFirestoreHistory()
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hapus Semua", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (history.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EmptyState(
                        title = "Belum ada riwayat",
                        subtitle = "Tonton anime hingga progress tersimpan, akan muncul di sini. Login untuk sinkronisasi cloud.",
                        icon = { Icon(Icons.Filled.History, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp)) }
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(history) { h ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(GlassBg)
                                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                                .clickable { onWatchClick(h.seriesUrl, h.episode) }
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
                                AsyncImage(model = h.cover, contentDescription = h.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                // progress bar glass
                                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(Color(0x66000000))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((h.progress / 100f).coerceIn(0f, 1f)).background(GoldPrimary))
                                }
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(50)).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                    Text("${h.progress}%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                if (h.completed) Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(8.dp).clip(CircleShape).background(Color(0xFF22C55E)).border(1.dp, Color.White, CircleShape))
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(h.judul, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 15.sp)
                                Text("Episode ${h.episode}", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("${fmtHistory(h.currentTime)} / ${fmtHistory(h.duration)}", color = Color(0xFFB8B8B8), fontSize = 11.sp)
                                Text(formatDateHistory(h.timestamp), color = Color(0xFF6B7280), fontSize = 10.sp)
                                if (h.completed) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x1422C55E)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text("Selesai", color = Color(0xFF22C55E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // progress text
                                    LinearProgressIndicator(
                                        progress = { h.progress / 100f },
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
