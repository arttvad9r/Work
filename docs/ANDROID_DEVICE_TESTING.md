# Android Device Testing

## 1. Prepare the environment

Enter the pinned development environment:

```bash
nix develop
```

Check the toolchain and ADB connection:

```bash
java -version
adb version
adb devices
```

The project target SDK is 37 and the minimum supported SDK is 26.

## 2. Start an emulator

The minimum supported test matrix is:

- API 26 for the `minSdk` boundary;
- API 35 as the stable modern baseline;
- API 37 for target-platform compatibility, including adaptive window behavior.

A historical API 37 blocker came from Espresso's reflective `InputManager.getInstance` access. WorkTime explicitly pins Espresso 3.7.0, whose stable release replaces that reflection path, so the obsolete failure must not be used as a waiver for target-platform testing.

The project flake provides Android SDK tooling but intentionally does not include large system images. Install required images into a writable SDK location, for example:

```bash
sdkmanager --sdk_root="$HOME/.android/sdk" \
  "system-images;android-26;google_apis;x86_64" \
  "system-images;android-35;google_apis;x86_64" \
  "system-images;android-37;google_apis;x86_64"
```

Create an AVD with `avdmanager` and start it with the emulator. A headless NixOS example is:

```bash
steam-run "$HOME/.android/sdk/emulator/emulator" @WorkTimeApi35QA \
  -no-window -no-audio -no-boot-anim \
  -gpu swiftshader_indirect -no-snapshot
```

Confirm that the device is online:

```bash
./scripts/adb.sh devices
```

## 3. Run instrumentation tests

With an online emulator:

```bash
./gradlew connectedDebugAndroidTest --stacktrace
```

The task runs the repository Android instrumentation suite. Device/API and the complete Gradle output should be recorded for every QA run.

Normal GitHub CI additionally runs:

```bash
./gradlew :app:pixel2Api30DebugAndroidTest \
  -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect

./gradlew :app:pixel6Api37DebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.worktime.app.ui.WorkTimeSmokeTest \
  -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

The API 30 device carries the full instrumentation suite. The API 37 managed device is a narrower target-platform Compose startup/UI smoke and does not replace physical-device adaptive QA.

## 4. NixOS-specific behavior

Generic Android SDK binaries are dynamically linked for conventional Linux distributions. On NixOS, a downloaded SDK `adb` or emulator may fail with the Nix dynamic-linker message. Use the FHS compatibility runner supplied by the development shell:

```bash
steam-run adb devices
steam-run "$ANDROID_HOME/emulator/emulator" @WorkTimeApi35QA
```

The repository wrapper is:

```bash
./scripts/adb.sh devices
```

It prefers `steam-run` when available and falls back to the already-compatible `adb` on systems where the wrapper is not installed.

The Android build also uses the existing FHS-compatible `aapt2` override from the flake; no application or SDK configuration changes are required for device QA.

## 5. Supported test matrix

| API | Status | Purpose/notes |
|---:|---|---|
| 26 | Required | `minSdk` boundary and Room/instrumentation smoke coverage |
| 35 | Required | Stable modern Android baseline and manual interaction QA |
| 37 | Required | Target SDK startup/runtime compatibility and adaptive window behavior; automated smoke plus physical/resizable-window evidence |

TalkBack requires an emulator/device image with the service available and enabled. Font-scale, rotation/window resize, dark-mode, persistence and core create/edit/delete flows should be recorded separately from automated test results.
