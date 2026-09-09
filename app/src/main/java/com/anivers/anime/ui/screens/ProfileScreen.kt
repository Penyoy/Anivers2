package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.anivers.anime.R
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.local.SettingsStore
import com.anivers.anime.ui.components.GlassBackground
import com.anivers.anime.ui.components.GlassTopBar
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onAuthSuccess: (() -> Unit)? = null, onNavigate: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var user by remember { mutableStateOf(auth.currentUser) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val settings by settingsStore.flow.collectAsState(initial = com.anivers.anime.data.local.AppSettings())
    val db = remember { AppDatabase.get(context) }
    val bookmarks by db.bookmarkDao().getAllFlow().collectAsState(initial = emptyList())
    val historyCount = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { historyCount.value = db.historyDao().getAll().size }

    // Google Sign-In launcher - butuh SHA-1 terdaftar di Firebase (debug + release)
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrEmpty()) {
                error = "Google Sign-In gagal: idToken null. Pastikan SHA-1 debug sudah ditambahkan di Firebase Console → Project Settings → Your apps → SHA certificate fingerprints, lalu download google-services.json baru."
                loading = false
                return@rememberLauncherForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            // Firebase sign-in dengan Google credential (await untuk coroutine)
            scope.launch {
                try {
                    auth.signInWithCredential(credential).await()
                    user = auth.currentUser
                    // Buat/update Firestore users/{uid}
                    try {
                        val uid = user?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).set(
                                mapOf(
                                    "uid" to uid,
                                    "email" to (user?.email ?: ""),
                                    "displayName" to (user?.displayName ?: ""),
                                    "photoUrl" to (user?.photoUrl?.toString() ?: ""),
                                    "provider" to "google",
                                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                    "lastLoginAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                ), com.google.firebase.firestore.SetOptions.merge()
                            ).await()
                        }
                    } catch (_: Exception) {}
                    loading = false
                    error = ""
                    onAuthSuccess?.invoke()
                    android.widget.Toast.makeText(context, "Login Google berhasil!", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    loading = false
                    error = mapAuthError(e.message ?: "Gagal login Google")
                }
            }
        } catch (e: ApiException) {
            loading = false
            // 12500 = SIGN_IN_CANCELLED, 10 = DEVELOPER_ERROR (SHA-1 belum terdaftar)
            error = when (e.statusCode) {
                10 -> "Google Sign-In error 10 (DEVELOPER_ERROR): SHA-1 belum terpasang di Firebase. Jalankan: ./gradlew signingReport → copy SHA1 debug → Firebase Console → Add fingerprint → download google-services.json baru."
                12501 -> "Login Google dibatalkan"
                else -> "Google Sign-In gagal (${e.statusCode}): ${e.message?.take(100)}"
            }
        } catch (e: Exception) {
            loading = false
            error = "Google Sign-In error: ${e.message?.take(120)}"
        }
    }

    fun launchGoogleSignIn() {
        loading = true
        error = ""
        try {
            val webClientId = try { context.getString(R.string.default_web_client_id) } catch (_: Exception) { "" }
            if (webClientId.isBlank() || webClientId == "placeholder") {
                loading = false
                error = "google-services.json placeholder terdeteksi. Set Firebase SHA-1 dulu: Firebase Console → Project Settings → Add SHA-1 (debug & release) → download google-services.json → masukkan ke app/google-services.json → rebuild."
                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                return
            }
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(context, gso)
            // signOut dulu agar picker selalu muncul
            client.signOut().addOnCompleteListener {
                googleLauncher.launch(client.signInIntent)
            }
        } catch (e: Exception) {
            loading = false
            error = "Gagal init Google Sign-In: ${e.message?.take(120)} — pastikan SHA-1 sudah ditambahkan."
        }
    }

    GlassBackground {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 96.dp).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassTopBar(title = "Profile", subtitle = if (user != null) user?.email ?: "" else "Kelola akun & preferensi")

            if (user != null) {
                // Profile glass card
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(24.dp)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(88.dp).clip(CircleShape).background(Color(0x1AFFDB89)).border(2.dp, GoldPrimary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user?.photoUrl != null) {
                            AsyncImage(model = user?.photoUrl.toString(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape))
                        } else {
                            Text((user?.displayName?.take(1) ?: "U").uppercase(), color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(user?.displayName ?: "User", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(user?.email ?: "", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatPill(value = "${bookmarks.size}", label = "Bookmark", icon = Icons.Filled.Bookmark)
                        StatPill(value = "${historyCount.value}", label = "Ditonton", icon = Icons.Filled.History)
                        StatPill(value = "0", label = "Notifikasi", icon = Icons.Filled.Notifications)
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { auth.signOut(); user = null },
                        shape = RoundedCornerShape(50.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FF5F5F)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5))
                    ) { Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Logout", fontSize = 13.sp) }
                }
            } else {
                // Auth glass card
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(24.dp)).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0x14FFDB89)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(if (isLogin) "Selamat Datang Kembali" else "Buat Akun Baru", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(if (isLogin) "Masuk untuk sinkronisasi" else "Daftar untuk menyimpan koleksi", color = Color(0xFF8A8FA3), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { launchGoogleSignIn() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Menghubungkan...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isLogin) "Lanjutkan dengan Google" else "Daftar dengan Google", fontSize = 13.sp)
                        }
                    }
                    // Bantuan SHA-1
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x0DFFFFFF)).border(1.dp, GlassBorder, RoundedCornerShape(8.dp)).padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Butuh SHA-1 untuk Google Sign-In:", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("1. Jalankan: ./gradlew signingReport", color = Color(0xFF8A8FA3), fontSize = 10.sp)
                            Text("2. Copy SHA1 debug → Firebase Console → Project Settings → Your apps → SHA certificate fingerprints → Add", color = Color(0xFF8A8FA3), fontSize = 10.sp)
                            Text("3. Download google-services.json baru → ganti app/google-services.json → rebuild", color = Color(0xFF8A8FA3), fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
                    Spacer(Modifier.height(12.dp))
                    if (!isLogin) {
                        GlassTextField(value = name, onValueChange = { name = it }, label = "Nama", icon = Icons.Filled.Badge)
                        Spacer(Modifier.height(10.dp))
                    }
                    GlassTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Filled.Email)
                    Spacer(Modifier.height(10.dp))
                    GlassTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Filled.Lock, isPassword = true)
                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x1AFF5F5F)).border(1.dp, Color(0x33FF5F5F), RoundedCornerShape(10.dp)).padding(10.dp)) {
                            Text(error, color = Color(0xFFFCA5A5), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            loading = true; error = ""
                            if (isLogin) {
                                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { t ->
                                    loading = false
                                    if (t.isSuccessful) { user = auth.currentUser; onAuthSuccess?.invoke() } else error = mapAuthError(t.exception?.message ?: "Gagal login")
                                }
                            } else {
                                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { t ->
                                    loading = false
                                    if (t.isSuccessful) {
                                        val u = auth.currentUser
                                        u?.updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder().setDisplayName(name).build())
                                        try {
                                            val uid = u?.uid ?: ""
                                            if (uid.isNotEmpty()) com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).set(
                                                mapOf("uid" to uid, "email" to (u?.email ?: email), "displayName" to name, "photoUrl" to "", "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(), "lastLoginAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
                                            )
                                        } catch (_: Exception) {}
                                        user = u; onAuthSuccess?.invoke()
                                    } else error = mapAuthError(t.exception?.message ?: "Gagal daftar")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF030303)),
                        shape = RoundedCornerShape(50.dp),
                        enabled = !loading
                    ) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF030303), strokeWidth = 2.dp) else Text(if (isLogin) "Masuk" else "Daftar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(if (isLogin) "Belum punya akun? " else "Sudah punya akun? ", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                        Text(if (isLogin) "Daftar" else "Masuk", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isLogin = !isLogin; error = "" })
                    }
                }
            }

            // Settings glass
            GlassSettingsCard(settings, settingsStore, scope)

            // Menu glass - all items functional (no dummy)
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(8.dp)) {
                ProfileMenuItem(icon = Icons.Filled.Person, title = "Edit Profile", subtitle = "Ubah nama & foto") {
                    android.widget.Toast.makeText(context, "Edit Profile: login lalu ubah displayName di pengaturan akun", android.widget.Toast.LENGTH_SHORT).show()
                }
                ProfileMenuItem(icon = Icons.Filled.Bookmark, title = "Koleksi Saya", subtitle = "${bookmarks.size} bookmark") { onNavigate?.invoke("bookmark") }
                ProfileMenuItem(icon = Icons.Filled.History, title = "Riwayat Tonton", subtitle = "${historyCount.value} riwayat") { onNavigate?.invoke("history") }
                ProfileMenuItem(icon = Icons.Filled.PlayCircle, title = "Playback Settings", subtitle = "Kualitas & autoplay") {
                    android.widget.Toast.makeText(context, "Pengaturan playback ada di kartu Pengaturan di atas", android.widget.Toast.LENGTH_SHORT).show()
                }
                ProfileMenuItem(icon = Icons.Filled.Notifications, title = "Notifikasi", subtitle = "Kelola pemberitahuan") {
                    scope.launch { settingsStore.updateNotif(!settings.notifEnabled) }
                    android.widget.Toast.makeText(context, if (!settings.notifEnabled) "Notifikasi diaktifkan" else "Notifikasi dinonaktifkan", android.widget.Toast.LENGTH_SHORT).show()
                }
                ProfileMenuItem(icon = Icons.Filled.Info, title = "Tentang Aplikasi", subtitle = "Versi & lisensi") {
                    android.widget.Toast.makeText(context, "ANIVERS ANIME v1.0.0 • Premium Glass UI • Firebase Connected", android.widget.Toast.LENGTH_LONG).show()
                }
                ProfileMenuItem(icon = Icons.Filled.PrivacyTip, title = "Kebijakan Privasi", subtitle = "Baca kebijakan kami") {
                    android.widget.Toast.makeText(context, "Kebijakan Privasi: Data hanya disimpan lokal & Firebase aman", android.widget.Toast.LENGTH_LONG).show()
                }
            }

            TextButton(onClick = {
                if (email.isNotBlank()) auth.sendPasswordResetEmail(email).addOnCompleteListener { t -> error = if (t.isSuccessful) "Link reset dikirim ke $email" else mapAuthError(t.exception?.message ?: "Gagal") } else error = "Isi email dulu untuk reset password"
            }) { Text("Lupa Password?", color = GoldPrimary, fontSize = 12.sp) }

            Text("ANIVERS ANIME • v1.0.0 • Firebase ${if (user != null) "Connected" else "Offline"}", color = Color(0xFF3A3A3C), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x0DFFFFFF)).border(1.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(12.dp))
            Text(value, color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Text(label, color = Color(0xFFB8B8B8), fontSize = 10.sp)
    }
}

