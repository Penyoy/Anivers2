# API Documentation — Animekita (animeloversv3-api.txt)

> Sumber kebenaran utama. Jangan karang endpoint. Semua field mengikuti response nyata.

Base utama: `https://apps.animekita.org/api/v1.2.5` (fallback `https://users.animekita.org/api/v1.2.5` untuk genreseries)

User-Agent wajib:
* Semua endpoint: `Dart/3.9 (dart:io)`
* Khusus `series/episode/data.php`: `Flutter/2.5.3`

---

## Endpoint List

| Endpoint | Method | Parameter | Response | Fungsi |
|----------|--------|-----------|----------|--------|
| `baruupload.php` | GET | `page` int query (1..n) | `[{id, url, judul, cover, lastch, lastup, genre[], sinopsis, studio, score, status, rilis, total_episode}]` | Anime baru di-upload, pagination. Contoh: `GET /baruupload.php?page=1` |
| `movie.php` | GET | — | `[{id, url, judul, cover, lastch, lastup}]` minimal | Movie list |
| `rekomendasi.php` | GET | — | `[{id, url, judul, cover, genre[], sinopsis, studio, score, status, rilis, total_episode}]` | Rekomendasi (Top). Digunakan untuk Hot/Popular |
| `home/ongoing.php` | GET | `page` int, `type` string (`all`) | `[{id, url, judul, cover, lastch, lastup, type}]` | Ongoing list, support pagination |
| `search.php` | GET | `keyword` string (min 2 chars, URL-encode) | `{"data":[{jumlah, result:[{id,url,judul,cover,genre[],sinopsis,studio,score,status,rilis,total_episode}], pagination:{page,per_page,total,total_pages,has_next}}]}` | Global search |
| `series.php` | POST | `url` query `?url=slug`, Body `{"get":"top","post_type":"1","post_id":"slug","token":""}` Content-Type `text/plain` | `{"data":[{id, series_id, bookmark, cover, judul, type, status, rating, published, author, genre[], genreurl[], sinopsis, chapter:[{id,ch,url,date,views}]}]}` atau `false` jika not found | Anime detail + episode list |
| `series/episode/data.php` | POST | `url` query `?url=al-xxx`, Body `{"post_type":"2","post_id":"al-154129-1","series_id":"slug","series_url":"slug","episode":"1","token":"75504c37..."}` | `{"is_cache":bool,"data":[{"episode_id", "reso":["360p","480p","720p","1080p"], "streams":{"360p":[{"link","provide","id","reso"}],...}, "resoSizeKb":{}}]}` | Streaming URLs per kualitas & server (mp4 `storage.animekita.org`) |
| `genreseries.php` | GET | `page` int, `url` string (`action/`) | `[{id, link, anime_name, thumb, genre[], sinopsis, studio, score, status}]` atau `[{id, url, judul, cover}]` normalizer handles both | Genre list, host fallback `users.animekita.org` |
| `jadwal.php` | POST | **no body** `Content-Length: 0` | `{"generatedAt": ts, "data":[{"day":"Senin","date":"31","date_ts": ts, "animeList":[{"anime_name","id","link","cover","updated"}]}]}` 7 days | Jadwal rilis mingguan |

---

## Detail Response Field

### BaruUpload / Movie / Rekomendasi / Ongoing item

```json
{
  "id": "150898",
  "url": "chaos-child-movie",
  "judul": "Chaos Child Movie",
  "cover": "https://cdn.myanimelist.net/images/anime/9/88031l.jpg",
  "lastch": "",
  "lastup": "Baru di Upload",
  "genre": ["Mystery","Sci-Fi"],
  "sinopsis": "...",
  "studio": "SILVER LINK.",
  "score": "6.48",
  "status": "Completed",
  "rilis": "17 Juni 2017",
  "total_episode": 2
}
```
*Ongoing tambahan `type:"all"`*

### Search

```json
{
  "data": [
    {
      "jumlah": 2,
      "result": [
        {
          "id": "154129",
          "url": "hell-mode-s2-sub-indo",
          "judul": "Hell Mode Season 2",
          "cover": "https://cdn.myanimelist.net/images/anime/1534/156314l.jpg",
          "genre": ["Action","Adventure"],
          "sinopsis": "...",
          "studio": "Yokohama Animation Lab",
          "score": "8.00",
          "status": "Ongoing",
          "rilis": "04 Juli 2026",
          "total_episode": 9
        }
      ],
      "pagination": {"page":1,"per_page":20,"total":2,"total_pages":1,"has_next":false}
    }
  ]
}
```

