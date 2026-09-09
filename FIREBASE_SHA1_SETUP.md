# Firebase Google Sign-In — SHA-1 Setup

Agar tombol **"Lanjutkan dengan Google"** di `ProfileScreen` berfungsi, SHA-1 debug & release HARUS terdaftar di Firebase Console.

## Kenapa error 10 / DEVELOPER_ERROR?
Google Sign-In memverifikasi SHA-1 dari APK yang memanggil `requestIdToken`. Jika SHA-1 tidak ada di Firebase → error 10.

## Cara ambil SHA-1 (debug)

### Opsi 1 — Gradle signingReport (paling mudah, auto-generate debug keystore)
```bash
./gradlew signingReport
```
Output contoh:
```
Variant: debug
Config: debug
Store: ~/.android/debug.keystore
SHA1: 5E:8F:16:06:2E:A3:CD:2C:4A:0D:54:78:76:BA:A6:F3:8C:AB:FF:25
SHA-256: ...
Valid until: ...
```

Copy `SHA1` tersebut.

### Opsi 2 — keytool manual
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```
Jika `~/.android/debug.keystore` belum ada, build sekali (`./gradlew assembleDebug`) akan membuatnya otomatis.

### Untuk Release (jika sudah punya keystore)
```bash
keytool -list -v -keystore app/release.keystore -alias <alias> -storepass <pass> | grep SHA1
```

## Tambahkan ke Firebase
1. Buka https://console.firebase.google.com → Project **anivers-anime** (atau project_id di `app/google-services.json`)
2. Project Settings (ikon gear) → **Your apps** → pilih Android `com.anivers.anime`
3. Scroll ke **SHA certificate fingerprints** → **Add fingerprint** → paste SHA1 debug tadi
4. Ulangi untuk SHA1 release jika ada
5. **Download `google-services.json` baru** → ganti file `app/google-services.json` di repo
6. Rebuild: `./gradlew assembleDebug`

## File di repo
- `app/build.gradle.kts` sudah menambahkan `play-services-auth:20.7.0` dan `Firebase Auth` Google provider.
- `app/google-services.json` placeholder akan otomatis diganti saat build jika secret `GOOGLE_SERVICES_JSON` ada di GitHub Actions (`.github/workflows/android.yml` step `Setup google-services.json`).
- `ProfileScreen.kt` sekarang implement Google Sign-In penuh dengan `GoogleSignInClient` + `GoogleAuthProvider.getCredential(idToken)` + Firestore `users/{uid}`. Tombol sudah aktif, error ditampilkan jika SHA1 belum terpasang.

## Test
- Install debug APK di device, buka **Profile → Lanjutkan dengan Google** → picker akun muncul → setelah pilih, toast **"Login Google berhasil!"** dan avatar muncul.
- Jika masih error 10, cek `logcat` ada `DEVELOPER_ERROR` → berarti SHA1 di Firebase masih salah / `google-services.json` belum di-update.
- Email/password tetap berfungsi sebagai fallback tanpa SHA1.

## Cara lewat GitHub Actions (kamu pakai Action, tidak punya keytool lokal)

Karena kamu build via **GitHub Actions**, ambil SHA-1 langsung dari log Actions — tidak perlu `keytool` lokal:

1. Push apapun ke `main` → `Actions` → `Android CI` → klik run terbaru → tab **Summary**
2. Scroll ke section **"SHA-1 Fingerprints — COPY TO FIREBASE"** (dibuat otomatis oleh workflow)
   ```
   ## SHA-1 Fingerprints — COPY TO FIREBASE
   ### Debug Keystore
   ```
   SHA1: 5E:8F:16:06:2E:A3:CD:2C:4A:0D:54:78:76:BA:A6:F3:8C:AB:FF:25
   SHA-256: ...
   ```
3. Copy **SHA1** debug → Firebase Console → Add fingerprint (step di atas)
4. Jika ada release keystore (`ANDROID_KEYSTORE_BASE64` secrets), section **Release Keystore** juga muncul — copy SHA1 release juga

**Debug ephemeral vs deterministik:**
- Default tanpa secret `ANDROID_DEBUG_KEYSTORE_BASE64`: SHA-1 **ganti tiap run** (runner baru `~/.android/debug.keystore` random) → kamu harus update Firebase tiap push (hanya untuk test cepat)
- **Rekomendasi:** bikin debug keystore stabil sekali:
  ```bash
  # lokal (jika punya Java) atau pakai Actions log sekali:
  keytool -genkey -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US" -validity 10000
  base64 -w0 debug.keystore   # copy output
  ```
  Set secret GitHub `ANDROID_DEBUG_KEYSTORE_BASE64` = output base64 tersebut → workflow decode ke `app/debug.keystore` → SHA-1 jadi **tetap** tiap run (workflow `Setup Debug Keystore`).

Workflow sekarang:
- `Setup Debug Keystore` (sebelum build) — pakai `app/debug.keystore` jika secret ada, else ephemeral
- `Setup Release Keystore` — decode `app/release.keystore` dari `ANDROID_KEYSTORE_BASE64` secrets
- `Build Debug APK` — `./gradlew assembleDebug` (pakai signingConfigs debug/release dari `app/build.gradle.kts`)
- `Get SHA-1 / SHA-256` — `keytool -list -v` + `./gradlew signingReport` → output ke **$GITHUB_STEP_SUMMARY** + log console → bisa di-copy langsung tanpa `keytool` lokal

## CI
GitHub Actions `Android CI` sekarang otomatis cetak SHA-1 di **Summary**. Tanpa SHA-1 terdaftar, Google Sign-In tetap tampilkan pesan bantuan SHA1 di UI (`ProfileScreen` help box) tapi app tetap build & jalan untuk semua fitur lain. Untuk CI dengan Google Sign-In real, set secret `GOOGLE_SERVICES_JSON` (base64 dari file asli) + `ANDROID_DEBUG_KEYSTORE_BASE64` (jika mau deterministik) di repo Settings → Secrets → Actions.
