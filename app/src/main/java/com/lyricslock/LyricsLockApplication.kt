package com.lyricslock

import android.app.Application
import androidx.room.Room
import com.lyricslock.data.db.AppDatabase
import com.lyricslock.data.network.LrcLibApi
import com.lyricslock.data.repository.LyricsRepository
import com.lyricslock.worker.CacheEvictionWorker

class LyricsLockApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var lyricsRepository: LyricsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "lyrics-lock-db")
            .fallbackToDestructiveMigration()
            .build()
        lyricsRepository = LyricsRepository(database, LrcLibApi())
        CacheEvictionWorker.schedule(this)
    }
}
