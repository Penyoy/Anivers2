# AniVerse — Anime Streaming Android (Kotlin)

Port native Android dari `hanime-main` (Vue) dengan API `animeloversv3-api.txt`. Siap compile via GitHub Actions, minSdk 21 (Android 5), targetSdk 35 (Android 15).

## Fitur sesuai request

- **Home**: klik search bar -> navigasi ke **Search** (tidak inline). Sertakan **Top Anime** (sorted rekomendasi by score). Sections: Trending (Ongoing), New Update (BaruUpload), Hot Anime, Completed (Movie), Jadwal teaser. **More/Show More** redirect ke Explore dengan filter sesuai pilihan (`explore?type=ongoing|baruupload|rekomendasi|movie`) — `HomeScreen.kt:92` & `ExploreScreen.kt:57`
- **Explore**: **Genre di atas** (15 genre pill grid) lalu **Rekomendasi anime dibawahnya** (grid 3 kolom). Menerima `type` dari Home More untuk filtering. — `ExploreScreen.kt:71`
- **Search**: dedicated screen dengan debounce preview 320ms, pagination, quick chips.
- **Recent / Bookmark / Profile**: mirror hanime-main:
  - Recent: `watch_history` Room sorted timestamp, progress bar `RecentScreen.kt:1`
  - Bookmark: Firestore sync + lokal Room, toolbar search+sort, gate login `BookmarkScreen.kt:1`
  - Profile: Firebase Auth (Email+Google stub), stats bookmark/history, Settings (`notifEnabled`, `autoplayNext`, `quality`, `dataSaver`) via DataStore `ProfileScreen.kt:1`

## Tech Stack

- **Kotlin + Jetpack Compose + Material3** + Navigation Compose + ViewModel
- **Retrofit + OkHttp** dengan `User-Agent: Dart/3.9` / `Flutter/2.5.3` untuk episode (`ApiInterceptor.kt`) — langsung ke `apps.animekita.org` tanpa proxy (Android bisa set UA, beda browser yang kena 403 `api.js:7`)
- **Coil** untuk cover CDN (CORS `*`), **Room** untuk bookmark/history, **DataStore** untuk settings
- **Media3 ExoPlayer** untuk `mp4` `storage.animekita.org` (`WatchScreen.kt`)
- **Firebase Auth + Firestore** (sync bookmark seperti `Bookmark.vue`)

## API Mapping

| Fitur | Endpoint | File |
|---|---|---|
| BaruUpload | `GET baruupload.php?page=1` | `AnimeRepository.kt:18` |
| Movie | `GET movie.php` | `getMovie()` |
| Rekomendasi/Top | `GET rekomendasi.php` | `getRekomendasi()` |
| Ongoing | `GET home/ongoing.php?page=1&type=all` | `getOngoing()` |
| Search | `GET search.php?keyword=` | `search()` |
| Series | `POST series.php?url=` body `{get,post_type,post_id,token:""}` | `getSeries()` |
| Episode | `POST series/episode/data.php?url=` body `token=EPISODE_TOKEN` | `getEpisodeData()` |
| Genre | `GET genreseries.php?page=1&url=action/` | `getGenre()` |
| Jadwal | `POST jadwal.php` (no body) | `getJadwal()` |

Token episode hardcode `Constants.kt:9` sesuai capture; untuk `series.php` token kosong.

## Struktur

```
app/src/main/java/com/anivers/anime/
  MainActivity.kt
  AniVerseApp.kt
  data/api/{ApiService,RetrofitClient,ApiInterceptor}
  data/model/Anime.kt
  data/local/{AppDatabase,Entities,Dao,SettingsStore}
  data/repository/{AnimeRepository,BookmarkRepository}
  ui/theme/Theme.kt
  ui/components/{AnimeCard,BottomBar,TopBar,Common}
  ui/navigation/NavGraph.kt
  ui/screens/{Home,Explore,Search,Detail,Watch,Genre,Jadwal,Recent,Bookmark,Profile}
  viewmodel/{Home,Explore,Search,Detail,Watch}ViewModel
  utils/{Constants,Normalizer}
```

## Build

### Via GitHub Actions (recommended, tanpa setup lokal)
1. Push repo ke GitHub. Workflow `.github/workflows/android.yml` otomatis `assembleDebug`.
2. Sediakan `app/google-services.json` yang valid (copy dari `google-services.json.example` lalu isi dari Firebase Console). Jika tidak ada, workflow buat placeholder agar CI tetap jalan (tapi login tidak fungsi).
3. Download artifact `anivers-debug-apk` dari tab Actions.

### Lokal
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Konfigurasi

- `app/build.gradle.kts:16` `buildConfigField API_BASE` default `https://apps.animekita.org/api/v1.2.5`
- Firebase: aktif (`com.google.gms.google-services` plugin). Sudah include `firebase-bom`. Untuk rilis ganti `google-services.json`.

## Catatan

- Android min 21 butuh `coreLibraryDesugaring` sudah diaktifkan.
- Player langsung play `mp4` 720p default, switch reso/server tersedia.
- Jika `EPISODE_TOKEN` expire, update di `Constants.kt`.
