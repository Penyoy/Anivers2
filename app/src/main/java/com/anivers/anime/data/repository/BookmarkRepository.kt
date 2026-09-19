package com.anivers.anime.data.repository

import android.content.Context
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.data.local.BookmarkEntity
import com.anivers.anime.data.local.ProgressEntity
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(context: Context) {
    private val fs = FirestoreRepository(context)
    private val db = AppDatabase.get(context)

    fun bookmarksFlow(): Flow<List<BookmarkEntity>> = fs.bookmarksFlow()
    fun historyFlow(limit: Int = 20): Flow<List<ProgressEntity>> = fs.watchProgressFlow()

    suspend fun isBookmarked(id: String, url: String): Boolean = fs.isBookmarked(id, url)

    suspend fun toggleBookmark(id: String, url: String, judul: String, cover: String, status: String = "", type: String = ""): Boolean =
        fs.toggleBookmark(id, url, judul, cover, status, type)

    suspend fun removeBookmark(id: String) = fs.removeBookmark(id)

    suspend fun clearAll() = fs.clearAllBookmarks()

    suspend fun upsertProgress(seriesUrl: String, episode: String, currentTime: Long, duration: Long, animeId: String = "", title: String = "", poster: String = "") =
        fs.upsertProgress(seriesUrl, episode, currentTime, duration, title.ifBlank { animeId }, poster)

    suspend fun addHistory(seriesUrl: String, episode: String, judul: String, cover: String, currentTime: Long, duration: Long, completed: Boolean = false) {
        val progress = if (duration > 0) ((currentTime.toDouble()/duration)*100).toInt().coerceIn(0,100) else 0
        // Write progress to Firestore (primary)
        fs.upsertProgress(seriesUrl, episode, currentTime, duration, judul, cover)
    }

    suspend fun getProgress(seriesUrl: String, episode: String) = fs.getProgress(seriesUrl, episode)

    suspend fun clearHistory() = fs.clearAllProgress()
}
