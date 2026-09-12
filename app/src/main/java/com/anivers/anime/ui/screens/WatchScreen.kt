package com.anivers.anime.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.anivers.anime.data.repository.BookmarkRepository
import com.anivers.anime.ui.components.CommentsSection
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import com.anivers.anime.viewmodel.WatchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    seriesUrl: String,
    episode: String,
    onBack: () -> Unit,
    onEpisodeClick: (String, String) -> Unit = { _, _ -> },
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
    val settingsStore = remember { com.anivers.anime.data.local.SettingsStore(context) }
    val appSettings by settingsStore.flow.collectAsState(initial = com.anivers.anime.data.local.AppSettings())

    LaunchedEffect(appSettings.quality, stream) {
        val resos = stream?.data?.firstOrNull()?.reso ?: emptyList()
        if (resos.isNotEmpty() && appSettings.quality in resos && appSettings.quality != quality) vm.setQuality(appSettings.quality)
    }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var currentLink by remember { mutableStateOf<String?>(null) }
    var currentServer by remember { mutableIntStateOf(0) }
    var showResume by remember { mutableStateOf(false) }
    var savedTime by remember { mutableLongStateOf(0L) }

    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPos by remember { mutableFloatStateOf(0f) }
    var sinopsisExpanded by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    val resizeLabel = when (resizeMode) {
        AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Original"
        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Regang"
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Full Crop"
        else -> "Original"
    }

    LaunchedEffect(seriesUrl, episode) { vm.load(seriesUrl, episode) }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3200)
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
                    override fun onIsPlayingChanged(isPlay: Boolean) { isPlaying = isPlay; if (isPlay) showControls = false else showControls = true }
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) duration = this@apply.duration.coerceAtLeast(0L)
                    }
                })
            }
            exoPlayer?.release()
            exoPlayer = p
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

    // Recent fix: hanya upsertProgress tiap 8s (dedup per episode) dengan cover/judul agar recent muncul, jangan addHistory tiap 8s (bikin 50 row)
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(8000)
            val p = exoPlayer ?: continue
            val pos = p.currentPosition / 1000
            val dur = p.duration / 1000
            if (dur > 5 && pos > 3) {
                repo.upsertProgress(seriesUrl, episode, pos, dur, seriesUrl, series?.judul ?: seriesUrl, series?.cover ?: "")
                // addHistory hanya saat pause/complete, tidak tiap 8s biar tidak 50 duplikat
                if (!p.isPlaying && pos >= dur - 2) {
                    repo.addHistory(seriesUrl, episode, series?.judul ?: seriesUrl, series?.cover ?: "", pos, dur, true)
                }
            }
        }
    }

    fun toggleFullscreen() {
        if (activity == null) return
        isFullscreen = !isFullscreen
        activity.requestedOrientation = if (isFullscreen) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    fun enterPip() {
        if (activity == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
            val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            try { activity.enterPictureInPictureMode(params) } catch (_: Exception) {}
        }
    }

    val supportsPip = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) else false
    }

    // Final: player di luar verticalScroll, original top/bottom nempel (pillar-box), tanpa border radius di portrait maupun landscape
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isFullscreen) Spacer(Modifier.statusBarsPadding().height(8.dp))

            when {
                loading -> Box(Modifier.fillMaxWidth().height(240.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = GoldPrimary)
                        Text("Memuat video...", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                    }
                }
                error != null -> Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0x1AFF5F5F)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color(0xFFFF5F5F), modifier = Modifier.size(28.dp))
                    }
                    Text("Gagal memuat video", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(error ?: "", color = Color(0xFF8A8FA3), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { vm.load(seriesUrl, episode) }, colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF030303)), shape = RoundedCornerShape(50.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Coba Lagi", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onBack, shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)) { Text("Kembali", color = Color.White) }
                }
                currentLink != null -> {
                    // GLASS PLAYER - original top/bottom nempel, sisi hitam jika perlu, tanpa radius
                    val playerModifier = if (isFullscreen) Modifier.fillMaxSize().background(Color.Black)
                    else Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
                Box(
                    modifier = playerModifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = { offset ->
                                    val p = exoPlayer ?: return@detectTapGestures
                                    val w = size.width
                                    if (offset.x < w / 2) p.seekTo((p.currentPosition - 10000).coerceAtLeast(0)) else p.seekTo((p.currentPosition + 10000).coerceAtMost(p.duration))
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
                                resizeMode = resizeMode
                            }
                        },
                        update = {
                            it.player = exoPlayer
                            it.resizeMode = resizeMode
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (showControls) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent, Color(0x88000000)))))

                    if (showResume) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xE6121214)).border(1.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("Lanjutkan menonton?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Terhenti di ${formatTime(savedTime)}", color = Color(0xFFAEB2C7), fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { exoPlayer?.seekTo(savedTime * 1000); exoPlayer?.play(); showResume = false }, colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary), shape = RoundedCornerShape(50.dp)) { Text("Lanjut", fontSize = 12.sp, color = Color(0xFF030303), fontWeight = FontWeight.Bold) }
                                    OutlinedButton(onClick = { showResume = false }, shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)) { Text("Awal", fontSize = 12.sp, color = Color.White) }
                                }
                            }
                        }
                    }

                    if (showControls) {
                        Row(modifier = Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Color(0x33FFFFFF), CircleShape).clickable { exoPlayer?.seekTo((exoPlayer!!.currentPosition - 10000).coerceAtLeast(0)) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Box(modifier = Modifier.size(68.dp).clip(CircleShape).background(GoldPrimary).clickable { exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() } }, contentAlignment = Alignment.Center) {
                                Icon(imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF030303), modifier = Modifier.size(36.dp))
                            }
                            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Color(0x33FFFFFF), CircleShape).clickable { exoPlayer?.seekTo((exoPlayer!!.currentPosition + 10000).coerceAtMost(exoPlayer!!.duration)) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }

                    if (showControls) {
                        Row(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xAA000000), Color.Transparent))).padding(horizontal = 12.dp, vertical = 10.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Color(0x1AFFFFFF), CircleShape).clickable { if (isFullscreen) toggleFullscreen() else onBack() }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "back", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(series?.judul ?: seriesUrl, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Episode ${episode.takeLast(8)} • ${if (isPlaying) "Memutar" else "Jeda"}", color = Color(0xFFAEB2C7), fontSize = 11.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Tombol pilih ukuran: Original (fit, tidak kepotong) vs Regang (fill) vs Full Crop (zoom) — user pilih sendiri
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Color(0x1AFFFFFF), CircleShape).clickable {
                                    resizeMode = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.AspectRatio, contentDescription = "scale $resizeLabel", tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) Color.White else GoldPrimary, modifier = Modifier.size(18.dp))
                                }
                                if (supportsPip) {
                                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Color(0x1AFFFFFF), CircleShape).clickable { enterPip() }, contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "pip", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Color(0x1AFFFFFF), CircleShape).clickable { showQualitySheet = true }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Settings, contentDescription = "quality", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x66000000)).border(1.dp, Color(0x1AFFFFFF), CircleShape).clickable { toggleFullscreen() }, contentAlignment = Alignment.Center) {
                                    Icon(if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, contentDescription = "fullscreen", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (showControls) {
                        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))).padding(horizontal = 12.dp, vertical = 10.dp)) {
                            val prog = if (duration > 0) (if (isUserSeeking) seekPos else position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                            Slider(
                                value = prog,
                                onValueChange = { v -> isUserSeeking = true; seekPos = v },
                                onValueChangeFinished = { exoPlayer?.seekTo((seekPos * duration).toLong()); isUserSeeking = false },
                                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary, inactiveTrackColor = Color(0x44FFFFFF)),
                                modifier = Modifier.fillMaxWidth().height(22.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${formatTime(position / 1000)} / ${formatTime(duration / 1000)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) Color(0x33000000) else Color(0x33FFDB89)).border(1.dp, if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) Color(0x33FFFFFF) else Color(0x33FFDB89), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp).clickable {
                                        resizeMode = when (resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                    }) {
                                        Text(resizeLabel, color = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) Color.White else GoldPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x33FFDB89)).border(1.dp, Color(0x1AFFDB89), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(quality, color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x33000000)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("Server ${currentServer + 1}", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (showQualitySheet) {
                    ModalBottomSheet(onDismissRequest = { showQualitySheet = false }, containerColor = Color(0xFF14141A), contentColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x14FFDB89)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Tune, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("Kualitas & Server", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Pilih sesuai koneksi", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                                }
                            }
                            Text("Kualitas", color = Color(0xFF8A8FA3), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val resos = stream?.data?.firstOrNull()?.reso ?: emptyList()
                                for (r in resos) {
                                    val sel = r == quality
                                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) GoldPrimary else GlassBg).border(1.dp, if (sel) GoldPrimary else GlassBorder, RoundedCornerShape(50)).clickable { vm.setQuality(r); showQualitySheet = false }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                        Text(r, color = if (sel) Color(0xFF030303) else Color(0xFFAEB2C7), fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            Text("Server", color = Color(0xFF8A8FA3), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val servers = stream?.data?.firstOrNull()?.streams?.get(quality) ?: emptyList()
                                for ((idx, _) in servers.withIndex()) {
                                    val sel = idx == currentServer
                                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) GoldPrimary else GlassBg).border(1.dp, if (sel) GoldPrimary else GlassBorder, RoundedCornerShape(50)).clickable { currentServer = idx; showQualitySheet = false }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                        Text("Server ${idx + 1}", color = if (sel) Color(0xFF030303) else Color(0xFFAEB2C7), fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            Text("Tampilan Video — pilih jangan full crop", color = Color(0xFF8A8FA3), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for ((mode, label, desc) in listOf(
                                    Triple(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Original", "tidak kepotong"),
                                    Triple(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Regang", "isi lebar"),
                                    Triple(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Full Crop", "potong")
                                )) {
                                    val sel = resizeMode == mode
                                    Column(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (sel) GoldPrimary else GlassBg).border(1.dp, if (sel) GoldPrimary else GlassBorder, RoundedCornerShape(12.dp)).clickable { resizeMode = mode; showQualitySheet = false }.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(label, color = if (sel) Color(0xFF030303) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(desc, color = if (sel) Color(0xFF030303).copy(0.7f) else Color(0xFF8A8FA3), fontSize = 10.sp)
                                    }
                                }
                            }
                            Text("Original = biarkan apa adanya (ada black bar tapi tidak kepotong). Regang = penuhi lebar tanpa full crop.", color = Color(0xFF5A5A6A), fontSize = 10.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            else -> Box(Modifier.fillMaxWidth().height(200.dp).background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.VideocamOff, contentDescription = null, tint = Color(0xFF8A8FA3), modifier = Modifier.size(32.dp))
                    Text("Video tidak tersedia", color = Color(0xFF8A8FA3), fontSize = 13.sp)
                    OutlinedButton(onClick = { vm.load(seriesUrl, episode) }, shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)) { Text("Coba Lagi", color = Color.White, fontSize = 12.sp) }
                }
            }
        }

        if (!isFullscreen) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(series?.judul ?: seriesUrl, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!series?.rating.isNullOrEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(14.dp))
                        Text(series!!.rating!!, color = Color(0xFFAEB2C7), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    if (!series?.type.isNullOrEmpty()) Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(series!!.type!!, color = Color(0xFFAEB2C7), fontSize = 11.sp) }
                    if (!series?.status.isNullOrEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (series!!.status == "Ongoing") Color(0xFF22C55E) else Color(0xFFEF4444)))
                        Text(series!!.status!!, color = Color(0xFFAEB2C7), fontSize = 11.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("${series?.chapter?.size ?: 0} Episode", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (!series?.sinopsis.isNullOrEmpty()) Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Sinopsis tersedia", color = Color(0xFF8A8FA3), fontSize = 11.sp)
                    }
                }

                if (!series?.sinopsis.isNullOrBlank()) {
                    var expanded by remember { mutableStateOf(sinopsisExpanded) }
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(16.dp)).clickable { expanded = !expanded; sinopsisExpanded = expanded }.padding(14.dp)) {
                        Text(series!!.sinopsis!!, color = Color(0xFFC7CAD6), fontSize = 13.sp, lineHeight = 18.sp, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text(if (expanded) "Ciutkan ▲" else "Selengkapnya ▼", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { expanded = !expanded; sinopsisExpanded = expanded })
                    }
                }

                if (!series?.genre.isNullOrEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (g in series!!.genre!!) Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(g, color = Color(0xFFDDDDDD), fontSize = 11.sp)
                        }
                    }
                }

                if (!series?.chapter.isNullOrEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Daftar Episode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${series!!.chapter!!.size} eps", color = Color(0xFF8A8FA3), fontSize = 11.sp)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(series!!.chapter!!) { ep ->
                            val isActive = ep.url == episode
                            Box(
                                modifier = Modifier.size(48.dp, 40.dp).clip(RoundedCornerShape(10.dp)).background(if (isActive) GoldPrimary else GlassBg).border(1.dp, if (isActive) GoldPrimary else GlassBorder, RoundedCornerShape(10.dp)).clickable {
                                    if (!isActive && ep.url != null) onEpisodeClick(seriesUrl, ep.url)
                                },
                                contentAlignment = Alignment.Center
                            ) { Text(ep.ch ?: "?", color = if (isActive) Color(0xFF030303) else Color(0xFF8A8FA3), fontSize = 12.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                    // Next/Prev buttons glass
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        val idx = series!!.chapter!!.indexOfFirst { it.url == episode }
                        val prev = if (idx > 0) series!!.chapter!![idx - 1] else null
                        val next = if (idx >= 0 && idx < series!!.chapter!!.size - 1) series!!.chapter!![idx + 1] else null
                        if (prev != null) OutlinedButton(onClick = { onEpisodeClick(seriesUrl, prev.url ?: "") }, shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Prev Ep ${prev.ch}", color = Color.White, fontSize = 12.sp)
                        } else Spacer(Modifier.weight(1f))
                        if (next != null) Button(onClick = { onEpisodeClick(seriesUrl, next.url ?: "") }, colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF030303)), shape = RoundedCornerShape(50.dp), modifier = Modifier.weight(1f)) {
                            Text("Next Ep ${next.ch}", fontWeight = FontWeight.Bold, fontSize = 12.sp); Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(16.dp))
                        } else Spacer(Modifier.weight(1f))
                    }
                    // Komentar per episode slug - disabled sementara (endpoint belum ketemu)
                    // CommentsSection(slug = episode.ifBlank { seriesUrl }, modifier = Modifier.padding(top = 12.dp))
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
