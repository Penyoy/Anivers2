package com.anivers.anime.data.api

import com.anivers.anime.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClientGateway {
    private val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val gateway: GatewayService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.GATEWAY_BASE) // placeholder - ganti dengan URL asli
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GatewayService::class.java)
    }
}
