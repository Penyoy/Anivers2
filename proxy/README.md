# ANIVERS ANIME — API Proxy

Layer antara aplikasi Android dan upstream `apps.animekita.org`. Android hanya tahu `PROXY_BASE_URL` (mis. `https://api.example.com`), secret tetap di server via env.

```
Android App → HTTPS → API Proxy → AnimeLovers API → Proxy → App
```

## Kenapa perlu proxy?
Upstream saat ini tidak butuh API key, tapi proxy tetap disiapkan agar:
* Jika nanti upstream menambah `UPSTREAM_API_KEY`, key tidak tertanam di APK (BuildConfig/strings.xml).
* Mengatasi Cloudflare challenge dengan UA khusus (`Dart/3.9`, `Flutter/2.5.3`) di sisi server, bukan browser.
* Centralized retry, caching, logging.

## Setup

```bash
cp .env.example .env
# edit .env
npm install
npm run dev    # http://localhost:3000
# test
curl http://localhost:3000/api/movie.php
curl http://localhost:3000/health
```

## Environment

| Var | Contoh | Ket |
|-----|--------|-----|
| `UPSTREAM_API_URL` | `https://apps.animekita.org/api/v1.2.5` | upstream utama |
| `ALT_UPSTREAM_API_URL` | `https://users.animekita.org/api/v1.2.5` | fallback genreseries |
| `UPSTREAM_API_KEY` | `secret123` | dimasukkan sebagai `x-api-key` ke upstream, **jangan** commit |
| `PORT` | `3000` | |
| `PROXY_BASE_URL` | `https://api.example.com` | URL yang dipakai Android `BuildConfig.API_BASE` |

## Android integration

`app/build.gradle.kts`:
```kotlin
buildConfigField("String","API_BASE","\"https://api.example.com/api/v1.2.5\"")
```
Atau via `local.properties`:
```
proxyUrl=https://api.example.com/api/v1.2.5
```
`RetrofitClient` membaca `BuildConfig.API_BASE` dan `PROXY_FALLBACKS`.

## Deploy

* **Vercel / Render / Fly.io / Cloudflare Worker**: set env vars di dashboard, jangan commit `.env`.
* Worker alternatif lihat `hanime-main/worker/proxy.js`.

## Security

* Jangan commit `.env`, `*.key`, `google-services.json` real.
* `UPSTREAM_API_KEY` hanya di server env, tidak di `strings.xml`, `BuildConfig`, `AndroidManifest`.