### Series Detail

```json
{
  "data": [
    {
      "id": 154224,
      "series_id": "kizumonogatari-i",
      "cover": "https://cdn.myanimelist.net/images/anime/1783/112810l.jpg",
      "judul": "Kizumonogatari I",
      "type": "Movie",
      "status": "Completed",
      "rating": "8.36",
      "published": "08 Januari 2016",
      "author": "Shaft",
      "genre": ["Action","Mystery"],
      "genreurl": ["Action","Mystery"],
      "sinopsis": "...",
      "chapter": [
        {"id":154502,"ch":"Movie","url":"al-154224-movie","date":"11 Agustus, 2026","views":2024}
      ]
    }
  ]
}
```

### Episode Data

```json
{
  "is_cache": true,
  "data": [
    {
      "episode_id": 154080,
      "reso": ["360p","480p","720p","1080p"],
      "streams": {
        "360p": [{"link":"https://storage.animekita.org/ro/bdd7-...mp4","provide":871,"id":774635,"reso":"360p"}],
        "480p": [{"link":"https://storage.animekita.org/ro/b5e2-...mp4"}],
        "720p": [{"link":"https://storage.animekita.org/ro/4290-...mp4"}],
        "1080p": [{"link":"https://storage.animekita.org/ro/3aae-...mp4"}]
      }
    }
  ]
}
```

Stream `link` adalah mp4 langsung, play via Media3 ExoPlayer. Jika hanya 1 kualitas, jangan buat selector palsu.

### Jadwal

```json
{
  "generatedAt": 1788179831,
  "data": [
    {
      "day": "Senin",
      "date": "31",
      "date_ts": 1788109200,
      "animeList": [
        {"anime_name":"Kuroneko to Majo","id":153900,"link":"kuroneko-to-majo-no-kyoushitsu-sub-indo","cover":"https://...","updated":1788112264}
      ]
    }
  ]
}
```

### Genre

```json
[
  {"id":"150437","link":"compass-20-sub-indo","anime_name":"#Compass 2.0","thumb":"https://cdn.myanimelist.net/...","genre":["Action"],"sinopsis":"...","studio":"Lay-duce","score":"5.30","status":"Completed"}
]
```

Note: field bisa `thumb`/`cover`, `link`/`url`, `anime_name`/`judul` — `Normalizer.kt` handle.

---

## Error Handling

| HTTP | Arti | Handling |
|------|------|----------|
| 400 | Bad request (keyword kosong, page invalid) | Tampilkan “Request tidak valid” |
| 401 | Unauthorized (jika proxy butuh key) | Cek `UPSTREAM_API_KEY` env |
| 403 | Cloudflare challenge | Retry dengan UA benar, fallback proxy |
| 404 | Endpoint/slug not found (`series.php` return `false`) | Tampilkan “Data tidak ditemukan” |
| 429 | Rate limit | Backoff + retry |
| 500 | Server error | Tampilkan “Terjadi kesalahan, coba lagi” |
| Timeout/Network | No internet | Tampilkan “Periksa koneksi internet” |

Semua Kotlin DTO mengikuti struktur di atas via `Gson` lenient + `Normalizer`.

---

## Token

* `series.php` : `token: ""` (kosong)
* `series/episode/data.php` : `token: "75504c37d8b97486a1211245f2fb21691ac11a791180d26ca2b0b2e85eaffb814185e2250e111b9abeee15ccb1eba55c2b880e335e124c02d517509ee377939d87053383ee0243354230a7a71eb88ffd71752be22f8f2d234884788f70c776ddf2beccfa"` — hardcode `Constants.EPISODE_TOKEN`

Jika expire, update `utils/Constants.kt:1`.

---

## Gambar

* Cover: `https://cdn.myanimelist.net/...`, `https://assets.animekita.org/cover/...` — load via Coil, disk+memory cache, placeholder/error.

---

## Catatan Penting

* Jangan request search per karakter tanpa debounce (320ms).
* Jangan load seluruh data sekaligus — pagination `page` param.
* Jangan buat URL video/subtitle palsu — hanya pakai yang dari API.
* Jadwal `POST` tanpa body, `Content-Length: 0` (via `@Headers("Content-Length: 0")` di `ApiService`).
* Semua request harus `Accept: application/json`.

