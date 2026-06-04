package com.japaneseminimal.musicplayer.viewmodel

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.japaneseminimal.musicplayer.data.model.Song
import com.japaneseminimal.musicplayer.service.MusicPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val volume: Float = 1.0f
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var mediaController: MediaController? = null
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackPosition()
        }
        
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updatePlaybackPosition()
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updatePlaybackPosition()
        }
    }

    init {
        initializeController()
        startProgressUpdates()
    }
    
    // Smooth progress bar updates
    private fun startProgressUpdates() {
        viewModelScope.launch {
            android.util.Log.d("PlayerViewModel", "Progress update loop started")
            while (true) {
                kotlinx.coroutines.delay(100) // Update every 100ms for smooth animation
                if (_playerState.value.isPlaying) {
                    android.util.Log.d("PlayerViewModel", "Updating position (isPlaying=true)")
                    updatePlaybackPosition()
                }
            }
        }
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), MusicPlaybackService::class.java)
        )

        val controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                try {
                    mediaController = controllerFuture.get()
                    mediaController?.addListener(playerListener)
                    android.util.Log.d("PlayerViewModel", "MediaController connected")
                } catch (e: Exception) {
                    android.util.Log.e("PlayerViewModel", "Connection failed", e)
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            mediaController?.let { controller ->
                try {
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(song.id.toString())
                        .setUri(song.uri)
                        .build()

                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                    controller.play()

                    _playerState.value = _playerState.value.copy(
                        currentSong = song,
                        isPlaying = true,
                        duration = song.duration
                    )
                } catch (e: Exception) {
                    android.util.Log.e("PlayerViewModel", "Playback error", e)
                }
            }
        }
    }

    fun playPause() {
        mediaController?.apply {
            if (isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        _playerState.value = _playerState.value.copy(currentPosition = position)
    }

    fun skipToNext() {
        mediaController?.seekToNext()
    }

    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun setVolume(volume: Float) {
        mediaController?.volume = volume
        _playerState.value = _playerState.value.copy(volume = volume)
    }

    private fun updatePlaybackPosition() {
        mediaController?.let { controller ->
            val position = controller.currentPosition
            val duration = controller.duration.coerceAtLeast(0L)
            android.util.Log.d("PlayerViewModel", "Position update: $position / $duration (isPlaying=${controller.isPlaying})")
            _playerState.value = _playerState.value.copy(
                currentPosition = position,
                duration = duration
            )
        } ?: run {
            android.util.Log.w("PlayerViewModel", "updatePlaybackPosition called but MediaController is null")
        }
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        super.onCleared()
    }
}
