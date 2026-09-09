package com.anivers.anime.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.repository.BookmarkRepository
import com.anivers.anime.ui.components.*
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import com.anivers.anime.viewmodel.DetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    slug: String,
    onEpisodeClick: (String, String) -> Unit,
    onGenreClick: (String) -> Unit,
    onBack: () -> Unit,
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
    var synopsisExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(slug) { vm.load(slug) }
    LaunchedEffect(detail) {
        detail?.let {
            val id = it.id?.toString() ?: slug
            isBookmarked = repo.isBookmarked(id, slug)
        }
    }

    GlassBackground {
        when {
            loading -> Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator(color = GoldPrimary)
                    Text("Memuat detail...", color = Color(0xFF8A8FA3), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) { ShimmerBox(modifier = Modifier.size(100.dp, 140.dp), corner = 16.dp) }
                    }
                }
            }
            error != null && detail == null -> Column(
                Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ErrorState(message = error ?: "Gagal memuat detail", onRetry = { vm.load(slug) })
                Spacer(Modifier.height(12.dp))
                GlassButton(text = "Kembali", onClick = onBack, isPrimary = false)
            }
            detail != null -> {
                val d = detail!!
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 96.dp)
                ) {
                    // Hero with parallax glass
                    val parallax = (scrollState.value * 0.32f).coerceIn(0f, 80f)
                    Box(
                        modifier = Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    ) {
                        AsyncImage(
                            model = d.cover,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = parallax }.blur(22.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.38f
                        )
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xFF030303)))))
                        // extra radial glow
                        Box(Modifier.fillMaxWidth().height(200.dp).align(Alignment.TopCenter).background(Brush.radialGradient(listOf(Color(0x22FFDB89), Color.Transparent), radius = 600f)))

                        // Back + actions top bar glass
                        Row(
                            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Color(0x1AFFFFFF), CircleShape).clickable { onBack() },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White, modifier = Modifier.size(20.dp)) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(CircleShape).background(if (isBookmarked) GoldPrimary else Color(0x66000000)).border(1.dp, Color(0x1AFFFFFF), CircleShape).clickable {
                                        scope.launch {
                                            val id = d.id?.toString() ?: slug
                                            val url = d.seriesId ?: slug
                                            val added = repo.toggleBookmark(id, url, d.judul ?: slug, d.cover ?: "", d.status ?: "", d.type ?: "")
                                            isBookmarked = added
                                        }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = null, tint = if (isBookmarked) Color(0xFF030303) else Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Cover + info glass
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 84.dp, bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(modifier = Modifier.width(138.dp).height(190.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))) {
                                AsyncImage(model = d.cover, contentDescription = d.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                // score badge
                                if (!d.rating.isNullOrEmpty()) {
                                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(50)).background(GoldPrimary).padding(horizontal = 7.dp, vertical = 3.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF030303), modifier = Modifier.size(12.dp))
                                            Text(d.rating, color = Color(0xFF030303), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(d.judul ?: slug, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp, maxLines = 3)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (!d.type.isNullOrEmpty()) Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(d.type, color = Color.White, fontSize = 11.sp)
                                    }
                                    if (!d.status.isNullOrEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (d.status == "Ongoing") Color(0xFF22C55E) else Color(0xFFEF4444)))
                                        Text(d.status, color = Color(0xFFD1D5DB), fontSize = 11.sp)
                                    }
                                }
                                // meta row
                                if (!d.published.isNullOrEmpty()) Text(d.published, color = Color(0xFF8A8FA3), fontSize = 11.sp, maxLines = 1)
                                // genre glass chips
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                                    for ((idx, g) in (d.genre ?: emptyList()).take(3).withIndex()) {
                                        val slugG = d.genreurl?.getOrNull(idx) ?: g.lowercase()
                                        Box(Modifier.clip(RoundedCornerShape(50)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(50)).clickable { onGenreClick(slugG) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(g, color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Synopsis glass card
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 16.dp).clip(RoundedCornerShape(20.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(16.dp).animateContentSize()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(50)).background(GoldPrimary))
                            Text("Sinopsis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (!d.published.isNullOrEmpty()) Text("• ${d.published}", color = Color(0xFF8A8FA3), fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        if (!d.sinopsis.isNullOrBlank()) {
                            Text(text = d.sinopsis ?: "", color = Color(0xFFC7CAD6), fontSize = 13.sp, lineHeight = 18.sp, maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            Text(text = if (synopsisExpanded) "Ciutkan ▲" else "Selengkapnya ▼", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded })
                        } else {
                            Text("Sinopsis tidak tersedia.", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val first = d.chapter?.sortedWith(compareBy { if (it.ch == "Movie") -1 else it.ch?.toIntOrNull() ?: 9999 })?.firstOrNull()
                                    if (first != null) onEpisodeClick(d.seriesId ?: slug, first.url ?: "")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF030303)),
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
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
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isBookmarked) GoldPrimary else Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isBookmarked) GoldPrimary else GlassBorder),
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isBookmarked) "Tersimpan" else "Simpan", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Episode list glass
                    if (!d.chapter.isNullOrEmpty()) {
                        val sorted = d.chapter.sortedWith(compareBy { if (it.ch == "Movie") -1 else it.ch?.toIntOrNull() ?: 9999 })
                        val chunkSize = 12
                        val chunks = sorted.chunked(chunkSize)
                        var selectedChunk by remember { mutableIntStateOf(0) }

                        Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(50)).background(GoldPrimary))
                                Text("Episode ${sorted.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            if (d.status == "Ongoing") Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x1422C55E)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("Ongoing", color = Color(0xFF22C55E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        if (chunks.size > 1) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(chunks.size) { idx ->
                                    val sel = idx == selectedChunk
                                    Box(
                                        Modifier.clip(RoundedCornerShape(50)).background(if (sel) GoldPrimary else GlassBg).border(1.dp, if (sel) GoldPrimary else GlassBorder, RoundedCornerShape(50)).clickable { selectedChunk = idx }.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("${idx * chunkSize + 1}-${minOf((idx + 1) * chunkSize, sorted.size)}", color = if (sel) Color(0xFF030303) else Color(0xFFB8B8B8), fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        val visible = chunks.getOrNull(selectedChunk) ?: sorted
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (ep in visible) {
                                var watched by remember { mutableStateOf(false) }
                                LaunchedEffect(ep.url) {
                                    val prog = AppDatabase.get(context).historyDao().getProgress(d.seriesId ?: slug, ep.url ?: "")
                                    watched = prog != null && prog.progress > 5
                                }
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassBg).border(1.dp, if (watched) Color(0x33FFDB89) else GlassBorder, RoundedCornerShape(16.dp)).clickable { onEpisodeClick(d.seriesId ?: slug, ep.url ?: "") }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (watched) Color(0x33FFDB89) else Color(0xFF1E1E24)).border(1.dp, if (watched) Color(0x33FFDB89) else GlassBorder, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(ep.ch ?: "?", color = if (watched) GoldPrimary else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Episode ${ep.ch}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(ep.date ?: "", color = Color(0xFF8A8FA3), fontSize = 11.sp)
                                        if (watched) Text("Sudah ditonton", color = GoldPrimary, fontSize = 10.sp)
                                    }
                                    if (watched) Box(Modifier.size(8.dp).clip(CircleShape).background(GoldPrimary))
                                    Box(Modifier.size(32.dp).clip(CircleShape).background(if (watched) GoldPrimary else Color(0x14FFFFFF)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = if (watched) Color(0xFF030303) else Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Episode belum tersedia", color = Color(0xFF8A8FA3), fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
