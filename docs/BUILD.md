# Build and CI

## Toolchain

- Java 17
- Gradle Wrapper 9.7.1
- Android Gradle Plugin 9.3.2
- Kotlin 2.4.10
- compile/target SDK 37
- Compose BOM 2026.08.00

Compose libraries, including Material 3, are resolved from the stable BOM. Do not add a Material 3 alpha override solely to work around editor IME behavior; the attempted alpha override did not eliminate keyboard rebuilding on the tested physical device and has been removed.

## Local verification

Use a compatible Android SDK/JDK installation and the checked-in wrapper:

```bash
./scripts/verify.sh
```

Equivalent tasks:

```bash
python3 scripts/static_audit.py
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Do not substitute an arbitrary system Gradle version when recording verification evidence; the wrapper defines the project Gradle version.

## Host environment

The project does not require NixOS or Nix to build. A normal Linux/macOS/Windows Android development environment is valid as long as Java/Android SDK requirements are met.

The repository keeps an optional Nix/FHS environment for reproducibility and for hosts where Android SDK binaries need compatibility wrapping. See `NIX.md` when using that path. Avoid mixing incompatible `adb` installations during physical-device testing.

## CI

`.github/workflows/android.yml` runs for pull requests and pushes to `main`. It checks out source, installs Java, validates the Gradle Wrapper, configures Gradle caching and runs the same static audit, JVM tests, lint and APK compilation gate described above.

Third-party actions are pinned to immutable commit SHAs and use Node 24-native releases. Verification reports and the debug APK are uploaded as short-lived workflow artifacts.

A red CI run must be classified from its actual logs rather than assumed to be infrastructure. Test synchronization should wait for observable operation completion instead of relying on `advanceUntilIdle()` when production work runs in `viewModelScope` outside the coroutine-test scheduler.

## Device verification

Automated verification does not replace physical-device QA for the interaction paths that depend on OEM behavior. Recheck at minimum:

- persistent numeric-editor/IME transitions;
- haptic feedback on the intentionally limited interaction set;
- modal-sheet tap/drag/insets;
- calendar gestures and contextual `Fill today`;
- system document picker import/export;
- home-screen widget refresh/tap-through and compact layout;
- launcher icon presentation after install/update.

Record exact device model, Android version and tested commit for a release candidate.
