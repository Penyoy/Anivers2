package com.anivers.anime.data.repository

import android.content.Context
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.local.SettingsStore
import com.anivers.anime.data.local.UnlockedAnimeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class KeysRepository(private val context: Context) {
    private val db = AppDatabase.get(context)
    private val store = SettingsStore(context)

    fun keysFlow(): Flow<Int> = kotlinx.coroutines.flow.map(store.flow) { it.keys }
    fun premiumFlow(): Flow<Boolean> = kotlinx.coroutines.flow.map(store.flow) { it.isPremium }
    suspend fun keys(): Int = store.flow.first().keys
    suspend fun isPremium(): Boolean = store.flow.first().isPremium

    suspend fun canEarn(): Boolean = keys() < com.anivers.anime.utils.Constants.MAX_KEYS

    suspend fun earnKey(): Boolean {
        if (!canEarn()) return false
        store.addKey(1)
        return true
    }

    fun unlockedFlow(): Flow<List<UnlockedAnimeEntity>> = db.unlockedDao().getAllFlow()
    suspend fun isUnlocked(slug: String): Boolean {
        if (isPremium()) return true
        return db.unlockedDao().isUnlocked(slug)
    }

    suspend fun unlock(slug: String, judul: String = "", cover: String = "", source: String = "key"): Boolean {
        if (isPremium()) {
            db.unlockedDao().insert(UnlockedAnimeEntity(slug = slug, judul = judul, cover = cover, source = "premium"))
            return true
        }
        if (isUnlocked(slug)) return true
        val consumed = store.consumeKey()
        if (!consumed) return false
        db.unlockedDao().insert(UnlockedAnimeEntity(slug = slug, judul = judul, cover = cover, source = source))
        return true
    }

    suspend fun hasKeyForUnlock(slug: String): Boolean = isPremium() || isUnlocked(slug) || keys() > 0
}
