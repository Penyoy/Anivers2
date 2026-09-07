package com.anivers.anime.data.api

import com.anivers.anime.utils.Constants
import okhttp3.Interceptor
import okhttp3.Response

class ApiInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val isEpisode = req.url.toString().contains("/series/episode/data.php")
        val builder = req.newBuilder()
            .header("User-Agent", if (isEpisode) Constants.UA_FLUTTER else Constants.UA_DART)
            .header("Accept", "application/json")
            .header("Accept-Encoding", "gzip")
        // remove browser-ish headers that trigger CF challenge
        builder.removeHeader("Referer")
        builder.removeHeader("Origin")
        return chain.proceed(builder.build())
    }
}
