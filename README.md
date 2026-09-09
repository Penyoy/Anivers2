![icon](icon-anivers.png)


# ANIVERS ANIME — Android Native Streaming Anime

Aplikasi Android native streaming anime premium dengan package `com.anivers.anime`. Source lengkap siap push ke GitHub dan build via GitHub Actions tanpa Android Studio.

**Package:** `com.anivers.anime`  
**Namespace / applicationId:** `com.anivers.anime`  
**Target:** `compileSdk 35`, `minSdk 24`, `targetSdk 35`  
**Bahasa:** Kotlin + Jetpack Compose Material3 + MVVM + Coroutines + StateFlow

---

## 1. Project Overview

ANIVERS ANIME adalah aplikasi streaming anime subtitle Indonesia dengan data dari `animeloversv3-api.txt` (Animekita). Desain premium cinematic dark theme:

* **Primary Gold** `#FFDB89` — accent
* **Background** `#030303` — dominan
* **Surface** `#2C2C2E` / **Surface Dark** `#1C1C1E`
* **Text Primary** `#FFFFFF`, **Secondary** `#B8B8B8`, **Divider** `#3A3A3C`, **Error** `#FF5F5F`, **Success** `#8FD694`

Bottom Navigation 5 item (§7): **Home, Search, Bookmark, History, Profile** — active gold `#FFDB89`, inactive abu-abu, animasi ringan.

Flow: `Splash (fade+scale ANIVERS ANIME gold) → Firebase Auth check → Home (login) / Welcome (guest) → Home/Detail/Episode → Media3 Player`

API: langsung `https://apps.animekita.org/api/v1.2.5` via Retrofit+OkHttp dengan UA `Dart/3.9` & `Flutter/2.5.3` untuk episode, siap dialihkan ke **API Proxy** tanpa ganti code.

---

## 2. Requirements

* Android SDK 35, JDK 17
* Firebase Project (Auth + Firestore)
* GitHub account untuk Actions
* `animeloversv3-api.txt` sudah ada di root repo (sumber kebenaran)

---

## 3. Package Name

```
com.anivers.anime
```
Semua `namespace`, `applicationId`, `AndroidManifest`, `google-services.json` → `com.anivers.anime`. Jangan pakai package contoh lain.

---

## 4. Firebase Setup

