# TennisBuddy
An app to keep Tennis score.

Kotlin Multiplatform project:

| Module | Purpose |
| --- | --- |
| `shared/` | Scoring rules (`MatchState`) and `MatchController`, in `commonMain`. Targets Android, iOS, JVM. |
| `androidApp/` | Jetpack Compose UI. |
| `iosApp/` | SwiftUI app (to be created in Xcode; see its README). |

## Commands
```
./gradlew :shared:jvmTest          # scoring unit tests, no device needed
./gradlew :androidApp:assembleDebug  # needs the Android SDK (ANDROID_HOME)
```

## Build and test on a device
CI (`.github/workflows/ci.yml`) runs tests and lint, then builds a debug APK and uploads it as the
`tennisbuddy-debug-apk` artifact on every PR and push. GitHub-hosted runners cannot reach your
device, so "deploy" means installing that APK locally:
```
./scripts/install-local.sh            # build on this machine, install via adb
./scripts/install-local.sh --from-ci  # install the latest CI-built APK (needs gh + adb)
```
Or download the artifact from the Actions run and sideload it on your phone.
