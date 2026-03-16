package com.verselock

import android.app.Application
import androidx.room.Room
import com.verselock.data.db.AppDatabase
import com.verselock.data.network.LrcLibApi
import com.verselock.data.repository.LyricsRepository
import com.verselock.worker.CacheEvictionWorker

class verseLockApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var lyricsRepository: LyricsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "verse-lock-db")
            .fallbackToDestructiveMigration()
            .build()
        lyricsRepository = LyricsRepository(database, LrcLibApi())
        CacheEvictionWorker.schedule(this)
    }
}
