# Tone Forge waveform update

The editor now has 16,384 time-aligned peak buckets, native-order PCM decoding,
float PCM support, multi-channel peak detection, flat silence, cancellation,
a decoder timeout, and guaranteed decoder cleanup.

Editing adds a time ruler, 1–16× zoom, a scrollbar for the zoomed viewport,
tap or drag to seek, a teal playback cursor, larger trim grips, 0.1-second
start/end nudges, hundredths in time labels, and selected duration. Playback
prepares asynchronously and pauses when the app leaves the foreground.
Metadata is read off the UI thread. Preview plays the selected range once.
Reopening the editor restores the last accepted range. New tracks default to
the whole track instead of a fixed 12–42-second range. Short clips can be trimmed.
Export and editor launch are guarded while another operation is running.

## Open and check

Open this project in Android Studio and let Gradle sync. Keep your own
local.properties SDK path. Run `./gradlew :app:assembleDebug` (Windows:
`gradlew.bat :app:assembleDebug`).

Check on a device:
- Load a short clip under 1.5 seconds; move both handles and use selection.
- Load a track with silence and sharp transients; confirm the waveform matches.
- Zoom to 16× and scroll to both ends; tap to seek and preview a selected range.
- Nudge both boundaries, accept, and reopen; the selected times should persist.
- Start playback, background the app, then return; playback should be paused.
- Export a newly loaded track without editing; it should export the full track.

Validation here: source diff whitespace check and ZIP integrity check.
A full Android build and device/UI checks could not run: this environment has
no Android SDK and cannot download the required Gradle distribution.

The archive excludes generated build caches and machine-local IDE/SDK settings.
Source, resources, Gradle configuration and wrapper, and Git history are retained.

## Reference-style waveform and Set now

The compact track header leaves more height for the continuous filled waveform.
RMS energy blended with transient peaks preserves quieter musical passages.
The save dialog offers an optional Set as default now checkbox for each sound
category. Android's modify-system-settings permission is requested if necessary;
a persisted pending choice is applied when the user returns. Denial leaves the
saved audio available and does not change the default. Samsung uses the same
supported Android API and stores sounds in the public sound folders. Android
8/9 storage permissions and legacy registration are handled separately.

Alarm changes are for the system default, not existing custom Samsung Clock
alarms. App notification channels and secondary-SIM sounds may have separate
settings. Samsung device testing is still required.

Test: save-only leaves defaults unchanged; Set now with permission applies the
chosen category; first-time grant and denial both preserve the exported file;
recreate the activity while in permission settings; test on Samsung and Android
8/9; check a quiet track and zoom into a transient. No full build is claimed.

Run the bundled installer with bash to update the existing project while keeping
Git history and local SDK settings. It backs up all replaced files first.
