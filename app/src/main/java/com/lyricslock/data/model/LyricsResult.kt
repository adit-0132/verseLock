package com.lyricslock.data.model

sealed class LyricsResult {
    data class Found(val lines: List<LrcLine>) : LyricsResult()
    object NotFound : LyricsResult()
    object Loading : LyricsResult()
}
