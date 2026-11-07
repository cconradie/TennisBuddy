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
