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
        val log = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(ApiInterceptor())
            .addInterceptor(log)
            .addInterceptor { chain ->
                val resp = chain.proceed(chain.request())
                // deteksi Cloudflare HTML challenge — seperti api.js isCloudflarePage
                val peek = resp.peekBody(2048).string()
                if (peek.contains("<!DOCTYPE", ignoreCase = true) && peek.contains("cloudflare", ignoreCase = true)) {
                    throw java.io.IOException("Diblokir Cloudflare (challenge HTML) di ${chain.request().url}")
                }
                resp
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
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
            .addConverterFactory(retrofit2.converter.scalars.ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val api: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
