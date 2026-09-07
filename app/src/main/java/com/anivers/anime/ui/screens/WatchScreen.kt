package com.anivers.anime.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.repository.BookmarkRepository
import com.anivers.anime.viewmodel.WatchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WatchScreen(
    seriesUrl: String,
    episode: String,
    vm: WatchViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val stream by vm.stream.collectAsState()
    val series by vm.series.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val quality by vm.quality.collectAsState()
    val scope = rememberCoroutineScope()
    val repo = remember { BookmarkRepository(context) }
    // grafik setting sinkron - baca dari SettingsStore agar setting di Profile ngaruh
    val settingsStore = remember { com.anivers.anime.data.local.SettingsStore(context) }
    val appSettings by settingsStore.flow.collectAsState(initial = com.anivers.anime.data.local.AppSettings())
    LaunchedEffect(appSettings.quality, stream) {
        val resos = stream?.data?.firstOrNull()?.reso ?: emptyList()
        if (resos.isNotEmpty() && appSettings.quality in resos && appSettings.quality != quality) {
            vm.setQuality(appSettings.quality)
        }
    }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var currentLink by remember { mutableStateOf<String?>(null) }
    var currentServer by remember { mutableStateOf(0) }
    var showResume by remember { mutableStateOf(false) }
    var savedTime by remember { mutableStateOf(0L) }

    // player UI states — YouTube style
    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPos by remember { mutableStateOf(0f) }
    var sinopsisExpanded by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }

    LaunchedEffect(seriesUrl, episode) { vm.load(seriesUrl, episode) }

    // auto-hide controls like YouTube
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

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
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlay: Boolean) { isPlaying = isPlay; showControls = !isPlay || showControls }
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) duration = this@apply.duration.coerceAtLeast(0L)
                    }
                })
            }
            exoPlayer?.release()
            exoPlayer = p
            // check saved progress
            scope.launch {
                val prog = repo.getProgress(seriesUrl, episode)
                if (prog != null && prog.currentTime > 5 && prog.progress in 4..94) {
                    savedTime = prog.currentTime
                    showResume = true
                    showControls = true
                }
            }
        }
        onDispose { }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer?.release() } }

    // position ticker
    LaunchedEffect(exoPlayer, isUserSeeking) {
        while (true) {
            delay(500)
            val p = exoPlayer
            if (p != null && !isUserSeeking) {
                position = p.currentPosition.coerceAtLeast(0L)
                duration = p.duration.coerceAtLeast(0L)
            }
        }
    }

    // progress save loop (8s)
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(8000)
            val p = exoPlayer ?: continue
            val pos = p.currentPosition / 1000
            val dur = p.duration / 1000
            if (dur > 5 && pos > 3) {
                repo.upsertProgress(seriesUrl, episode, pos, dur)
                repo.addHistory(seriesUrl, episode, series?.judul ?: seriesUrl, series?.cover ?: "", pos, dur, !p.isPlaying && pos >= dur - 2)
            }
        }
    }

    // fullscreen handling
    fun toggleFullscreen() {
        if (activity == null) return
        isFullscreen = !isFullscreen
        activity.requestedOrientation = if (isFullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(bottom = if (isFullscreen) 0.dp else 100.dp)
    ) {
        if (!isFullscreen) Spacer(Modifier.height(48.dp))

        when {
            loading -> Box(Modifier.fillMaxWidth().height(if (isFullscreen) 240.dp else 240.dp).background(Color.Black), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFDB89)) }
            error != null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Gagal memuat video", color = Color.White)
                Text(error ?: "", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.load(seriesUrl, episode) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89)), shape = RoundedCornerShape(50.dp)) { Text("Coba Lagi") }
            }
            currentLink != null -> {
                // YOUTUBE-STYLE PLAYER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (isFullscreen) 16f / 9f else 16f / 9f)
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = { offset ->
                                    val p = exoPlayer ?: return@detectTapGestures
                                    val w = size.width
                                    if (offset.x < w / 2) p.seekTo((p.currentPosition - 10000).coerceAtLeast(0))
                                    else p.seekTo((p.currentPosition + 10000).coerceAtMost(p.duration))
                                }
                            )
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                useController = false
                                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                            }
                        },
                        update = { it.player = exoPlayer },
                        modifier = Modifier.fillMaxSize()
                    )

                    // gradient top/bottom for readability
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color(0x66000000), Color.Transparent, Color(0x88000000))
                            )
                        )
                    )

                    // resume toast
                    if (showResume) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)
                                .clip(RoundedCornerShape(12.dp)).background(Color(0xE6050714)).padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("Lanjutkan?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Berhenti di ${formatTime(savedTime)}", color = Color(0xFFAEB2C7), fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        exoPlayer?.seekTo(savedTime * 1000); exoPlayer?.play(); showResume = false
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89)), shape = RoundedCornerShape(50.dp)) { Text("Lanjutkan", fontSize = 12.sp) }
                                    OutlinedButton(onClick = { showResume = false }, shape = RoundedCornerShape(50.dp)) { Text("Awal", fontSize = 12.sp, color = Color.White) }
                                }
                            }
                        }
                    }

                    // CENTER PLAY/PAUSE (big)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { exoPlayer?.seekTo((exoPlayer!!.currentPosition - 10000).coerceAtLeast(0)) },
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x66000000))
                            ) { Icon(Icons.Filled.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp)) }
                            IconButton(
                                onClick = { exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() } },
                                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xCC5B5BD6))
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            IconButton(
                                onClick = { exoPlayer?.seekTo((exoPlayer!!.currentPosition + 10000).coerceAtMost(exoPlayer!!.duration)) },
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x66000000))
                            ) { Icon(Icons.Filled.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp)) }
                        }
                    }

                    // TOP BAR (title + settings) - YouTube style
                    androidx.compose.animation.AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { /* back handled by nav */ }, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x66000000))) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "back", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(series?.judul ?: seriesUrl, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Episode ${episode.takeLast(5)} • ${if (isPlaying) "Memutar" else "Dijeda"}", color = Color(0xFFAEB2C7), fontSize = 11.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(onClick = { showQualitySheet = true }, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x66000000))) {
                                    Icon(Icons.Filled.Settings, contentDescription = "quality", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { toggleFullscreen() }, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x66000000))) {
                                    Icon(if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, contentDescription = "fullscreen", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // BOTTOM CONTROLS - progress + time + fullscreen
                    androidx.compose.animation.AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
                        Column(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000)))).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            // slider youtube style
                            val prog = if (duration > 0) (if (isUserSeeking) seekPos else position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                            Slider(
                                value = prog,
                                onValueChange = { v -> isUserSeeking = true; seekPos = v },
                                onValueChangeFinished = {
                                    exoPlayer?.seekTo((seekPos * duration).toLong()); isUserSeeking = false
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFDB89),
                                    activeTrackColor = Color(0xFFFFDB89),
                                    inactiveTrackColor = Color(0x44FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth().height(20.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${formatTime(position/1000)} / ${formatTime(duration/1000)}", color = Color.White, fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(quality, color = Color(0xFFA5B4FC), fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0x33000000)).padding(horizontal = 6.dp, vertical = 2.dp))
                                    Text("Server ${currentServer+1}", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // quality bottom sheet
                if (showQualitySheet) {
                    ModalBottomSheet(onDismissRequest = { showQualitySheet = false }, containerColor = Color(0xFF1A1A1A)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Kualitas & Server", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Kualitas", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val resos = stream?.data?.firstOrNull()?.reso ?: emptyList()
                                for (r in resos) {
                                    val sel = r == quality
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) Color(0xFFFFDB89) else Color(0x14FFFFFF)).clickable { vm.setQuality(r); showQualitySheet = false }.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) { Text(r, color = if (sel) Color.White else Color(0xFFAEB2C7), fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
                                }
                            }
                            Text("Server", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val servers = stream?.data?.firstOrNull()?.streams?.get(quality) ?: emptyList()
                                for ((idx, _) in servers.withIndex()) {
                                    val sel = idx == currentServer
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) Color(0xFFFFDB89) else Color(0x14FFFFFF)).clickable { currentServer = idx; showQualitySheet = false }.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) { Text("Server ${idx+1}", color = if (sel) Color.White else Color(0xFFAEB2C7), fontSize = 13.sp) }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            else -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("Video tidak tersedia", color = Color(0xFF8A8FA3)) }
        }

        if (!isFullscreen) {
            // INFO + SINOPSIS (expandable 3 baris) + EPISODE NAV — polished
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("${series?.judul ?: seriesUrl}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!series?.rating.isNullOrEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(14.dp))
                        Text(series!!.rating!!, color = Color(0xFFAEB2C7), fontSize = 12.sp)
                    }
                    if (!series?.type.isNullOrEmpty()) Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x1AFFFFFF)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(series!!.type!!, color = Color(0xFFAEB2C7), fontSize = 11.sp) }
                    if (!series?.status.isNullOrEmpty()) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (series!!.status == "Ongoing") Color(0xFF22C55E) else Color(0xFFEF4444)))
                        Text(series!!.status!!, color = Color(0xFFAEB2C7), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))

                // SINOPSIS 3 baris expandable — YouTube style
                if (!series?.sinopsis.isNullOrBlank()) {
                    var expanded by remember { mutableStateOf(sinopsisExpanded) }
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0DFFFFFF)).clickable { expanded = !expanded; sinopsisExpanded = expanded }.padding(12.dp)
                    ) {
                        Text(
                            text = series!!.sinopsis!!,
                            color = Color(0xFFAEB2C7),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (expanded) "Ciutkan" else "Selengkapnya",
                            color = Color(0xFF6366F1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { expanded = !expanded; sinopsisExpanded = expanded }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                } else if (loading) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x0DFFFFFF)))
                }

                // genre chips
                if (!series?.genre.isNullOrEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        for (g in series!!.genre!!) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x1AFFFFFF)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(g, color = Color(0xFFDDDDDD), fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (!series?.chapter.isNullOrEmpty()) {
                    Text("Episode ${series!!.chapter!!.size}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(series!!.chapter!!) { ep ->
                            val isActive = ep.url == episode
                            Box(
                                modifier = Modifier.size(44.dp, 36.dp).clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) Color(0xFFFFDB89) else Color(0x1AFFFFFF))
                                    .clickable { /* nav to same screen with new ep */ },
                                contentAlignment = Alignment.Center
                            ) { Text(ep.ch ?: "?", color = if (isActive) Color.White else Color(0xFF8A8FA3), fontSize = 12.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal) }
                        }
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
