package com.anivers.anime.data.repository

import android.content.Context
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.local.BookmarkEntity
import com.anivers.anime.data.local.ProgressEntity
import com.anivers.anime.data.local.UnlockedAnimeEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val context: Context) {
    private val db = AppDatabase.get(context)
    private val firestore get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid

    private fun ensureUserDoc(u: String) {
        try {
            firestore.collection("users").document(u).set(
                mapOf(
                    "uid" to u,
                    "email" to (auth.currentUser?.email ?: ""),
                    "displayName" to (auth.currentUser?.displayName ?: ""),
                    "lastLoginAt" to FieldValue.serverTimestamp()
                ), com.google.firebase.firestore.SetOptions.merge()
            )
        } catch (_: Exception) {}
    }

    // ---- BOOKMARKS ----

    fun bookmarksFlow(): Flow<List<BookmarkEntity>> = callbackFlow {
        val u = uid
        if (u == null) { trySend(emptyList()); close(); return@callbackFlow }
        val reg = firestore.collection("users").document(u)
            .collection("bookmarks")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val id = (data["animeId"] ?: data["id"] ?: doc.id).toString()
                    val url = (data["url"] ?: id).toString()
                    val judul = (data["title"] ?: data["judul"] ?: "").toString()
                    val cover = (data["poster"] ?: data["cover"] ?: "").toString()
                    BookmarkEntity(id = id, url = url, judul = judul, cover = cover)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun toggleBookmark(id: String, url: String, judul: String, cover: String, status: String = "", type: String = ""): Boolean {
        val u = uid ?: return false
        val animeId = id.ifEmpty { url }
        val docRef = firestore.collection("users").document(u).collection("bookmarks").document(animeId)
        val exists = try { docRef.get().await().exists() } catch (_: Exception) { false }
        if (exists) {
            docRef.delete().await()
            try { db.bookmarkDao().deleteById(id) } catch (_: Exception) {}
            return false
        } else {
            docRef.set(
                mapOf(
                    "animeId" to animeId, "title" to judul, "poster" to cover,
                    "status" to status, "type" to type, "url" to url,
                    "judul" to judul, "cover" to cover,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            try { db.bookmarkDao().insert(BookmarkEntity(id = id, url = url, judul = judul, cover = cover)) } catch (_: Exception) {}
            ensureUserDoc(u)
            return true
        }
    }

    suspend fun removeBookmark(id: String) {
        val u = uid ?: return
        firestore.collection("users").document(u).collection("bookmarks").document(id).delete().await()
        try { db.bookmarkDao().deleteById(id) } catch (_: Exception) {}
    }

    suspend fun isBookmarked(id: String, url: String): Boolean {
        val u = uid ?: return false
        return try {
            val docId = id.ifEmpty { url }
            firestore.collection("users").document(u).collection("bookmarks").document(docId).get().await().exists()
        } catch (_: Exception) {
            try { db.bookmarkDao().exists(id) || db.bookmarkDao().existsByUrl(url) } catch (_: Exception) { false }
        }
    }

    suspend fun clearAllBookmarks() {
        val u = uid ?: return
        val snap = firestore.collection("users").document(u).collection("bookmarks").get().await()
        for (doc in snap.documents) doc.reference.delete()
        try { db.bookmarkDao().clearAll() } catch (_: Exception) {}
    }

    // ---- WATCH PROGRESS ----

    fun watchProgressFlow(): Flow<List<ProgressEntity>> = callbackFlow {
        val u = uid
        if (u == null) { trySend(emptyList()); close(); return@callbackFlow }
        val reg = firestore.collection("users").document(u)
            .collection("watchProgress")
            .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val seriesUrl = (data["seriesUrl"] ?: data["animeId"] ?: "").toString()
                    val episode = (data["episode"] ?: data["episodeId"] ?: "").toString()
                    val judul = (data["title"] ?: data["judul"] ?: seriesUrl).toString()
                    val cover = (data["poster"] ?: data["cover"] ?: "").toString()
                    val pos = (data["position"] as? Number)?.toLong() ?: 0L
                    val dur = (data["duration"] as? Number)?.toLong() ?: 0L
                    val prog = (data["progress"] as? Number)?.toInt() ?: if (dur > 0) ((pos.toDouble() / dur) * 100).toInt() else 0
                    val ts = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
                    if (seriesUrl.isNotBlank()) ProgressEntity(
                        key = "$seriesUrl|$episode", seriesUrl = seriesUrl, episode = episode,
                        currentTime = pos, duration = dur, progress = prog, updatedAt = ts,
                        judul = judul, cover = cover
                    ) else null
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun upsertProgress(seriesUrl: String, episode: String, currentTime: Long, duration: Long, title: String = "", poster: String = "") {
        val u = uid ?: return
        if (duration < 5) return
        val progress = ((currentTime.toDouble() / duration.toDouble()) * 100).toInt().coerceIn(0, 100)
        val completed = duration > 0 && (currentTime.toDouble() / duration.toDouble()) >= 0.90
        val episodeId = episode.ifEmpty { "$seriesUrl|$episode" }
        firestore.collection("users").document(u)
            .collection("watchProgress").document(episodeId)
            .set(
                mapOf(
                    "animeId" to seriesUrl, "episodeId" to episodeId, "episodeNumber" to episode,
                    "title" to title.ifBlank { seriesUrl }, "poster" to poster,
                    "position" to currentTime, "duration" to duration, "progress" to progress,
                    "completed" to completed, "updatedAt" to FieldValue.serverTimestamp(),
                    "seriesUrl" to seriesUrl, "episode" to episode
                ), com.google.firebase.firestore.SetOptions.merge()
            ).await()
        try { db.historyDao().upsertProgress(ProgressEntity(key = "$seriesUrl|$episode", seriesUrl = seriesUrl, episode = episode, currentTime = currentTime, duration = duration, progress = progress, judul = title.ifBlank { seriesUrl }, cover = poster)) } catch (_: Exception) {}
    }

    suspend fun getProgress(seriesUrl: String, episode: String): ProgressEntity? {
        val u = uid
        if (u != null) {
            try {
                val doc = firestore.collection("users").document(u).collection("watchProgress").document(episode).get().await()
                if (doc.exists()) {
                    val data = doc.data ?: return null
                    val pos = (data["position"] as? Number)?.toLong() ?: 0L
                    val dur = (data["duration"] as? Number)?.toLong() ?: 0L
                    val prog = (data["progress"] as? Number)?.toInt() ?: 0
                    return ProgressEntity(key = "$seriesUrl|$episode", seriesUrl = seriesUrl, episode = episode, currentTime = pos, duration = dur, progress = prog, judul = (data["title"] ?: seriesUrl).toString(), cover = (data["poster"] ?: "").toString())
                }
            } catch (_: Exception) {}
        }
        return try { db.historyDao().getProgress(seriesUrl, episode) } catch (_: Exception) { null }
    }

    suspend fun clearAllProgress() {
        val u = uid
        if (u != null) {
            try {
                val snap = firestore.collection("users").document(u).collection("watchProgress").get().await()
                for (doc in snap.documents) doc.reference.delete()
            } catch (_: Exception) {}
            try {
                val hSnap = firestore.collection("users").document(u).collection("history").get().await()
                for (doc in hSnap.documents) doc.reference.delete()
            } catch (_: Exception) {}
        }
        try { db.historyDao().clearAll() } catch (_: Exception) {}
    }

    // ---- KEYS (Firestore primary) ----

    fun keysFlow(): Flow<Int> = callbackFlow {
        val u = uid
        if (u == null) { trySend(0); close(); return@callbackFlow }
        val reg = firestore.collection("users").document(u)
            .collection("keys").document("config")
            .addSnapshotListener { snap, _ ->
                val count = (snap?.getLong("count") ?: 0L).toInt().coerceIn(0, 6)
                trySend(count)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getKeys(): Int {
        val u = uid ?: return 0
        return try {
            val snap = firestore.collection("users").document(u).collection("keys").document("config").get().await()
            (snap.getLong("count") ?: 0L).toInt().coerceIn(0, 6)
        } catch (_: Exception) { 0 }
    }

    suspend fun addKeys(n: Int): Boolean {
        val u = uid ?: return false
        val current = getKeys()
        if (current >= 6) return false
        val toAdd = minOf(n, 6 - current)
        if (toAdd <= 0) return false
        firestore.collection("users").document(u).collection("keys").document("config")
            .set(mapOf("count" to (current + toAdd), "updatedAt" to FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge()).await()
        return true
    }

    suspend fun consumeKey(): Boolean {
        val u = uid ?: return false
        val current = getKeys()
        if (current <= 0) return false
        firestore.collection("users").document(u).collection("keys").document("config")
            .set(mapOf("count" to (current - 1), "updatedAt" to FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge()).await()
        return true
    }

    // ---- UNLOCKED ANIME (Firestore primary) ----

    fun unlockedFlow(): Flow<List<UnlockedAnimeEntity>> = callbackFlow {
        val u = uid
        if (u == null) { trySend(emptyList()); close(); return@callbackFlow }
        val reg = firestore.collection("users").document(u)
            .collection("unlockedAnime")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    UnlockedAnimeEntity(
                        slug = (data["slug"] ?: doc.id).toString(),
                        judul = (data["judul"] ?: "").toString(),
                        cover = (data["cover"] ?: "").toString(),
                        source = (data["source"] ?: "key").toString()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun isUnlocked(slug: String): Boolean {
        val u = uid ?: return false
        return try {
            firestore.collection("users").document(u).collection("unlockedAnime").document(slug).get().await().exists()
        } catch (_: Exception) {
            try { db.unlockedDao().isUnlocked(slug) } catch (_: Exception) { false }
        }
    }

    suspend fun unlock(slug: String, judul: String = "", cover: String = "", source: String = "key"): Boolean {
        val u = uid ?: return false
        firestore.collection("users").document(u).collection("unlockedAnime").document(slug).set(
            mapOf(
                "slug" to slug, "judul" to judul, "cover" to cover,
                "source" to source, "unlockedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        try { db.unlockedDao().insert(UnlockedAnimeEntity(slug = slug, judul = judul, cover = cover, source = source)) } catch (_: Exception) {}
        return true
    }

    suspend fun hasUnlocked(slug: String, isPremium: Boolean): Boolean {
        if (isPremium) return true
        return isUnlocked(slug)
    }
}
