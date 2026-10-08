# DFUSE Tone Forge v0.7-beta

- Seven taps on DFUSE unlock the full Settings page; the gear stays available.
- Saved Purple, Teal, Ember, Blue, and White themes cover the app and editor.
- Preferred save type appears first in the save dialog.
- Preview looping and initial trim presets: full track, 5, 15, or 30 seconds.
- M4A/AAC export quality: 96, 192, or 256 kbps (requested encoder bitrate).
- Export fade in/out: off, 0.5, 1, or 2 seconds, limited for short clips.
- Clear waveform cache and open the sound-setting permission screen.
- Lighter waveform sample analysis, progressive display, and completed-waveform caching.
- Website download and feature descriptions updated for this beta.

## Notes

Fades affect exported audio; editor preview plays the original selection.
Trim presets apply when loading new tracks. Existing selections are preserved.
Export currently uses M4A/AAC; MP3/WAV and file-picker saving are future work.
Samsung device testing remains pending. Existing custom alarms, app notification
channels, and secondary-SIM ringtones may retain separate sounds.

## Build and validation

Version: 0.7-beta. Version code: 7. Tag: v0.7-beta.
This beta APK is signed with the build machine's Android debug key, matching
Android Studio debug installs made using that same key.
Source, installer, archive, and website checks were performed in preparation.
The previous full Settings page compiled on the user's PC. The added export,
fade, and playback settings require local build and device validation.
