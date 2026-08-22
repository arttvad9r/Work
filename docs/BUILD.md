# Build and CI

## Toolchain

- Java 17
- Gradle Wrapper 9.5.0
- Android Gradle Plugin 9.3.1
- Kotlin 2.4.10
- compile/target SDK 37
- Compose BOM 2026.08.00

Compose libraries, including Material 3, are resolved from the stable BOM. Do not add a Material 3 alpha override solely to work around editor IME behavior; the attempted `1.5.0-alpha26` override did not eliminate keyboard rebuilding on the tested physical device and has been removed.

## Local verification

```bash
./scripts/verify.sh
```

The script uses the checked-in Gradle Wrapper. Equivalent tasks:

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

Do not substitute an arbitrary system Gradle installation for the project wrapper when recording verification evidence.

## CI

`.github/workflows/android.yml` checks out source, installs Java, configures Gradle caching, then executes the same checked-in wrapper for static audit, JVM tests, lint and APK builds.

GitHub Actions runners may currently fail to start because the account's Actions usage limit is exhausted. A run that contains no executed workflow steps is an infrastructure/account limitation; it is neither a successful build nor evidence of a compiler/test failure.

When runner capacity becomes available, rerun the workflow from the current branch head and retain the resulting APK/test/lint artifacts. Until then, local wrapper output and documented physical-device QA are the relevant verification evidence.

## Device verification

The `main` baseline has been exercised on physical hardware by the project owner. Interaction changes that affect IME focus, bottom-sheet drag/tap behavior or insets still require a focused device rerun before merge. Record exact device model, Android version and commit in `DEVICE_QA_REPORT.md` when available.

## NixOS note

Generic Android SDK binaries may require an FHS-compatible runner. Use the repository's documented Nix/FHS setup or compatible host tooling; do not mix incompatible `adb` binaries during device testing.
