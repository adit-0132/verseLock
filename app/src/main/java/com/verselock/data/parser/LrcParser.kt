package com.verselock.data.parser

import com.verselock.data.model.LrcLine

object LrcParser {

    // Matches [mm:ss.xx] or [mm:ss.xxx] timestamp format
    private val TIMESTAMP_REGEX = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

    /**
     * Parses a raw .lrc string into a sorted list of (timestampMs, text) pairs.
     * Lines with empty text are included as musical breaks (shown as "♪").
     */
    fun parse(lrc: String): List<LrcLine> {
        return lrc.lines()
            .mapNotNull { line -> parseLine(line.trim()) }
            .sortedBy { it.timestampMs }
    }

    private fun parseLine(line: String): LrcLine? {
        val match = TIMESTAMP_REGEX.find(line) ?: return null
        val (minutes, seconds, centisStr, text) = match.destructured
        val centis = centisStr.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        val timestampMs = minutes.toLong() * 60_000L +
                seconds.toLong() * 1_000L +
                centis
        val cleanText = text.trim().ifEmpty { "♪" }
        return LrcLine(timestampMs, cleanText)
    }
}
