# Build and CI

## Toolchain

- Java 17
- Gradle Wrapper 9.5.0
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

`.github/workflows/android.yml` checks out source, installs Java, configures Gradle caching and executes the same checked-in wrapper for static audit, JVM tests, lint and APK builds.

GitHub Actions runs may fail before job steps begin because of account/runner availability. A run with no executed verification steps is an infrastructure result: it is neither successful Gradle verification nor evidence of a compiler/test defect.

When runner capacity is available, rerun CI on the current `main` candidate and retain APK/test/lint artifacts.

## Device verification

Automated verification does not replace physical-device QA for the interaction paths that previously depended on OEM behavior. Recheck at minimum:

- persistent numeric-editor/IME transitions;
- modal-sheet tap/drag/insets;
- calendar gestures and contextual `Fill today`;
- system document picker import/export;
- home-screen widget refresh/tap-through;
- launcher icon presentation after install/update.

Record exact device model, Android version and tested commit for a release candidate.
