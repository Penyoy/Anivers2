package com.anivers.anime.utils

import com.anivers.anime.data.model.Anime
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object Normalizer {

    fun coerceGenre(raw: Any?): List<String> {
        return when (raw) {
            is List<*> -> raw.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
            is String -> if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }

    fun normalizeAnime(map: Map<String, Any?>): Anime {
        // wrapper {jumlah,result} bukan anime
        if (map.containsKey("jumlah") && map.containsKey("result")) return Anime()
        val rawUrl = (map["link"] ?: map["url"] ?: map["slug"] ?: map["series_id"] ?: "").toString()
        val url = rawUrl.trim().trim('/').lowercase()
        val judul = (map["anime_name"] ?: map["judul"] ?: map["title"] ?: map["name"] ?: "Tanpa Judul").toString().trim()
        val cover = (map["thumb"] ?: map["cover"] ?: map["image"] ?: map["thumbnail"] ?: "").toString().trim()
        val genre = coerceGenre(map["genre"] ?: map["genres"])
        return Anime(
            id = (map["id"] ?: "").toString(),
            url = url,
            judul = judul,
            cover = cover,
            genre = genre,
            sinopsis = (map["sinopsis"] ?: "").toString(),
            studio = (map["studio"] ?: map["author"] ?: "").toString(),
            score = (map["score"] ?: map["rating"] ?: "").toString(),
            status = (map["status"] ?: "").toString(),
            rilis = (map["rilis"] ?: map["published"] ?: map["release_date"] ?: "").toString(),
            totalEpisode = (map["total_episode"] ?: "").toString(),
            lastch = (map["lastch"] ?: map["episode"] ?: "").toString(),
            lastup = (map["lastup"] ?: "").toString(),
            type = (map["type"] ?: "").toString(),
            raw = map
        )
    }

    fun sanitizeSlug(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = raw.trim()
        try { s = java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) {}
        s = s.lowercase()
        s = s.replace(Regex("^https?://[^/]+/(anime/)?"), "")
        s = s.trim('/').split("?")[0].split("#")[0]
        s = s.replace(Regex("\\s+"), "-")
        s = s.replace(Regex("[^a-z0-9\\-_]"), "-").replace(Regex("-+"), "-")
        return s.trim('-')
    }

    fun parseEpisodeNumber(postUrl: String?): String {
        if (postUrl.isNullOrBlank()) return ""
        val s = postUrl.trim()
        Regex("-(\\d+)$").find(s)?.let { return it.groupValues[1] }
        if (s.endsWith("movie", ignoreCase = true)) return "movie"
        Regex("episode[-_]?(\\d+)", RegexOption.IGNORE_CASE).find(s)?.let { return it.groupValues[1] }
        return s
    }
}
