package com.anivers.anime.data.api

import com.anivers.anime.data.model.*
import com.anivers.anime.utils.Constants
import com.google.gson.JsonElement
import retrofit2.http.*

interface ApiService {

    @GET("baruupload.php")
    suspend fun getBaruUpload(@Query("page") page: Int = 1): List<Map<String, Any>>

    @GET("movie.php")
    suspend fun getMovie(): List<Map<String, Any>>

    @GET("rekomendasi.php")
    suspend fun getRekomendasi(): List<Map<String, Any>>

    @GET("home/ongoing.php")
    suspend fun getOngoing(
        @Query("page") page: Int = 1,
        @Query("type") type: String = "all"
    ): List<Map<String, Any>>

    @GET("search.php")
    suspend fun search(@Query("keyword") keyword: String): JsonElement

    @POST("jadwal.php")
    @Headers("Content-Length: 0")
    suspend fun getJadwal(): JadwalResponse

    @POST("series.php")
    @Headers("Content-Type: text/plain; charset=utf-8")
    suspend fun getSeries(
        @Query("url") url: String,
        @Body body: Map<String, String>
    ): JsonElement

    @POST("series/episode/data.php")
    @Headers("Content-Type: text/plain; charset=utf-8")
    suspend fun getEpisodeData(
        @Query("url") url: String,
        @Body body: Map<String, String>
    ): EpisodeDataResponse

    @GET("genreseries.php")
    suspend fun getGenre(
        @Query("page") page: Int,
        @Query("url") url: String
    ): List<Map<String, Any>>
}
