package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anivers.anime.data.model.Anime
import com.anivers.anime.ui.components.AnimeCard
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

    // genre definitions atas
    val genres = remember {
        listOf(
            GenreDef("Action", "action", "Pertarungan epic", Color(0xFF991B1B), Brush.linearGradient(listOf(Color(0xFF7F1D1D), Color(0xFF450A0A)))),
            GenreDef("Adventure", "adventure", "Petualangan seru", Color(0xFF14532D), Brush.linearGradient(listOf(Color(0xFF14532D), Color(0xFF052E16)))),
            GenreDef("Comedy", "comedy", "Ngakak abis", Color(0xFF92400E), Brush.linearGradient(listOf(Color(0xFF92400E), Color(0xFF451A03)))),
            GenreDef("Drama", "drama", "Menguras emosi", Color(0xFF581C87), Brush.linearGradient(listOf(Color(0xFF581C87), Color(0xFF3B0764)))),
            GenreDef("Fantasy", "fantasy", "Dunia sihir", Color(0xFF312E81), Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF242457)))),
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

    // handle initialType filtering for rekomendasi/etc
    val filteredList: List<Anime> = remember(initialType, rekomendasi, homeState) {
        when (initialType) {
            "ongoing" -> homeState.ongoing
            "baruupload" -> homeState.baruUpload
            "movie" -> homeState.movies
            "rekomendasi", "top" -> rekomendasi
            "jadwal" -> emptyList()
            else -> rekomendasi
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303))
            .padding(bottom = 100.dp)
    ) {
        // hero - langsung genre tanpa stats
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).padding(top = 48.dp)) {
            Text("Jelajahi Genre", color = Color(0xFFF8FAFC), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = com.anivers.anime.ui.theme.BestyFontFamily)
            Text("Pilih mood kamu hari ini", color = Color(0xFF8A8FA3), fontSize = 13.sp)
        }

        // GENRE GRID AT TOP (requested)
        Text(
            "Browse by Genre", color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold, fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        // 2 columns grid manually in LazyColumn
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (row in genres.chunked(2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            for (g in row) {
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x0DFFFFFF)).clickable { onGenreClick(g.slug) }
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(g.color),
                                            contentAlignment = Alignment.Center
                                        ) { Text(g.name.take(1), color = Color.White, fontWeight = FontWeight.Bold) }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(g.name, color = Color(0xFFF8FAFC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(g.desc, color = Color(0xFF8A8FA3), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Rekomendasi dibawah genre
                    Text("Rekomendasi Lain", color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (loading) {
                        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFFDB89))
                        }
                    } else {
                        val list = if (filteredList.isNotEmpty()) filteredList else rekomendasi
                        // grid 3 columns
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (row in list.take(12).chunked(3)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    for (a in row) {
                                        Box(Modifier.weight(1f)) { AnimeCard(anime = a, onClick = { onAnimeClick(a.url) }, showBookmark = false) }
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatCard(value: String, label: String, color: Color) {
    Column(
        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0x0DFFFFFF)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Category, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = Color(0xFF8A8FA3), fontSize = 11.sp)
    }
}
