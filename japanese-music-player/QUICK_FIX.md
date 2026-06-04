# Quick Fix Guide - Common Issues

## Issue: No Songs in Library ❌

**Why**: Emulator has no music files by default

**Fix**:

```bash
# 1. Open Command Prompt (Windows)
cd C:\Users\ragha\AppData\Local\Android\Sdk\platform-tools

# 2. Download a test MP3 or use your own, then:
adb push "C:\path\to\song.mp3" /sdcard/Music/test.mp3

# 3. Restart the app
```

**Alternative**:

- Download MP3 in emulator browser → Save to Downloads
- Use Device File Explorer in Android Studio

---

## Issue: Songs Show But Won't Play ❌

**Why**: Permissions not granted or service issue

**Fix**:

1. **Grant Permissions**:
   - Open Settings on emulator
   - Apps → Japanese Music Player
   - Permissions → Enable "Music and audio"

2. **Force Restart App**:
   - Settings → Apps → Japanese Music Player → Force Stop
   - Open app again

3. **Check Logcat** (Android Studio):
   - View → Tool Windows → Logcat
   - Look for red errors when tapping a song

---

## Issue: App Crashes ❌

**Why**: Build or permission errors

**Fix**:

1. Clean and rebuild:

   ```
   Build → Clean Project
   Build → Rebuild Project
   ```

2. Uninstall from emulator:

   ```bash
   adb uninstall com.japaneseminimal.musicplayer
   ```

3. Run app fresh

---

## Issue: Service Not Starting ❌

**Why**: MediaController connection failed

**Fix**:

1. Check AndroidManifest.xml has the service declared
2. Restart emulator completely
3. Check Logcat for "MusicPlaybackService" errors

---

## Quick Test Checklist ✅

- [ ] Music file added to `/sdcard/Music/`
- [ ] Permissions granted in Settings
- [ ] App restarted after adding music
- [ ] No errors in Logcat
- [ ] Emulator volume is up
- [ ] Song appears in Library tab

---

## Still Not Working?

**Try this test MP3**:

1. Download any MP3 from: <https://incompetech.com/music/royalty-free/>
2. Save it to your computer
3. Push to emulator:

   ```bash
   adb push "C:\Downloads\yourfile.mp3" /sdcard/Music/
   ```

4. Force stop and reopen the app

**Check what you see**:

- Library tab empty? → Music not added correctly
- Songs show but no play? → Permission or service issue
- App crashes? → Check Logcat for errors
