package com.anivers.anime.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.repository.BookmarkRepository
import com.anivers.anime.ui.components.*
import android.app.Activity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import com.anivers.anime.data.ads.AdsManager
import com.anivers.anime.data.local.SettingsStore
import com.anivers.anime.data.repository.KeysRepository
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import com.anivers.anime.viewmodel.DetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    slug: String,
    onEpisodeClick: (String, String) -> Unit,
    onGenreClick: (String) -> Unit,
    onBack: () -> Unit,
    vm: DetailViewModel = viewModel()
) {
    val detail by vm.detail.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var isBookmarked by remember { mutableStateOf(false) }
    val repo = remember { BookmarkRepository(context) }
    val keysRepo = remember { KeysRepository(context) }
    val settingsStore = remember { SettingsStore(context) }
    val settings by settingsStore.flow.collectAsState(initial = com.anivers.anime.data.local.AppSettings())
    var isUnlocked by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var synopsisExpanded by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showDetailCountdown by remember { mutableStateOf(false) }
    var detailCountdown by remember { mutableIntStateOf(40) }
    LaunchedEffect(showDetailCountdown) {
        if (showDetailCountdown) {
            detailCountdown = com.anivers.anime.utils.Constants.COUNTDOWN_SEC.toInt()
            while (detailCountdown > 0 && showDetailCountdown) {
                kotlinx.coroutines.delay(1000)
                detailCountdown--
            }
            if (showDetailCountdown && detailCountdown == 0) {
                showDetailCountdown = false
                val before = settings.keys
                val ok = keysRepo.earnKeys(com.anivers.anime.utils.Constants.COUNTDOWN_REWARD_KEYS)
                if (ok) android.widget.Toast.makeText(context, "Dapat ${com.anivers.anime.utils.Constants.COUNTDOWN_REWARD_KEYS} kunci! ${before} -> ${minOf(6, before + 3)}/6", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(slug) { vm.load(slug) }
    LaunchedEffect(slug, settings.isPremium) { isUnlocked = keysRepo.isUnlocked(slug) }
    LaunchedEffect(Unit) { AdsManager.preload(context) }
    LaunchedEffect(detail) {
        detail?.let {
            val id = it.id?.toString() ?: slug
            isBookmarked = repo.isBookmarked(id, slug)
        }
    }

    GlassBackground {
        when {
            loading -> Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator(color = GoldPrimary)
                    Text("Memuat detail...", color = Color(0xFF8A8FA3), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) { ShimmerBox(modifier = Modifier.size(100.dp, 140.dp), corner = 16.dp) }
                    }
                }
            }
            error != null && detail == null -> Column(
                Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ErrorState(message = error ?: "Gagal memuat detail", onRetry = { vm.load(slug) })
                Spacer(Modifier.height(12.dp))
                GlassButton(text = "Kembali", onClick = onBack, isPrimary = false)
            }
            detail != null -> {
                val d = detail!!
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 96.dp)
                ) {
                    // Hero with parallax glass
                    val parallax = (scrollState.value * 0.32f).coerceIn(0f, 80f)
                    Box(
                        modifier = Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    ) {
                        AsyncImage(
                            model = d.cover,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = parallax }.blur(22.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.38f
                        )
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xFF030303)))))
                        // extra radial glow
                        Box(Modifier.fillMaxWidth().height(200.dp).align(Alignment.TopCenter).background(Brush.radialGradient(listOf(Color(0x22FFDB89), Color.Transparent), radius = 600f)))

                        // Back + actions top bar glass
                        Row(
                            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Color(0x1AFFFFFF), CircleShape).clickable { onBack() },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White, modifier = Modifier.size(20.dp)) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(CircleShape).background(if (isBookmarked) GoldPrimary else Color(0x66000000)).border(1.dp, Color(0x1AFFFFFF), CircleShape).clickable {
                                        scope.launch {
                                            val id = d.id?.toString() ?: slug
                                            val url = d.seriesId ?: slug
                                            val added = repo.toggleBookmark(id, url, d.judul ?: slug, d.cover ?: "", d.status ?: "", d.type ?: "")
                                            isBookmarked = added
                                        }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = null, tint = if (isBookmarked) Color(0xFF030303) else Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Cover + info glass
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 84.dp, bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(modifier = Modifier.width(138.dp).height(190.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))) {
                                AsyncImage(model = d.cover, contentDescription = d.judul, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                // score badge
                                if (!d.rating.isNullOrEmpty()) {
                                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(50)).background(GoldPrimary).padding(horizontal = 7.dp, vertical = 3.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF030303), modifier = Modifier.size(12.dp))
                                            Text(d.rating, color = Color(0xFF030303), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(d.judul ?: slug, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp, maxLines = 3)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (!d.type.isNullOrEmpty()) Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(d.type, color = Color.White, fontSize = 11.sp)
                                    }
                                    if (!d.status.isNullOrEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (d.status == "Ongoing") Color(0xFF22C55E) else Color(0xFFEF4444)))
                                        Text(d.status, color = Color(0xFFD1D5DB), fontSize = 11.sp)
                                    }
                                }
                                // meta row
                                if (!d.published.isNullOrEmpty()) Text(d.published, color = Color(0xFF8A8FA3), fontSize = 11.sp, maxLines = 1)
                                // genre glass chips
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                                    for ((idx, g) in (d.genre ?: emptyList()).take(3).withIndex()) {
                                        val slugG = d.genreurl?.getOrNull(idx) ?: g.lowercase()
                                        Box(Modifier.clip(RoundedCornerShape(50)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(50)).clickable { onGenreClick(slugG) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(g, color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Synopsis glass card
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 16.dp).clip(RoundedCornerShape(20.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(16.dp).animateContentSize()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(50)).background(GoldPrimary))
                            Text("Sinopsis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (!d.published.isNullOrEmpty()) Text("• ${d.published}", color = Color(0xFF8A8FA3), fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        if (!d.sinopsis.isNullOrBlank()) {
                            Text(text = d.sinopsis ?: "", color = Color(0xFFC7CAD6), fontSize = 13.sp, lineHeight = 18.sp, maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            Text(text = if (synopsisExpanded) "Ciutkan ▲" else "Selengkapnya ▼", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded })
                        } else {
                            Text("Sinopsis tidak tersedia.", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        // Kunci status chip
                        if (!isUnlocked && !settings.isPremium) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x1AFF5F5F)).border(1.dp, Color(0x33FF5F5F), RoundedCornerShape(12.dp)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFFF5F5F), modifier = Modifier.size(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Terkunci — butuh 1 kunci (permanen)", color = Color(0xFFFF5F5F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Kunci: ${settings.keys}/6 • Premium buka semua", color = Color(0xFFB8B8B8), fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        } else if (settings.isPremium) {
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x1422C55E)).border(1.dp, Color(0x3322C55E), RoundedCornerShape(12.dp)).padding(10.dp)) {
                                Text("Premium aktif — semua episode terbuka", color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x1422C55E)).border(1.dp, Color(0x3322C55E), RoundedCornerShape(12.dp)).padding(10.dp)) {
                                Text("Terbuka — 1 kunci terpakai permanen", color = Color(0xFF22C55E), fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (isUnlocked || settings.isPremium) {
                                            val first = d.chapter?.sortedWith(compareBy { if (it.ch == "Movie") -1 else it.ch?.toIntOrNull() ?: 9999 })?.firstOrNull()
                                            if (first != null) onEpisodeClick(d.seriesId ?: slug, first.url ?: "")
                                        } else if (settings.keys > 0) {
                                            val ok = keysRepo.unlock(slug, d.judul ?: slug, d.cover ?: "")
                                            if (ok) { isUnlocked = true; android.widget.Toast.makeText(context, "Terbuka! 1 kunci terpakai", android.widget.Toast.LENGTH_SHORT).show()
                                                val first = d.chapter?.sortedWith(compareBy { if (it.ch == "Movie") -1 else it.ch?.toIntOrNull() ?: 9999 })?.firstOrNull()
                                                if (first != null) onEpisodeClick(d.seriesId ?: slug, first.url ?: "")
                                            }
                                        } else {
                                            showLockDialog = true
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isUnlocked || settings.isPremium || settings.keys > 0) GoldPrimary else Color(0xFF3A3A3C), contentColor = if (isUnlocked || settings.isPremium || settings.keys > 0) Color(0xFF030303) else Color.White),
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(if (isUnlocked || settings.isPremium) Icons.Filled.PlayArrow else Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isUnlocked || settings.isPremium) "Mulai Nonton" else if (settings.keys > 0) "Buka (1 Kunci)" else "Terkunci", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val id = d.id?.toString() ?: slug
                                        val url = d.seriesId ?: slug
                                        val added = repo.toggleBookmark(id, url, d.judul ?: slug, d.cover ?: "", d.status ?: "", d.type ?: "")
                                        isBookmarked = added
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isBookmarked) GoldPrimary else Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isBookmarked) GoldPrimary else GlassBorder),
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isBookmarked) "Tersimpan" else "Simpan", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Episode list glass
                    if (!d.chapter.isNullOrEmpty()) {
                        val sorted = d.chapter.sortedWith(compareBy { if (it.ch == "Movie") -1 else it.ch?.toIntOrNull() ?: 9999 })
                        val chunkSize = 12
                        val chunks = sorted.chunked(chunkSize)
                        var selectedChunk by remember { mutableIntStateOf(0) }

                        Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(50)).background(GoldPrimary))
                                Text("Episode ${sorted.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            if (d.status == "Ongoing") Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x1422C55E)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("Ongoing", color = Color(0xFF22C55E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        if (chunks.size > 1) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(chunks.size) { idx ->
                                    val sel = idx == selectedChunk
                                    Box(
                                        Modifier.clip(RoundedCornerShape(50)).background(if (sel) GoldPrimary else GlassBg).border(1.dp, if (sel) GoldPrimary else GlassBorder, RoundedCornerShape(50)).clickable { selectedChunk = idx }.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("${idx * chunkSize + 1}-${minOf((idx + 1) * chunkSize, sorted.size)}", color = if (sel) Color(0xFF030303) else Color(0xFFB8B8B8), fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        val visible = chunks.getOrNull(selectedChunk) ?: sorted
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (ep in visible) {
                                var watched by remember { mutableStateOf(false) }
                                LaunchedEffect(ep.url) {
                                    val prog = AppDatabase.get(context).historyDao().getProgress(d.seriesId ?: slug, ep.url ?: "")
                                    watched = prog != null && prog.progress > 5
                                }
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassBg).border(1.dp, if (watched) Color(0x33FFDB89) else if (!isUnlocked && !settings.isPremium) Color(0x33FF5F5F) else GlassBorder, RoundedCornerShape(16.dp)).clickable {
                                        scope.launch {
                                            if (isUnlocked || settings.isPremium) onEpisodeClick(d.seriesId ?: slug, ep.url ?: "")
                                            else if (settings.keys > 0) {
                                                if (keysRepo.unlock(slug, d.judul ?: slug, d.cover ?: "")) { isUnlocked = true; onEpisodeClick(d.seriesId ?: slug, ep.url ?: "") }
                                            } else showLockDialog = true
                                        }
                                    }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (watched) Color(0x33FFDB89) else Color(0xFF1E1E24)).border(1.dp, if (watched) Color(0x33FFDB89) else GlassBorder, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(ep.ch ?: "?", color = if (watched) GoldPrimary else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Episode ${ep.ch}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(ep.date ?: "", color = Color(0xFF8A8FA3), fontSize = 11.sp)
                                        if (watched) Text("Sudah ditonton", color = GoldPrimary, fontSize = 10.sp)
                                    }
                                    if (!isUnlocked && !settings.isPremium) Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFFF5F5F), modifier = Modifier.size(16.dp))
                                    else if (watched) Box(Modifier.size(8.dp).clip(CircleShape).background(GoldPrimary))
                                    Box(Modifier.size(32.dp).clip(CircleShape).background(if (watched) GoldPrimary else if (!isUnlocked && !settings.isPremium) Color(0x33FF5F5F) else Color(0x14FFFFFF)), contentAlignment = Alignment.Center) {
                                        Icon(if (!isUnlocked && !settings.isPremium) Icons.Filled.Lock else Icons.Filled.PlayArrow, contentDescription = null, tint = if (watched) Color(0xFF030303) else if (!isUnlocked && !settings.isPremium) Color(0xFFFF5F5F) else Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Episode belum tersedia", color = Color(0xFF8A8FA3), fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    if (showLockDialog) {
                        AlertDialog(
                            onDismissRequest = { showLockDialog = false },
                            containerColor = Color(0xFF1A1A1E),
                            titleContentColor = Color.White,
                            textContentColor = Color(0xFFB8B8B8),
                            title = { Text("Terkunci", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Anime ini butuh 1 kunci (permanen) untuk dibuka. Kunci mu: ${settings.keys}/6", fontSize = 13.sp)
                                    if (showDetailCountdown) {
                                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x1AFFDB89)).border(1.dp, GoldPrimary, RoundedCornerShape(10.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Menunggu $detailCountdown detik...", color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(4.dp))
                                            LinearProgressIndicator(progress = { (40 - detailCountdown) / 40f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)), color = GoldPrimary, trackColor = Color(0x33FFFFFF))
                                            Text("Jika ada iklan: 30s = 3 kunci. Jika tidak: 40s = 3 kunci. Maks 6.", color = Color(0xFFB8B8B8), fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        }
                                    } else {
                                        Text("Jika ada iklan: tonton 30 detik = 3 kunci. Jika tidak ada iklan: countdown 40 detik = 3 kunci. Maks tumpuk 6.", fontSize = 11.sp, color = Color(0xFF8A8FA3))
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (settings.keys >= 6) {
                                            android.widget.Toast.makeText(context, "Sudah maks 6 kunci", android.widget.Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (activity != null) {
                                            AdsManager.show(activity,
                                                onRewarded = {
                                                    scope.launch {
                                                        val ok = keysRepo.earnKeys(com.anivers.anime.utils.Constants.AD_REWARD_KEYS)
                                                        if (ok) {
                                                            val after = minOf(6, settings.keys + com.anivers.anime.utils.Constants.AD_REWARD_KEYS)
                                                            android.widget.Toast.makeText(context, "Dapat ${com.anivers.anime.utils.Constants.AD_REWARD_KEYS} kunci! Sekarang $after/6", android.widget.Toast.LENGTH_SHORT).show()
                                                            showLockDialog = false
                                                        }
                                                    }
                                                },
                                                onFailed = { _ ->
                                                    showDetailCountdown = true
                                                    android.widget.Toast.makeText(context, "Iklan tidak tersedia, countdown 40 detik untuk 3 kunci", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else android.widget.Toast.makeText(context, "Activity tidak tersedia", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF030303)),
                                    shape = RoundedCornerShape(50.dp)
                                ) { Icon(Icons.Filled.PlayArrow, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(if (showDetailCountdown) "Menunggu $detailCountdown" else "Tonton Iklan 30s (+3)", fontSize = 12.sp) }
                            },
                            dismissButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (settings.keys > 0) {
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                val ok = keysRepo.unlock(slug, detail?.judul ?: slug, detail?.cover ?: "")
                                                if (ok) { isUnlocked = true; showLockDialog = false; android.widget.Toast.makeText(context, "Terbuka!", android.widget.Toast.LENGTH_SHORT).show() }
                                            }
                                        }, shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)) {
                                            Icon(Icons.Filled.VpnKey, null, Modifier.size(14.dp), tint = GoldPrimary); Spacer(Modifier.width(4.dp)); Text("Pakai 1 Kunci", color = GoldPrimary, fontSize = 11.sp)
                                        }
                                    }
                                    TextButton(onClick = { showLockDialog = false }) { Text("Tutup", color = Color.White) }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
