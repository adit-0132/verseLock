package com.verselock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.verselock.service.LyricsService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startForegroundService(Intent(context, LyricsService::class.java))
        }
    }
}
