# DFUSE Tone Forge

DFUSE Tone Forge is an Android ringtone creation app built with Kotlin and Jetpack Compose.

Create custom ringtones from online media using a streamlined workflow:

Download → Extract Audio → Edit Waveform → Trim → Forge Ringtone

## Features

* Download media from supported URLs
* Automatic audio extraction
* Continuous waveform editor with a compact landscape layout
* Zoom up to 16× with timeline scrolling
* Tap to seek, playback cursor, and selection preview
* Precise trimming with 0.1-second adjustments
* Dual trim handles
* Ringtone creation
* Save to public Ringtones, Notifications, or Alarms folders
* Apply the chosen default sound from the app
* Modify-system-settings permission flow with return-to-app handling
* Samsung-compatible Android APIs (Samsung device testing pending)
* Easter egg unlock: tap DFUSE seven times; Settings gear stays available
* Five saved themes: Purple, Teal, Ember, Blue, and White
* Progressive waveform generation with lighter analysis and waveform caching
* Preferred save type, loop preview, and initial trim presets
* M4A/AAC export quality at 96, 192, or 256 kbps
* Adjustable export fade in/out (0.5, 1, or 2 seconds)
* Waveform cache clearing and sound-permission shortcut

## Built With

* Kotlin
* Jetpack Compose
* Android Media3
* yt-dlp Android

## Third-Party Libraries

### yt-dlp Android

Media downloading support provided by:

https://github.com/yausername/youtubedl-android

Powered by:

https://github.com/yt-dlp/yt-dlp

Please review their licenses and terms before distribution.

### Android Media3

Used for audio processing and ringtone generation.

https://developer.android.com/media/media3

## Disclaimer

DFUSE Tone Forge does not provide media content.

Users are responsible for complying with copyright laws, platform terms of service, and applicable regulations when downloading or processing media.

This project is intended for personal audio editing and ringtone creation.

## Status

Current release preparation: **0.7-beta** (version code 7).

Waveform UI and default notification application were tested by the user on a Motorola phone. Samsung, default ringtone/alarm, and Android 8/9 testing remain pending.

### Planned Features

* MP3 and WAV export
* Save audio through Android’s file picker

## Author

DFUSE

## Terminal beta release

Run `bash publish-beta.sh` from the project directory after installing the update.
It builds a debug-signed APK, commits the release files, pushes `main`, and creates
GitHub prerelease `v0.7-beta` with the APK used by the website download link.
GitHub CLI must be installed and authenticated. Keep using the same debug key
for these beta updates; a different key will not update an existing installation.
