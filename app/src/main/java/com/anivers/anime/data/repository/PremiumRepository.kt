package com.anivers.anime.data.repository

import android.content.Context
import com.anivers.anime.data.api.RetrofitClientGateway
import com.anivers.anime.data.api.PremiumRequest
import com.anivers.anime.data.local.SettingsStore
import com.anivers.anime.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class PremiumRepository(private val context: Context) {
    private val store = SettingsStore(context)
    private val gateway = RetrofitClientGateway.gateway

    // Akun-based: premiumUntil disimpan di Firestore users/{uid} + cache lokal
    suspend fun isPremium(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            try {
                val snap = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                val remoteUntil = snap.getLong("premiumUntil") ?: 0L
                if (remoteUntil > 0) {
                    if (remoteUntil != store.flow.first().premiumUntil) store.updatePremium(remoteUntil, snap.getString("premiumPlan") ?: "")
                    return remoteUntil > System.currentTimeMillis()
                }
            } catch (_: Exception) {}
        }
        val s = store.flow.first()
        return s.premiumUntil > System.currentTimeMillis()
    }

    suspend fun getPremiumUntil(): Long {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            try {
                val snap = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                val remoteUntil = snap.getLong("premiumUntil") ?: 0L
                if (remoteUntil > 0) return remoteUntil
            } catch (_: Exception) {}
        }
        return store.flow.first().premiumUntil
    }

    // Placeholder purchase: coba gateway, fallback mock + tulis ke Firestore akun (bukan aplikasi)
    suspend fun purchase(plan: String): Result<Long> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) return Result.failure(Exception("Harus login dulu untuk premium akun"))
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
            val resp = try { gateway.purchasePremium(PremiumRequest(uid = uid, plan = plan, amount = amount)) } catch (_: Exception) { null }
            val until = if (resp != null && resp.success && resp.premiumUntil > 0) resp.premiumUntil else System.currentTimeMillis() + days * 24 * 60 * 60 * 1000
            // Tulis ke Firestore akun (source of truth) + cache lokal
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).set(
                    mapOf("premiumUntil" to until, "premiumPlan" to plan, "premiumUpdatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            } catch (_: Exception) {}
            store.updatePremium(until, plan)
            Result.success(until)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshStatus(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            // Cek gateway dulu, fallback Firestore
            try {
                val st = gateway.getPremiumStatus(uid)
                if (st.isPremium && st.premiumUntil > 0) {
                    store.updatePremium(st.premiumUntil, st.plan ?: "")
                    // sync ke Firestore juga
                    try { com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).set(mapOf("premiumUntil" to st.premiumUntil, "premiumPlan" to (st.plan ?: "")), com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
                    return true
                }
            } catch (_: Exception) {}
            val snap = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
            val remoteUntil = snap.getLong("premiumUntil") ?: 0L
            if (remoteUntil > 0) {
                store.updatePremium(remoteUntil, snap.getString("premiumPlan") ?: "")
                remoteUntil > System.currentTimeMillis()
            } else false
        } catch (_: Exception) { isPremium() }
    }

    suspend fun clearPremium() { store.clearPremium() }
}
