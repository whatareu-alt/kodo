package com.japaneseminimal.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(60.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(40.dp),
                tint = Color.White.copy(alpha = 0.9f)
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Play/Pause button - Large and prominent (Apple Music style)
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(48.dp),
                tint = Color.Black
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Next button
        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(60.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(40.dp),
                tint = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
