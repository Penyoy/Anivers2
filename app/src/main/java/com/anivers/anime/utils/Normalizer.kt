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
        val originalUrl = rawUrl.trim() // preserve exact server link for API
        val url = rawUrl.trim().trim('/').lowercase()
        val judul = (map["anime_name"] ?: map["judul"] ?: map["title"] ?: map["name"] ?: "Tanpa Judul").toString().trim()
        val cover = (map["thumb"] ?: map["cover"] ?: map["image"] ?: map["thumbnail"] ?: "").toString().trim()
        val genre = coerceGenre(map["genre"] ?: map["genres"])
        return Anime(
            id = (map["id"] ?: "").toString(),
            url = url,
            originalUrl = originalUrl,
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
        s = s.trim('-')
        // alias khusus untuk kasus gotoubun (Reqable sering 5toubun, user ketik gotoubun)
        s = s.replace("gotoubun", "5toubun")
            .replace("hanayome-season-2", "hanayome-s2")
            .replace("season-2", "s2")
            .replace("season2", "s2")
        return s.trim('-')
    }

    fun slugCandidates(raw: String?): List<String> {
        val base = sanitizeSlug(raw)
        if (base.isEmpty()) return emptyList()
        val set = LinkedHashSet<String>()
        fun add(v: String) {
            if (v.isBlank()) return
            set.add(v)
            set.add("$v/")
            // also without trailing slash
            set.add(v.trimEnd('/'))
        }
        fun expandSuffix(v: String): List<String> {
            val no = v.removeSuffix("-sub-indo").removeSuffix("-subtitle-indonesia").removeSuffix("/")
            return listOf(v, "$no-sub-indo", "$no-subtitle-indonesia", no)
        }
        // honorific strip
        val honor = base.removeSuffix("-san").removeSuffix("-sama").removeSuffix("-kun").removeSuffix("-chan").removeSuffix("-senpai")
        val seeds = mutableListOf(base, honor).distinct()
        for (seed in seeds) {
            for (suf in expandSuffix(seed)) {
                add(suf)
                if ("-s2" in suf) {
                    add(suf.replace("-s2", "-season-2"))
                    add(suf.replace("-s2", "-2"))
                }
                if ("-season-2" in suf) add(suf.replace("-season-2", "-s2"))
                if ("gotoubun" in suf) add(suf.replace("gotoubun", "5toubun"))
                if ("5toubun" in suf) {
                    add(suf.replace("5toubun", "gotoubun"))
                    add(suf.replace("5toubun", "5-toubun"))
                    add(suf.replace("5toubun", "gotobun"))
                }
                if (suf.contains("5-toubun")) add(suf.replace("5-toubun", "5toubun"))
                add(suf.replace("_", "-")); add(suf.replace("-", "_"))
                if ("-kei-" in suf) add(suf.replace("-kei-", "kei-"))
                if ("nichijou-kei" in suf) add(suf.replace("nichijou-kei", "nichijoukei"))
            }
        }
        // golden kamuy word order reversal
        if (base == "golden-kamuy" || base == "golden-kamuy-sub-indo") {
            add("kamuy-golden-subtitle-indonesia"); add("kamuy-golden-subtitle-indonesia/")
            add("kamuy-golden-sub-indo"); add("kamuy-golden-sub-indo/")
        }
        // truncation for long slugs like ijiranaide-nagatoro-san -> ijiranaide-sub-indo (server shortens)
        val parts = base.split("-")
        if (parts.size > 2) {
            val first = parts.first()
            add("$first-sub-indo"); add("$first-subtitle-indonesia")
            if (parts.size >= 2) {
                val first2 = parts.take(2).joinToString("-")
                add("$first2-sub-indo"); add("$first2-subtitle-indonesia")
            }
        }
        // inou-battle word order swap
        if (base.contains("inou-battle-wa")) {
            add(base.replace("inou-battle-wa", "inou-wa-battle"))
            add(base.replace("inou-battle-wa-nichijou-kei", "inou-wa-battle-nichijou"))
        }
        if (base.contains("nichijou-kei-no-naka-de")) {
            add(base.replace("nichijou-kei-no-naka-de", "nichijou"))
            add(base.replace("-nichijou-kei-no-naka-de", ""))
        }
        return set.filter { it.trim('/').length >= 2 }.distinct().take(24)
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
