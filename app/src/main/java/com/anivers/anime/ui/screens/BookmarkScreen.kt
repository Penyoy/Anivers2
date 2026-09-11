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
import com.anivers.anime.ui.components.EmptyState
import com.anivers.anime.ui.components.GlassBackground
import com.anivers.anime.ui.components.GlassTopBar
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
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

    // Pull dari Firebase saat login agar save terambil dari cloud
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            try { repo.pullFromFirebase() } catch (_: Exception) {}
        }
    }

    val filtered = remember(bookmarks, query, sort) {
        var list = bookmarks.filter { it.url.isNotEmpty() && it.url != "undefined" }
        if (query.isNotBlank()) list = list.filter { it.judul.contains(query, ignoreCase = true) }
        when (sort) {
            "az" -> list.sortedBy { it.judul.lowercase() }
            "oldest" -> list.sortedBy { it.addedAt }
            else -> list.sortedByDescending { it.addedAt }
        }
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
            GlassTopBar(
                title = "Save",
                subtitle = if (bookmarks.isNotEmpty()) "${bookmarks.size} anime tersimpan" else "Koleksi anime favoritmu"
            )

            if (!isLoggedIn) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EmptyState(
                        title = "Login untuk koleksi",
                        subtitle = "Bookmark tersimpan di akun dan tersinkron ke semua perangkat. Buka tab Profile untuk login.",
                        icon = { Icon(Icons.Filled.Bookmark, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp)) },
                        actionText = "Buka Profile",
                        onAction = { android.widget.Toast.makeText(context, "Buka tab Profile untuk login", android.widget.Toast.LENGTH_SHORT).show() }
                    )
                }
            } else if (bookmarks.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    EmptyState(
                        title = "Belum ada yang disimpan",
                        subtitle = "Ketuk ikon bookmark pada anime favoritmu untuk menyimpannya di sini. Semua tersimpan otomatis ke cloud.",
                        icon = { Icon(Icons.Filled.Bookmark, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp)) }
                    )
                }
            } else {
                // Toolbar glass
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(GlassBg)
                        .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Cari di koleksi...", color = Color(0xFF5C6076), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF5C6076), modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0x33FFDB89),
                            unfocusedBorderColor = GlassBorder
                        ),
                        shape = RoundedCornerShape(50.dp),
                        singleLine = true
                    )
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)) {
                            Text(sort, color = Color(0xFFAEB2C7), fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Color(0xFF1E1E24)) {
                            DropdownMenuItem(text = { Text("Terbaru", color = Color.White) }, onClick = { sort = "newest"; expanded = false })
                            DropdownMenuItem(text = { Text("Terlama", color = Color.White) }, onClick = { sort = "oldest"; expanded = false })
                            DropdownMenuItem(text = { Text("A-Z", color = Color.White) }, onClick = { sort = "az"; expanded = false })
                        }
                    }
                    IconButton(onClick = { scope.launch { repo.clearAll() } }, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x1AFF5F5F))) {
                        Icon(Icons.Filled.Delete, contentDescription = "clear", tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (filtered.isEmpty() && query.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada hasil untuk \"$query\"", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered) { b ->
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(GlassBg)
                                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onAnimeClick(b.url) }
                            ) {
                                AsyncImage(model = b.cover, contentDescription = b.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                // gradient
                                Box(modifier = Modifier.fillMaxWidth().height(64.dp).align(Alignment.BottomCenter).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000)))))
                                // top bookmark badge
                                Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(50)).background(GoldPrimary).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("Saved", color = Color(0xFF030303), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                // delete
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x99000000))
                                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                        .clickable { scope.launch { repo.removeBookmark(b.id) } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                                }
                                // bottom title overlay
                                Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                                    Text(b.judul, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 13.sp)
                                    Text(
                                        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id")).format(java.util.Date(b.addedAt)),
                                        color = Color(0xFFB8B8B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
