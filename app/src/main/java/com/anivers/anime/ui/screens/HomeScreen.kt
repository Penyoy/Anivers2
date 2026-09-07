package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303))
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        // TopBar inside Home (custom)
        TopBar(
            onSearchClick = onNavigateSearch,
            onProfileClick = {},
            showSearch = false // we show large search below ala request but click navigates
        )

        // Big Search Card - reference Proyek Baru 51.png black pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF0A0A0A))
                .clickable { onNavigateSearch() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFFFDB89), modifier = Modifier.size(18.dp))
                Text("Cari anime...", color = Color(0xFF8A8FA3), fontSize = 14.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF030303), modifier = Modifier.size(16.dp)) }
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

        // Featured slider - top anime + ongoing pool
        val featured = (state.topAnime + state.ongoing).distinctBy { it.url }.take(5)
        if (featured.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(featured.size) { idx ->
                    val a = featured[idx]
                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A1A1A))
                            .clickable { onAnimeClick(a.url) }
                    ) {
                        AsyncImage(model = a.cover, contentDescription = a.judul, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color(0xBF000000)))
                            )
                        )
                        Column(
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89)).padding(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("Featured", color = Color(0xFF030303), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            Text(a.judul, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                            if (a.sinopsis.isNotBlank()) {
                                Text(a.sinopsis, color = Color(0xFFAEB2C7), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp)
                            } else {
                                Text(a.genre.take(2).joinToString(" • "), color = Color(0xFFAEB2C7), fontSize = 11.sp)
                            }
                        }
                        Box(
                            modifier = Modifier.align(Alignment.Center).size(44.dp).clip(RoundedCornerShape(50)).background(Color(0x66000000)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                    }
                }
            }
        }

        // Continue Watching
        if (history.isNotEmpty()) {
            SectionHeader(title = "Continue Watching", onMore = { /* already recent is separate */ })
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history.size) { i ->
                    val h = history[i]
                    Column(
                        modifier = Modifier.width(120.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x0DFFFFFF)).clickable { /* navigate to watch - need seriesUrl/episode */ }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f/4f).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(model = h.cover, contentDescription = h.judul, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Color(0x66000000))
                            ) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(h.progress/100f).background(Color(0xFFFFDB89)))
                            }
                        }
                        Text(h.judul, color = Color(0xFFE6E8EE), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp))
                    }
                }
            }
        }

        // TOP ANIME Section (requested di home)
        if (state.topAnime.isNotEmpty()) {
            SectionHeader(title = "Top Anime", onMore = { onMoreClick("rekomendasi") })
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(minOf(state.topAnime.size, 10)) { idx ->
                    val a = state.topAnime[idx]
                    Column(
                        modifier = Modifier.width(140.dp).clickable { onAnimeClick(a.url) }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f/4.2f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1A1A1A))) {
                            AsyncImage(model = a.cover, contentDescription = a.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Box(
                                modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000))))
                            )
                            Box(
                                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFF242457)).padding(horizontal = 6.dp, vertical = 2.dp)
                            ) { Text("#${idx+1}", color = Color(0xFFA5B4FC), fontSize = 10.sp) }
                            Text(
                                a.judul, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Trending / Ongoing
        if (state.ongoing.isNotEmpty()) {
            SectionHeader(title = "Trending", onMore = { onMoreClick("ongoing") })
            AnimeGridSection(animes = state.ongoing.take(8), onAnimeClick = onAnimeClick)
        }

        // New Update
        if (state.baruUpload.isNotEmpty()) {
            SectionHeader(title = "New Update Anime", onMore = { onMoreClick("baruupload") })
            AnimeGridSection(animes = state.baruUpload.take(8), onAnimeClick = onAnimeClick)
        }

        // Genres pill quick
        SectionHeader(title = "Genres")
        val genres = listOf("action","adventure","comedy","drama","fantasy","horror","mystery","romance","sci-fi","slice-of-life","supernatural","sports","mecha")
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(genres.size) { i ->
                val g = genres[i]
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x0DFFFFFF)).clickable { onGenreClick(g) }.padding(horizontal = 14.dp, vertical = 8.dp)
                ) { Text(g.replace("-"," ").split(" ").joinToString(" ") { it.replaceFirstChar { c-> c.uppercase() } }, color = Color(0xFFAEB2C7), fontSize = 12.sp) }
            }
        }

        // Hot Anime (rekomendasi slice 0..2)
        if (state.rekomendasi.isNotEmpty()) {
            SectionHeader(title = "Hot Anime", onMore = { onMoreClick("rekomendasi") })
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((idx, a) in state.rekomendasi.take(2).withIndex()) {
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(3f/4.2f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1A1A1A)).clickable { onAnimeClick(a.url) }
                    ) {
                        AsyncImage(model = a.cover, contentDescription = a.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000)))))
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF242457)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("#${idx+1}", color = Color(0xFFA5B4FC), fontSize = 10.sp) }
                            Text(a.judul, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Completed / Movie
        if (state.movies.isNotEmpty()) {
            SectionHeader(title = "Completed Anime", onMore = { onMoreClick("movie") })
            AnimeGridSection(animes = state.movies.take(8), onAnimeClick = onAnimeClick)
        }

        // Jadwal teaser 7 days
        if (state.jadwal.isNotEmpty()) {
            SectionHeader(title = "Jadwal", onMore = { onMoreClick("jadwal") })
            var activeDay by remember { mutableStateOf(0) }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.jadwal.take(7).size) { i ->
                    val day = state.jadwal[i]
                    val selected = i == activeDay
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(if (selected) Color(0xFF242457) else Color.Transparent)
                            .clickable { activeDay = i }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text(day.day?.take(3) ?: "", color = if (selected) Color(0xFFA5B4FC) else Color(0xFF8A8FA3), fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))
            val list = state.jadwal.getOrNull(activeDay)?.animeList?.take(8) ?: emptyList()
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list.size) { idx ->
                    val a = list[idx]
                    Column(modifier = Modifier.width(96.dp).clickable { a.link?.let { onAnimeClick(it) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(model = a.cover, contentDescription = a.animeName, modifier = Modifier.size(96.dp, 128.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.height(4.dp))
                        Text(a.animeName ?: "", color = Color(0xFFCBD5E1), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AnimeGridSection(animes: List<Anime>, onAnimeClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 2 rows grid manual
        val rows = animes.chunked(3)
        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (a in row) {
                    Box(modifier = Modifier.weight(1f)) {
                        AnimeCard(anime = a, onClick = { onAnimeClick(a.url) }, showBookmark = false)
                    }
                }
                // fill empty
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
