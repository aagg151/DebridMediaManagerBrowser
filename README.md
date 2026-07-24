# Debrid Media Manager Browser (Android)

A hybrid Android app for the [Debrid Media Manager](https://debridmediamanager.com) site with a
native Real-Debrid layer for downloading, playing, deleting and organizing your videos.

## What it does

| Tab | What it is |
|-----|------------|
| **Browse** | Full-screen WebView of the DMM site — search, browse and organize exactly as on the website. In-page file links are captured and queued into the native download manager. |
| **Library** | Native list of your Real-Debrid torrents (pulled from the RD API). Tap an item to pick a file, then **Play** (built-in player) or **Download**. The trash icon **deletes** the torrent from your account. Filter box + a **Sort** toggle (Date / Name / Size) for organizing. |
| **Downloads** | Native download queue backed by Android's `DownloadManager`, with live progress. Completed files get a **Play** button; remove clears them. Files land in `Movies/DebridBrowser/`. |

Videos play in a built-in **Media3 / ExoPlayer** full-screen player (streams and downloaded files).
An optional setting hands playback off to an external player (VLC / MX) instead.

## First-time setup

1. Install the APK (below).
2. Open the app → **⋮ → Settings**.
3. Paste your **Real-Debrid API token** from <https://real-debrid.com/apitoken>, tap **Test connection**, then **Save**.
   - The token is only needed for the native **Library** / **Downloads** features. The **Browse** tab works by logging into DMM directly in the WebView.

## Build

Requires the Android SDK + JDK 17–21. On this machine, build with Android Studio's bundled JDK
(system Java 25 is too new for Gradle 8.10.2):

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Or just open the folder in Android Studio and press **Run**.

## Install on a device

Enable USB debugging on the phone, connect it, then:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Stack

- Kotlin, single-Activity + 3 Fragments, ViewBinding, Material 3
- `androidx.webkit` WebView (Browse)
- OkHttp + Real-Debrid REST API (`/torrents`, `/torrents/info`, `/unrestrict/link`, `/torrents/delete`, `/user`)
- `androidx.media3` ExoPlayer (built-in player)
- System `DownloadManager` (download queue)
- minSdk 26 · targetSdk 35 · AGP 8.7.2 · Gradle 8.10.2

## Notes / next steps

- The app ships **unsigned debug**. For a shareable release build, set up a signing config.
- "Organize" is currently filter + sort in the Library plus DMM's own organize UI in Browse. Native
  collections/folders could be added on top of the RD API if you want them.
