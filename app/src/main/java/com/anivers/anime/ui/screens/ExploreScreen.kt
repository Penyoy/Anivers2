package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anivers.anime.data.model.Anime
import com.anivers.anime.ui.components.AnimeCard
import com.anivers.anime.ui.components.GlassBackground
import com.anivers.anime.ui.components.GlassTopBar
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import com.anivers.anime.viewmodel.ExploreViewModel
import com.anivers.anime.viewmodel.HomeViewModel

data class GenreDef(val name: String, val slug: String, val desc: String, val color: Color, val gradient: Brush)

@Composable
fun ExploreScreen(
    initialType: String = "",
    onAnimeClick: (String) -> Unit,
    onGenreClick: (String) -> Unit,
    exploreVm: ExploreViewModel = viewModel(),
    homeVm: HomeViewModel = viewModel()
) {
    val rekomendasi by exploreVm.rekomendasi.collectAsState()
    val loading by exploreVm.loading.collectAsState()
    val homeState by homeVm.state.collectAsState()

    LaunchedEffect(Unit) { exploreVm.load() }

    val genres = remember {
        listOf(
            GenreDef("Action", "action", "Pertarungan epic", Color(0xFF991B1B), Brush.linearGradient(listOf(Color(0xFF7F1D1D), Color(0xFF450A0A)))),
            GenreDef("Adventure", "adventure", "Petualangan seru", Color(0xFF14532D), Brush.linearGradient(listOf(Color(0xFF14532D), Color(0xFF052E16)))),
            GenreDef("Comedy", "comedy", "Ngakak abis", Color(0xFF92400E), Brush.linearGradient(listOf(Color(0xFF92400E), Color(0xFF451A03)))),
            GenreDef("Drama", "drama", "Menguras emosi", Color(0xFF581C87), Brush.linearGradient(listOf(Color(0xFF581C87), Color(0xFF3B0764)))),
            GenreDef("Fantasy", "fantasy", "Dunia sihir", Color(0xFF312E81), Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF7C3AED)))),
            GenreDef("Horror", "horror", "Mencekam", Color(0xFF111827), Brush.linearGradient(listOf(Color(0xFF1F2937), Color(0xFF020617)))),
            GenreDef("Isekai", "isekai", "Dunia lain", Color(0xFF164E63), Brush.linearGradient(listOf(Color(0xFF164E63), Color(0xFF083344)))),
            GenreDef("Mecha", "mecha", "Robot raksasa", Color(0xFF1E293B), Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF020617)))),
            GenreDef("Mystery", "mystery", "Misteri kelam", Color(0xFF7C2D12), Brush.linearGradient(listOf(Color(0xFF7C2D12), Color(0xFF431407)))),
            GenreDef("Romance", "romance", "Bikin baper", Color(0xFF831843), Brush.linearGradient(listOf(Color(0xFF831843), Color(0xFF500724)))),
            GenreDef("Sci-Fi", "sci-fi", "Masa depan", Color(0xFF134E4A), Brush.linearGradient(listOf(Color(0xFF134E4A), Color(0xFF042F2E)))),
            GenreDef("Slice of Life", "slice-of-life", "Keseharian", Color(0xFF92400E), Brush.linearGradient(listOf(Color(0xFF92400E), Color(0xFF451A03)))),
            GenreDef("Sports", "sports", "Semangat juang", Color(0xFF14532D), Brush.linearGradient(listOf(Color(0xFF14532D), Color(0xFF052E16)))),
            GenreDef("Supernatural", "supernatural", "Kekuatan gaib", Color(0xFF4C1D95), Brush.linearGradient(listOf(Color(0xFF4C1D95), Color(0xFF2E1065)))),
            GenreDef("Thriller", "thriller", "Deg-degan", Color(0xFF7F1D1D), Brush.linearGradient(listOf(Color(0xFF7F1D1D), Color(0xFF450A0A)))),
        )
    }

    val filteredList: List<Anime> = remember(initialType, rekomendasi, homeState) {
        when (initialType) {
            "ongoing" -> homeState.ongoing
            "baruupload" -> homeState.baruUpload
            "movie" -> homeState.movies
            "rekomendasi", "top" -> rekomendasi
            else -> rekomendasi
        }
    }

    val isCategory = initialType in listOf("ongoing", "baruupload", "movie", "rekomendasi", "top")
    val categoryTitle = when (initialType) {
        "ongoing" -> "Trending • Ongoing"
        "baruupload" -> "Baru Rilis"
        "movie" -> "Movie • Completed"
        "rekomendasi", "top" -> "Rekomendasi"
        else -> null
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
            GlassTopBar(
                title = categoryTitle ?: "Jelajahi Genre",
                subtitle = if (isCategory) "${filteredList.size} anime • $initialType" else "Pilih mood kamu hari ini"
            )

            LazyColumn(modifier = Modifier.weight(1f, fill = false), contentPadding = PaddingValues(bottom = 16.dp)) {
                // Kategori view: langsung list tanpa grid genre (fix more -> genre bug)
                if (isCategory) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (loading) {
                                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldPrimary) }
                            } else if (filteredList.isEmpty()) {
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Belum ada data", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("Kategori $initialType kosong", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    for (row in filteredList.chunked(3)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                            for (a in row) Box(Modifier.weight(1f)) { AnimeCard(anime = a, onClick = { onAnimeClick(a.url) }, showBookmark = false) }
                                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                } else {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (row in genres.chunked(2)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    for (g in row) {
                                        Box(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(g.gradient).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)).clickable { onGenreClick(g.slug) }.padding(14.dp)
                                        ) {
                                            Column {
                                                Text(g.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                Text(g.desc, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                                Spacer(Modifier.height(6.dp))
                                                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x33000000)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                                    Text("Jelajahi →", color = Color.White, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(50)).background(GoldPrimary))
                                Text("Rekomendasi Lain", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            if (loading) {
                                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldPrimary) }
                            } else {
                                val list = if (filteredList.isNotEmpty()) filteredList else rekomendasi
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    for (row in list.take(12).chunked(3)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                            for (a in row) Box(Modifier.weight(1f)) { AnimeCard(anime = a, onClick = { onAnimeClick(a.url) }, showBookmark = false) }
                                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}
