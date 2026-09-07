package com.anivers.anime.data.repository

import com.anivers.anime.data.api.RetrofitClient
import com.anivers.anime.data.model.*
import com.anivers.anime.utils.Normalizer
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AnimeRepository(
    private val api: com.anivers.anime.data.api.ApiService = RetrofitClient.api
) {
    private val gson = Gson()

    suspend fun getBaruUpload(page: Int = 1): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        try {
            api.getBaruUpload(page).map { Normalizer.normalizeAnime(it) }.filter { it.url.isNotEmpty() && it.url != "undefined" }
        } catch (e: Exception) {
            android.util.Log.e("ANIVERS_API", "getBaruUpload failed page=$page", e)
            throw e
        }
    }

    suspend fun getMovie(): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        try {
            api.getMovie().map { Normalizer.normalizeAnime(it) }.filter { it.url.isNotEmpty() }
        } catch (e: Exception) {
            android.util.Log.e("ANIVERS_API", "getMovie failed", e)
            throw e
        }
    }

    suspend fun getRekomendasi(): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        try {
            api.getRekomendasi().map { Normalizer.normalizeAnime(it) }.filter { it.url.isNotEmpty() }
        } catch (e: Exception) {
            android.util.Log.e("ANIVERS_API", "getRekomendasi failed", e)
            throw e
        }
    }

    suspend fun getOngoing(page: Int = 1, type: String = "all"): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        try {
            api.getOngoing(page, type).map { Normalizer.normalizeAnime(it) }.filter { it.url.isNotEmpty() }
        } catch (e: Exception) {
            android.util.Log.e("ANIVERS_API", "getOngoing failed page=$page type=$type", e)
            throw e
        }
    }

    suspend fun getJadwal(): JadwalResponse = withContext(Dispatchers.IO) {
        try { api.getJadwal() } catch (e: Exception) {
            android.util.Log.e("ANIVERS_API", "getJadwal failed", e)
            throw e
        }
    }

    suspend fun search(keyword: String): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        try {
            val raw = api.search(keyword)
            extractSearchAnimes(raw)
        } catch (e: Exception) {
            android.util.Log.e("ANIVERS_API", "search failed keyword=$keyword", e)
            throw e
        }
    }

    suspend fun searchRaw(keyword: String): JsonElement = withContext(Dispatchers.IO) {
        try { api.search(keyword) } catch (e: Exception) {
            android.util.Log.e("ANIVERS_API", "searchRaw failed", e); throw e
        }
    }

    private fun createSeriesBody(clean: String): okhttp3.RequestBody {
        val map = mapOf("get" to "top", "post_type" to "1", "post_id" to clean.trimEnd('/'), "token" to "")
        val json = gson.toJson(map)
        return json.toRequestBody("text/plain; charset=utf-8".toMediaType())
    }

    private fun parseRawSeries(raw: String): com.google.gson.JsonElement {
        val t = raw.trim()
        if (t.isEmpty() || t.equals("false", ignoreCase = true) || t.equals("null", ignoreCase = true)) {
            return com.google.gson.JsonParser.parseString("null")
        }
        // strip HTML warning prefix like <br /><b>Warning...  {"data":...
        val cleaned = if (t.contains("<br") && t.contains("{")) {
            val idx = t.indexOf("{")
            if (idx >= 0) t.substring(idx) else t
        } else t
        return try {
            com.google.gson.JsonParser.parseString(cleaned)
        } catch (e: Exception) {
            android.util.Log.w("ANIVERS_API", "parseRawSeries fail raw=${t.take(200)}", e)
            com.google.gson.JsonParser.parseString("null")
        }
    }

    suspend fun getSeries(slug: String): SeriesDetail? = withContext(Dispatchers.IO) {
        val cleans = Normalizer.slugCandidates(slug)
        require(cleans.isNotEmpty() && cleans.first().trim('/').length >= 2) { "Slug tidak valid: $slug" }
        var lastErr: Exception? = null
        // coba slug langsung dulu, lalu kandidat alias
        for (clean in cleans) {
            val cleanNoSlash = clean.trimEnd('/')
            val cleanWithSlash = if (clean.endsWith("/")) clean else "$clean/"
            for (c in listOf(cleanNoSlash, cleanWithSlash).distinct()) {
                try {
                    android.util.Log.d("ANIVERS_API", "getSeries try slug=$c orig=$slug")
                    val body = createSeriesBody(c)
                    val respBody = api.getSeriesRaw(c.trimEnd('/'), body)
                    val raw = respBody.string()
                    android.util.Log.d("ANIVERS_API", "getSeries raw for $c: ${raw.take(200)}")
                    if (raw.trim().equals("false", ignoreCase = true) || raw.trim().isEmpty()) {
                        android.util.Log.w("ANIVERS_API", "getSeries raw false/empty for $c")
                        continue
                    }
                    val el = parseRawSeries(raw)
                    val parsed = parseSeriesDetail(el)
                    if (parsed != null) return@withContext parsed
                    android.util.Log.w("ANIVERS_API", "getSeries parse null for $c raw=${raw.take(120)}")
                } catch (e: Exception) {
                    android.util.Log.w("ANIVERS_API", "getSeries fail clean=$c", e)
                    lastErr = e
                }
            }
        }
        // fallback: search keyword generik (progressive)
        try {
            fun fallbackKeywords(s: String): List<String> {
                val words = s.replace('-', ' ').split(" ").filter { it.isNotBlank() && it !in setOf("sub","indo","subtitle","indonesia") }
                val stop = setOf("wa","no","wo","ni","ke","naka","de","toubun","hanayome","san","sama","kun","chan")
                val set = LinkedHashSet<String>()
                if (words.isNotEmpty()) set.add(words.take(2).joinToString(" "))
                if (words.isNotEmpty()) set.add(words.first())
                val filtered = words.filter { it !in stop }
                if (filtered.isNotEmpty()) set.add(filtered.take(3).joinToString(" "))
                set.add(s.replace('-',' ').substringBefore("-sub").take(40))
                return set.filter { it.length>=2 && it.length<=40 }.distinct()
            }
            var searchResults: List<com.anivers.anime.data.model.Anime> = emptyList()
            var usedKw = ""
            for (kw in fallbackKeywords(slug)) {
                try {
                    android.util.Log.d("ANIVERS_API", "getSeries fallback search kw=$kw orig=$slug")
                    val sr = api.search(kw)
                    val lst = extractSearchAnimes(sr)
                    if (lst.isNotEmpty()) { searchResults = lst; usedKw = kw; break }
                } catch (_: Exception) {}
            }
            if (searchResults.isNotEmpty()) {
                fun score(a: com.anivers.anime.data.model.Anime): Int {
                    val normUrl = Normalizer.sanitizeSlug(a.url)
                    val q = Normalizer.sanitizeSlug(slug)
                    var s = normUrl.split("-").intersect(q.split("-").toSet()).size * 10
                    if (a.judul.contains(q.replace("-"," "), ignoreCase = true)) s += 5
                    // prefer judul yang mengandung kata pertama query
                    val firstWord = usedKw.split(" ").firstOrNull() ?: ""
                    if (firstWord.isNotEmpty() && a.judul.contains(firstWord, ignoreCase = true)) s += 3
                    return s
                }
                val ranked = searchResults.sortedByDescending { score(it) }.take(3)
                for (best in ranked) {
                    if (best.url.isBlank()) continue
                    val canon = best.url.trimEnd('/') + "/"
                    val variants = listOf(canon, canon.trimEnd('/'), canon.replace("-subtitle-indonesia", "-sub-indo"), canon.replace("-sub-indo", "-subtitle-indonesia")).distinct()
                    for (v in variants) {
                        try {
                            android.util.Log.d("ANIVERS_API", "getSeries fallback retry ${best.judul} url=$v for $slug")
                            val body = createSeriesBody(v)
                            val raw2 = api.getSeriesRaw(v.trimEnd('/'), body).string()
                            if (raw2.trim().equals("false", ignoreCase = true)) continue
                            val el2 = parseRawSeries(raw2)
                            val parsed2 = parseSeriesDetail(el2)
                            if (parsed2 != null) return@withContext parsed2
                        } catch (e: Exception) {
                            android.util.Log.w("ANIVERS_API", "fallback retry fail $v", e)
                            lastErr = e
                        }
                    }
                }
            } else {
                android.util.Log.w("ANIVERS_API", "getSeries fallback search empty for $slug")
            }
        } catch (e: Exception) {
            android.util.Log.w("ANIVERS_API", "getSeries search fallback fail", e)
            lastErr = e
        }
        // jika semua gagal, throw last error atau null
        if (lastErr != null) throw lastErr
        // coba raw dump untuk debug
        try {
            val first = cleans.first()
            val body = createSeriesBody(first)
            val raw = api.getSeriesRaw(first.trimEnd('/'), body).string()
            android.util.Log.e("ANIVERS_SERIES_RAW", "raw for $first: ${raw.take(400)}")
        } catch (_: Exception) {}
        null
    }

    suspend fun getEpisodeData(postUrl: String, seriesUrl: String, episode: String? = null): EpisodeDataResponse = withContext(Dispatchers.IO) {
        try {
            val cleanPost = Normalizer.sanitizeSlug(postUrl)
            val cleanSeries = Normalizer.sanitizeSlug(seriesUrl)
            require(cleanPost.isNotEmpty() && cleanPost != "undefined") { "Episode slug invalid" }
            require(cleanSeries.isNotEmpty() && cleanSeries != "undefined") { "Series slug invalid" }
            val epNum = if (!episode.isNullOrBlank() && episode != "1") episode else Normalizer.parseEpisodeNumber(cleanPost).ifEmpty { "1" }
            val body = mapOf(
                "post_type" to "2",
                "post_id" to cleanPost,
                "series_id" to cleanSeries,
                "series_url" to cleanSeries,
                "episode" to if (epNum == "movie") "1" else epNum,
                "token" to com.anivers.anime.utils.Constants.EPISODE_TOKEN
            )
            api.getEpisodeData(cleanPost, body)
        } catch (e: Exception) {
            android.util.Log.e("ANIVERS_API", "getEpisodeData failed post=$postUrl series=$seriesUrl", e)
            throw e
        }
    }

    suspend fun getGenre(page: Int = 1, genreUrl: String): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        try {
            val raw = api.getGenre(page, genreUrl)
            raw.map { Normalizer.normalizeAnime(it) }.filter { it.url.isNotEmpty() && it.url != "undefined" }
        } catch (e: Exception) {
            android.util.Log.e("ANIVERS_API", "getGenre failed genre=$genreUrl page=$page", e)
            throw e
        }
    }

    // helpers - mirrors api.js extractSearchAnimes
    fun extractSearchAnimes(element: JsonElement): List<com.anivers.anime.data.model.Anime> {
        if (element == null || element.isJsonNull) return emptyList()
        try {
            if (element.isJsonArray) {
                val arr = element.asJsonArray
                if (arr.size() > 0 && arr[0].asJsonObject.has("judul")) {
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val list: List<Map<String, Any>> = gson.fromJson(element, type)
                    return list.map { Normalizer.normalizeAnime(it) }
                }
            }
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                if (obj.has("data")) {
                    val data = obj.get("data")
                    if (data.isJsonArray) {
                        val arr = data.asJsonArray
                        if (arr.size() > 0) {
                            val first = arr[0].asJsonObject
                            if (first.has("result")) {
                                val result = first.get("result")
                                if (result.isJsonArray) {
                                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                                    val list: List<Map<String, Any>> = gson.fromJson(result, type)
                                    return list.map { Normalizer.normalizeAnime(it) }
                                }
                            }
                            if (first.has("judul") || first.has("link")) {
                                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                                val list: List<Map<String, Any>> = gson.fromJson(data, type)
                                return list.map { Normalizer.normalizeAnime(it) }
                            }
                        }
                    }
                    if (data.isJsonObject) {
                        val dObj = data.asJsonObject
                        if (dObj.has("result")) {
                            val result = dObj.get("result")
                            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                            val list: List<Map<String, Any>> = gson.fromJson(result, type)
                            return list.map { Normalizer.normalizeAnime(it) }
                        }
                    }
                }
                if (obj.has("result")) {
                    val result = obj.get("result")
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val list: List<Map<String, Any>> = gson.fromJson(result, type)
                    return list.map { Normalizer.normalizeAnime(it) }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return emptyList()
    }

    private fun parseSeriesDetail(element: JsonElement): SeriesDetail? {
        return try {
            if (element == null || element.isJsonNull) return null
            // jika server balas primitive string/false (cases gotoubun not found: "false" 5 char)
            if (element.isJsonPrimitive) {
                val prim = element.asJsonPrimitive
                if (prim.isString) {
                    val s = prim.asString.trim()
                    android.util.Log.w("ANIVERS_API", "parseSeriesDetail primitive string: $s")
                    if (s.equals("false", ignoreCase = true) || s.equals("null", ignoreCase = true) || s.isEmpty()) return null
                }
                if (prim.isBoolean && !prim.asBoolean) return null
                return null
            }
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                if (obj.has("data")) {
                    val d = obj.get("data")
                    if (d == null || d.isJsonNull) return null
                    if (d.isJsonPrimitive) {
                        // data: "false" / "null"
                        android.util.Log.w("ANIVERS_API", "parseSeriesDetail data primitive: $d")
                        return null
                    }
                    if (d.isJsonArray && d.asJsonArray.size() > 0) {
                        val first = d.asJsonArray[0]
                        if (first.isJsonPrimitive) return null
                        return gson.fromJson(first, SeriesDetail::class.java)
                    }
                    if (d.isJsonObject) return gson.fromJson(d, SeriesDetail::class.java)
                } else {
                    // langsung object series tanpa wrapper data (beberapa mirror)
                    if (obj.has("judul") || obj.has("cover") || obj.has("chapter")) {
                        return gson.fromJson(obj, SeriesDetail::class.java)
                    }
                }
            }
            if (element.isJsonArray && element.asJsonArray.size() > 0) {
                val first = element.asJsonArray[0]
                if (first.isJsonObject) return gson.fromJson(first, SeriesDetail::class.java)
            }
            android.util.Log.w("ANIVERS_API", "parseSeriesDetail unhandled shape: ${element.toString().take(200)}")
            null
        } catch (e: Exception) {
            android.util.Log.e("ANIVERS_API", "parseSeriesDetail error element=${element.toString().take(200)}", e)
            null
        }
    }
}
