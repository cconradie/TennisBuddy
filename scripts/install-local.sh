#!/usr/bin/env bash
# Install the debug APK on a connected device/emulator (adb required).
#   ./scripts/install-local.sh            # build locally, then install
#   ./scripts/install-local.sh --from-ci  # download latest CI APK for this branch (gh CLI required), then install
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ "${1:-}" == "--from-ci" ]]; then
  branch=$(git rev-parse --abbrev-ref HEAD)
  run_id=$(gh run list --branch "$branch" --workflow CI --status success --limit 1 --json databaseId -q '.[0].databaseId')
  [[ -n "$run_id" ]] || { echo "No successful CI run found for $branch" >&2; exit 1; }
  rm -rf build/ci-apk && mkdir -p build/ci-apk
  gh run download "$run_id" -n tennisbuddy-debug-apk -D build/ci-apk
  apk=$(ls build/ci-apk/*.apk | head -1)
else
  ./gradlew :androidApp:assembleDebug
  apk=$(ls androidApp/build/outputs/apk/debug/*.apk | head -1)
fi

adb install -r "$apk"
adb shell am start -n com.tennisbuddy.android/.MainActivity
