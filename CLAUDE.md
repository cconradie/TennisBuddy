# TennisBuddy

Tennis score-keeping app built with Kotlin Multiplatform: shared scoring logic, Jetpack Compose on Android, SwiftUI on iOS (planned).

## Summary of current state

- **`shared/`** (done): `Scoring.kt` has `MatchState`, `MatchConfig`, `SetScore`, `Player`. It is immutable; `pointWonBy(player)` returns the next state. Covers 0/15/30/40, deuce/advantage, win-by-two games, tiebreak at 6-6 (first to 7, win by 2), best-of-N sets, match winner. `MatchController.kt` wraps it in a `StateFlow` with undo and reset. Targets: Android, iOS (x64/arm64/simulator), JVM.
- **`androidApp/`** (basic): Compose scoreboard in `MainActivity.kt` (point buttons, undo, new match). Not yet run on a device.
- **`iosApp/`** (not started): README only; the Xcode project must be created on a Mac.
- **Tooling:** Gradle wrapper 8.14.3, version catalog in `gradle/libs.versions.toml` (Kotlin 2.1.21, AGP 8.7.3), CI in `.github/workflows/ci.yml`.
- **Verified:** the 9 scoring tests in `shared/src/commonTest` pass on the JVM. The Android and iOS builds have never been run (the cloud sandbox cannot reach Google's Maven repo).

## Commands

Always use the wrapper, never a system `gradle`:

```
./gradlew :shared:jvmTest            # scoring tests, no device or Android SDK needed
./gradlew :androidApp:assembleDebug  # needs Android SDK (ANDROID_HOME) and Google Maven access
./gradlew :androidApp:lint
```

## Conventions

- Put all scoring and game rules in `shared/commonMain`; keep UI code free of rules.
- New rules need a test in `shared/commonTest` first (`kotlin.test`).
- Keep `MatchState` immutable; mutate only through `MatchController`.
- Dependencies and plugin versions go in `gradle/libs.versions.toml`.
- Min SDK 26, compile/target SDK 35, Java 17.

## Plan

1. **Confirm the build** (first priority). Run `./gradlew :shared:jvmTest :androidApp:assembleDebug` somewhere with Android SDK access (CI or a dev machine) and fix any Gradle config errors.
2. **Android UI polish.** Move state to a lifecycle-aware `ViewModel` so rotation does not reset the match. Add a proper scoreboard layout, server indicator, tiebreak label, and a Material theme.
3. **Match setup screen.** Player names, best of 1/3/5, optional no-ad scoring, optional match tiebreak instead of a final set.
4. **Rules to add in `shared`** (with tests): no-ad games, serve tracking (alternate each game, tiebreak rotation), final-set tiebreak variants.
5. **Persistence.** Save the in-progress match and match history (SQLDelight or DataStore, in `shared`).
6. **iOS app.** Create the Xcode project per `iosApp/README.md`, build a SwiftUI scoreboard on `MatchController`.
7. **Release.** App icon, signing config, Play Store listing, CI artifacts.

## Known limitations

- Platform-specific build files are untested in the cloud sandbox; treat the first CI run as the real check.
- The GitHub App may need to be installed on `cconradie/TennisBuddy` before sessions can push.
