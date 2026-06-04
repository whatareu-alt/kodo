# Debug Steps - Song Not Playing

## What We Just Added

I've added **detailed logging** to help us find the problem. Now rebuild and run the app, then:

## Steps to Debug

### 1. Open Logcat in Android Studio

- **View → Tool Windows → Logcat**
- Clear the log (trash icon)

### 2. Try to Play a Song

- Tap a song in the Library
- Watch the Logcat output

### 3. Look for These Messages

**✅ Good Messages (what you SHOULD see):**

```
PlayerViewModel: playSong called for: [Song Name]
PlayerViewModel: Creating MediaItem for URI: content://...
PlayerViewModel: Playback started. Player state: 3
PlayerViewModel: MediaController connected successfully
PlayerViewModel: Position: 1234 / 180000
```

**❌ Error Messages (what might appear):**

```
PlayerViewModel: MediaController is null!
PlayerViewModel: Failed to connect MediaController
PlayerViewModel: Error playing song
ExoPlayer: Source error
```

### 4. Common Issues and What Logcat Will Show

**Issue: MediaController is null**

- Message: `"MediaController is null!"`
- **Fix**: Service not starting properly
  - Check if service is declared in AndroidManifest
  - Restart the app

**Issue: Permission denied**

- Message: `"Permission denied"` or `"Source error"`
- **Fix**: Grant storage permissions in Settings

**Issue: File not found**

- Message: `"FileNotFoundException"` or `"Source error"`
- **Fix**: Music file doesn't exist at that URI
  - Re-add the music file using ADB

**Issue: No position updates**

- No `"Position: X / Y"` messages appearing
- **Fix**: Playback not actually starting
  - Check ExoPlayer errors in Logcat

## After Checking Logcat

**Tell me what you see:**

- Do you see "MediaController connected successfully"?
- Do you see "playSong called for: [song name]"?
- Do you see "Position: X / Y" messages?
- Any red error messages?

**Copy and paste the relevant Logcat output** and I'll help you fix the exact issue!

## Quick Rebuild

```
1. Build → Clean Project
2. Build → Rebuild Project
3. Run the app
4. Tap a song
5. Check Logcat
```
