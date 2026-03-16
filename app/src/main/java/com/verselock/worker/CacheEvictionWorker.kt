package com.verselock.worker

import android.content.Context
import androidx.work.*
import com.verselock.verseLockApplication
import java.util.concurrent.TimeUnit

class CacheEvictionWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = (applicationContext as verseLockApplication).database.lyricsDao()
        val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        dao.deleteOlderThan(cutoff)
        val count = dao.count()
        if (count > 500) dao.deleteOldest(count - 500)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CacheEvictionWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresCharging(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "cache_eviction",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
