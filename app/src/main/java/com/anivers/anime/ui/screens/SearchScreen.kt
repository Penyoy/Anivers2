package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anivers.anime.ui.components.AnimeCard
import com.anivers.anime.ui.components.EmptyState
import com.anivers.anime.ui.components.GlassBackground
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import com.anivers.anime.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String,
    onAnimeClick: (String) -> Unit,
    onBack: () -> Unit,
    vm: SearchViewModel = viewModel()
) {
    var input by remember { mutableStateOf(initialQuery) }
    val results by vm.results.collectAsState()
    val loading by vm.loading.collectAsState()
    val preview by vm.preview.collectAsState()
    var hasSearched by remember { mutableStateOf(initialQuery.isNotEmpty()) }
    var sort by remember { mutableStateOf("Terbaru") }
    var statusFilter by remember { mutableStateOf("Semua") }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            vm.setQuery(initialQuery)
            vm.search(initialQuery)
            hasSearched = true
        }
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
            // Top bar glass with back
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, GlassBorder, CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text("Pencarian", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; vm.preview(it) },
                    placeholder = { Text("Cari judul anime...", color = Color(0xFF5C6076), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF5C6076)) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (input.isNotEmpty()) {
                                IconButton(onClick = { input = ""; vm.clear() }) { Icon(Icons.Filled.Clear, contentDescription = null, tint = Color(0xFF8A8FA3), modifier = Modifier.size(18.dp)) }
                            }
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(GoldPrimary).clickable {
                                val q = input.trim()
                                if (q.length >= 2) { vm.setQuery(q); vm.search(q); hasSearched = true }
                            }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF030303), modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GlassBg,
                        unfocusedContainerColor = GlassBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary.copy(alpha = 0.6f),
                        unfocusedBorderColor = GlassBorder
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp)
                )

                if (hasSearched && vm.query.collectAsState().value.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Filter: \"${vm.query.collectAsState().value}\" • ${results.size} hasil", color = Color(0xFFAEB2C7), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("Hapus", color = Color(0xFFFCA5A5), fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { input = ""; vm.clear(); hasSearched = false }.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }

                if (input.trim().length >= 2 && !hasSearched && preview.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(16.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Preview \"${input}\"", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        for (a in preview.take(4)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x0DFFFFFF)).clickable { onAnimeClick(a.url) }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                coil.compose.AsyncImage(model = a.cover, contentDescription = null, modifier = Modifier.size(40.dp, 52.dp).clip(RoundedCornerShape(8.dp)))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(a.judul, color = Color.White, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Medium)
                                    Text(a.genre.take(2).joinToString(" • "), color = Color(0xFF8A8FA3), fontSize = 11.sp, maxLines = 1)
                                }
                                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(GoldPrimary), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF030303), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Button(onClick = { val q = input.trim(); vm.setQuery(q); vm.search(q); hasSearched = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary), shape = RoundedCornerShape(12.dp)) {
                            Text("Lihat semua hasil", fontSize = 13.sp, color = Color(0xFF030303), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                !hasSearched -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0x14FFDB89)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                        }
                        Text("Mulai mencari", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Ketik minimal 2 huruf, preview akan muncul otomatis.", color = Color(0xFF8A8FA3), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (kw in listOf("naruto", "one piece", "isekai", "movie")) Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x1AFFFFFF)).border(1.dp, GlassBorder, RoundedCornerShape(50)).clickable { input = kw; vm.setQuery(kw); vm.search(kw); hasSearched = true }.padding(horizontal = 12.dp, vertical = 7.dp)) {
                                Text("#$kw", color = Color(0xFFAEB2C7), fontSize = 11.sp)
                            }
                        }
                    }
                }
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldPrimary) }
                results.isNotEmpty() -> {
                    val filtered = remember(results, sort, statusFilter) {
                        var list = results
                        when (statusFilter) {
                            "Ongoing" -> list = list.filter { it.status.equals("Ongoing", ignoreCase = true) }
                            "Completed" -> list = list.filter { it.status.equals("Completed", ignoreCase = true) }
                            "Movie" -> list = list.filter { it.type.contains("Movie", ignoreCase = true) }
                        }
                        when (sort) {
                            "Score" -> list.sortedByDescending { it.score.toDoubleOrNull() ?: 0.0 }
                            "A-Z" -> list.sortedBy { it.judul.lowercase() }
                            else -> list
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${filtered.size} hasil", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(sort, color = GoldPrimary, fontSize = 12.sp)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Color(0xFF1E1E24)) {
                                    for (s in listOf("Terbaru", "Score", "A-Z")) DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { sort = s; expanded = false })
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (chip in listOf("Semua", "Ongoing", "Completed", "Movie")) {
                                        val isActive = chip == statusFilter
                                        Box(Modifier.clip(RoundedCornerShape(50)).background(if (isActive) GoldPrimary else GlassBg).border(1.dp, if (isActive) GoldPrimary else GlassBorder, RoundedCornerShape(50)).clickable { statusFilter = chip }.padding(horizontal = 12.dp, vertical = 7.dp)) {
                                            Text(chip, color = if (isActive) Color(0xFF030303) else Color(0xFFB8B8B8), fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            items(filtered) { a -> AnimeCard(anime = a, onClick = { onAnimeClick(a.url) }, showBookmark = false) }
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EmptyState(title = "Tidak ditemukan", subtitle = "Tidak ada anime untuk \"${vm.query.collectAsState().value}\"", actionText = "Hapus Filter", onAction = { input = ""; vm.clear(); hasSearched = false })
                    }
                }
            }
        }
    }
}
