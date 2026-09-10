package com.anivers.anime.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Placeholder gateway untuk premium one-time 1/3/5 bulan
// Ganti BASE_URL di RetrofitClientGateway ke URL asli gateway kamu
data class PremiumRequest(
    val uid: String,
    val plan: String, // 1m / 3m / 5m
    val amount: Int
)

data class PremiumResponse(
    val success: Boolean = false,
    val premiumUntil: Long = 0L,
    val paymentUrl: String? = null,
    val message: String? = null
)

data class PremiumStatusResponse(
    val isPremium: Boolean = false,
    val premiumUntil: Long = 0L,
    val plan: String? = null
)

interface GatewayService {
    @POST("premium/purchase")
    suspend fun purchasePremium(@Body req: PremiumRequest): PremiumResponse

    @GET("premium/status/{uid}")
    suspend fun getPremiumStatus(@Path("uid") uid: String): PremiumStatusResponse
}
