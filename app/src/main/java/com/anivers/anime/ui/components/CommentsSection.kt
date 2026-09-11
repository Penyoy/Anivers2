package com.anivers.anime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Send
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
import com.anivers.anime.data.model.Comment
import com.anivers.anime.data.repository.AnimeRepository
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import kotlinx.coroutines.launch

@Composable
fun CommentsSection(slug: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { AnimeRepository() }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                comments = repo.getComments(slug)
            } catch (e: Exception) { error = e.message?.take(100) }
            loading = false
        }
    }
    LaunchedEffect(slug) { load() }

    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x14FFDB89)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ChatBubble, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            }
            Text("Komentar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("${comments.size}", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { load() }) { Text("Refresh", color = GoldPrimary, fontSize = 11.sp) }
        }
        // Input
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF1E1E24)).border(1.dp, GlassBorder, CircleShape), contentAlignment = Alignment.Center) {
                if (user?.photoUrl != null) AsyncImage(model = user.photoUrl.toString(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                else Text((user?.displayName?.take(1) ?: "A").uppercase(), color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Tulis komentar untuk ${slug.take(30)}...", color = Color(0xFF6B7280), fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0x0AFFFFFF), unfocusedContainerColor = Color(0x0AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = GoldPrimary.copy(0.5f), unfocusedBorderColor = GlassBorder),
                singleLine = false,
                maxLines = 3
            )
            IconButton(
                onClick = {
                    if (input.isBlank()) return@IconButton
                    sending = true
                    scope.launch {
                        val ok = repo.postComment(slug, input.trim(), user?.displayName ?: "Anon")
                        sending = false
                        if (ok) { input = ""; load() } else {
                            // fallback tambah lokal ke list (optimistic)
                            val newC = Comment(id = System.currentTimeMillis().toString(), user = user?.displayName ?: "Kamu", message = input.trim(), date = "baru saja")
                            comments = listOf(newC) + comments
                            input = ""
                        }
                    }
                },
                enabled = !sending && input.isNotBlank(),
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (input.isNotBlank()) GoldPrimary else Color(0x33FFFFFF))
            ) {
                if (sending) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF030303))
                else Icon(Icons.Filled.Send, null, tint = if (input.isNotBlank()) Color(0xFF030303) else Color.White, modifier = Modifier.size(18.dp))
            }
        }
        // List
        when {
            loading -> Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(24.dp)) }
            error != null -> Text("Gagal load: $error", color = Color(0xFFFCA5A5), fontSize = 11.sp)
            comments.isEmpty() -> Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0DFFFFFF)).padding(12.dp), contentAlignment = Alignment.Center) {
                Text("Belum ada komentar untuk slug ini. Jadi yang pertama!", color = Color(0xFF8A8FA3), fontSize = 12.sp)
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in comments) {
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0DFFFFFF)).border(1.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF1E1E24)), contentAlignment = Alignment.Center) {
                            if (c.displayAvatar.isNotBlank()) AsyncImage(model = c.displayAvatar, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            else Text(c.displayName.take(1).uppercase(), color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(c.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(c.displayDate, color = Color(0xFF6B7280), fontSize = 10.sp)
                            }
                            Text(c.displayMessage, color = Color(0xFFD1D5DB), fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
        Text("Endpoint: /comments.php?slug=${slug} • ${comments.size} komentar", color = Color(0xFF3A3A3C), fontSize = 10.sp)
    }
}
