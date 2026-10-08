#!/usr/bin/env bash
set -euo pipefail
source_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_dir="${1:-$HOME/Projects/DFUSE-Tone-Forge}"
backup_dir="$HOME/Projects/Tone-Forge-backup-$(date +%Y%m%d-%H%M%S)"
files=(
README.md
RELEASE-NOTES.md
EDITOR-IMPROVEMENTS.md
app/build.gradle.kts
app/src/main/AndroidManifest.xml
docs/index.html
docs/style.css
publish-beta.sh
install-update.sh
app/src/main/java/com/example/dfusetoneforge/MainActivity.kt
app/src/main/java/com/example/dfusetoneforge/AudioEditorActivity.kt
app/src/main/java/com/example/dfusetoneforge/WaveformCache.kt
app/src/main/java/com/example/dfusetoneforge/FadeAudioProcessor.kt
app/src/main/java/com/example/dfusetoneforge/RingToneForge.kt
app/src/main/java/com/example/dfusetoneforge/SoundDefaults.kt
app/src/main/java/com/example/dfusetoneforge/StorageUtils.kt
app/src/main/java/com/example/dfusetoneforge/ui/theme/Theme.kt
)
[[ -f "$project_dir/settings.gradle.kts" ]] || { echo "Project not found: $project_dir"; exit 1; }
for file in "${files[@]}"; do [[ -f "$source_dir/$file" ]] || { echo "Update missing: $file"; exit 1; }; done
for file in "${files[@]}"; do
 if [[ -f "$project_dir/$file" ]]; then
  mkdir -p "$backup_dir/$(dirname "$file")"
  cp -p "$project_dir/$file" "$backup_dir/$file"
 fi
done
for file in "${files[@]}"; do
 mkdir -p "$project_dir/$(dirname "$file")"
 cp "$source_dir/$file" "$project_dir/$file"
done
echo "Updated Tone Forge. Backup: $backup_dir"
echo "Ready for 0.7-beta. From the project folder run: bash publish-beta.sh"
