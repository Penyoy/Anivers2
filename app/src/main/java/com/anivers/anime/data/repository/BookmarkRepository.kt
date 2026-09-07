package com.anivers.anime.data.repository

import android.content.Context
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.local.BookmarkEntity
import com.anivers.anime.data.local.HistoryEntity
import com.anivers.anime.data.local.ProgressEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BookmarkRepository(private val context: Context) {
    private val db = AppDatabase.get(context)
    private val bookmarkDao = db.bookmarkDao()
    private val historyDao = db.historyDao()

    fun bookmarksFlow(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllFlow()
    fun historyFlow(limit: Int = 20): Flow<List<HistoryEntity>> = historyDao.getRecentFlow(limit)

    suspend fun isBookmarked(id: String, url: String): Boolean {
        return bookmarkDao.exists(id) || bookmarkDao.existsByUrl(url)
    }

    suspend fun toggleBookmark(id: String, url: String, judul: String, cover: String): Boolean {
        val exists = bookmarkDao.exists(id) || bookmarkDao.existsByUrl(url)
        if (exists) {
            // try delete by both
            bookmarkDao.deleteById(id)
            // also try delete by url scan
            val all = bookmarkDao.getAll()
            all.filter { it.url == url && it.id != id }.forEach { bookmarkDao.deleteById(it.id) }
            syncToFirebase()
            return false
        } else {
            bookmarkDao.insert(BookmarkEntity(id = id, url = url, judul = judul, cover = cover))
            syncToFirebase()
            return true
        }
    }

    suspend fun removeBookmark(id: String) { bookmarkDao.deleteById(id); syncToFirebase() }
    suspend fun clearAll() { bookmarkDao.clearAll(); syncToFirebase() }

    suspend fun upsertProgress(seriesUrl: String, episode: String, currentTime: Long, duration: Long) {
        if (duration < 5) return
        val progress = ((currentTime.toDouble() / duration.toDouble()) * 100).toInt().coerceIn(0, 100)
        val key = "$seriesUrl|$episode"
        historyDao.upsertProgress(ProgressEntity(key = key, seriesUrl = seriesUrl, episode = episode, currentTime = currentTime, duration = duration, progress = progress))
        // also keep history for Recent page
        val judul = seriesUrl // will be overwritten by caller with real judul
        // we insert history entry for Recent; caller should provide judul/cover via addHistory
    }

    suspend fun addHistory(seriesUrl: String, episode: String, judul: String, cover: String, currentTime: Long, duration: Long, completed: Boolean = false) {
        val progress = if (duration > 0) ((currentTime.toDouble()/duration)*100).toInt().coerceIn(0,100) else 0
        historyDao.insert(HistoryEntity(seriesUrl = seriesUrl, episode = episode, judul = judul, cover = cover, currentTime = currentTime, duration = duration, progress = progress, completed = completed))
        // upsert progress as well
        historyDao.upsertProgress(ProgressEntity(key = "$seriesUrl|$episode", seriesUrl = seriesUrl, episode = episode, currentTime = currentTime, duration = duration, progress = progress))
    }

    suspend fun getProgress(seriesUrl: String, episode: String) = historyDao.getProgress(seriesUrl, episode)
    suspend fun getAllHistory() = historyDao.getAll()

    suspend fun clearHistory() { historyDao.clearAll() }

    private suspend fun syncToFirebase() {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val list = bookmarkDao.getAll()
            val payload = list.map { mapOf("id" to it.id, "url" to it.url, "judul" to it.judul, "cover" to it.cover, "addedAt" to it.addedAt) }
            FirebaseFirestore.getInstance().collection("users").document(user.uid).set(mapOf("bookmarks" to payload))
        } catch (_: Exception) {}
    }

    suspend fun pullFromFirebase() {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return
            FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
                .addOnSuccessListener { snap ->
                    try {
                        val arr = snap.get("bookmarks") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            for (m in arr) {
                                val id = m["id"]?.toString() ?: continue
                                val url = m["url"]?.toString() ?: continue
                                val judul = m["judul"]?.toString() ?: ""
                                val cover = m["cover"]?.toString() ?: ""
                                val added = (m["addedAt"] as? Long) ?: (m["addedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                                bookmarkDao.insert(BookmarkEntity(id = id, url = url, judul = judul, cover = cover, addedAt = added))
                            }
                        }
                    } catch (_: Exception) {}
                }
        } catch (_: Exception) {}
    }
}
