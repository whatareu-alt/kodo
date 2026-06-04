package com.japaneseminimal.musicplayer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.japaneseminimal.musicplayer.ui.components.PlaybackControls
import com.japaneseminimal.musicplayer.ui.components.VolumeControl
import com.japaneseminimal.musicplayer.viewmodel.PlayerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong = playerState.currentSong

    // Animated progress for smooth slider
    val animatedProgress by animateFloatAsState(
        targetValue = if (playerState.duration > 0) {
            (playerState.currentPosition.toFloat() / playerState.duration.toFloat())
        } else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "progress"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Blurred background (Apple Music style)
        AsyncImage(
            model = currentSong?.albumArtUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(100.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )

        // Dark overlay for better text contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            // Large Album Art (Apple Music style - bigger and more prominent)
            AsyncImage(
                model = currentSong?.albumArtUri,
                contentDescription = "Album Art",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = true
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Song Title - Large and bold (Apple Music style)
            Text(
                text = currentSong?.title ?: "No song playing",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Artist - Medium weight
            Text(
                text = currentSong?.artist ?: "",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                ),
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Album - Lighter
            Text(
                text = currentSong?.album ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            // Progress Slider - Thicker like Apple Music
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = animatedProgress,
                    onValueChange = { newValue ->
                        val newPosition = (newValue * playerState.duration).toLong()
                        onSeek(newPosition)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color.White, shape = RoundedCornerShape(6.dp))
                        )
                    }
                )

                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(playerState.currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatTime(playerState.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls
            PlaybackControls(
                isPlaying = playerState.isPlaying,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Volume Control
            VolumeControl(
                volume = playerState.volume,
                onVolumeChange = onVolumeChange,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatTime(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, remainingSeconds)
}
