# DFUSE Tone Forge v0.6-beta

A larger continuous waveform makes quiet passages and transients easier to see.
The compact editor adds 16× zoom, timeline scrolling, tap-to-seek, a live playback
cursor, selection preview, and 0.1-second trim adjustments. Accepted selections
are restored when reopening the editor, and short clips no longer hit the old
1.5-second trim limit.

Save clips as ringtones, notifications, or alarms. Choose Set as default now to
apply the exported sound directly. When needed, Tone Forge opens Android's
modify-system-settings permission screen and applies the pending choice on return.

Playback preparation and metadata loading avoid blocking the editor. Playback
stops when leaving the app. New tracks export their full range unless trimmed.

## Validation and limitations

- User confirmed the updated waveform and default notification sound on Motorola.
- Source whitespace, installer behavior, and ZIP integrity checks passed.
- Android build is required locally; no APK was built in this environment.
- Samsung, Android 8/9, and default ringtone/alarm device checks are pending.
- Default alarm changes do not override existing custom alarm sounds.
- App notification channels and secondary-SIM ringtones may use separate settings.

## Build for distribution

Version name: 0.6-beta. Version code: 6. Release tag: v0.6-beta.

In Android Studio use Build > Generate Signed App Bundle / APK > APK > release.
Use the same signing key as previous releases so existing installations can update.
Name the signed APK DFUSE-Tone-Forge-v0.6-beta.apk and attach it to the GitHub
release tagged v0.6-beta with these notes. Keep signing credentials out of Git.
