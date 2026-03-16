package com.verselock.util

object TrackKeyUtils {
    /**
     * Normalized cache key: lowercased, trimmed, punctuation stripped.
     * "The Strokes" + "The Adults Are Talking" → "the strokes::the adults are talking"
     */
    fun make(artist: String, title: String): String {
        val norm = { s: String -> s.lowercase().trim().replace(Regex("[^a-z0-9 ]"), "").trim() }
        return "${norm(artist)}::${norm(title)}"
    }
}
