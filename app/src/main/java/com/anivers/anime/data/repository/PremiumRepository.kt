package com.anivers.anime.data.repository

import android.content.Context
import com.anivers.anime.data.api.RetrofitClientGateway
import com.anivers.anime.data.api.PremiumRequest
import com.anivers.anime.data.local.SettingsStore
import com.anivers.anime.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first

class PremiumRepository(private val context: Context) {
    private val store = SettingsStore(context)
    private val gateway = RetrofitClientGateway.gateway

    suspend fun isPremium(): Boolean {
        val s = store.flow.first()
        return s.premiumUntil > System.currentTimeMillis()
    }

    suspend fun getPremiumUntil(): Long = store.flow.first().premiumUntil

    // Placeholder purchase: coba gateway, fallback mock jika gagal (untuk tes tanpa backend)
    suspend fun purchase(plan: String): Result<Long> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest_${System.currentTimeMillis()}"
        val amount = when (plan) {
            "1m" -> Constants.PREMIUM_1M_PRICE
            "3m" -> Constants.PREMIUM_3M_PRICE
            "5m" -> Constants.PREMIUM_5M_PRICE
            else -> Constants.PREMIUM_1M_PRICE
        }
        val days = when (plan) {
            "1m" -> Constants.PREMIUM_1M_DAYS
            "3m" -> Constants.PREMIUM_3M_DAYS
            "5m" -> Constants.PREMIUM_5M_DAYS
            else -> Constants.PREMIUM_1M_DAYS
        }
        return try {
            val resp = gateway.purchasePremium(PremiumRequest(uid = uid, plan = plan, amount = amount))
            if (resp.success && resp.premiumUntil > 0) {
                store.updatePremium(resp.premiumUntil, plan)
                Result.success(resp.premiumUntil)
            } else {
                // Fallback mock jika gateway placeholder / belum siap
                val until = System.currentTimeMillis() + days * 24 * 60 * 60 * 1000
                store.updatePremium(until, plan)
                Result.success(until)
            }
        } catch (e: Exception) {
            // Mock untuk tes tanpa backend - langsung aktifkan premium lokal
            val until = System.currentTimeMillis() + days * 24 * 60 * 60 * 1000
            store.updatePremium(until, plan)
            Result.success(until)
        }
    }

    suspend fun refreshStatus(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return isPremium()
        return try {
            val st = gateway.getPremiumStatus(uid)
            if (st.isPremium && st.premiumUntil > 0) {
                store.updatePremium(st.premiumUntil, st.plan ?: "")
                true
            } else false
        } catch (_: Exception) { isPremium() }
    }

    suspend fun clearPremium() { store.clearPremium() }
}
