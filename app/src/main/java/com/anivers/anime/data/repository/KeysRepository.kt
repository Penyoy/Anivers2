package com.anivers.anime.data.repository

import android.content.Context
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.local.UnlockedAnimeEntity
import kotlinx.coroutines.flow.Flow

class KeysRepository(private val context: Context) {
    private val fs = FirestoreRepository(context)
    private val db = AppDatabase.get(context)

    fun keysFlow(): Flow<Int> = fs.keysFlow()

    suspend fun keys(): Int = fs.getKeys()
    suspend fun isPremium(): Boolean = try { PremiumRepository(context).isPremium() } catch (_: Exception) { false }

    suspend fun canEarn(): Boolean = keys() < com.anivers.anime.utils.Constants.MAX_KEYS

    suspend fun earnKey(): Boolean = earnKeys(com.anivers.anime.utils.Constants.AD_REWARD_KEYS)
    suspend fun earnKeys(n: Int): Boolean {
        if (keys() >= com.anivers.anime.utils.Constants.MAX_KEYS) return false
        return fs.addKeys(n)
    }

    fun unlockedFlow(): Flow<List<UnlockedAnimeEntity>> = fs.unlockedFlow()
    suspend fun isUnlocked(slug: String): Boolean {
        if (isPremium()) return true
        return fs.isUnlocked(slug)
    }

    suspend fun unlock(slug: String, judul: String = "", cover: String = "", source: String = "key"): Boolean {
        if (isPremium()) {
            return fs.unlock(slug, judul, cover, "premium")
        }
        if (isUnlocked(slug)) return true
        val consumed = fs.consumeKey()
        if (!consumed) return false
        return fs.unlock(slug, judul, cover, source)
    }

    suspend fun hasKeyForUnlock(slug: String): Boolean = isPremium() || isUnlocked(slug) || keys() > 0
}
