package com.japaneseminimal.musicplayer.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtUri: Uri?,
    val duration: Long,
    val uri: Uri,
    val dateAdded: Long = 0
) {
    val durationFormatted: String
        get() {
            val seconds = duration / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%d:%02d", minutes, remainingSeconds)
        }
}
