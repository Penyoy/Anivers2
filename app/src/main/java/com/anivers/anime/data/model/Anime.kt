package com.anivers.anime.data.model

import com.google.gson.annotations.SerializedName

data class Anime(
    val id: String = "",
    val url: String = "", // sanitized slug for routing
    val originalUrl: String = "", // exact link from server (preserve slash/case) for API
    val judul: String = "Tanpa Judul",
    val cover: String = "",
    val genre: List<String> = emptyList(),
    val sinopsis: String = "",
    val studio: String = "",
    val score: String = "",
    val status: String = "",
    val rilis: String = "",
    val totalEpisode: String = "",
    val lastch: String = "",
    val lastup: String = "",
    val type: String = "",
    val raw: Map<String, Any?> = emptyMap()
)

// Search wrapper
data class SearchResponse(
    val data: List<SearchData>? = null
)
data class SearchData(
    val jumlah: Int? = null,
    val result: List<Map<String, Any>>? = null,
    val pagination: Pagination? = null
)
data class Pagination(
    val page: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    val total: Int? = null,
    @SerializedName("total_pages") val totalPages: Int? = null,
    @SerializedName("has_next") val hasNext: Boolean? = null,
    @SerializedName("next_page") val nextPage: String? = null
)

// Series detail
data class SeriesResponse(
    val data: List<SeriesDetail>? = null
)
data class SeriesDetail(
    val id: Long? = null,
    @SerializedName("series_id") val seriesId: String? = null,
    val judul: String? = null,
    val cover: String? = null,
    val type: String? = null,
    val status: String? = null,
    val rating: String? = null,
    val published: String? = null,
    val author: String? = null,
    val genre: List<String>? = null,
    val genreurl: List<String>? = null,
    val sinopsis: String? = null,
    val chapter: List<Episode>? = null,
    val history: List<String>? = null
)
data class Episode(
    val id: Long? = null,
    val ch: String? = null,
    val url: String? = null,
    val date: String? = null,
    val views: Long? = null
)

// Episode streams
data class EpisodeDataResponse(
    @SerializedName("is_cache") val isCache: Boolean? = null,
    val data: List<EpisodeStream>? = null
)
data class EpisodeStream(
    @SerializedName("episode_id") val episodeId: Long? = null,
    val reso: List<String>? = null,
    val streams: Map<String, List<StreamLink>>? = null
)
data class StreamLink(
    val link: String? = null,
    val provide: Int? = null,
    val id: Long? = null,
    val reso: String? = null
)

// Jadwal
data class JadwalResponse(
    val generatedAt: Long? = null,
    val data: List<JadwalDay>? = null
)
data class JadwalDay(
    val day: String? = null,
    val date: String? = null,
    @SerializedName("animeList") val animeList: List<JadwalAnime>? = null
)
data class JadwalAnime(
    @SerializedName("anime_name") val animeName: String? = null,
    val id: Long? = null,
    val link: String? = null,
    val cover: String? = null,
    val updated: Long? = null
)

// Genre
data class GenreItem(
    val id: String? = null,
    val link: String? = null,
    @SerializedName("anime_name") val animeName: String? = null,
    val thumb: String? = null,
    val genre: List<String>? = null,
    val sinopsis: String? = null,
    val studio: String? = null,
    val score: String? = null,
    val status: String? = null
)
