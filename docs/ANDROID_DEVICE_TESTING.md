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
- API 35 as the stable modern baseline.

API 37 is an experimental/known-issue target because the current Compose instrumentation stack fails in Espresso before the assertion.

The project flake provides Android SDK tooling but intentionally does not include large system images. Install required images into a writable SDK location, for example:

```bash
sdkmanager --sdk_root="$HOME/.android/sdk" \
  "system-images;android-26;google_apis;x86_64" \
  "system-images;android-35;google_apis;x86_64"
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

The task runs the Room instrumentation test and the Compose startup smoke test. Device/API and the complete Gradle output should be recorded for every QA run.

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
| 35 | Required | Stable modern Android baseline |
| 37 | Experimental / known issue | Target SDK baseline; current AndroidX Test/Espresso stack fails with `InputManager.getInstance` reflection error |

TalkBack requires an emulator/device image with the service available and enabled. Font-scale, rotation, dark-mode, persistence and core create/edit/delete flows should be recorded separately from automated test results.