1. Buka [Firebase Console](https://console.firebase.google.com) → Add Project
2. Add Android App:
   * Package: `com.anivers.anime`
   * App nickname: `ANIVERS ANIME`
   * SHA-256: lihat §7
3. Download `google-services.json` → simpan di `app/google-services.json`
4. Enable **Authentication → Email/Password** (dan Google jika perlu)
5. Enable **Firestore Database** → Create in Test Mode → nanti ganti rules `firestore.rules`

---

## 5. Firebase Authentication

* **Login:** Email, Password
* **Register:** Username/displayName, Email, Password, Confirm Password
* **Forgot Password:** `sendPasswordResetEmail`
* **Logout:** Profile → Logout
* **Google Sign-In:** opsional, butuh SHA-256 (§7)
* **Error handling tanpa stack trace:** `invalid-email`, `wrong-password`, `user-not-found`, `email-already-in-use`, `weak-password`, `network`

`FirebaseAuth.getInstance()` + `FirebaseFirestore.getInstance()` di `AniVerseApp.kt:1` & `BookmarkRepository.kt:1`.

---

## 6. Firestore

Semua data user **WAJIB Firestore**, bukan backend lain.

**Struktur:**
```
users/{uid}
  {uid, email, displayName, photoUrl, createdAt, lastLoginAt}
users/{uid}/bookmarks/{animeId}
  {animeId, title, poster, status, type, createdAt, updatedAt}
users/{uid}/history/{historyId}
  {animeId, episodeId, episodeNumber, title, poster, watchedAt, position, duration, completed}
users/{uid}/watchProgress/{episodeId}
  {animeId, episodeId, episodeNumber, position, duration, updatedAt, completed}
```

Local `Room` & `DataStore` hanya cache/settings (`AppDatabase.kt:1`, `SettingsStore.kt:1`). Source primary tetap Firestore.

---

## 7. SHA-256

**Ini adalah Android Signing Certificate SHA-256, bukan fingerprint sensor.**

Dapatkan via:

```bash
./gradlew signingReport
```

Output debug (`~/.android/debug.keystore`) dan release (keystore kamu) **berbeda**.

Masukkan ke Firebase:

```
Firebase Console → Project Settings → Your Apps → Android App → SHA certificate fingerprints → Add fingerprint → paste SHA-256
```

Wajib jika pakai Google Sign-In, FCM.

---

## 8. google-services.json

* Real file: `app/google-services.json` — **jangan commit** (sudah di `.gitignore`)
* Example: `app/google-services.json.example` — commit sebagai template
* Jika file belum ada, CI generate placeholder agar build tetap jalan (Auth tidak fungsi)
* README ini instruksi: “Place your Firebase google-services.json in app/google-services.json”

---

## 9. API Configuration

Sumber kebenaran: `animeloversv3-api.txt` — **jangan karang endpoint**. DTO Kotlin di `data/model/Anime.kt:1` & `data/api/ApiService.kt:1` mengikuti response nyata:

| Fitur | Endpoint | Method | File |
|-------|----------|--------|------|
| BaruUpload | `baruupload.php?page=1` | GET | `getBaruUpload()` |
| Movie | `movie.php` | GET | `getMovie()` |
| Rekomendasi | `rekomendasi.php` | GET | `getRekomendasi()` |
| Ongoing | `home/ongoing.php?page=1&type=all` | GET | `getOngoing()` |
| Search | `search.php?keyword=` | GET | `search()` |
| Jadwal | `jadwal.php` | POST (no body) | `getJadwal()` |
| Series | `series.php?url=` body `{get,post_type,post_id,token:""}` | POST | `getSeries()` |
| Episode Data | `series/episode/data.php?url=` body `{token: Constants.EPISODE_TOKEN}` | POST | `getEpisodeData()` |
| Genre | `genreseries.php?page=1&url=action/` | GET | `getGenre()` |

`API_DOCUMENTATION.md` berisi tabel lengkap.

Token episode: `Constants.kt:9` hardcode `75504c37...` sesuai capture; `series.php` token kosong.

---

## 10. API Proxy

Arsitektur (§2):

```
Android → HTTPS → API Proxy → AnimeLovers API → Proxy → Android
Android hanya tau proxy URL, mis. https://api.example.com
```

Proxy meng-hide secret jika upstream butuh key:

```
UPSTREAM_API_URL=https://apps.animekita.org/api/v1.2.5
UPSTREAM_API_KEY=secret  # hanya di server env
```

* Android `BuildConfig.API_BASE` default `https://apps.animekita.org/api/v1.2.5` — langsung.
* Untuk pakai proxy, override di `local.properties` / `build.gradle.kts`:

```kotlin
buildConfigField("String","API_BASE","\"https://api.example.com/api/v1.2.5\"")
```

Folder `proxy/` (Node.js) sudah siap: `proxy/package.json`, `proxy/src/index.js`, `proxy/.env.example`. Jangan commit `proxy/.env`.

Jika upstream tidak butuh secret, tetap pakai arsitektur proxy-ready.

---

## 11. Environment Variables

**Proxy (`proxy/.env`):**

| Var | Contoh |
|-----|--------|
| `UPSTREAM_API_URL` | `https://apps.animekita.org/api/v1.2.5` |
| `ALT_UPSTREAM_API_URL` | `https://users.animekita.org/api/v1.2.5` |
| `UPSTREAM_API_KEY` | `your_secret` (jika perlu) |
| `PORT` | `3000` |

Copy `proxy/.env.example` → `proxy/.env`, jangan commit.

**Android (`local.properties`):**

```properties
sdk.dir=/opt/android-sdk
proxyUrl=https://api.example.com/api/v1.2.5
```

**Jangan simpan di:** `Kotlin source`, `strings.xml`, `BuildConfig` hardcode secret, `AndroidManifest`, APK.

---

## 12. GitHub Actions

Workflow: `.github/workflows/android.yml`

```
Checkout → Setup JDK17 → Setup Android SDK → chmod +x gradlew → Generate Wrapper if missing
→ Create google-services.json from secret/placeholder → ./gradlew assembleDebug → Upload APK
```

Trigger: `push` ke `main/master`, `pull_request`, `workflow_dispatch`.

Artifact: `anivers-anime-debug.apk` (dan `anivers-anime-release.apk` jika keystore secrets ada).

**Tanpa Android Studio:** push → Actions autobuild.

---

## 13. Debug Build

```bash
git push origin main
# Actions → Download anivers-anime-debug.apk
```

Lokal (jika punya JDK):

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Placeholder `google-services.json` membuat debug tetap build tanpa Firebase real (login tidak fungsi).

---

## 14. Release Build

Sediakan keystore, **jangan commit** `*.jks`:

```bash
keytool -genkey -v -keystore release.jks -alias anivers -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 release.jks > keystore.b64
```

Set di GitHub **Settings → Secrets**:

* `ANDROID_KEYSTORE_BASE64` (isi `keystore.b64`)
* `ANDROID_KEYSTORE_PASSWORD`
* `ANDROID_KEY_ALIAS`
* `ANDROID_KEY_PASSWORD`

Jika secrets ada, workflow otomatis `assembleRelease` + upload `anivers-anime-release.apk`. Jika belum, debug tetap jalan.

---

## 15. GitHub Secrets

| Secret | Isi |
|--------|-----|
| `GOOGLE_SERVICES_JSON` | `base64 -w 0 app/google-services.json` atau raw JSON |
| `ANDROID_KEYSTORE_BASE64` | `base64 -w 0 release.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | password keystore |
| `ANDROID_KEY_ALIAS` | alias |
| `ANDROID_KEY_PASSWORD` | key password |

Workflow decode otomatis: `base64 --decode > app/google-services.json` & `app/release.keystore`.

---

## 16. Cara Mendapatkan APK

1. Push ke `https://github.com/Penyoy/Anivers2.git`
2. Buka GitHub → **Actions** → pilih run terbaru → **Artifacts** → `anivers-anime-debug.apk`
3. Install di device Android (allow unknown sources)

Lokal APK path: `app/build/outputs/apk/debug/` dan `release/` jika ada keystore.

---

## 17. Firestore Security Rules

File: `firestore.rules`

* User hanya akses `users/{uid}` dan subcollection miliknya via `request.auth.uid == userId`
* Auth wajib untuk bookmark/history/progress/profile
* Deny `/{document=**}` default

Deploy:

```bash
firebase deploy --only firestore:rules
# atau copy manual di Console → Firestore → Rules
```

Lihat `firestore.rules:1` untuk validasi `isOwner()`, `isValidBookmark()` dll.

---

## Tambahan

* **Gradle Wrapper:** `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar` wajib ada — CI generate jika placeholder/missing.
* **.gitignore:** `.gradle/`, `build/`, `**/build/`, `local.properties`, `.env`, `*.jks`, `google-services.json`
* **Struktur:** lihat `AniversAnime/` di §41 request — sudah sesuai.
* **Build cek SHA:** `./gradlew signingReport` untuk Firebase Console.

Sumber API terdokumentasi penuh di `API_DOCUMENTATION.md`.
