package com.anivers.anime.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    fun getAllFlow(): Flow<List<BookmarkEntity>>
    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    suspend fun getAll(): List<BookmarkEntity>
    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BookmarkEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(b: BookmarkEntity)
    @Query("DELETE FROM bookmarks WHERE id = :id") suspend fun deleteById(id: String)
    @Query("DELETE FROM bookmarks") suspend fun clearAll()
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE id = :id)") suspend fun exists(id: String): Boolean
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)") suspend fun existsByUrl(url: String): Boolean
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 20): Flow<List<HistoryEntity>>
    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<HistoryEntity>
    @Query("SELECT * FROM watch_history WHERE seriesUrl = :seriesUrl AND episode = :episode LIMIT 1")
    suspend fun get(seriesUrl: String, episode: String): HistoryEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(h: HistoryEntity)
    @Query("DELETE FROM watch_history") suspend fun clearAll()

    // progress per episode
    @Query("SELECT * FROM watch_progress WHERE seriesUrl = :seriesUrl AND episode = :episode LIMIT 1")
    suspend fun getProgress(seriesUrl: String, episode: String): ProgressEntity?
    @Query("SELECT * FROM watch_progress ORDER BY updatedAt DESC")
    suspend fun getAllProgress(): List<ProgressEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProgress(p: ProgressEntity)
    @Query("DELETE FROM watch_progress WHERE `key` = :key") suspend fun deleteProgress(key: String)
}
