# Build and CI

## Toolchain

- Java 17
- Gradle Wrapper 9.5.0
- Android Gradle Plugin 9.3.1
- Kotlin 2.4.10
- compile/target SDK 37

## Local verification

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

## CI

`.github/workflows/android.yml` is configured to check out source, install Java/Gradle, run static audit, JVM tests, lint, APK builds and upload reports/artifacts.

The latest observed pull-request run for the compact-interface branch completed in a few seconds with a job containing no steps and no downloadable logs. This means the runner never started the configured workflow body. It must be treated as an Actions infrastructure/account/runner failure, not as a successful build and not as a compiler failure.

When CI becomes available, rerun it from the current branch head and retain the resulting APK/test/lint artifacts.

## NixOS note

Generic Android SDK binaries may require an FHS-compatible runner. Use the repository's documented Nix/FHS setup or compatible host tooling; do not mix incompatible `adb` binaries during device testing.
