package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch

@Composable
fun GenreScreen(slug: String, onAnimeClick: (String) -> Unit) {
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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070E)).padding(bottom = 80.dp)) {
        // hero
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF3730A3).copy(alpha = 0.2f), Color.Transparent))
            ).padding(horizontal = 16.dp, vertical = 16.dp).padding(top = 48.dp)
        ) {
            Column {
                Text(slug.replace("-", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c-> c.uppercase() } }, color = Color.White, fontSize = 20.sp)
                Text("${list.size} anime • Hal $page", color = Color(0xFF8A8FA3), fontSize = 12.sp)
            }
        }
        if (loading && list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3730A3)) }
        } else if (list.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Belum ada anime", color = Color.White)
                Text("Tidak ada anime untuk $slug", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                if (error != null) Text(error!!, color = Color(0xFFFBBF24), fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { load(true) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3730A3)), shape = RoundedCornerShape(50.dp)) { Text("Coba Lagi") }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(list) { a -> AnimeCard(anime = a, onClick = { onAnimeClick(a.url) }) }
                if (hasMore) {
                    item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(onClick = { load(false) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0x14FFFFFF)), shape = RoundedCornerShape(12.dp)) { Text("Muat Lebih", color = Color(0xFFAEB2C7), fontSize = 12.sp) }
                    } }
                }
            }
        }
    }
}
