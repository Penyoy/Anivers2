package com.anivers.anime.data.repository

import android.content.Context
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.local.BookmarkEntity
import com.anivers.anime.data.local.HistoryEntity
import com.anivers.anime.data.local.ProgressEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class BookmarkRepository(private val context: Context) {
    private val db = AppDatabase.get(context)
    private val bookmarkDao = db.bookmarkDao()
    private val historyDao = db.historyDao()
    private val firestore get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    fun bookmarksFlow(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllFlow()
    fun historyFlow(limit: Int = 20): Flow<List<HistoryEntity>> = historyDao.getRecentFlow(limit)

    suspend fun isBookmarked(id: String, url: String): Boolean {
        return bookmarkDao.exists(id) || bookmarkDao.existsByUrl(url)
    }

    suspend fun toggleBookmark(id: String, url: String, judul: String, cover: String, status: String = "", type: String = ""): Boolean {
        val exists = bookmarkDao.exists(id) || bookmarkDao.existsByUrl(url)
        if (exists) {
            bookmarkDao.deleteById(id)
            val all = bookmarkDao.getAll()
            all.filter { it.url == url && it.id != id }.forEach { bookmarkDao.deleteById(it.id) }
            // Firestore subcollection delete
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val animeId = id.ifEmpty { url }
                    firestore.collection("users").document(uid)
                        .collection("bookmarks").document(animeId).delete()
                }
            } catch (_: Exception) {}
            return false
        } else {
            val entity = BookmarkEntity(id = id, url = url, judul = judul, cover = cover)
            bookmarkDao.insert(entity)
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val animeId = id.ifEmpty { url }
                    firestore.collection("users").document(uid)
                        .collection("bookmarks").document(animeId)
                        .set(
                            mapOf(
                                "animeId" to animeId,
                                "title" to judul,
                                "poster" to cover,
                                "status" to status,
                                "type" to type,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp(),
                                "url" to url,
                                "judul" to judul,
                                "cover" to cover
                            )
                        )
                    // ensure users/{uid} doc exists
                    firestore.collection("users").document(uid).set(
                        mapOf(
                            "uid" to uid,
                            "email" to (auth.currentUser?.email ?: ""),
                            "displayName" to (auth.currentUser?.displayName ?: ""),
                            "lastLoginAt" to FieldValue.serverTimestamp()
                        ), com.google.firebase.firestore.SetOptions.merge()
                    )
                }
            } catch (_: Exception) {}
            return true
        }
    }

    suspend fun removeBookmark(id: String) {
        bookmarkDao.deleteById(id)
        try {
            val uid = auth.currentUser?.uid ?: return
            firestore.collection("users").document(uid).collection("bookmarks").document(id).delete()
        } catch (_: Exception) {}
    }

    suspend fun clearAll() {
        val all = bookmarkDao.getAll()
        bookmarkDao.clearAll()
        try {
            val uid = auth.currentUser?.uid ?: return
            for (b in all) {
                firestore.collection("users").document(uid).collection("bookmarks").document(b.id).delete()
            }
        } catch (_: Exception) {}
    }

    suspend fun upsertProgress(seriesUrl: String, episode: String, currentTime: Long, duration: Long, animeId: String = "", title: String = "", poster: String = "") {
        if (duration < 5) return
        val progress = ((currentTime.toDouble() / duration.toDouble()) * 100).toInt().coerceIn(0, 100)
        val key = "$seriesUrl|$episode"
        val completed = if (duration > 0) (currentTime.toDouble() / duration.toDouble()) >= 0.90 else false
        historyDao.upsertProgress(ProgressEntity(key = key, seriesUrl = seriesUrl, episode = episode, currentTime = currentTime, duration = duration, progress = progress, judul = title.ifBlank { seriesUrl }, cover = poster))
        // Firestore watchProgress subcollection with throttling handled by caller (8s)
        try {
            val uid = auth.currentUser?.uid ?: return
            val episodeId = episode.ifEmpty { key }
            firestore.collection("users").document(uid)
                .collection("watchProgress").document(episodeId)
                .set(
                    mapOf(
                        "animeId" to (animeId.ifEmpty { seriesUrl }),
                        "episodeId" to episodeId,
                        "episodeNumber" to episode,
                        "title" to title,
                        "poster" to poster,
                        "position" to currentTime,
                        "duration" to duration,
                        "progress" to progress,
                        "completed" to completed,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "seriesUrl" to seriesUrl,
                        "episode" to episode
                    ), com.google.firebase.firestore.SetOptions.merge()
                )
        } catch (_: Exception) {}
    }

    suspend fun addHistory(seriesUrl: String, episode: String, judul: String, cover: String, currentTime: Long, duration: Long, completed: Boolean = false) {
        val progress = if (duration > 0) ((currentTime.toDouble()/duration)*100).toInt().coerceIn(0,100) else 0
        historyDao.insert(HistoryEntity(seriesUrl = seriesUrl, episode = episode, judul = judul, cover = cover, currentTime = currentTime, duration = duration, progress = progress, completed = completed))
        historyDao.upsertProgress(ProgressEntity(key = "$seriesUrl|$episode", seriesUrl = seriesUrl, episode = episode, currentTime = currentTime, duration = duration, progress = progress, judul = judul, cover = cover))
        // Firestore history subcollection
        try {
            val uid = auth.currentUser?.uid ?: return
            val historyId = "${seriesUrl}_${episode}_${System.currentTimeMillis()}"
            firestore.collection("users").document(uid)
                .collection("history").document(historyId)
                .set(
                    mapOf(
                        "animeId" to seriesUrl,
                        "episodeId" to episode,
                        "episodeNumber" to episode,
                        "title" to judul,
                        "poster" to cover,
                        "watchedAt" to FieldValue.serverTimestamp(),
                        "position" to currentTime,
                        "duration" to duration,
                        "completed" to completed,
                        "progress" to progress,
                        "seriesUrl" to seriesUrl,
                        "episode" to episode,
                        "judul" to judul,
                        "cover" to cover
                    )
                )
            // also update watchProgress for Continue Watching dedup
            upsertProgress(seriesUrl, episode, currentTime, duration, seriesUrl, judul, cover)
        } catch (_: Exception) {}
    }

    suspend fun getProgress(seriesUrl: String, episode: String) = historyDao.getProgress(seriesUrl, episode)
    suspend fun getAllHistory() = historyDao.getAll()
    suspend fun clearHistory() { historyDao.clearAll(); clearFirestoreHistory() }

    suspend fun clearFirestoreHistory() {
        try {
            val uid = auth.currentUser?.uid ?: return
            val snap = firestore.collection("users").document(uid).collection("history").get().await()
            for (doc in snap.documents) doc.reference.delete()
            val progSnap = firestore.collection("users").document(uid).collection("watchProgress").get().await()
            for (doc in progSnap.documents) doc.reference.delete()
        } catch (_: Exception) {}
    }

    suspend fun pullFromFirebase() {
        try {
            val uid = auth.currentUser?.uid ?: return
            // Pull bookmarks subcollection
            val snap = firestore.collection("users").document(uid).collection("bookmarks").get().await()
            for (doc in snap.documents) {
                val data = doc.data ?: continue
                val id = (data["animeId"] ?: data["id"] ?: doc.id).toString()
                val url = (data["url"] ?: id).toString()
                val judul = (data["title"] ?: data["judul"] ?: "").toString()
                val cover = (data["poster"] ?: data["cover"] ?: "").toString()
                bookmarkDao.insert(BookmarkEntity(id = id, url = url, judul = judul, cover = cover))
            }
            // Legacy fallback: old single doc users/{uid} with bookmarks array
            try {
                val legacy = firestore.collection("users").document(uid).get().await()
                val arr = legacy.get("bookmarks") as? List<Map<String, Any>>
                if (!arr.isNullOrEmpty()) {
                    for (m in arr) {
                        val id = m["id"]?.toString() ?: continue
                        val url = m["url"]?.toString() ?: continue
                        val judul = m["judul"]?.toString() ?: ""
                        val cover = m["cover"]?.toString() ?: ""
                        if (!bookmarkDao.exists(id)) bookmarkDao.insert(BookmarkEntity(id = id, url = url, judul = judul, cover = cover))
                    }
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }
}
