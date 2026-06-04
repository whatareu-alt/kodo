# Troubleshooting Music Playback Issues

If music is not playing in the app, try these solutions:

## Issue 1: No Music Files in Emulator

**Solution**: Add music files to the emulator first

- See `HOW_TO_ADD_MUSIC.md` for detailed instructions
- Quick test: Use ADB to push a music file

## Issue 2: Permissions Not Granted

**Solution**:

1. Open Android Settings on the emulator
2. Go to Apps → Japanese Music Player → Permissions
3. Enable "Music and audio" permission
4. Restart the app

## Issue 3: Service Not Starting

**Solution**:

1. Check Android Studio Logcat for errors
2. Filter by "MusicPlayback" or "ExoPlayer"
3. Look for permission or initialization errors

## Issue 4: MediaController Not Connected

**Symptoms**: Songs appear but don't play when tapped

**Solution**:

1. Kill and restart the app completely
2. Check Logcat for "MediaController" connection errors
3. Make sure the service is declared in AndroidManifest.xml

## Issue 5: Audio Focus Issues

**Solution**:

1. Make sure no other apps are playing audio
2. Check device volume is not muted
3. Try adjusting the volume slider in the app

## Quick Test Steps

1. **Add a test music file**:

   ```bash
   adb push "C:\path\to\song.mp3" /sdcard/Music/test.mp3
   ```

2. **Grant permissions manually**:
   - Settings → Apps → Japanese Music Player → Permissions → Allow all

3. **Restart the app completely**:
   - Force stop from Android Settings
   - Launch again

4. **Check Logcat**:
   - In Android Studio: View → Tool Windows → Logcat
   - Filter: "MusicPlayback|ExoPlayer|MediaController"

## Common Error Messages

### "Permission denied"

- Grant storage/media permissions in Settings

### "Unable to connect to service"

- Service may not be starting - check AndroidManifest.xml
- Try rebooting the emulator

### "No audio track found"

- File format may not be supported
- Try a different MP3 file

### "MediaController not ready"

- Service initialization issue
- Restart the app

## Debug Mode

To see what's happening, check Android Studio Logcat while:

1. Opening the app
2. Tapping on a song
3. Pressing play button

Look for any red error messages or warnings.

## Still Not Working?

If none of the above works:

1. Clean and rebuild the project (Build → Clean Project, then Build → Rebuild Project)
2. Uninstall the app from emulator
3. Run the app again fresh
4. Check that the emulator has audio enabled (emulator settings)
