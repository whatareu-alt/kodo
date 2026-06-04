# Fix: Song Plays But No Sound

## Quick Fixes (Try in Order)

### 1. Check Emulator Volume

- Click the **volume up button** on the emulator (side buttons)
- Or use your keyboard volume keys while emulator is focused
- Make sure it's not muted

### 2. Check App Volume Slider

- In the app's **Player screen**, look for the volume slider
- It's between the progress bar and play/pause buttons
- Drag it to the right (increase volume)

### 3. Enable Emulator Audio

**In Android Studio:**

1. Stop the emulator
2. Go to **Tools → AVD Manager**
3. Click the **pencil icon** (Edit) on your emulator
4. Click **Show Advanced Settings**
5. Scroll down to **Emulated Performance**
6. Make sure **Audio output** is enabled
7. Save and restart emulator

### 4. Check Windows Audio

- Make sure your **computer speakers/headphones** are working
- Check Windows volume mixer
- Make sure Android Emulator is not muted in Windows

### 5. Test Emulator Audio

**Try playing audio in another app:**

1. Open **YouTube** or **Chrome** in the emulator
2. Play a video
3. If you hear sound → App issue
4. If no sound → Emulator audio disabled

### 6. Restart Everything

```bash
# Kill emulator
adb kill-server

# Restart Android Studio
# Launch emulator again
# Run the app
```

## If Still No Sound

### Check ExoPlayer Audio Settings

The app uses ExoPlayer which should handle audio automatically, but verify:

1. **In Android Studio Logcat**, filter by "ExoPlayer"
2. Look for audio-related messages when playing
3. Check for errors like:
   - "Audio track init failed"
   - "No audio decoder"
   - "Audio sink error"

### Alternative: Use Physical Device

If emulator audio continues to fail:

1. Connect your Android phone via USB
2. Enable USB debugging
3. Run the app on your phone
4. Audio should work perfectly

## Expected Behavior

✅ Song shows in player screen
✅ Play button changes to pause
✅ Progress bar moves
✅ **Sound comes from speakers** ← This should happen!

## Most Common Cause

**Emulator audio is disabled by default in some AVD configurations**

**Solution**: Recreate the AVD with audio enabled, or use a physical device.
