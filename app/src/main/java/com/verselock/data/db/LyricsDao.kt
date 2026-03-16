package com.verselock.data.db

import androidx.room.*

@Dao
interface LyricsDao {

    @Query("SELECT * FROM cached_lyrics WHERE trackKey = :key")
    suspend fun getByKey(key: String): CachedLyrics?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CachedLyrics)

    @Query("DELETE FROM cached_lyrics WHERE fetchedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM cached_lyrics")
    suspend fun count(): Int

    // Evict oldest entries when over limit
    @Query("""
        DELETE FROM cached_lyrics WHERE trackKey IN (
            SELECT trackKey FROM cached_lyrics 
            ORDER BY fetchedAt ASC 
            LIMIT :count
        )
    """)
    suspend fun deleteOldest(count: Int)
}
