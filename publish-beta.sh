#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
repo="dfuse06/DFUSE-Tone-Forge"
tag="v0.7-beta"
apk="$HOME/Downloads/DFUSE-Tone-Forge-v0.7-beta.apk"
command -v gh >/dev/null || { echo "GitHub CLI missing. Install it: sudo pacman -S github-cli"; exit 1; }
gh auth status
[[ "$(git branch --show-current)" == "main" ]] || { echo "Run this release from main."; exit 1; }
case "$(git remote get-url origin)" in
 https://github.com/dfuse06/DFUSE-Tone-Forge|https://github.com/dfuse06/DFUSE-Tone-Forge.git|git@github.com:dfuse06/DFUSE-Tone-Forge.git) ;;
 *) echo "Origin must point to $repo."; exit 1;;
esac
if gh release view "$tag" --repo "$repo" >/dev/null 2>&1; then
 echo "$tag already exists. Stop to review it before replacing an APK."; exit 1
fi
if [[ -n "$(git diff --cached --name-only)" ]]; then
 echo "There are already staged changes. Commit or unstage them before running the release."; exit 1
fi
bash ./gradlew :app:assembleDebug
mkdir -p "$HOME/Downloads"
cp app/build/outputs/apk/debug/app-debug.apk "$apk"
echo "APK built: $apk"
git add -- README.md RELEASE-NOTES.md EDITOR-IMPROVEMENTS.md app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/example/dfusetoneforge/MainActivity.kt app/src/main/java/com/example/dfusetoneforge/AudioEditorActivity.kt app/src/main/java/com/example/dfusetoneforge/WaveformCache.kt app/src/main/java/com/example/dfusetoneforge/FadeAudioProcessor.kt app/src/main/java/com/example/dfusetoneforge/RingToneForge.kt app/src/main/java/com/example/dfusetoneforge/SoundDefaults.kt app/src/main/java/com/example/dfusetoneforge/StorageUtils.kt app/src/main/java/com/example/dfusetoneforge/ui/theme/Theme.kt docs/index.html docs/style.css install-update.sh publish-beta.sh
if ! git diff --cached --quiet; then
 git commit -m "Release 0.7-beta: unlocked settings, themes and waveform improvements"
fi
git push origin main
release_commit="$(git rev-parse HEAD)"
gh release create "$tag" "$apk" --repo "$repo" --target "$release_commit" --title "Tone Forge v0.7 Beta" --notes-file RELEASE-NOTES.md --prerelease
echo "Published: https://github.com/$repo/releases/tag/$tag"
echo "Website APK link: https://github.com/$repo/releases/download/$tag/DFUSE-Tone-Forge-v0.7-beta.apk"
