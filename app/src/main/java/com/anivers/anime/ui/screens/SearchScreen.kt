package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anivers.anime.data.model.Anime
import com.anivers.anime.ui.components.AnimeCard
import com.anivers.anime.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String,
    onAnimeClick: (String) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303))
            .padding(bottom = 100.dp)
    ) {
        // Search bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    vm.preview(it)
                },
                placeholder = {
                    Text("Cari judul anime...", color = Color(0xFF5C6076), fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF5C6076))
                },
                trailingIcon = {
                    Row {
                        if (input.isNotEmpty()) {
                            IconButton(onClick = { input = ""; vm.clear() }) {
                                Icon(Icons.Filled.Clear, contentDescription = null, tint = Color(0xFF8A8FA3))
                            }
                        }
                        IconButton(onClick = {
                            vm.setQuery(input.trim())
                            vm.search(input.trim())
                            hasSearched = true
                        }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .background(Color(0xFFFFDB89), RoundedCornerShape(50))
                                    .padding(6.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x0DFFFFFF),
                    unfocusedContainerColor = Color(0x0DFFFFFF),
                    focusedTextColor = Color(0xFFE6E8EE),
                    unfocusedTextColor = Color(0xFFE6E8EE),
                    focusedBorderColor = Color(0xFFFFDB89),
                    unfocusedBorderColor = Color(0x1AFFFFFF)
                ),
                singleLine = true,
                shape = RoundedCornerShape(50.dp)
            )

            // Active filter info
            if (hasSearched && vm.query.collectAsState().value.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x0DFFFFFF))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Filter: \"${vm.query.collectAsState().value}\" • ${results.size} hasil",
                        color = Color(0xFFAEB2C7),
                        fontSize = 12.sp
                    )
                    Text(
                        "Hapus",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            input = ""; vm.clear(); hasSearched = false
                        }
                    )
                }
            }

            // Preview panel when typing >=2
            if (input.trim().length >= 2 && !hasSearched && preview.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(8.dp)
                ) {
                    Text("Preview \"${input}\"", color = Color(0xFFFFDB89), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    for (a in preview) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAnimeClick(a.url) }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            coil.compose.AsyncImage(
                                model = a.cover,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp, 48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.judul, color = Color(0xFFE6E8EE), fontSize = 12.sp, maxLines = 1)
                                Text(
                                    a.genre.take(2).joinToString(" • "),
                                    color = Color(0xFF8A8FA3),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            vm.setQuery(input.trim()); vm.search(input.trim()); hasSearched = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Lihat semua hasil", fontSize = 13.sp, color = Color(0xFF030303)) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            !hasSearched -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x0DFFFFFF))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color(0xFFFFDB89),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Mulai mencari", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Ketik minimal 2 huruf, preview akan muncul otomatis.",
                        color = Color(0xFF8A8FA3),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (kw in listOf("naruto", "one piece", "isekai", "movie")) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0x1AFFFFFF))
                                    .clickable {
                                        input = kw; vm.setQuery(kw); vm.search(kw); hasSearched = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("#$kw", color = Color(0xFFAEB2C7), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFFDB89))
                }
            }
            results.isNotEmpty() -> {
                val filtered = remember(results, sort, statusFilter) {
                    var list = results
                    // Status filter
                    when (statusFilter) {
                        "Ongoing" -> list = list.filter {
                            it.status.equals("Ongoing", ignoreCase = true)
                        }
                        "Completed" -> list = list.filter {
                            it.status.equals("Completed", ignoreCase = true)
                        }
                        "Movie" -> list = list.filter {
                            it.type?.contains("Movie", ignoreCase = true) == true
                        }
                    }
                    // Sort
                    when (sort) {
                        "Score" -> list.sortedByDescending { it.score.toDoubleOrNull() ?: 0.0 }
                        "A-Z" -> list.sortedBy { it.judul.lowercase() }
                        else -> list
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Result count + sort
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${filtered.size} hasil",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                shape = RoundedCornerShape(50.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(sort, color = Color(0xFFFFDB89), fontSize = 12.sp)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                for (s in listOf("Terbaru", "Score", "A-Z")) {
                                    DropdownMenuItem(
                                        text = { Text(s) },
                                        onClick = { sort = s; expanded = false }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    // Status filter chips - functional
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (chip in listOf("Semua", "Ongoing", "Completed", "Movie")) {
                                    val isActive = chip == statusFilter
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (isActive) Color(0xFFFFDB89) else Color(0xFF1C1C1E)
                                            )
                                            .clickable { statusFilter = chip }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            chip,
                                            color = if (isActive) Color(0xFF030303) else Color(0xFFB8B8B8),
                                            fontSize = 11.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    // Results grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered) { a ->
                            AnimeCard(anime = a, onClick = { onAnimeClick(a.url) })
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x0DFFFFFF))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tidak ditemukan", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Tidak ada anime untuk \"${vm.query.collectAsState().value}\"",
                        color = Color(0xFF8A8FA3),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { input = ""; vm.clear(); hasSearched = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89)),
                        shape = RoundedCornerShape(50.dp)
                    ) { Text("Hapus Filter", fontSize = 12.sp, color = Color(0xFF030303)) }
                }
            }
        }
    }
}
