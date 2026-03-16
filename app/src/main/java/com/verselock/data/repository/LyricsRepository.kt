package com.verselock.data.repository

import com.verselock.data.db.AppDatabase
import com.verselock.data.db.CachedLyrics
import com.verselock.data.model.LrcLine
import com.verselock.data.model.LyricsResult
import com.verselock.data.network.LrcLibApi
import com.verselock.data.parser.LrcParser
import com.verselock.util.TrackKeyUtils

class LyricsRepository(
    private val db: AppDatabase,
    private val api: LrcLibApi
) {
    companion object {
        private const val TTL_MS = 30L * 24 * 60 * 60 * 1000  // 30 days
        private const val MAX_CACHE_ENTRIES = 500
    }

    /**
     * Returns parsed lyrics for a track.
     * Checks Room cache first; fetches from LRCLib on miss.
     * Caches both hits and misses (miss = hasLyrics=false) to avoid hammering the API.
     */
    suspend fun getLyrics(artist: String, title: String): LyricsResult {
        val key = TrackKeyUtils.make(artist, title)
        val dao = db.lyricsDao()
        val now = System.currentTimeMillis()

        // 1. Cache check
        val cached = dao.getByKey(key)
        if (cached != null && (now - cached.fetchedAt) < TTL_MS) {
            return if (cached.hasLyrics) {
                LyricsResult.Found(LrcParser.parse(cached.lrcContent))
            } else {
                LyricsResult.NotFound
            }
        }

        // 2. Fetch from LRCLib
        val lrc = api.fetchSyncedLyrics(artist, title)
        val hasLyrics = lrc != null

        // 3. Store in cache
        dao.insert(
            CachedLyrics(
                trackKey = key,
                lrcContent = lrc ?: "",
                fetchedAt = now,
                hasLyrics = hasLyrics,
                title = title,
                artist = artist
            )
        )

        return if (hasLyrics) {
            LyricsResult.Found(LrcParser.parse(lrc!!))
        } else {
            LyricsResult.NotFound
        }
    }
}
