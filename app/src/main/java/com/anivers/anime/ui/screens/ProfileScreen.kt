package com.anivers.anime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import coil.compose.AsyncImage
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.local.SettingsStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onAuthSuccess: (() -> Unit)? = null) {
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

    LaunchedEffect(Unit) {
        // history count
        historyCount.value = db.historyDao().getAll().size
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF030303)).verticalScroll(rememberScrollState()).padding(bottom = 100.dp).padding(horizontal = 16.dp).padding(top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (user != null) {
            // profile card
            Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF2C2C2E)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(84.dp).clip(CircleShape).background(Color(0xFFFFDB89)), contentAlignment = Alignment.Center) {
                    if (user?.photoUrl != null) AsyncImage(model = user?.photoUrl.toString(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    else Text((user?.displayName?.take(2) ?: "U").uppercase(), color = Color(0xFF030303), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(user?.displayName ?: "User", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(user?.email ?: "", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${bookmarks.size}", color = Color(0xFFFFDB89), fontWeight = FontWeight.Bold); Text("Bookmark", color = Color(0xFFB8B8B8), fontSize = 11.sp) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${historyCount.value}", color = Color(0xFFFFDB89), fontWeight = FontWeight.Bold); Text("Ditonton", color = Color(0xFFB8B8B8), fontSize = 11.sp) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("0", color = Color(0xFFFFDB89), fontWeight = FontWeight.Bold); Text("Notifikasi", color = Color(0xFFB8B8B8), fontSize = 11.sp) }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { auth.signOut(); user = null },
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5))
                ) { Text("Logout", fontSize = 13.sp) }
            }
        } else {
            // auth card
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF2C2C2E)).padding(20.dp)) {
                Text(if (isLogin) "Login" else "Daftar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(if (isLogin) "Masuk untuk bookmark dan riwayat" else "Buat akun baru", color = Color(0xFFB8B8B8), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        // Google login stub - need GoogleSignIn client, show placeholder
                        error = "Google Sign-In butuh SHA-1 di Firebase. Gunakan email untuk sekarang."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(50.dp)
                ) { Text(if (isLogin) "Login dengan Google" else "Daftar dengan Google", fontSize = 13.sp) }
                Spacer(Modifier.height(12.dp))
                if (!isLogin) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                if (error.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(error, color = Color(0xFFFCA5A5), fontSize = 12.sp) }
                Spacer(Modifier.height(12.dp))
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
                                    // Save user profile to Firestore users/{uid}
                                    try {
                                        val uid = u?.uid ?: ""
                                        if (uid.isNotEmpty()) {
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).set(
                                                mapOf(
                                                    "uid" to uid,
                                                    "email" to (u?.email ?: email),
                                                    "displayName" to name,
                                                    "photoUrl" to "",
                                                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                                    "lastLoginAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                                )
                                            )
                                        }
                                    } catch (_: Exception) {}
                                    user = u
                                    onAuthSuccess?.invoke()
                                } else error = mapAuthError(t.exception?.message ?: "Gagal daftar")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDB89)),
                    shape = RoundedCornerShape(50.dp),
                    enabled = !loading
                ) { Text(if (isLogin) "Login" else "Daftar", fontSize = 14.sp) }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(if (isLogin) "Belum punya akun? " else "Sudah punya akun? ", color = Color(0xFF8A8FA3), fontSize = 12.sp)
                    Text(if (isLogin) "Daftar di sini" else "Login di sini", color = Color(0xFFFFDB89), fontSize = 12.sp, modifier = Modifier.clickable { isLogin = !isLogin; error = "" })
                }
            }
        }

        // settings card
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF2C2C2E)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Color(0xFFFFDB89), modifier = Modifier.size(18.dp))
                Text("Pengaturan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            SettingRow("Notifikasi", "Tampilkan toast saat gagal", settings.notifEnabled) { scope.launch { settingsStore.updateNotif(it) } }
            SettingRow("Autoplay episode selanjutnya", "Otomatis putar berikutnya", settings.autoplayNext) { scope.launch { settingsStore.updateAutoplay(it) } }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Kualitas default", color = Color.White, fontSize = 13.sp); Text("Resolusi awal player", color = Color(0xFF8A8FA3), fontSize = 11.sp) }
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(50.dp)) { Text(settings.quality, color = Color(0xFFFFDB89), fontSize = 12.sp) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        for (q in listOf("480p","720p","1080p")) {
                            DropdownMenuItem(text = { Text(q) }, onClick = { scope.launch { settingsStore.updateQuality(q) }; expanded = false })
                        }
                    }
                }
            }
            SettingRow("Mode hemat data", "Kurangi animasi", settings.dataSaver) { scope.launch { settingsStore.updateSaver(it) } }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { scope.launch { settingsStore.reset() } }, shape = RoundedCornerShape(50.dp)) { Text("Reset Pengaturan", fontSize = 12.sp, color = Color(0xFFAEB2C7)) }
        }

        // Additional menu per spec
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF2C2C2E)).padding(16.dp)) {
            ProfileMenuItem("Edit Profile") {}
            ProfileMenuItem("Bookmark") {}
            ProfileMenuItem("History") {}
            ProfileMenuItem("Playback Settings") {}
            ProfileMenuItem("Notification Settings") {}
            ProfileMenuItem("About") {}
            ProfileMenuItem("Privacy Policy") {}
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            // Forgot password
            if (email.isNotBlank()) {
                auth.sendPasswordResetEmail(email).addOnCompleteListener { t ->
                    error = if (t.isSuccessful) "Link reset dikirim ke $email" else mapAuthError(t.exception?.message ?: "Gagal")
                }
            } else error = "Isi email dulu untuk reset password"
        }) { Text("Lupa Password?", color = Color(0xFFFFDB89), fontSize = 12.sp) }

        Spacer(Modifier.height(8.dp))
        Text("ANIVERS ANIME • v1.0.0 • Firebase ${if (user!=null) "Connected" else "Offline"}", color = Color(0xFF3A3A3C), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp)
            Text(subtitle, color = Color(0xFFB8B8B8), fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFFDB89)))
    }
}

@Composable
private fun ProfileMenuItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 13.sp)
        Text(">", color = Color(0xFF3A3A3C), fontSize = 12.sp)
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