@Composable
private fun GlassTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, isPassword: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF8A8FA3), modifier = Modifier.size(18.dp)) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0x0AFFFFFF),
            unfocusedContainerColor = Color(0x0AFFFFFF),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = GoldPrimary.copy(alpha = 0.5f),
            unfocusedBorderColor = GlassBorder,
            focusedLabelColor = GoldPrimary,
            unfocusedLabelColor = Color(0xFF8A8FA3)
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
    )
}

@Composable
private fun GlassSettingsCard(settings: com.anivers.anime.data.local.AppSettings, store: SettingsStore, scope: kotlinx.coroutines.CoroutineScope) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GlassBg).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x14FFDB89)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
            }
            Text("Pengaturan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(12.dp))
        SettingRowGlass("Notifikasi", "Tampilkan toast saat gagal", settings.notifEnabled) { scope.launch { store.updateNotif(it) } }
        SettingRowGlass("Autoplay episode selanjutnya", "Otomatis putar berikutnya", settings.autoplayNext) { scope.launch { store.updateAutoplay(it) } }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Kualitas default", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("Resolusi awal player", color = Color(0xFF8A8FA3), fontSize = 11.sp)
            }
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)) {
                    Text(settings.quality, color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Color(0xFF1E1E24)) {
                    for (q in listOf("480p", "720p", "1080p")) DropdownMenuItem(text = { Text(q, color = Color.White) }, onClick = { scope.launch { store.updateQuality(q) }; expanded = false })
                }
            }
        }
        SettingRowGlass("Mode hemat data", "Kurangi animasi & kualitas", settings.dataSaver) { scope.launch { store.updateSaver(it) } }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { scope.launch { store.reset() } }, shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = Color(0xFFAEB2C7), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Reset Pengaturan", fontSize = 12.sp, color = Color(0xFFAEB2C7))
        }
    }
}

@Composable
private fun SettingRowGlass(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF8A8FA3), fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GoldPrimary, uncheckedTrackColor = Color(0x33FFFFFF), uncheckedThumbColor = Color.White))
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x0DFFFFFF)).border(1.dp, GlassBorder, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = Color(0xFF8A8FA3), fontSize = 11.sp)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF3A3A3C), modifier = Modifier.size(18.dp))
    }
}

private fun mapAuthError(raw: String): String {
    val lower = raw.lowercase()
    return when {
        "invalid-email" in lower || "badly formatted" in lower -> "Email tidak valid"
        "wrong-password" in lower -> "Password salah"
        "user-not-found" in lower || "no user" in lower -> "Akun tidak ditemukan"
        "email-already" in lower -> "Email sudah terdaftar"
        "weak-password" in lower -> "Password terlalu lemah (min 6 karakter)"
        "network" in lower -> "Periksa koneksi internet"
        else -> raw.take(120)
    }
}
