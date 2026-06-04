package com.japaneseminimal.musicplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VolumeControl(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VolumeDown,
            contentDescription = "Volume Down",
            modifier = Modifier.size(20.dp),
            tint = Color.White.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            modifier = Modifier.weight(1f),
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Volume Up",
            modifier = Modifier.size(20.dp),
            tint = Color.White.copy(alpha = 0.6f)
        )
    }
}
