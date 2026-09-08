package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.repository.BookmarkRepository
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
    var firestoreHistory by remember { mutableStateOf(emptyList<Map<String, Any>>()) }
    val scope = rememberCoroutineScope()
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

    LaunchedEffect(Unit) {
        dao.getAll().let { history = it.sortedByDescending { h -> h.timestamp } }
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val snap = FirebaseFirestore.getInstance().collection("users").document(uid)
                    .collection("history")
                    .orderBy("watchedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(50).get().await()
                firestoreHistory = snap.documents.mapNotNull { it.data }
            }
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303))
            .padding(bottom = 100.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = Color(0xFFFFDB89))
                Text(
                    "History",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (history.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x14FFDB89))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${history.size} tontonan",
                            color = Color(0xFFFFDB89),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Text(
                "Continue Watching & Recently Watched",
                color = Color(0xFFB8B8B8),
                fontSize = 12.sp
            )
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                dao.clearAll()
                                history = emptyList()
                                repo.clearFirestoreHistory()
                            }
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5F5F))
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hapus Semua", fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (history.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x14FFDB89)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = Color(0xFFFFDB89),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Belum ada riwayat",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tonton anime hingga progress tersimpan, akan muncul di sini.",
                    color = Color(0xFFB8B8B8),
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (!isLoggedIn) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Login untuk sinkronisasi Firestore",
                        color = Color(0xFFFFDB89),
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { h ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1C1C1E))
                            .clickable { onWatchClick(h.seriesUrl, h.episode) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cover with progress bar
                        Box(
                            modifier = Modifier
                                .size(72.dp, 92.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = h.cover,
                                contentDescription = h.judul,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Progress bar at bottom
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(Color(0x66000000))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((h.progress / 100f).coerceIn(0f, 1f))
                                        .background(Color(0xFFFFDB89))
                                )
                            }
                            // Progress percentage
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x99000000))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("${h.progress}%", color = Color.White, fontSize = 9.sp)
                            }
                        }
                        // Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                h.judul,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2
                            )
                            Text(
                                "Episode ${h.episode} • ${fmtHistory(h.currentTime)} / ${fmtHistory(h.duration)}",
                                color = Color(0xFFFFDB89),
                                fontSize = 11.sp
                            )
                            Text(
                                "${formatDateHistory(h.timestamp)} • sisa ${fmtHistory((h.duration - h.currentTime).coerceAtLeast(0))}",
                                color = Color(0xFFB8B8B8),
                                fontSize = 11.sp
                            )
                            if (h.completed) {
                                Text("Selesai", color = Color(0xFF8FD694), fontSize = 11.sp)
                            }
                        }
                        // Play button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFFDB89)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF030303),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
