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
        api.getBaruUpload(page).map { Normalizer.normalizeAnime(it) }.filter { it.url.isNotEmpty() && it.url != "undefined" }
    }

    suspend fun getMovie(): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        api.getMovie().map { Normalizer.normalizeAnime(it) }.filter { it.url.isNotEmpty() }
    }

    suspend fun getRekomendasi(): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        api.getRekomendasi().map { Normalizer.normalizeAnime(it) }.filter { it.url.isNotEmpty() }
    }

    suspend fun getOngoing(page: Int = 1, type: String = "all"): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        api.getOngoing(page, type).map { Normalizer.normalizeAnime(it) }.filter { it.url.isNotEmpty() }
    }

    suspend fun getJadwal(): JadwalResponse = withContext(Dispatchers.IO) { api.getJadwal() }

    suspend fun search(keyword: String): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        val raw = api.search(keyword)
        extractSearchAnimes(raw)
    }

    suspend fun searchRaw(keyword: String): JsonElement = withContext(Dispatchers.IO) { api.search(keyword) }

    suspend fun getSeries(slug: String): SeriesDetail? = withContext(Dispatchers.IO) {
        val clean = Normalizer.sanitizeSlug(slug)
        require(clean.length >= 2 && clean != "undefined" && clean != "null") { "Slug tidak valid: $slug" }
        val body = mapOf("get" to "top", "post_type" to "1", "post_id" to clean, "token" to "")
        val res = api.getSeries(clean, body)
        parseSeriesDetail(res)
    }

    suspend fun getEpisodeData(postUrl: String, seriesUrl: String, episode: String? = null): EpisodeDataResponse = withContext(Dispatchers.IO) {
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
    }

    suspend fun getGenre(page: Int = 1, genreUrl: String): List<com.anivers.anime.data.model.Anime> = withContext(Dispatchers.IO) {
        val raw = api.getGenre(page, genreUrl)
        raw.map { Normalizer.normalizeAnime(it) }.filter { it.url.isNotEmpty() && it.url != "undefined" }
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
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                if (obj.has("data")) {
                    val d = obj.get("data")
                    if (d.isJsonArray && d.asJsonArray.size() > 0) {
                        return gson.fromJson(d.asJsonArray[0], SeriesDetail::class.java)
                    }
                    if (d.isJsonObject) return gson.fromJson(d, SeriesDetail::class.java)
                }
            }
            if (element.isJsonArray && element.asJsonArray.size() > 0) {
                return gson.fromJson(element.asJsonArray[0], SeriesDetail::class.java)
            }
            null
        } catch (e: Exception) { e.printStackTrace(); null }
    }
}
