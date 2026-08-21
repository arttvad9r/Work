# Build and CI

## Supported toolchain

Pinned project baseline as of 20 August 2026:

| Component | Version |
| --- | --- |
| JDK | 17 |
| Android Gradle Plugin | 9.3.1 |
| Gradle | 9.5.0 |
| compileSdk / targetSdk | 37 |
| Kotlin Compose plugin | 2.4.10 |
| KSP | 2.3.10 |
| Compose BOM | 2026.08.00 |
| Room | 2.8.4 |
| DataStore | 1.2.1 |

AGP 9 uses built-in Kotlin support. The app intentionally does not apply `org.jetbrains.kotlin.android`.

## Static verification

No Android SDK is required:

```bash
python3 scripts/static_audit.py
```

## Full local verification

Requirements:

- JDK 17;
- Android SDK Platform 37;
- a working Android SDK accepted by Gradle/AGP;
- the committed Gradle Wrapper (the project does not require a system Gradle installation).

Linux/macOS:

```bash
./scripts/verify.sh
```

Windows PowerShell:

```powershell
./scripts/verify.ps1
```

Equivalent Gradle command:

```bash
./gradlew --no-daemon \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  --stacktrace
```

## Gradle Wrapper status

The Gradle Wrapper is committed and pins Gradle 9.5.0. Use `./gradlew` on Linux/macOS and `gradlew.bat` on Windows. The repository verification scripts already use the wrapper.

If the wrapper distribution cache is missing, the first run downloads the pinned Gradle distribution. Do not replace the wrapper with an unpinned system Gradle for repository verification.

## Room schemas

KSP is configured with:

```text
room.schemaLocation = app/schemas
```

The Room v1 schema JSON is committed under `app/schemas`. Future schema-version changes require a migration and migration test before merge.

## CI

`.github/workflows/android.yml` runs on `main`, `feat/**`, `fix/**` and pull requests. It:

1. installs JDK 17;
2. installs Gradle 9.5.0;
3. runs the static audit;
4. runs JVM unit tests;
5. runs Android lint;
6. assembles the debug APK;
7. compiles the instrumentation APK;
8. uploads verification reports;
9. uploads `app-debug.apk` when successful.

The workflow **compiles** Android instrumentation tests but does not execute them on an emulator. Device/emulator execution is tracked separately in `ANDROID_QA.md`.

## Build evidence policy

Evidence recorded on 21 August 2026: JVM tests, lint, debug APK and instrumentation APK assembly passed. Room and Compose instrumentation passed on API 26/API 35; API 37 Compose smoke is blocked by an AndroidX Test/Espresso reflection incompatibility.
