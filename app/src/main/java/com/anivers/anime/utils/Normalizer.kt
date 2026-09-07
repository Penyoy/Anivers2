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
            set.add(v); set.add("$v/"); set.add(v.trimEnd('/'))
        }
        fun expandSuffix(v: String): List<String> {
            val no = v.removeSuffix("-sub-indo").removeSuffix("-subtitle-indonesia").removeSuffix("/")
            return listOf(v, "$no-sub-indo", "$no-subtitle-indonesia", no)
        }
        // honorific strip
        val honor = base.removeSuffix("-san").removeSuffix("-sama").removeSuffix("-kun").removeSuffix("-chan").removeSuffix("-senpai").removeSuffix("-sensei")
        val seeds = mutableListOf(base, honor).distinct()
        // also truncated first 1-2 tokens for server shortened like ijiranaide-nagatoro-san -> ijiranaide-sub-indo
        val parts = base.split("-")
        if (parts.size > 2) {
            val first = parts.first()
            seeds.add("$first-sub-indo"); seeds.add("$first-subtitle-indonesia")
            if (parts.size >= 2) {
                val first2 = parts.take(2).joinToString("-")
                seeds.add("$first2-sub-indo"); seeds.add("$first2-subtitle-indonesia")
            }
        }
        // word order reversal for golden kamuy
        if (base.contains("golden-kamuy")) {
            seeds.add("kamuy-golden-subtitle-indonesia"); seeds.add("kamuy-golden-sub-indo")
        }
        if (base.contains("kamuy-golden")) {
            seeds.add("golden-kamuy"); seeds.add("golden-kamuy-sub-indo")
        }
        for (seed in seeds.distinct()) {
            for (suf in expandSuffix(seed)) {
                add(suf)
                if ("-s2" in suf) { add(suf.replace("-s2", "-season-2")); add(suf.replace("-s2", "-2")) }
                if ("-season-2" in suf) add(suf.replace("-season-2", "-s2"))
                if ("gotoubun" in suf) add(suf.replace("gotoubun", "5toubun"))
                if ("5toubun" in suf) {
                    add(suf.replace("5toubun", "gotoubun")); add(suf.replace("5toubun", "5-toubun")); add(suf.replace("5toubun", "gotobun"))
                }
                if (suf.contains("5-toubun")) add(suf.replace("5-toubun", "5toubun"))
                add(suf.replace("_", "-")); add(suf.replace("-", "_"))
                if ("-kei-" in suf) add(suf.replace("-kei-", "kei-"))
                if ("nichijou-kei" in suf) add(suf.replace("nichijou-kei", "nichijoukei"))
                if ("nichijoukei" in suf) add(suf.replace("nichijoukei", "nichijou-kei"))
                // inou-battle word order swap - handle all variants
                if (suf.contains("inou-battle-wa")) add(suf.replace("inou-battle-wa", "inou-wa-battle"))
                if (suf.contains("inou-wa-battle")) add(suf.replace("inou-wa-battle", "inou-battle-wa"))
                if (suf.contains("nichijou-kei-no-naka-de")) {
                    add(suf.replace("nichijou-kei-no-naka-de", "nichijou"))
                    add(suf.replace("-nichijou-kei-no-naka-de", ""))
                    add(suf.replace("nichijou-kei-no-naka-de", "nichijou-subtitle-indonesia"))
                }
            }
        }
        return set.filter { it.trim('/').length >= 2 }.distinct().take(28)
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
