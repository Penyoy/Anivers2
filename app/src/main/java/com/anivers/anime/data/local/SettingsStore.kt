package com.anivers.anime.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore(name = "settings")

data class AppSettings(
    val notifEnabled: Boolean = true,
    val autoplayNext: Boolean = true,
    val quality: String = "720p",
    val dataSaver: Boolean = false,
    // Premium (via API gateway placeholder, in-app one-time 1/3/5 bulan)
    val premiumUntil: Long = 0L,
    val premiumPlan: String = "", // 1m / 3m / 5m
    val isPremium: Boolean = false
)

class SettingsStore(private val context: Context) {
    private val KEY_NOTIF = booleanPreferencesKey("notifEnabled")
    private val KEY_AUTOPLAY = booleanPreferencesKey("autoplayNext")
    private val KEY_QUALITY = stringPreferencesKey("quality")
    private val KEY_SAVER = booleanPreferencesKey("dataSaver")

    private val KEY_PREMIUM_UNTIL = longPreferencesKey("premiumUntil")
    private val KEY_PREMIUM_PLAN = stringPreferencesKey("premiumPlan")

    val flow: Flow<AppSettings> = context.ds.data.map { p ->
        val until = p[KEY_PREMIUM_UNTIL] ?: 0L
        AppSettings(
            notifEnabled = p[KEY_NOTIF] ?: true,
            autoplayNext = p[KEY_AUTOPLAY] ?: true,
            quality = p[KEY_QUALITY] ?: "720p",
            dataSaver = p[KEY_SAVER] ?: false,
            premiumUntil = until,
            premiumPlan = p[KEY_PREMIUM_PLAN] ?: "",
            isPremium = until > System.currentTimeMillis()
        )
    }

    suspend fun updateNotif(v: Boolean) { context.ds.edit { it[KEY_NOTIF] = v } }
    suspend fun updateAutoplay(v: Boolean) { context.ds.edit { it[KEY_AUTOPLAY] = v } }
    suspend fun updateQuality(v: String) { context.ds.edit { it[KEY_QUALITY] = v } }
    suspend fun updateSaver(v: Boolean) { context.ds.edit { it[KEY_SAVER] = v } }
    suspend fun updatePremium(until: Long, plan: String) { context.ds.edit { it[KEY_PREMIUM_UNTIL] = until; it[KEY_PREMIUM_PLAN] = plan } }
    suspend fun clearPremium() { context.ds.edit { it.remove(KEY_PREMIUM_UNTIL); it.remove(KEY_PREMIUM_PLAN) } }
    suspend fun reset() { context.ds.edit { it.clear() } }
}
