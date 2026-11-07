# iosApp

Placeholder for the SwiftUI app. The `shared` module already builds the static
`Shared` framework for `iosArm64`, `iosX64` and `iosSimulatorArm64`.

To finish on a Mac:
1. Create an Xcode project here (iOS App, SwiftUI).
2. Add a "Run Script" build phase: `cd "$SRCROOT/.." && ./gradlew :shared:embedAndSignAppleFrameworkForXcode`
3. Add `$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` to Framework Search Paths and link `Shared`.
4. `import Shared` and drive `MatchController` (its `state` is a `StateFlow`).
