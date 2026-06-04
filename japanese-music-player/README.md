# Japanese Music Player - Android App

A minimalist music player for Android inspired by Japanese design principles, featuring clean aesthetics, thoughtful use of negative space (Ma), and subtle elegance.

## Features

- 🎵 **Music Playback**: Play music from your device's storage
- 🎨 **Japanese Minimal Design**: Clean UI with generous spacing and natural colors
- 🔍 **Search**: Quickly find songs, artists, or albums
- 🎛️ **Playback Controls**: Play, pause, skip, and seek functionality
- 📱 **Modern Android**: Built with Jetpack Compose and Media3

## Design Philosophy

This app embodies core Japanese aesthetic principles:

- **Ma (間)**: Generous negative space for visual breathing room
- **Wabi-Sabi (侘寂)**: Beauty in simplicity and authenticity
- **Shibui (渋い)**: Understated elegance
- **Kanso (簡素)**: Simplicity without sacrifice

## Technology Stack

- **Kotlin**: Modern Android development language
- **Jetpack Compose**: Declarative UI framework
- **Material Design 3**: Latest Material Design components
- **Media3 (ExoPlayer)**: Advanced media playback
- **MediaSession**: System integration (lock screen, notifications)
- **Room**: Local database for playlists (future feature)

## Requirements

- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Android Studio**: Hedgehog | 2023.1.1 or later

## Setup Instructions

1. **Clone or Open in Android Studio**:

   ```
   File → Open → Navigate to this directory
   ```

2. **Sync Gradle**: Android Studio will automatically sync Gradle files

3. **Run the App**:
   - Connect an Android device with USB debugging enabled, or start an emulator
   - Click the Run button (green play icon) or press `Shift+F10`

4. **Grant Permissions**: When the app launches, grant permissions to access media files

5. **Add Music**: Ensure you have audio files on your device for testing

## Project Structure

```
app/src/main/java/com/japaneseminimal/musicplayer/
├── data/
│   ├── model/          # Data classes (Song)
│   └── repository/     # Data access (MusicRepository)
├── service/            # Background service (MusicPlaybackService)
├── ui/
│   ├── components/     # Reusable UI components
│   ├── screens/        # Main screens (Player, Library)
│   └── theme/          # Theme configuration (colors, typography)
├── viewmodel/          # ViewModels (PlayerViewModel, LibraryViewModel)
├── MainActivity.kt     # Main activity
└── MusicPlayerApplication.kt  # Application class
```

## Key Components

- **MusicPlaybackService**: Foreground service for continuous music playback
- **PlayerScreen**: Main player interface with album art and controls
- **LibraryScreen**: Browse and search your music library
- **MusicRepository**: Accesses device music using MediaStore API

## Permissions

The app requires the following permissions:

- `READ_MEDIA_AUDIO` (Android 13+) / `READ_EXTERNAL_STORAGE` (older versions)
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`
- `POST_NOTIFICATIONS` (Android 13+)

## Future Enhancements

- Playlist creation and management
- Audio equalizer
- Lyrics support
- Multiple theme variants
- Shuffle and repeat modes
- Widget support
- Android Auto integration

## License

This project is open source and available for learning purposes.
