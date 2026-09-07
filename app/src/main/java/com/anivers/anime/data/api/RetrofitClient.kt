package com.anivers.anime.data.api

import com.anivers.anime.BuildConfig
import com.anivers.anime.utils.Constants
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val gson = GsonBuilder().setLenient().create()

    private val client by lazy {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor(ApiInterceptor())
            .addInterceptor(log)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        // BuildConfig.API_BASE sudah include /api/v1.2.5 ; Retrofit butuh trailing /
        val base = try { BuildConfig.API_BASE } catch (_: Exception) { Constants.BASE_URL + Constants.API_PREFIX }
        val normalized = if (base.endsWith("/")) base else "$base/"
        Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val api: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
