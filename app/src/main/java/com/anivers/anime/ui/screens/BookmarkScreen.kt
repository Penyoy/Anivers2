package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.repository.BookmarkRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(onAnimeClick: (String) -> Unit) {
    val context = LocalContext.current
    val repo = remember { BookmarkRepository(context) }
    val db = remember { AppDatabase.get(context) }
    val bookmarks by db.bookmarkDao().getAllFlow().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("newest") }
    val scope = rememberCoroutineScope()
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

    val filtered = remember(bookmarks, query, sort) {
        var list = bookmarks.filter { it.url.isNotEmpty() && it.url != "undefined" }
        if (query.isNotBlank()) list = list.filter { it.judul.contains(query, ignoreCase = true) }
        when (sort) {
            "az" -> list.sortedBy { it.judul }
            "oldest" -> list.sortedBy { it.addedAt }
            else -> list.sortedByDescending { it.addedAt }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303))
            .padding(bottom = 100.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Color(0xFFFFDB89))
                Text(
                    "Koleksi Saya",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isLoggedIn && bookmarks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x14FFDB89))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${bookmarks.size} anime",
                            color = Color(0xFFFFDB89),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Text(
                "Semua anime yang kamu simpan ada di sini.",
                color = Color(0xFF8A8FA3),
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(12.dp))

        if (!isLoggedIn) {
            // Not logged in
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x14FFDB89)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFFFFDB89),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Login untuk melihat koleksi",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Bookmark tersimpan di akun dan tersinkron ke semua perangkat.",
                    color = Color(0xFF8A8FA3),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89)),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Login Sekarang", fontSize = 13.sp, color = Color(0xFF030303))
                }
            }
        } else if (bookmarks.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x14FFDB89)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFFFFDB89),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Belum ada yang disimpan",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Ketuk ikon bookmark pada anime favoritmu untuk menyimpannya di sini.",
                    color = Color(0xFF8A8FA3),
                    fontSize = 12.sp
                )
            }
        } else {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text("Cari di koleksi...", color = Color(0xFF5C6076), fontSize = 12.sp)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color(0xFF5C6076),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x0DFFFFFF),
                        unfocusedContainerColor = Color(0x0DFFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0x1AFFFFFF),
                        unfocusedBorderColor = Color(0x1AFFFFFF)
                    ),
                    shape = RoundedCornerShape(50.dp),
                    singleLine = true
                )
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(sort, color = Color(0xFFAEB2C7), fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Terbaru") },
                            onClick = { sort = "newest"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Terlama") },
                            onClick = { sort = "oldest"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("A-Z") },
                            onClick = { sort = "az"; expanded = false }
                        )
                    }
                }
                IconButton(onClick = {
                    scope.launch { repo.clearAll() }
                }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "clear",
                        tint = Color(0xFFFCA5A5)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty() && query.isNotEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tidak ada hasil untuk \"$query\"",
                        color = Color(0xFF8A8FA3),
                        fontSize = 12.sp
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered) { b ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1C1C1E))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onAnimeClick(b.url) }
                        ) {
                            AsyncImage(
                                model = b.cover,
                                contentDescription = b.judul,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Delete button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0x99000000))
                                    .clickable {
                                        scope.launch { repo.removeBookmark(b.id) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                b.judul,
                                color = Color(0xFFE6E8EE),
                                fontSize = 11.sp,
                                maxLines = 2,
                                lineHeight = 12.sp
                            )
                            Text(
                                java.text.SimpleDateFormat(
                                    "dd MMM yyyy",
                                    java.util.Locale("id")
                                ).format(java.util.Date(b.addedAt)),
                                color = Color(0xFF5C6076),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
