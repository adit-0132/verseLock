package com.verselock.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_lyrics")
data class CachedLyrics(
    @PrimaryKey
    val trackKey: String,           // normalized "artist::title"
    val lrcContent: String,         // raw LRC string, empty string = confirmed not found
    val fetchedAt: Long,            // System.currentTimeMillis()
    val hasLyrics: Boolean,         // false = confirmed miss, skip refetch during TTL
    val title: String = "",
    val artist: String = ""
)
