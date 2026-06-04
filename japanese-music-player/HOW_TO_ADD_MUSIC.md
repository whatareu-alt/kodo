# How to Add Music to Android Emulator

Since the emulator doesn't have music files by default, here are several ways to add music for testing:

## Method 1: Using ADB (Recommended)

1. **Download sample music** or use your own MP3 files

2. **Open Command Prompt/Terminal** and navigate to your Android SDK platform-tools folder:

   ```bash
   cd C:\Users\ragha\AppData\Local\Android\Sdk\platform-tools
   ```

3. **Push music files to the emulator**:

   ```bash
   adb push "path\to\your\music.mp3" /sdcard/Music/
   ```

   Example:

   ```bash
   adb push "C:\Users\ragha\Downloads\song.mp3" /sdcard/Music/
   ```

4. **Scan media files** so Android recognizes them:

   ```bash
   adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/
   ```

## Method 2: Using Android Studio Device File Explorer

1. In Android Studio, go to **View → Tool Windows → Device File Explorer**

2. Navigate to `/sdcard/Music/`

3. Right-click on the `Music` folder

4. Select **Upload** and choose your music files

5. After uploading, restart the app or reboot the emulator

## Method 3: Download Music in Emulator Browser

1. Open the **Chrome browser** in the emulator

2. Search for **"free music downloads"** or use sites like:
   - Free Music Archive (freemusicarchive.org)
   - Incompetech (incompetech.com)

3. Download MP3 files directly to the emulator

4. Files will be in `/sdcard/Download/`

## Method 4: Using Drag & Drop (Some Emulators)

1. Simply **drag and drop** MP3 files onto the emulator window

2. Files will be saved to `/sdcard/Download/`

3. Move them to `/sdcard/Music/` using a file manager app

## After Adding Music

1. **Restart the app** or pull down and refresh the library screen

2. The songs should appear in the library

3. Tap any song to play it

## Free Sample Music Sources

- **Incompetech**: <https://incompetech.com/music/royalty-free/>
- **Free Music Archive**: <https://freemusicarchive.org/>
- **YouTube Audio Library**: <https://www.youtube.com/audiolibrary>

## Troubleshooting

If music doesn't appear:

1. Make sure files are in `/sdcard/Music/` folder
2. Restart the app
3. Check file permissions in app settings
4. Reboot the emulator
