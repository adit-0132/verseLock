package com.lyricslock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.lyricslock.LyricsLockApplication
import com.lyricslock.R
import com.lyricslock.data.model.LrcLine
import com.lyricslock.data.model.LyricsResult
import com.lyricslock.data.model.TrackInfo
import com.lyricslock.ui.LockScreenActivity
import com.lyricslock.util.lockScreenEnabledFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LyricsService : Service() {

    companion object {
        private const val CHANNEL_ID = "lyrics_lock_service"
        private const val NOTIF_ID = 1001
        private const val SYNC_INTERVAL_MS = 250L

        // Shared state — observed by LockScreenViewModel
        private val _currentTrack = MutableStateFlow<TrackInfo?>(null)
        private val _currentLyricIndex = MutableStateFlow(-1)
        private val _lyricsResult = MutableStateFlow<LyricsResult>(LyricsResult.Loading)
        private val _isPlaying = MutableStateFlow(false)

        val currentTrack: StateFlow<TrackInfo?> = _currentTrack.asStateFlow()
        val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()
        val lyricsResult: StateFlow<LyricsResult> = _lyricsResult.asStateFlow()
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        // Playback position tracking
        private var lastKnownPositionMs: Long = 0L
        private var lastPositionUpdateRealtime: Long = 0L
        private var isCurrentlyPlaying: Boolean = false

        fun onTrackChanged(context: Context, track: TrackInfo) {
            _currentTrack.value = track
            _lyricsResult.value = LyricsResult.Loading
            _currentLyricIndex.value = -1
            val intent = Intent(context, LyricsService::class.java).apply {
                action = ACTION_TRACK_CHANGED
                putExtra(EXTRA_TITLE, track.title)
                putExtra(EXTRA_ARTIST, track.artist)
                putExtra(EXTRA_ALBUM, track.album)
                putExtra(EXTRA_ART_URI, track.albumArtUri)
            }
            context.startForegroundService(intent)
        }

        fun onPlaybackStarted(context: Context, positionMs: Long, realtimeMs: Long) {
            isCurrentlyPlaying = true
            lastKnownPositionMs = positionMs
            lastPositionUpdateRealtime = realtimeMs
            _isPlaying.value = true
            val intent = Intent(context, LyricsService::class.java).apply {
                action = ACTION_PLAYBACK_STARTED
                putExtra(EXTRA_POSITION, positionMs)
                putExtra(EXTRA_REALTIME, realtimeMs)
            }
            context.startForegroundService(intent)
        }

        fun onPlaybackPaused(context: Context) {
            isCurrentlyPlaying = false
            _isPlaying.value = false
            val intent = Intent(context, LyricsService::class.java).apply {
                action = ACTION_PLAYBACK_PAUSED
            }
            context.startForegroundService(intent)
        }

        fun getCurrentPositionMs(): Long {
            return if (isCurrentlyPlaying) {
                lastKnownPositionMs + (SystemClock.elapsedRealtime() - lastPositionUpdateRealtime)
            } else {
                lastKnownPositionMs
            }
        }

        private const val ACTION_TRACK_CHANGED = "com.lyricslock.TRACK_CHANGED"
        private const val ACTION_PLAYBACK_STARTED = "com.lyricslock.PLAYBACK_STARTED"
        private const val ACTION_PLAYBACK_PAUSED = "com.lyricslock.PLAYBACK_PAUSED"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ARTIST = "artist"
        private const val EXTRA_ALBUM = "album"
        private const val EXTRA_ART_URI = "art_uri"
        private const val EXTRA_POSITION = "position"
        private const val EXTRA_REALTIME = "realtime"
    }

    private lateinit var wakeLock: PowerManager.WakeLock
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var parsedLines: List<LrcLine> = emptyList()
    private var lockScreenEnabled: Boolean = true
    private var lockScreenLaunched: Boolean = false

    private val syncRunnable = object : Runnable {
        override fun run() {
            if (isCurrentlyPlaying) {
                val posMs = getCurrentPositionMs()
                updateActiveLine(posMs)
            }
            handler.postDelayed(this, SYNC_INTERVAL_MS)
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF && isCurrentlyPlaying) {
                launchLockScreen()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Waiting for music..."))

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "LyricsLock::WakeLock"
        )

        // Observe lock screen pref
        serviceScope.launch {
            lockScreenEnabledFlow().collect { enabled ->
                lockScreenEnabled = enabled
                if (!enabled) {
                    releaseWakeLock()
                    lockScreenLaunched = false
                }
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        }
        
        handler.post(syncRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TRACK_CHANGED -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: return START_STICKY
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: return START_STICKY
                val album = intent.getStringExtra(EXTRA_ALBUM) ?: ""
                val artUri = intent.getStringExtra(EXTRA_ART_URI)
                handleTrackChanged(TrackInfo(title, artist, album, albumArtUri = artUri))
            }
            ACTION_PLAYBACK_STARTED -> {
                val pos = intent.getLongExtra(EXTRA_POSITION, 0L)
                val rt = intent.getLongExtra(EXTRA_REALTIME, SystemClock.elapsedRealtime())
                lastKnownPositionMs = pos
                lastPositionUpdateRealtime = rt
                isCurrentlyPlaying = true
                acquireWakeLock()
                launchLockScreen()
            }
            ACTION_PLAYBACK_PAUSED -> {
                isCurrentlyPlaying = false
                releaseWakeLock()
            }
        }
        return START_STICKY
    }

    private fun handleTrackChanged(track: TrackInfo) {
        parsedLines = emptyList()
        serviceScope.launch {
            val app = applicationContext as LyricsLockApplication
            val result = app.lyricsRepository.getLyrics(track.artist, track.title)
            withContext(Dispatchers.Main) {
                _lyricsResult.value = result
                if (result is LyricsResult.Found) {
                    parsedLines = result.lines
                }
                updateNotification("♪ ${track.artist} – ${track.title}")
            }
        }
    }

    private fun updateActiveLine(posMs: Long) {
        if (parsedLines.isEmpty()) return
        var idx = parsedLines.indexOfLast { it.timestampMs <= posMs }
        if (idx < 0) idx = 0
        if (idx != _currentLyricIndex.value) {
            _currentLyricIndex.value = idx
        }
    }

    private fun acquireWakeLock() {
        if (lockScreenEnabled && !wakeLock.isHeld) {
            wakeLock.acquire(4 * 60 * 60 * 1000L) // 4 hour max safety timeout
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) wakeLock.release()
    }

    private fun launchLockScreen() {
        if (!lockScreenEnabled) return
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        startActivity(intent)
        lockScreenLaunched = true
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LyricsLock Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps LyricsLock running in background"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LyricsLock")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_music_note)
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        handler.removeCallbacks(syncRunnable)
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }
}
