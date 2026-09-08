package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
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
import com.anivers.anime.data.model.Anime
import com.anivers.anime.ui.components.*
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303))
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp)
    ) {
        // Profile header - luxe gold on dark
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).padding(top = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(50)).background(Color(0xFF2C2C2E)),
                contentAlignment = Alignment.Center
            ) {
                if (authUser?.photoUrl != null) {
                    AsyncImage(model = authUser.photoUrl.toString(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                } else {
                    Text((authUser?.displayName?.take(2) ?: "A").uppercase(), color = Color(0xFFFFDB89), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(authUser?.displayName ?: "Anivers User", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(authUser?.email ?: "Selamat datang kembali", color = Color(0xFFB8B8B8), fontSize = 11.sp)
            }
            // notif dot
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(50)).background(Color(0xFF2C2C2E)), contentAlignment = Alignment.Center) {
                Text("•", color = Color(0xFFFFDB89), fontSize = 18.sp)
            }
        }

        // Search pill - cinematic, tipis overlay bukan tebal Wibuku floating_home_search
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF1C1C1E))
                .clickable { onNavigateSearch() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFFFDB89), modifier = Modifier.size(18.dp))
                Text("Cari anime...", color = Color(0xFFB8B8B8), fontSize = 14.sp, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF030303), modifier = Modifier.size(16.dp))
                }
            }
        }

        // TabRow ala Wibuku ViewPager2 tapi beda: 3 tab pill, indicator gold, tidak pakai icon
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF030303),
            contentColor = Color(0xFFFFDB89),
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 2.dp,
                        color = Color(0xFFFFDB89)
                    )
                }
            },
            divider = { Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E))) }
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == idx) Color(0xFFFFDB89) else Color(0xFFB8B8B8),
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    selectedContentColor = Color(0xFFFFDB89),
                    unselectedContentColor = Color(0xFFB8B8B8)
                )
            }
        }

        if (state.loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingSkeleton() }
            return@Column
        }
        if (state.error != null && state.ongoing.isEmpty() && state.baruUpload.isEmpty()) {
            ErrorState(message = state.error!!, onRetry = { vm.load() }, debugDetail = state.debugDetail)
            return@Column
        }

        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> UntukmuTab(state, history, onAnimeClick, onMoreClick, onGenreClick)
            1 -> JadwalTab(state, onAnimeClick)
            2 -> TerpopulerTab(state, onAnimeClick, onMoreClick)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun UntukmuTab(state: com.anivers.anime.viewmodel.HomeUiState, history: List<com.anivers.anime.data.local.HistoryEntity>, onAnimeClick: (String)->Unit, onMoreClick: (String)->Unit, onGenreClick: (String)->Unit) {
    // Featured hero - tinggi 210dp, gradient tipis beda Wibuku 16:9 full
    val featured = (state.topAnime + state.ongoing).distinctBy { it.url }.take(5)
    if (featured.isNotEmpty()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(featured.size) { idx ->
                val a = featured[idx]
                // parallax subtle via graphicsLayer
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1C1E))
                        .clickable { onAnimeClick(a.url) }
                        .graphicsLayer { alpha = 1f }
                ) {
                    AsyncImage(model = a.cover, contentDescription = a.judul, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6030303)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Featured", color = Color(0xFF030303), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(a.judul, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(a.genre.take(2).joinToString(" • ").ifEmpty { a.status }, color = Color(0xFFB8B8B8), fontSize = 11.sp, maxLines = 1)
                    }
                    Box(Modifier.align(Alignment.Center).size(48.dp).clip(RoundedCornerShape(50)).background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (history.isNotEmpty()) {
        SectionHeader(title = "Lanjutkan Nonton", onMore = { onMoreClick("history") })
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(history.size) { i ->
                val h = history[i]
                Column(Modifier.width(118.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1C1C1E)).clickable { /* watch */ }) {
                    Box(Modifier.fillMaxWidth().aspectRatio(3f/4.2f).clip(RoundedCornerShape(14.dp))) {
                        AsyncImage(model = h.cover, contentDescription = h.judul, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Color(0x33000000))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((h.progress/100f).coerceIn(0f,1f)).background(Color(0xFFFFDB89)))
                        }
                    }
                    Text(h.judul, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }

    if (state.ongoing.isNotEmpty()) {
        SectionHeader(title = "Trending", onMore = { onMoreClick("ongoing") })
        AnimeGridSection(animes = state.ongoing.take(6), onAnimeClick = onAnimeClick)
    }
    if (state.baruUpload.isNotEmpty()) {
        SectionHeader(title = "Baru Rilis", onMore = { onMoreClick("baruupload") })
        AnimeGridSection(animes = state.baruUpload.take(6), onAnimeClick = onAnimeClick)
    }

    SectionHeader(title = "Jelajahi Genre")
    val genres = listOf("action","adventure","comedy","drama","fantasy","horror","mystery","romance","sci-fi","slice-of-life","supernatural","sports")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(genres.size) { i ->
            val g = genres[i]
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF1C1C1E)).clickable { onGenreClick(g) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(g.replace("-"," ").split(" ").joinToString(" ") { it.replaceFirstChar { c-> c.uppercase()} }, color = Color(0xFFFFDB89), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (state.rekomendasi.isNotEmpty()) {
        SectionHeader(title = "Hot Anime", onMore = { onMoreClick("rekomendasi") })
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for ((idx, a) in state.rekomendasi.take(2).withIndex()) {
                Box(Modifier.weight(1f).height(190.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF1C1C1E)).clickable { onAnimeClick(a.url) }) {
                    AsyncImage(model = a.cover, contentDescription = a.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC030303)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("#${idx+1}", color = Color(0xFF030303), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(4.dp))
                        Text(a.judul, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun JadwalTab(state: com.anivers.anime.viewmodel.HomeUiState, onAnimeClick: (String)->Unit) {
    if (state.jadwal.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Jadwal belum tersedia", color = Color(0xFFB8B8B8)) }
        return
    }
    var activeDay by remember { mutableIntStateOf(0) }
    // day pills - pill gold selected, beda Wibuku ViewPager2 tab
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.jadwal.take(7).size) { i ->
            val day = state.jadwal[i]
            val sel = i == activeDay
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (sel) Color(0xFFFFDB89) else Color(0xFF1C1C1E))
                    .clickable { activeDay = i }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.day ?: "", color = if (sel) Color(0xFF030303) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(day.date ?: "", color = if (sel) Color(0xFF030303).copy(alpha=0.7f) else Color(0xFFB8B8B8), fontSize = 11.sp)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    val list = state.jadwal.getOrNull(activeDay)?.animeList ?: emptyList()
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (a in list) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF1C1C1E)).clickable { a.link?.let { onAnimeClick(it) } }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(model = a.cover, contentDescription = a.animeName, modifier = Modifier.size(56.dp, 76.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f)) {
                    Text(a.animeName ?: "", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text("Update terbaru", color = Color(0xFFFFDB89), fontSize = 11.sp)
                }
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89).copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFFFFDB89), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TerpopulerTab(state: com.anivers.anime.viewmodel.HomeUiState, onAnimeClick: (String)->Unit, onMoreClick: (String)->Unit) {
    if (state.topAnime.isNotEmpty()) {
        SectionHeader(title = "Top Anime", onMore = { onMoreClick("rekomendasi") })
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(minOf(state.topAnime.size, 10)) { idx ->
                val a = state.topAnime[idx]
                Column(Modifier.width(132.dp).clickable { onAnimeClick(a.url) }) {
                    Box(Modifier.fillMaxWidth().aspectRatio(3f/4f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1C1C1E))) {
                        AsyncImage(model = a.cover, contentDescription = a.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("#${idx+1}", color = Color(0xFF030303), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (a.score.isNotBlank()) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xCC030303)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Text("★ ${a.score}", color = Color(0xFFFFDB89), fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(a.judul, color = Color.White, fontSize = 12.sp, maxLines = 2, fontWeight = FontWeight.Medium, lineHeight = 14.sp)
                }
            }
        }
    }
    if (state.movies.isNotEmpty()) {
        SectionHeader(title = "Movie • Completed", onMore = { onMoreClick("movie") })
        AnimeGridSection(animes = state.movies.take(6), onAnimeClick = onAnimeClick)
    }
    if (state.rekomendasi.isNotEmpty()) {
        SectionHeader(title = "Rekomendasi Untukmu", onMore = { onMoreClick("rekomendasi") })
        AnimeGridSection(animes = state.rekomendasi.take(6), onAnimeClick = onAnimeClick)
    }
}

@Composable
private fun AnimeGridSection(animes: List<Anime>, onAnimeClick: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val rows = animes.chunked(3)
        for (row in rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (a in row) Box(Modifier.weight(1f)) { AnimeCard(anime = a, onClick = { onAnimeClick(a.url) }, showBookmark = false) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
