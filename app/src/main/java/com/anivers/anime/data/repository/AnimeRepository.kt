package com.anivers.anime.data.repository

import com.anivers.anime.data.api.RetrofitClient
import com.anivers.anime.data.model.*
import com.anivers.anime.utils.Normalizer
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun getSeries(slug: String): SeriesDetail? = withContext(Dispatchers.IO) {
        val cleans = Normalizer.slugCandidates(slug)
        require(cleans.isNotEmpty() && cleans.first().length >= 2) { "Slug tidak valid: $slug" }
        var lastErr: Exception? = null
        // coba slug langsung dulu, lalu kandidat alias, lalu search fallback
        for (clean in cleans) {
            try {
                android.util.Log.d("ANIVERS_API", "getSeries try slug=$clean orig=$slug")
                val body = mapOf("get" to "top", "post_type" to "1", "post_id" to clean, "token" to "")
                val res = api.getSeries(clean, body)
                val parsed = parseSeriesDetail(res)
                if (parsed != null) return@withContext parsed
                // jika parse null tapi request 200, coba next candidate
                android.util.Log.w("ANIVERS_API", "getSeries parse null for $clean raw=${res.toString().take(120)}")
            } catch (e: Exception) {
                android.util.Log.w("ANIVERS_API", "getSeries fail clean=$clean", e)
                lastErr = e
                // jika error Expected value at line 1 column 5 (plain false), coba next
                if (e.message?.contains("Expected value") == true || e.message?.contains("BEGIN_ARRAY") == true) continue
                else throw e
            }
        }
        // fallback: search keyword dari judul
        try {
            val keyword = slug.replace('-', ' ').take(40)
            android.util.Log.d("ANIVERS_API", "getSeries fallback search keyword=$keyword")
            val searchRes = try { api.search(keyword) } catch (_: Exception) { null }
            if (searchRes != null) {
                val list = extractSearchAnimes(searchRes)
                val best = list.firstOrNull { it.url.isNotEmpty() && it.judul.contains("hanayome", ignoreCase = true) } ?: list.firstOrNull()
                if (best != null && best.url.isNotEmpty() && best.url != cleans.first()) {
                    android.util.Log.d("ANIVERS_API", "getSeries fallback found ${best.url} for $slug")
                    val body = mapOf("get" to "top", "post_type" to "1", "post_id" to best.url, "token" to "")
                    val res2 = api.getSeries(best.url, body)
                    val parsed2 = parseSeriesDetail(res2)
                    if (parsed2 != null) return@withContext parsed2
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ANIVERS_API", "getSeries search fallback fail", e)
        }
        // jika semua gagal, throw last error atau null
        if (lastErr != null) throw lastErr
        // coba raw dump untuk debug
        try {
            val first = cleans.first()
            val body = mapOf("get" to "top", "post_type" to "1", "post_id" to first, "token" to "")
            val res = api.getSeries(first, body)
            android.util.Log.e("ANIVERS_SERIES_RAW", "raw for $first: ${res.toString().take(400)}")
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
