package com.japaneseminimal.musicplayer.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.japaneseminimal.musicplayer.MainActivity

class MusicPlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeSession()
    }

    private fun initializePlayer() {
        // High-quality audio attributes
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Build ExoPlayer with enhanced audio quality
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSkipSilenceEnabled(true)  // Skip silence for better experience
            .setWakeMode(C.WAKE_MODE_LOCAL)  // Keep CPU awake during playback
            .build()
            
        // Set audio session ID for effects
        player.setAudioSessionId(C.AUDIO_SESSION_ID_UNSET)
            
        // Add listener for debugging
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val state = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                android.util.Log.d("MusicPlaybackService", "Playback state: $state")
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicPlaybackService", "Player error: ${error.message}", error)
            }
        })
    }

    private fun initializeSession() {
        val sessionActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, MediaLibrarySessionCallback())
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    private inner class MediaLibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(
                LibraryResult.ofItem(
                    MediaItem.Builder()
                        .setMediaId("root")
                        .build(),
                    params
                )
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return Futures.immediateFuture(
                LibraryResult.ofItemList(emptyList(), params)
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updatedMediaItems = mediaItems.map { mediaItem ->
                // If the MediaItem already has a URI in localConfiguration, use it
                if (mediaItem.localConfiguration != null) {
                    mediaItem
                } else {
                    // Otherwise, build a new MediaItem with the URI from mediaId
                    mediaItem.buildUpon()
                        .setUri(mediaItem.mediaId)
                        .build()
                }
            }.toMutableList()
            return Futures.immediateFuture(updatedMediaItems)
        }
    }
}
