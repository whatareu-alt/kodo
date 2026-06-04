package com.japaneseminimal.musicplayer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.japaneseminimal.musicplayer.service.MusicPlaybackService
import com.japaneseminimal.musicplayer.ui.screens.LibraryScreen
import com.japaneseminimal.musicplayer.ui.screens.PlayerScreen
import com.japaneseminimal.musicplayer.ui.theme.JapaneseMusicPlayerTheme
import com.japaneseminimal.musicplayer.viewmodel.LibraryViewModel
import com.japaneseminimal.musicplayer.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startMusicService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            JapaneseMusicPlayerTheme {
                MainScreen()
            }
        }
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        requestPermissionLauncher.launch(permissions)
    }

    private fun startMusicService() {
        val intent = Intent(this, MusicPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel()

    val playerState by playerViewModel.playerState.collectAsState()
    val libraryState by libraryViewModel.libraryState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "Player") },
                    label = { Text("Player") },
                    selected = currentRoute == "player",
                    onClick = {
                        navController.navigate("player") {
                            popUpTo("library") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                    label = { Text("Library") },
                    selected = currentRoute == "library",
                    onClick = {
                        navController.navigate("library") {
                            popUpTo("player") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "library",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("player") {
                PlayerScreen(
                    playerState = playerState,
                    onPlayPause = { playerViewModel.playPause() },
                    onSkipNext = { playerViewModel.skipToNext() },
                    onSkipPrevious = { playerViewModel.skipToPrevious() },
                    onSeek = { position -> playerViewModel.seekTo(position) },
                    onVolumeChange = { volume -> playerViewModel.setVolume(volume) }
                )
            }

            composable("library") {
                LibraryScreen(
                    songs = libraryState.songs,
                    isLoading = libraryState.isLoading,
                    searchQuery = libraryState.searchQuery,
                    onSearchQueryChange = { query ->
                        libraryViewModel.searchSongs(query)
                    },
                    onSongClick = { song ->
                        playerViewModel.playSong(song)
                        navController.navigate("player")
                    }
                )
            }
        }
    }
}
