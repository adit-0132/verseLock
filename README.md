# 🎵 verseLock

**verseLock** is an Android app that transforms your lock screen into a real-time, time-synced lyrics display — automatically triggered whenever music starts playing. Built with Kotlin and Jetpack Compose, it hooks into the system media session to detect what's playing, fetches synced lyrics from [LRCLib](https://lrclib.net), and displays them in a stunning atmospheric UI right over your lock screen.

> 🔐 **Privacy first:** verseLock never collects, transmits, or stores any personal data. Everything stays on your device.

---

## ✨ Features

- **Auto Lock Screen Lyrics** — The lock screen appears automatically when music starts playing and dismisses when playback stops (with a 30-second grace period)
- **Real-Time Sync** — Lyrics scroll and highlight in sync with the actual playback position
- **Dynamic Album Art Backgrounds** — Extracts dominant colors from the album artwork and uses them to paint ambient background blobs that smoothly animate as tracks change
- **Media Controls** — Previous, Play/Pause, and Next buttons directly on the lock screen
- **Zero Configuration** — Uses [LRCLib](https://lrclib.net), a free and open public lyrics API with no API key required. Everything works out of the box
- **Offline Cache** — Fetched lyrics are cached locally in a Room database for up to 30 days, so they work offline after the first play
- **Universal Compatibility** — Works with Spotify, YouTube Music, Apple Music, or any media app that publishes a `MediaSession`
- **Immersive UI** — Full edge-to-edge display with the navigation bar hidden, punchy glassmorphic design, and fluid animations

---

## 📸 UI Design

The lock screen features a dark atmospheric design with:

- **Large time + date clock** at the top with a spacer to clear hole-punch cameras
- **Track pill** showing album art thumbnail, title, artist, and media controls
- **Animated lyrics scroller** — the active line is large and bright, surrounding lines fade out with distance
- **Ambient blob backgrounds** that pulse with colors extracted from the current album art
- **Smooth crossfades** between lyrics states (loading → lyrics → no lyrics found)

---

## 🏗️ Architecture

```
com.verselock/
├── verseLockApplication.kt       # App entry point; wires up Room DB & Repository
├── data/
│   ├── db/                        # Room database, DAO, entity
│   ├── model/                     # TrackInfo, LrcLine, LyricsResult
│   ├── network/                   # LrcLibApi (Ktor HTTP client)
│   ├── parser/                    # LrcParser — parses .lrc timestamps
│   └── repository/                # LyricsRepository — cache-first lyrics fetching
├── service/
│   ├── MediaListenerService.kt    # NotificationListenerService; reads MediaSession
│   └── LyricsService.kt          # Foreground service; orchestrates sync + lock screen
├── receiver/
│   └── BootReceiver.kt           # Restarts services on device boot
├── ui/
│   ├── LockScreenActivity.kt     # Full-screen lock screen Compose UI
│   ├── LockScreenViewModel.kt    # Bridges LyricsService StateFlows to UI
│   ├── SettingsActivity.kt       # Launcher screen; notification access + toggle
│   └── theme/                    # Compose theme, typography (Space Grotesk, DM Sans)
├── util/
│   ├── Prefs.kt                  # DataStore-backed preferences
│   └── TrackKeyUtils.kt          # Normalized cache key generation
└── worker/
    └── CacheEvictionWorker.kt    # Periodic WorkManager task to evict stale lyrics
```

**Stack:**
- **UI:** Jetpack Compose + Material 3
- **Database:** Room (SQLite)
- **Networking:** Ktor (lightweight, no OkHttp overhead)
- **Serialization:** Kotlinx Serialization
- **Async:** Kotlin Coroutines + StateFlow
- **Image Loading:** Coil
- **Color Extraction:** AndroidX Palette
- **Background Work:** WorkManager + Foreground Service

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android device or emulator running **API 27+** (Android 8.1 Oreo)
- A music app installed (Spotify, YouTube Music, etc.)

### Build & Install

1. Clone the repository:
   ```bash
   git clone https://github.com/adit-0132/verseLock.git
   cd verseLock
   ```

2. Open the project in Android Studio and let Gradle sync.

3. Build and install on your device:
   ```bash
   ./gradlew installDebug
   ```

### First-Time Setup

1. **Open verseLock** from the app drawer.
2. Tap **"Grant Access"** when prompted, and enable **verseLock** in the Notification Access settings screen.
3. Return to the app — the warning card will disappear once access is granted.
4. Make sure the **"Lock Screen Lyrics"** toggle is enabled.
5. Start playing music in any app — the lock screen will automatically appear! 🎶

> No API keys, no accounts, no extra configuration needed.

---

## 🔒 Permissions

| Permission | Reason |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Required to read the active `MediaSession` from other apps |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keeps the lyrics sync service alive while music plays |
| `WAKE_LOCK` | Keeps the screen on while the lock screen is showing |
| `INTERNET` | Fetches lyrics from LRCLib |
| `RECEIVE_BOOT_COMPLETED` | Restarts the listener service after device reboot |

---

## 🔐 Privacy

verseLock is designed to be **fully private and transparent**. Here's exactly what happens with your data:

- **Notification access is used solely to read the currently playing track** (song title and artist) from the system `MediaSession`. It is **never** used to read, log, or transmit notification content from any other app.
- **No data is ever sent to any server operated by verseLock.** The only outbound network request the app makes is an anonymous query to the public [LRCLib](https://lrclib.net) API to fetch lyrics for the current track title and artist name — the same information visible on your lock screen anyway.
- **Lyrics are cached locally** in a Room database on your device and are never uploaded or shared anywhere.
- **Album artwork** is accessed directly from the system `MediaSession` (provided by your music app) and is stored only temporarily in the app's private cache directory for display purposes.
- **No analytics, no crash reporting, no tracking libraries** of any kind are included in the app.
- **No accounts, no sign-in, no telemetry** — the app functions entirely offline after lyrics are cached, with the only external touchpoint being the open LRCLib lyrics endpoint.

In short: the notification access permission exists purely to detect what song is playing so the correct lyrics can be shown. Your listening habits, notifications, and device data never leave your phone.

---

## 🌐 Lyrics Source

verseLock fetches time-synced lyrics from [**LRCLib**](https://lrclib.net):

- Completely **free** and **open source**
- **No API key** required — anonymous requests work out of the box
- Returns lyrics in `.lrc` format with per-line timestamps
- Community-maintained with a vast catalog

If LRCLib doesn't have lyrics for a track, it shows "No lyrics found" gracefully and caches the miss to avoid repeated failed requests.

---

## ⚙️ How It Works

1. `MediaListenerService` listens for `MediaSession` changes via `NotificationListenerService`
2. When a track starts, it passes the track info (title, artist, album art bitmap) to `LyricsService`
3. `LyricsService` fetches lyrics from `LyricsRepository` (Room cache first, then LRCLib API)
4. Every 250ms, `LyricsService` computes the current playback position and finds the matching lyric line
5. State is broadcast via `StateFlow` objects to `LockScreenViewModel`
6. `LockScreenActivity` is launched as a fullscreen overlay over the keyguard, observing the ViewModel

---

## 🎨 Customization

Currently all visual customization lives in `LockScreenActivity.kt`. The design system uses:

- **Primary font:** [Space Grotesk](https://fonts.google.com/specimen/Space+Grotesk) (clock)
- **Body font:** [DM Sans](https://fonts.google.com/specimen/DM+Sans) (lyrics, track info)
- **Base background:** `#0A0F14` (deep navy-black)
- **Blob colors:** Dynamically extracted from album artwork via AndroidX Palette

---

## 📄 License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

---

## 🙏 Acknowledgments

- [LRCLib](https://lrclib.net) — for the wonderful free lyrics API
- [Jetpack Compose](https://developer.android.com/compose) — for making Android UI actually enjoyable
- The Android open-source community
