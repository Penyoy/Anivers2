package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.repository.BookmarkRepository
import com.anivers.anime.viewmodel.DetailViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    slug: String,
    onEpisodeClick: (String, String) -> Unit,
    onGenreClick: (String) -> Unit,
    vm: DetailViewModel = viewModel()
) {
    val detail by vm.detail.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBookmarked by remember { mutableStateOf(false) }
    val repo = remember { BookmarkRepository(context) }

    LaunchedEffect(slug) { vm.load(slug) }
    LaunchedEffect(detail) {
        detail?.let {
            val id = it.id?.toString() ?: slug
            isBookmarked = repo.isBookmarked(id, slug)
        }
    }

    when {
        loading -> Box(Modifier.fillMaxSize().background(Color(0xFF030303)), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFDB89)) }
        error != null && detail == null -> Column(
            Modifier.fillMaxSize().background(Color(0xFF030303)).padding(24.dp).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Gagal memuat detail", color = Color(0xFFF8FAFC), fontSize = 16.sp)
            Text(error ?: "", color = Color(0xFF8A8FA3), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.load(slug) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89))) { Text("Coba Lagi") }
        }
        detail != null -> {
            val d = detail!!
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF030303)).verticalScroll(rememberScrollState()).padding(bottom = 100.dp)
            ) {
                // hero with blur bg
                Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                    AsyncImage(
                        model = d.cover, contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(20.dp),
                        contentScale = ContentScale.Crop, alpha = 0.3f
                    )
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x0005070E), Color(0xFF030303)))))
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 56.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = d.cover, contentDescription = d.judul,
                            modifier = Modifier.width(160.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                            Text(d.judul ?: slug, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!d.rating.isNullOrEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(14.dp))
                                    Text(d.rating, color = Color(0xFFAEB2C7), fontSize = 12.sp)
                                }
                                if (!d.type.isNullOrEmpty()) Text(d.type, color = Color(0xFFAEB2C7), fontSize = 12.sp)
                                if (!d.status.isNullOrEmpty()) {
                                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(if (d.status == "Ongoing") Color(0xFF22C55E) else Color(0xFFEF4444)))
                                    Text(d.status, color = Color(0xFFAEB2C7), fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                for ((idx, g) in (d.genre ?: emptyList()).withIndex()) {
                                    val slugG = d.genreurl?.getOrNull(idx) ?: g.lowercase()
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x1AFFFFFF)).clickable { onGenreClick(slugG) }.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) { Text(g, color = Color(0xFFDDDDDD), fontSize = 11.sp) }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(d.sinopsis ?: "Sinopsis tidak tersedia.", color = Color(0xFFAEB2C7), fontSize = 12.sp, lineHeight = 16.sp, maxLines = 6)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (FirebaseAuth.getInstance().currentUser == null) {
                                        // simple toast via snackbar? use scope
                                        return@Button
                                    }
                                    scope.launch {
                                        val id = d.id?.toString() ?: slug
                                        val url = d.seriesId ?: slug
                                        val added = repo.toggleBookmark(id, url, d.judul ?: slug, d.cover ?: "")
                                        isBookmarked = added
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isBookmarked) Color(0xFFFFDB89) else Color.Transparent),
                                border = if (isBookmarked) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isBookmarked) "Tersimpan" else "Bookmark", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // episodes
                if (!d.chapter.isNullOrEmpty()) {
                    val sorted = d.chapter.sortedWith(compareBy(
                        { if (it.ch == "Movie") -1 else it.ch?.toIntOrNull() ?: 9999 }
                    ))
                    Text("Episode ${sorted.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (ep in sorted) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0DFFFFFF)).clickable {
                                    val seriesId = d.seriesId ?: slug
                                    val epUrl = ep.url ?: ""
                                    onEpisodeClick(seriesId, epUrl)
                                }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFDB89)), contentAlignment = Alignment.Center) {
                                    Text(ep.ch ?: "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Episode ${ep.ch}", color = Color(0xFFCCCCCC), fontSize = 13.sp)
                                    Text(ep.date ?: "", color = Color(0xFF666666), fontSize = 11.sp)
                                }
                                if (ep.views != null) Text("${ep.views}", color = Color(0xFF666666), fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Episode belum tersedia", color = Color(0xFF8A8FA3), fontSize = 14.sp)
                        Text("Silakan coba lagi nanti.", color = Color(0xFF666666), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
