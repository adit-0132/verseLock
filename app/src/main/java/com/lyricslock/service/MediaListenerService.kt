package com.lyricslock.service

import android.content.ComponentName
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import com.lyricslock.data.model.TrackInfo

class MediaListenerService : NotificationListenerService() {

    private var activeController: MediaController? = null
    private var currentTrack: TrackInfo? = null

    private val controllerCallback = object : MediaController.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata ?: return
            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: return
            val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
            
            var artUriStr = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)

            if (artUriStr == null) {
                val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                if (bitmap != null) {
                    try {
                        val file = java.io.File(cacheDir, "current_album_art.png")
                        java.io.FileOutputStream(file).use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                        }
                        artUriStr = android.net.Uri.fromFile(file).toString()
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

            val newTrack = TrackInfo(title, artist, album, albumArtUri = artUriStr, durationMs = duration)
            if (newTrack != currentTrack) {
                currentTrack = newTrack
                LyricsService.onTrackChanged(this@MediaListenerService, newTrack)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            when (state?.state) {
                PlaybackState.STATE_PLAYING -> {
                    LyricsService.onPlaybackStarted(
                        this@MediaListenerService,
                        state.position,
                        state.lastPositionUpdateTime
                    )
                }
                PlaybackState.STATE_PAUSED,
                PlaybackState.STATE_STOPPED -> {
                    LyricsService.onPlaybackPaused(this@MediaListenerService)
                }
                else -> {}
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshActiveController()
    }

    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshActiveController()
    }

    private fun refreshActiveController() {
        val manager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
        val component = ComponentName(this, MediaListenerService::class.java)
        try {
            val controllers = manager.getActiveSessions(component)
            val newController = controllers.firstOrNull()
            if (newController?.sessionToken != activeController?.sessionToken) {
                activeController?.unregisterCallback(controllerCallback)
                activeController = newController
                newController?.registerCallback(controllerCallback)
                // Immediately read current state
                newController?.metadata?.let { controllerCallback.onMetadataChanged(it) }
                newController?.playbackState?.let { controllerCallback.onPlaybackStateChanged(it) }
            }
        } catch (e: SecurityException) {
            // Notification access not granted
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "com.lyricslock.ACTION_PLAY" -> activeController?.transportControls?.play()
            "com.lyricslock.ACTION_PAUSE" -> activeController?.transportControls?.pause()
            "com.lyricslock.ACTION_NEXT" -> activeController?.transportControls?.skipToNext()
            "com.lyricslock.ACTION_PREV" -> activeController?.transportControls?.skipToPrevious()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        activeController?.unregisterCallback(controllerCallback)
        super.onDestroy()
    }
}
