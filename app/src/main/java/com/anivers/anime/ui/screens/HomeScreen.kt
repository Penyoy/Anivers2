package com.anivers.anime.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.model.Anime
import com.anivers.anime.ui.components.*
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import com.anivers.anime.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateSearch: () -> Unit,
    onSearchQuery: (String) -> Unit,
    onAnimeClick: (String) -> Unit,
    onMoreClick: (String) -> Unit,
    onGenreClick: (String) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val history by AppDatabase.get(context).historyDao().getRecentFlow(6).collectAsState(initial = emptyList())
    val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Untukmu", "Jadwal", "Terpopuler")
    val scrollState = rememberScrollState()

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp)
                .statusBarsPadding()
        ) {
            // === HEADER GLASS ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E24))
                        .border(1.dp, Color(0x33FFDB89), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (authUser?.photoUrl != null) {
                        AsyncImage(
                            model = authUser.photoUrl.toString(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            (authUser?.displayName?.take(1) ?: "A").uppercase(),
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Selamat datang,", color = Color(0xFF8A8FA3), fontSize = 11.sp)
                    Text(
                        authUser?.displayName ?: "Anivers User",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary)
                        .clickable { onNavigateSearch() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color(0xFF030303), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // === TAB PILLS GLASS ===
            Row(
                modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(50)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(50)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { idx, title ->
                    val selected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) GoldPrimary else Color.Transparent)
                            .clickable { selectedTab = idx }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Text(
                            title,
                            color = if (selected) Color(0xFF030303) else Color(0xFFB8B8B8),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Loading / Error
            if (state.loading) {
                Spacer(Modifier.height(16.dp))
                ShimmerGridPlaceholder()
                return@Column
            }
            if (state.error != null && state.ongoing.isEmpty() && state.baruUpload.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                ErrorState(message = state.error!!, onRetry = { vm.load() }, debugDetail = state.debugDetail)
                return@Column
            }

            Spacer(Modifier.height(16.dp))

            // Tab content with fade slide - fix tumpuk dengan SizeTransform clip
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(280)) + slideInVertically(animationSpec = tween(280)) { it / 8 } togetherWith
                            fadeOut(animationSpec = tween(220))).using(SizeTransform(clip = true))
                },
                label = "homeTab",
                modifier = Modifier.fillMaxWidth()
            ) { tab ->
                when (tab) {
                    0 -> UntukmuTabGlass(state, history, onAnimeClick, onMoreClick, onGenreClick)
                    1 -> JadwalTabGlass(state, onAnimeClick)
                    2 -> TerpopulerTabGlass(state, onAnimeClick, onMoreClick)
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun UntukmuTabGlass(
    state: com.anivers.anime.viewmodel.HomeUiState,
    history: List<com.anivers.anime.data.local.HistoryEntity>,
    onAnimeClick: (String) -> Unit,
    onMoreClick: (String) -> Unit,
    onGenreClick: (String) -> Unit
) {
    val featured = (state.topAnime + state.ongoing).distinctBy { it.url }.take(5)

    if (featured.isNotEmpty()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(featured.size) { idx ->
                val a = featured[idx]
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(182.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF14141A))
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                        .clickable { onAnimeClick(a.url) }
                ) {
                    AsyncImage(model = a.cover, contentDescription = a.judul, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xE6030303)))))
                    // top badge
                    Row(modifier = Modifier.align(Alignment.TopStart).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GoldPrimary).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("FEATURED", color = Color(0xFF030303), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        if (a.score.isNotBlank()) Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xCC000000)).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("★ ${a.score}", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(modifier = Modifier.align(Alignment.Center).size(56.dp).clip(CircleShape).background(Color(0x99FFDB89)).border(1.dp, Color(0x33FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF030303), modifier = Modifier.size(30.dp))
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Text(a.judul, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(a.genre.take(3).joinToString(" • ").ifEmpty { a.status.ifEmpty { "Trending now" } }, color = Color(0xFFD0D0D8), fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }

    if (history.isNotEmpty()) {
        SectionHeaderGlass(title = "Lanjutkan Nonton", subtitle = "${history.size} progress tersimpan")
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(history.size) { i ->
                val h = history[i]
                Column(
                    Modifier
                        .width(132.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassBg)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable { /* keep history click via detail not watch directly */ }
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp))) {
                        AsyncImage(model = h.cover, contentDescription = h.judul, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Color(0x33000000))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((h.progress / 100f).coerceIn(0f, 1f)).background(GoldPrimary))
                        }
                        Box(Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(50)).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("${h.progress}%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(h.judul, color = Color.White, fontSize = 11.sp, maxLines = 2, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Ep ${h.episode}", color = GoldPrimary, fontSize = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (state.ongoing.isNotEmpty()) {
        SectionHeaderGlass(title = "Trending Minggu Ini", subtitle = "Paling banyak ditonton", onMore = { onMoreClick("ongoing") })
        AnimeGridSectionGlass(animes = state.ongoing.take(6), onAnimeClick = onAnimeClick)
        Spacer(Modifier.height(6.dp))
    }

    if (state.baruUpload.isNotEmpty()) {
        SectionHeaderGlass(title = "Baru Rilis", subtitle = "Update terbaru hari ini", onMore = { onMoreClick("baruupload") })
        AnimeGridSectionGlass(animes = state.baruUpload.take(6), onAnimeClick = onAnimeClick)
        Spacer(Modifier.height(6.dp))
    }

    SectionHeaderGlass(title = "Jelajahi Genre", subtitle = "Temukan sesuai mood")
    val genres = listOf("action", "adventure", "comedy", "drama", "fantasy", "horror", "mystery", "romance", "sci-fi", "slice-of-life", "supernatural", "sports")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(genres.size) { i ->
            val g = genres[i]
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GlassBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(50))
                    .clickable { onGenreClick(g) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(g.replace("-", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }, color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (state.rekomendasi.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        SectionHeaderGlass(title = "Hot Anime", subtitle = "Rekomendasi premium", onMore = { onMoreClick("rekomendasi") })
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for ((idx, a) in state.rekomendasi.take(2).withIndex()) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF14141A))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .clickable { onAnimeClick(a.url) }
                ) {
                    AsyncImage(model = a.cover, contentDescription = a.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC030303)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(50)).background(GoldPrimary).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("#${idx + 1} Top", color = Color(0xFF030303), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(a.judul, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun JadwalTabGlass(
    state: com.anivers.anime.viewmodel.HomeUiState,
    onAnimeClick: (String) -> Unit
) {
    if (state.jadwal.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            EmptyState(title = "Jadwal belum tersedia", subtitle = "Coba refresh halaman")
        }
        return
    }
    var activeDay by remember { mutableIntStateOf(0) }
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.jadwal.take(7).size) { i ->
            val day = state.jadwal[i]
            val sel = i == activeDay
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (sel) GoldPrimary else GlassBg)
                    .border(1.dp, if (sel) GoldPrimary else GlassBorder, RoundedCornerShape(50))
                    .clickable { activeDay = i }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.day ?: "", color = if (sel) Color(0xFF030303) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(day.date ?: "", color = if (sel) Color(0xFF030303).copy(alpha = 0.7f) else Color(0xFFB8B8B8), fontSize = 10.sp)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    val list = state.jadwal.getOrNull(activeDay)?.animeList ?: emptyList()
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (a in list) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable { a.link?.let { onAnimeClick(it) } }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(model = a.cover, contentDescription = a.animeName, modifier = Modifier.size(56.dp, 76.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(a.animeName ?: "", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 15.sp)
                    Text("Update • ${state.jadwal.getOrNull(activeDay)?.day ?: ""}", color = GoldPrimary, fontSize = 11.sp)
                }
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0x14FFDB89)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(16.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                Text("Tidak ada jadwal hari ini", color = Color(0xFFB8B8B8), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TerpopulerTabGlass(
    state: com.anivers.anime.viewmodel.HomeUiState,
    onAnimeClick: (String) -> Unit,
    onMoreClick: (String) -> Unit
) {
    if (state.topAnime.isNotEmpty()) {
        SectionHeaderGlass(title = "Top 10 Anime", subtitle = "Rating tertinggi", onMore = { onMoreClick("rekomendasi") })
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(minOf(state.topAnime.size, 10)) { idx ->
                val a = state.topAnime[idx]
                Column(Modifier.width(136.dp).clickable { onAnimeClick(a.url) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF14141A))
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(model = a.cover, contentDescription = a.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000)))))
                        Box(Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(50)).background(GoldPrimary).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("#${idx + 1}", color = Color(0xFF030303), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (a.score.isNotBlank()) {
                            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xCC000000)).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                Text("★ ${a.score}", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(a.judul, color = Color.White, fontSize = 12.sp, maxLines = 2, fontWeight = FontWeight.Medium, lineHeight = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (state.movies.isNotEmpty()) {
        SectionHeaderGlass(title = "Movie • Completed", onMore = { onMoreClick("movie") })
        AnimeGridSectionGlass(animes = state.movies.take(6), onAnimeClick = onAnimeClick)
        Spacer(Modifier.height(6.dp))
    }
    if (state.rekomendasi.isNotEmpty()) {
        SectionHeaderGlass(title = "Rekomendasi Untukmu", onMore = { onMoreClick("rekomendasi") })
        AnimeGridSectionGlass(animes = state.rekomendasi.take(6), onAnimeClick = onAnimeClick)
    }
}

@Composable
private fun AnimeGridSectionGlass(animes: List<Anime>, onAnimeClick: (String) -> Unit) {
    // Fix: hindari tumpuk dengan FlowRow-like chunked + fixed height + weight fillMaxWidth
    Column(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val rows = animes.chunked(3)
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (a in row) {
                    AnimeCard(
                        anime = a,
                        onClick = { onAnimeClick(a.url) },
                        showBookmark = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
