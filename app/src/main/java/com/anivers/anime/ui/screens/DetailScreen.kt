package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.repository.BookmarkRepository
import com.anivers.anime.ui.components.LoadingSkeleton
import com.anivers.anime.viewmodel.DetailViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    slug: String,
    onEpisodeClick: (String, String) -> Unit,
    onGenreClick: (String) -> Unit,
    vm: DetailViewModel = viewModel()
) {
    val detail by vm.detail.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBookmarked by remember { mutableStateOf(false) }
    val repo = remember { BookmarkRepository(context) }
    val scrollState = rememberScrollState()

    LaunchedEffect(slug) { vm.load(slug) }
    LaunchedEffect(detail) {
        detail?.let {
            val id = it.id?.toString() ?: slug
            isBookmarked = repo.isBookmarked(id, slug)
        }
    }

    when {
        loading -> Box(Modifier.fillMaxSize().background(Color(0xFF030303)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(color = Color(0xFFFFDB89))
                // warm shimmer placeholder beda Wibuku dingin
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { Box(Modifier.size(100.dp, 140.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1C1C1E))) }
                }
            }
        }
        error != null && detail == null -> Column(
            Modifier.fillMaxSize().background(Color(0xFF030303)).padding(24.dp).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Gagal memuat detail", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(error ?: "", color = Color(0xFFB8B8B8), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.load(slug) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89), contentColor = Color(0xFF030303)), shape = RoundedCornerShape(12.dp)) { Text("Coba Lagi", fontWeight = FontWeight.Bold) }
        }
        detail != null -> {
            val d = detail!!
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF030303)).verticalScroll(scrollState).padding(bottom = 100.dp)
            ) {
                // Cinematic hero - tinggi 380, tumpul 24dp bawah, parallax tipis (beda Wibuku full 0dp)
                val parallax = (scrollState.value * 0.3f).coerceIn(0f, 60f)
                Box(modifier = Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))) {
                    AsyncImage(
                        model = d.cover, contentDescription = null,
                        modifier = Modifier.fillMaxSize().graphicsLayer { translationY = parallax }.blur(18.dp),
                        contentScale = ContentScale.Crop, alpha = 0.35f
                    )
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x00030303), Color(0xFF030303)))))
                    // cover + info
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 52.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        AsyncImage(
                            model = d.cover, contentDescription = d.judul,
                            modifier = Modifier.width(130.dp).height(180.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.judul ?: slug, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp, maxLines = 3)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!d.rating.isNullOrEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFDB89)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Icon(Icons.Filled.Star, null, tint = Color(0xFF030303), modifier = Modifier.size(12.dp))
                                            Text(d.rating, color = Color(0xFF030303), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (!d.type.isNullOrEmpty()) Text(d.type, color = Color(0xFFB8B8B8), fontSize = 11.sp)
                                if (!d.status.isNullOrEmpty()) {
                                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(if (d.status == "Ongoing") Color(0xFF8FD694) else Color(0xFFFF5F5F)))
                                    Text(d.status, color = Color(0xFFB8B8B8), fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // genre chips bulat beda Wibuku Flexbox kotak
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                                for ((idx, g) in (d.genre ?: emptyList()).take(3).withIndex()) {
                                    val slugG = d.genreurl?.getOrNull(idx) ?: g.lowercase()
                                    Box(
                                        Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF2C2C2E)).clickable { onGenreClick(slugG) }.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) { Text(g, color = Color(0xFFFFDB89), fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                                }
                            }
                        }
                    }
                }

                // Info card sinopsis + actions
                Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF1C1C1E)).padding(16.dp)) {
                    Text(d.sinopsis ?: "Sinopsis tidak tersedia.", color = Color(0xFFB8B8B8), fontSize = 12.sp, lineHeight = 17.sp, maxLines = 5)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val first = d.chapter?.sortedWith(compareBy { if (it.ch == "Movie") -1 else it.ch?.toIntOrNull() ?: 9999 })?.firstOrNull()
                                if (first != null) onEpisodeClick(d.seriesId ?: slug, first.url ?: "")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89), contentColor = Color(0xFF030303)),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mulai Nonton", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val id = d.id?.toString() ?: slug
                                    val url = d.seriesId ?: slug
                                    val added = repo.toggleBookmark(id, url, d.judul ?: slug, d.cover ?: "", d.status ?: "", d.type ?: "")
                                    isBookmarked = added
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isBookmarked) Color(0xFFFFDB89) else Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isBookmarked) Color(0xFFFFDB89) else Color(0xFF3A3A3C)),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isBookmarked) "Tersimpan" else "Simpan", fontSize = 12.sp)
                        }
                    }
                }

                // Episode range chips - inspirasi Wibuku sticky_range_selector tapi 12 per page, gold pill
                if (!d.chapter.isNullOrEmpty()) {
                    val sorted = d.chapter.sortedWith(compareBy { if (it.ch == "Movie") -1 else it.ch?.toIntOrNull() ?: 9999 })
                    val chunkSize = 12
                    val chunks = sorted.chunked(chunkSize)
                    var selectedChunk by remember { mutableIntStateOf(0) }
                    Text("Episode ${sorted.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(8.dp))
                    if (chunks.size > 1) {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(chunks.size) { idx ->
                                val sel = idx == selectedChunk
                                Box(
                                    Modifier.clip(RoundedCornerShape(50)).background(if (sel) Color(0xFFFFDB89) else Color(0xFF1C1C1E)).clickable { selectedChunk = idx }.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) { Text("${idx*chunkSize+1}-${minOf((idx+1)*chunkSize, sorted.size)}", color = if (sel) Color(0xFF030303) else Color(0xFFB8B8B8), fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    val visible = chunks.getOrNull(selectedChunk) ?: sorted
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (ep in visible) {
                            // history progress check via Room (cached)
                            var watched by remember { mutableStateOf(false) }
                            LaunchedEffect(ep.url) {
                                val prog = AppDatabase.get(context).historyDao().getProgress(d.seriesId ?: slug, ep.url ?: "")
                                watched = prog != null && prog.progress > 5
                            }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF1C1C1E)).clickable { onEpisodeClick(d.seriesId ?: slug, ep.url ?: "") }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (watched) Color(0xFFFFDB89).copy(alpha=0.2f) else Color(0xFF2C2C2E)), contentAlignment = Alignment.Center) {
                                    Text(ep.ch ?: "?", color = if (watched) Color(0xFFFFDB89) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Episode ${ep.ch}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(ep.date ?: "", color = Color(0xFFB8B8B8), fontSize = 11.sp)
                                }
                                if (watched) Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89)))
                                Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFFB8B8B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Episode belum tersedia", color = Color(0xFFB8B8B8), fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
