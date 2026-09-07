package com.anivers.anime.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.repository.BookmarkRepository
import com.anivers.anime.viewmodel.WatchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WatchScreen(
    seriesUrl: String,
    episode: String,
    vm: WatchViewModel = viewModel()
) {
    val context = LocalContext.current
    val stream by vm.stream.collectAsState()
    val series by vm.series.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val quality by vm.quality.collectAsState()
    val scope = rememberCoroutineScope()
    val repo = remember { BookmarkRepository(context) }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var currentLink by remember { mutableStateOf<String?>(null) }
    var currentServer by remember { mutableStateOf(0) }
    var showResume by remember { mutableStateOf(false) }
    var savedTime by remember { mutableStateOf(0L) }

    LaunchedEffect(seriesUrl, episode) { vm.load(seriesUrl, episode) }

    DisposableEffect(stream, quality, currentServer) {
        val data = stream?.data?.firstOrNull()
        val links = data?.streams?.get(quality) ?: data?.streams?.values?.firstOrNull() ?: emptyList()
        val link = links.getOrNull(currentServer)?.link
        if (link != null && link != currentLink) {
            currentLink = link
            val p = ExoPlayer.Builder(context).build().apply {
                val item = MediaItem.fromUri(link)
                setMediaItem(item)
                prepare()
                playWhenReady = true
            }
            exoPlayer?.release()
            exoPlayer = p
            // check saved progress
            scope.launch {
                val prog = repo.getProgress(seriesUrl, episode)
                if (prog != null && prog.currentTime > 5 && prog.progress in 4..94) {
                    savedTime = prog.currentTime
                    showResume = true
                }
            }
        }
        onDispose { }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer?.release() } }

    // progress save loop
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(8000)
            val p = exoPlayer ?: continue
            val pos = p.currentPosition / 1000
            val dur = p.duration / 1000
            if (dur > 5 && pos > 3) {
                repo.upsertProgress(seriesUrl, episode, pos, dur)
                // also save to history for Recent
                repo.addHistory(seriesUrl, episode, series?.judul ?: seriesUrl, series?.cover ?: "", pos, dur, p.isPlaying.not() && pos >= dur - 2)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(bottom = 80.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().height(240.dp).background(Color.Black), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3730A3)) }
            error != null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Gagal memuat video", color = Color.White)
                Text(error ?: "", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.load(seriesUrl, episode) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3730A3))) { Text("Coba Lagi") }
            }
            currentLink != null -> {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black)) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                useController = true
                            }
                        },
                        update = { it.player = exoPlayer },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (showResume) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)
                                .clip(RoundedCornerShape(12.dp)).background(Color(0xCC05070E)).padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("Lanjutkan?", color = Color.White, fontSize = 13.sp)
                                    Text("Berhenti di ${formatTime(savedTime)}", color = Color(0xFF8A8FA3), fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        exoPlayer?.seekTo(savedTime * 1000)
                                        exoPlayer?.play()
                                        showResume = false
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3730A3)), shape = RoundedCornerShape(50.dp)) { Text("Lanjutkan", fontSize = 12.sp) }
                                    OutlinedButton(onClick = { showResume = false }, shape = RoundedCornerShape(50.dp)) { Text("Awal", fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                }
                // controls reso + server
                val resos = stream?.data?.firstOrNull()?.reso ?: emptyList()
                if (resos.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xE6050714)).padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (r in resos) {
                                val sel = r == quality
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) Color(0xFF3730A3) else Color(0x14FFFFFF)).clickable { vm.setQuality(r) }.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) { Text(r, color = if (sel) Color.White else Color(0xFF8A8FA3), fontSize = 12.sp) }
                            }
                        }
                        val servers = stream?.data?.firstOrNull()?.streams?.get(quality) ?: emptyList()
                        if (servers.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for ((idx, _) in servers.withIndex()) {
                                    val sel = idx == currentServer
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) Color(0xFF3730A3) else Color(0x14FFFFFF)).clickable { currentServer = idx }.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) { Text("Server ${idx+1}", color = if (sel) Color.White else Color(0xFF8A8FA3), fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                }
            }
            else -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("Video tidak tersedia", color = Color(0xFF8A8FA3)) }
        }

        // info + episode nav
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("${series?.judul ?: seriesUrl} - Episode ${episode.takeLast(2)}", color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            if (!series?.chapter.isNullOrEmpty()) {
                Text("Episode", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(series!!.chapter!!) { ep ->
                        val isActive = ep.url == episode
                        Box(
                            modifier = Modifier.size(44.dp, 36.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) Color(0xFF3730A3) else Color(0x1AFFFFFF))
                                .clickable { /* nav to same screen with new ep */ },
                            contentAlignment = Alignment.Center
                        ) { Text(ep.ch ?: "?", color = if (isActive) Color.White else Color(0xFF8A8FA3), fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

private fun formatTime(sec: Long): String {
    if (sec <= 0) return "00:00"
    val s = (sec % 60).toString().padStart(2, '0')
    val m = ((sec / 60) % 60).toString().padStart(2, '0')
    val h = sec / 3600
    return if (h > 0) "$h:$m:$s" else "$m:$s"
}
