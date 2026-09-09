package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anivers.anime.data.model.Anime
import com.anivers.anime.data.repository.AnimeRepository
import com.anivers.anime.ui.components.AnimeCard
import com.anivers.anime.ui.components.ErrorState
import com.anivers.anime.ui.components.GlassBackground
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import kotlinx.coroutines.launch

@Composable
fun GenreScreen(slug: String, onAnimeClick: (String) -> Unit, onBack: () -> Unit) {
    var list by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val repo = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    fun load(reset: Boolean = false) {
        scope.launch {
            if (reset) { page = 1; list = emptyList() }
            loading = true
            error = null
            try {
                val res = repo.getGenre(if (reset) 1 else page, "$slug/")
                if (reset) list = res else list = list + res
                hasMore = res.size >= 20
                if (res.isNotEmpty() && !reset) page++
            } catch (e: Exception) { error = e.message }
            loading = false
        }
    }
    LaunchedEffect(slug) { load(true) }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
            // Glass header with back
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, GlassBorder, CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(slug.replace("-", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }, color = Color.White, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("${list.size} anime • Genre", color = Color(0xFF8A8FA3), fontSize = 11.sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GoldPrimary).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(slug.take(12), color = Color(0xFF030303), fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }

            // hero glass
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0x33FFDB89), Color(0x1A7C3AED)))).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(slug.replace("-", " ").uppercase(), color = Color.White, fontSize = 13.sp, letterSpacing = 1.2.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Jelajahi koleksi ${slug.replace("-", " ")} terbaik", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (loading && list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldPrimary) }
            } else if (list.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (error != null) ErrorState(message = error!!, onRetry = { load(true) }) else {
                        Text("Belum ada anime", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Tidak ada anime untuk $slug", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { load(true) }, colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF030303)), shape = RoundedCornerShape(50.dp)) { Text("Coba Lagi") }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(list) { a -> AnimeCard(anime = a, onClick = { onAnimeClick(a.url) }, showBookmark = false) }
                    if (hasMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                OutlinedButton(onClick = { load(false) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)) {
                                    Text("Muat Lebih", color = Color(0xFFAEB2C7), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
