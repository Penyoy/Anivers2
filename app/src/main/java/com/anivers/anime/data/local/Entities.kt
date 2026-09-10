package com.anivers.anime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String, // use anime url or id string
    val url: String,
    val judul: String,
    val cover: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val autoId: Long = 0,
    val seriesUrl: String,
    val episode: String, // postUrl slug e.g. al-154129-1
    val judul: String,
    val cover: String,
    val currentTime: Long, // seconds *1000? keep seconds
    val duration: Long,
    val progress: Int, // 0-100
    val timestamp: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)

@Entity(tableName = "watch_progress")
data class ProgressEntity(
    @PrimaryKey val key: String, // "$seriesUrl|$episode"
    val seriesUrl: String,
    val episode: String,
    val currentTime: Long,
    val duration: Long,
    val progress: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "unlocked_anime")
data class UnlockedAnimeEntity(
    @PrimaryKey val slug: String,
    val judul: String = "",
    val cover: String = "",
    val unlockedAt: Long = System.currentTimeMillis(),
    val source: String = "ad" // ad / premium / key
)
